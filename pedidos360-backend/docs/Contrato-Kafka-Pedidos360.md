# Contrato inicial de eventos Kafka de Pedidos360

Estado: borrador compartido. El nombre del topic aún es provisional.

## Topic

- Nombre propuesto: `pedidos.eventos`
- Se usa un solo topic para eventos de ciclo de vida del pedido.

## Clave del registro

- La clave Kafka es `orderId` convertido a texto.
- Así, los cambios de un mismo pedido se envían con la misma clave y Kafka los dirige a la misma partición, conservando su orden dentro de ella.

## Tipos de evento

- `PEDIDO_CREADO`: se publica después de guardar un pedido nuevo.
- `ESTADO_PEDIDO_CAMBIADO`: se publica después de guardar una transición de estado válida.

## Valor JSON

```json
{
  "eventId": "uuid-del-evento",
  "eventType": "PEDIDO_CREADO",
  "occurredAt": "2026-11-20T14:30:00Z",
  "orderId": 123,
  "clienteId": "CLIENTE-001",
  "estado": "CREADO",
  "items": [
    {
      "productId": 2,
      "cantidad": 2
    }
  ]
}
```

`occurredAt` se representa como fecha y hora ISO-8601 en UTC. Los nombres y tipos de los campos deben mantenerse equivalentes en el DTO de `ms-orders` y el DTO independiente de `ms-bodega`.

## Comportamiento del publicador

- `ms-orders` publica el evento después de guardar el pedido en Oracle.
- RabbitMQ continúa gestionando sus notificaciones; Kafka transporta estos eventos de pedido.
- El envío Kafka actual es asíncrono. Si falla, `ms-orders` escribe el error en el log; la respuesta HTTP del pedido no espera confirmación del broker.

## Pendientes antes de cerrar el contrato

- Confirmar el nombre definitivo del topic.
- Confirmar que `orderId` es la clave elegida por el equipo.
- Acordar qué acción de inventario corresponde a cada tipo de evento y qué servicio es dueño del stock.
