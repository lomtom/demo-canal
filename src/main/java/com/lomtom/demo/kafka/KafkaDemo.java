package com.lomtom.demo.kafka;

import com.github.houbb.markdown.toc.core.impl.AtxMarkdownToc;
import com.github.houbb.markdown.toc.vo.TocGen;
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

    public void send(String name) {
        System.out.println("生产。。。。。。。");
        kafkaTemplate.send("user", name);
    }

    @KafkaListener(topics = "user")
    public void consumer(ConsumerRecord consumerRecord) {
        System.out.println("消费。。。。。。。");
        Optional<Object> kafkaMassage = Optional.ofNullable(consumerRecord.value());
        if (kafkaMassage.isPresent()) {
            Object o = kafkaMassage.get();
            System.out.println(o);
        }
    }

    public static void main(String[] args) {
        /**
         * 生成md菜单
         */
        TocGen tocGen = AtxMarkdownToc.newInstance()
                .genTocFile("D:\\project\\demo\\demo-canal\\README.md");
        System.out.println(tocGen);

    }
}