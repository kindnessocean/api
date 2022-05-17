package org.dapamoha.api.config.producer;


import org.apache.kafka.clients.producer.Producer;
import org.dapamoha.shared.domain.util.StringUtil;
import org.dapamoha.shared.kafka.model.emailRequest.EmailRequestKey;
import org.dapamoha.shared.kafka.model.emailRequest.EmailRequestValue;
import org.dapamoha.shared.kafka.producer.KafkaProducerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailRequestKafkaProducerConfig {

    private final Long TRANSACTIONAL_ID_LENGTH = 10L;
    @Value("${org.dapamoha.shared.config.mq.bootstrapServer}")
    private String bootstrapServer;

    @Bean
    public Producer<EmailRequestKey, EmailRequestValue> createEntityEventProducer() {
        return new KafkaProducerBuilder<EmailRequestKey, EmailRequestValue>()
                .buildKafkaProducer(bootstrapServer, StringUtil.generateString(TRANSACTIONAL_ID_LENGTH));
    }
}
