# Pedidos360

Sistema de pedidos trabajado como proyecto semestral de Cloud Native.

## Estructura

- **pedidos360-backend**: microservicios Spring Boot, persistencia en Oracle Autonomous Database y mensajería RabbitMQ. Sus requisitos, configuración y pasos para iniciarlo están en [su README](pedidos360-backend/README.md).
- **pedidos360-frontend**: aplicación Angular. Para desarrollo local se ejecuta con npm desde esa carpeta; consulta también el README del backend para la configuración de autenticación y API.
- **pedidos360-backend/ms-bodega**: base inicial con Spring Kafka; todavía no tiene productores, consumidores ni Kafka Streams y no forma parte del Docker Compose integrado.
- **pedidos360-backend/api-gateway-local**: gateway local independiente del Compose principal.

## Inicio local

1. Configura Docker Desktop, Oracle y Microsoft Entra ID siguiendo el README del backend.
2. Copia pedidos360-backend/.env.example como pedidos360-backend/.env y completa los datos de tu entorno.
3. Desde pedidos360-backend, compila los servicios e inicia Docker Compose según las instrucciones de su README.
4. En otra terminal, desde pedidos360-frontend, ejecuta npm ci y luego npm start.

## Datos sensibles

El archivo .env y la Wallet de Oracle se configuran localmente y no se suben al repositorio. No agregues contraseñas de cuentas de prueba a archivos versionados.