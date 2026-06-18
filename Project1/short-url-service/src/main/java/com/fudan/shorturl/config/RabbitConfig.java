package com.fudan.shorturl.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("!test")
public class RabbitConfig {

    public static final String EXCHANGE = "short-url.exchange";
    public static final String ACCESS_QUEUE = "short-url.access.queue";
    public static final String ACCESS_ROUTING_KEY = "access";

    public static final String DLX_EXCHANGE = "short-url.dlx.exchange";
    public static final String DLX_QUEUE = "short-url.access.dlq";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange shortUrlExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange shortUrlDlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue accessQueue() {
        return QueueBuilder.durable(ACCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ACCESS_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue accessDlq() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    @Bean
    public Binding accessBinding() {
        return BindingBuilder.bind(accessQueue()).to(shortUrlExchange()).with(ACCESS_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(accessDlq()).to(shortUrlDlxExchange()).with(ACCESS_ROUTING_KEY);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(converter);
        template.setMandatory(true);

        template.setConfirmCallback((correlation, ack, cause) -> {
            if (!ack) {
                log.error("RabbitMQ publisher-confirm 失败 correlation={} cause={}", correlation, cause);
            }
        });
        template.setReturnsCallback(returned -> log.error(
                "RabbitMQ publisher-return 路由失败 exchange={} routingKey={} replyText={} message={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText(), returned.getMessage()));
        return template;
    }
}
