package org.dapamoha.api.service.mq.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.dapamoha.shared.kafka.model.emailRequest.EmailRequestKey;
import org.dapamoha.shared.kafka.model.emailRequest.EmailRequestValue;
import org.dapamoha.shared.kafka.util.ProducerMqUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailRequestProducer {

    private final Producer<EmailRequestKey, EmailRequestValue> producer;

    private final ProducerMqUtil<EmailRequestKey, EmailRequestValue> producerMqUtil;
    @Value("${org.dapamoha.shared.config.mq.topic.emailRequest}")
    private String topic;

    public EmailRequestProducer(final Producer<EmailRequestKey, EmailRequestValue> emailRequestProducer) {
        this.producer = emailRequestProducer;
        producerMqUtil = new ProducerMqUtil<EmailRequestKey, EmailRequestValue>(log);
    }

    public void produceRequest(EmailRequestKey key, EmailRequestValue value) {
        producerMqUtil.produce(producer, topic, key, value);
    }
}
