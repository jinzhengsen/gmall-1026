package com.atguigu.gmall.pms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import javax.annotation.PostConstruct;

@Configuration
@Slf4j
public class RabbitConfig {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init(){
        //确认消息是否到达交换机
        this.rabbitTemplate.setConfirmCallback((@Nullable CorrelationData correlationData, boolean ack, @Nullable String cause)->{
            if (ack){
                System.out.println("消息已成功到达交换机。。。。");
            }else {
                System.out.println("消息没有到达交换机.."+cause);
            }
        });
        //确认消息是否到达队列
        this.rabbitTemplate.setReturnCallback((Message message, int replyCode, String replyText, String exchange, String routingKey)->{
            log.warn("消息没有到达队列，来自于交换机:{},路由键:{},消息内容：{}",exchange,routingKey,new java.lang.String(message.getBody()));
        });

    }
}
