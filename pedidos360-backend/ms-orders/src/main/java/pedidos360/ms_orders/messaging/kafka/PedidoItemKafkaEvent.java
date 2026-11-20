package pedidos360.ms_orders.messaging.kafka;

public record PedidoItemKafkaEvent(
        Long productId,
        Integer cantidad
) {
}
