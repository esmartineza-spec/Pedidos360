# Pedidos360 - Backend

Backend del sistema Pedidos360. Está formado por microservicios Spring Boot, usa Oracle Autonomous Database para los datos y RabbitMQ para procesar eventos de pedidos de forma asíncrona.

## Componentes

| Componente | Puerto | Responsabilidad |
|---|---:|---|
| ms-bff | 8080 dentro de Docker; http://localhost en el equipo | Punto de entrada para Angular. Valida JWT y deriva solicitudes a los servicios. |
| ms-orders | 8081, interno | Crea y consulta pedidos, actualiza sus estados y publica eventos en RabbitMQ. |
| ms-catalog | 8082, interno | Consulta productos y administra precios y stock. |
| ms-notificaciones | 8083 dentro de Docker; http://localhost:8083 solo en el equipo | Consume eventos, registra notificaciones y tickets simulados, y ofrece la API administrativa de RabbitMQ. |
| rabbitmq | 5672, interno; consola en http://localhost:15672 solo en el equipo | Broker de mensajería y consola para observar colas, exchanges, bindings y mensajes. |

El flujo normal es Angular → ms-bff → ms-orders o ms-catalog. ms-orders publica eventos en RabbitMQ y ms-notificaciones los procesa. Los mensajes de correo, push y ticket son simulaciones registradas en los logs; no se envían a servicios reales.

El módulo api-gateway-local es independiente y no se inicia con el Docker Compose descrito aquí. Para el flujo local integrado, Angular se comunica con ms-bff.

## Requisitos

- Docker Desktop iniciado con el motor Linux y Docker Compose.
- Node.js y npm compatibles con las versiones fijadas en el package-lock.json del frontend.
- Acceso a Oracle Autonomous Database, credenciales de conexión y la Wallet de Oracle.
- Microsoft Entra ID configurado para la aplicación, necesario para iniciar sesión desde Angular.

## Preparación local

### Variables de entorno

El archivo .env debe estar en esta carpeta, junto a docker-compose.yml. Parte copiando .env.example como .env y completa tus valores locales. Debe definir DB_URL, DB_USERNAME, DB_PASSWORD, ORACLE_WALLET_PATH, RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USERNAME, RABBITMQ_PASSWORD y RABBITMQ_VHOST.

.env contiene credenciales: no lo compartas ni lo subas al repositorio. El .gitignore excluye ese archivo. El archivo .env.example solo contiene valores ficticios para orientar la configuración.

Dentro de Docker Compose, RabbitMQ se alcanza con el host rabbitmq y el puerto 5672. La consola de RabbitMQ usa las credenciales definidas en el .env.

### Wallet de Oracle

Docker Compose lee la ruta local de la Wallet desde ORACLE_WALLET_PATH y la monta en modo solo lectura como /wallet dentro de ms-catalog y ms-orders. Configura en .env una ruta absoluta de tu equipo (en Windows puedes usar barras /), por ejemplo C:/ruta/local/Wallet_PEDIDOS360. La carpeta debe contener los archivos entregados por Oracle, incluido tnsnames.ora. Mantén la Wallet fuera del repositorio.

La conexión Oracle usa DB_URL, DB_USERNAME y DB_PASSWORD. Hibernate valida el esquema existente; no crea las tablas automáticamente.

## Inicio local

### 1. Backend y RabbitMQ

Los Dockerfile copian un JAR ya compilado desde la carpeta target de cada servicio. En PowerShell, desde esta carpeta, genera primero los cuatro JAR que usa Compose:

    .\ms-bff\mvnw.cmd -f .\ms-bff\pom.xml clean package -DskipTests
    .\ms-orders\mvnw.cmd -f .\ms-orders\pom.xml clean package -DskipTests
    .\ms-catalog\mvnw.cmd -f .\ms-catalog\pom.xml clean package -DskipTests
    .\ms-notificaciones\mvnw.cmd -f .\ms-notificaciones\pom.xml clean package -DskipTests

Luego inicia Docker Compose:

    docker compose up --build -d
    docker compose ps

Espera a que los servicios estén activos. Para revisar sus logs:

    docker compose logs -f ms-bff ms-orders ms-catalog ms-notificaciones rabbitmq

El BFF queda disponible en http://localhost. La consola de RabbitMQ está en http://localhost:15672; inicia sesión con el usuario y la contraseña definidos en el .env.

### 2. Frontend Angular

Abre otra terminal:

    cd ..\pedidos360-frontend
    npm ci
    npm start

Abre http://localhost:4200. Al usar npm start, Angular toma src/environments/environment.ts y llama al BFF en http://localhost. La dirección de retorno http://localhost:4200 debe estar registrada en Microsoft Entra ID.

environment.prod.ts apunta a API Gateway en AWS y corresponde al entorno desplegado. Para la revisión local usa npm start.

### Detener los contenedores

Desde la carpeta del backend:

    docker compose down

## API del BFF

Las rutas protegidas requieren un token de acceso válido, obtenido por Angular mediante Microsoft Entra ID.

| Método | Ruta | Uso |
|---|---|---|
| GET | /api/catalog/productos | Listar productos. |
| POST | /api/catalog/productos | Crear producto. |
| PUT | /api/catalog/productos/{id}/stock/aumentar | Aumentar stock. |
| GET | /api/pedidos | Listar pedidos. |
| POST | /api/pedidos | Crear pedido con una lista de items. |
| PUT | /api/pedidos/{id}/estado?estado=ACEPTADO | Cambiar el estado de un pedido. |
| GET | /api/user | Consultar los atributos del token autenticado. |

La interfaz muestra las acciones según el rol: catálogo para administradores y operadores, y gestión de usuarios para administradores. El acceso al BFF requiere autenticación.

## API administrativa de RabbitMQ

En la configuración local, esta API se publica solo en el equipo local, en http://localhost:8083. Puede probarse con Postman.

| Método | Ruta | Ejemplo de cuerpo o parámetros |
|---|---|---|
| POST | /api/admin/rabbitmq/queues | {"name":"nombre.cola"} |
| DELETE | /api/admin/rabbitmq/queues/{name} | - |
| POST | /api/admin/rabbitmq/exchanges | {"name":"nombre.exchange","type":"DIRECT"}; también acepta TOPIC |
| DELETE | /api/admin/rabbitmq/exchanges/{name} | - |
| POST | /api/admin/rabbitmq/bindings | {"queue":"nombre.cola","exchange":"nombre.exchange","routingKey":"clave"} |
| DELETE | /api/admin/rabbitmq/bindings?queue=nombre.cola&exchange=nombre.exchange&routingKey=clave | - |

La API valida nombres y routing keys. Las operaciones de creación responden 201 Created y las eliminaciones 204 No Content.

## Mensajería RabbitMQ

ms-orders publica eventos en exchanges direct y topic. ms-notificaciones agrupa sus consumidores por función:

- Correo simulado: pedidos.notify.email.q.
- Push simulado: pedidos.notify.push.q.
- Ticket de cocina simulado: pedidos.notify.ticket.q.

Cada flujo tiene una cola de reintento con espera configurada y una DLQ para mensajes que no se pueden procesar. Los consumidores confirman explícitamente los mensajes procesados y registran los errores en logs. La consola de RabbitMQ permite observar el estado y las tasas de las colas. Obtener mensajes desde la consola puede retirarlos de la cola, según el modo de confirmación elegido.

## Estructura

    pedidos360-backend/
    ├── api-gateway-local/
    ├── ms-bff/
    ├── ms-catalog/
    ├── ms-notificaciones/
    ├── ms-orders/
    ├── docker-compose.yml
    └── README.md

Cada microservicio Java tiene su propio pom.xml y Maven Wrapper. La ejecución integrada recomendada es Docker Compose, que construye los servicios desde sus Dockerfiles.

