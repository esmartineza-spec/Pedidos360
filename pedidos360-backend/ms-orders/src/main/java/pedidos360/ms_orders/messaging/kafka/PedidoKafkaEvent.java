package pedidos360.ms_orders.messaging.kafka;


import java.time.Instant;
import java.util.List;

public record PedidoKafkaEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long orderId,
        String clienteId,
        String estado,
        List<PedidoItemKafkaEvent> items
) {
}
