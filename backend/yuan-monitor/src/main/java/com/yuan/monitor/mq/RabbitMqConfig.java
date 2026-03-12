package com.yuan.monitor.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.yuan.api.mq.MqConstants.*;

@Configuration
public class RabbitMqConfig {
    @Bean
    public TopicExchange testExchange(){
        return new TopicExchange(TEST_MONITOR_EXCHANGE);
    }
    @Bean
    public Queue testStatusQueue(){
        return new Queue(TEST_STATUS_QUEUE, true);
    }
    @Bean
    public Binding testStatusBinding(){
        return BindingBuilder.bind(testStatusQueue()).to(testExchange()).with(TEST_STATUS_ROUTING_KEY);
    }
    @Bean
    public Queue testMetricQueue(){
        return new Queue(TEST_METRIC_QUEUE, true);
    }
    @Bean
    public Binding testMetricBinding(){
        return BindingBuilder.bind(testMetricQueue()).to(testExchange()).with(TEST_METRIC_ROUTING_KEY);
    }
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}
