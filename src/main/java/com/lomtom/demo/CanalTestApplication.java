package com.lomtom.demo;

import com.lomtom.demo.kafka.KafkaDemo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

/**
 * @author lomtom
 */
@SpringBootApplication
public class CanalTestApplication {

    @Autowired
    KafkaDemo kafkaDemo;

    @PostConstruct
    public void test(){
        kafkaDemo.send("lomtom");
    }

    public static void main(String[] args) {
        SpringApplication.run(CanalTestApplication.class,args);
    }
}
