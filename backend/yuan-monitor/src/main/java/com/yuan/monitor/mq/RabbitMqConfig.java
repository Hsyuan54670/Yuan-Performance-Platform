package com.yuan.monitor.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.yuan.api.mq.MqConstants.TEST_METRIC_QUEUE;
import static com.yuan.api.mq.MqConstants.TEST_METRIC_ROUTING_KEY;
import static com.yuan.api.mq.MqConstants.TEST_STATUS_QUEUE;
import static com.yuan.api.mq.MqConstants.TEST_STATUS_ROUTING_KEY;
import static com.yuan.api.mq.MqConstants.YUAN_TEST_EXCHANGE;

@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange testExchange() {
        return new TopicExchange(YUAN_TEST_EXCHANGE);
    }

    @Bean
    public Queue testStatusQueue() {
        return new Queue(TEST_STATUS_QUEUE, true);
    }

    @Bean
    public Binding testStatusBinding() {
        return BindingBuilder.bind(testStatusQueue()).to(testExchange()).with(TEST_STATUS_ROUTING_KEY);
    }

    @Bean
    public Queue testMetricQueue() {
        return new Queue(TEST_METRIC_QUEUE, true);
    }

    @Bean
    public Binding testMetricBinding() {
        return BindingBuilder.bind(testMetricQueue()).to(testExchange()).with(TEST_METRIC_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,Jackson2JsonMessageConverter converter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);
        return rabbitTemplate;
    }

}
