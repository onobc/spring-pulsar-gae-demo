package com.example.demo.longevity;

import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableScheduling
@RestController
public class SpringPulsarLongevityDemoApplication {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String topic;
    private final PulsarTemplate<String> pulsarTemplate;
    private long sendCounter = 0L;

    public SpringPulsarLongevityDemoApplication(@Value("${demo-topic-name}") String topic, PulsarTemplate<String> pulsarTemplate) {
        this.topic = topic;
        this.pulsarTemplate = pulsarTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringPulsarLongevityDemoApplication.class, args);
    }

    @GetMapping("/hello/{name:unknown}")
    public String hello(@PathVariable String name) throws PulsarClientException {
        String msg =  "Hello " + name;
        pulsarTemplate.send(topic, msg);
        this.logger.info("*** Sent " + msg);
        return msg;
    }

    @Scheduled(initialDelay = 10_000, fixedDelay = 1_000)
    public void sendMessage() throws PulsarClientException {
        String msg =  "Hello_" + sendCounter;
        pulsarTemplate.send(topic, msg);
        if (sendCounter++ % 5 == 0) {
            this.logger.info("*** Sent {}", msg);
        }
        if (sendCounter % 10 == 0) {
            try {
                Thread.sleep(15_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @PulsarListener(subscriptionName = "sp-gae-demo-subscription", topics = "${demo-topic-name}")
    void listen1(String msg) {
        long count = Long.parseLong(msg.substring("Hello_".length()));
        if (count % 5 == 0) {
            this.logger.info("*** Received {}", msg);
        }
    }

}
