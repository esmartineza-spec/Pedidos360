package pedidos360.ms_orders.messaging.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PedidoKafkaPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(PedidoKafkaPublisher.class);

    private final KafkaTemplate<String, PedidoKafkaEvent> kafkaTemplate;
    private final String topic;

    public PedidoKafkaPublisher(
            KafkaTemplate<String, PedidoKafkaEvent> kafkaTemplate,
            @Value("${kafka.topic.pedidos}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publicar(PedidoKafkaEvent evento) {
        String key = evento.orderId().toString();

        kafkaTemplate.send(topic, key, evento)
                .whenComplete((resultado, error) -> {
                    if (error != null) {
                        log.error(
                                "No se pudo publicar el evento Kafka del pedido {}",
                                evento.orderId(),
                                error
                        );
                        return;
                    }

                    var metadata = resultado.getRecordMetadata();
                    log.info(
                            "Evento Kafka publicado: topic={}, pedido={}, partición={}, offset={}",
                            metadata.topic(),
                            evento.orderId(),
                            metadata.partition(),
                            metadata.offset()
                    );
                });
    }
}
