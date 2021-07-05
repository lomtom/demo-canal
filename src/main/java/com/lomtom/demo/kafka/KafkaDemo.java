package com.lomtom.demo.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author lomtom
 * @date 2021/7/5 13:57
 **/
@Component
public class KafkaDemo {
    @Autowired
    private KafkaTemplate kafkaTemplate;

    public void send(String name){
        System.out.println("生产。。。。。。。");
        kafkaTemplate.send("user", name);
    }

    @KafkaListener(topics = "user")
    public void consumer(ConsumerRecord consumerRecord){
        System.out.println("消费。。。。。。。");
        Optional<Object> kafkaMassage = Optional.ofNullable(consumerRecord.value());
        if(kafkaMassage.isPresent()){
            Object o = kafkaMassage.get();
            System.out.println(o);
        }
    }
}