package com.example.demo.longevity;

import java.time.Instant;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@ConfigurationPropertiesScan("com.example.demo")
@EnableScheduling
@RestController
public class SpringPulsarLongevityDemoApplication {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PulsarTemplate<String> pulsarTemplate;
    private final DemoProperties demoProperties;
    private long sendCounter = 0L;

    public SpringPulsarLongevityDemoApplication(PulsarTemplate<String> pulsarTemplate, DemoProperties demoProperties) {
        this.pulsarTemplate = pulsarTemplate;
        this.demoProperties = demoProperties;
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringPulsarLongevityDemoApplication.class, args);
    }

    @GetMapping("/")
    String hello() {
        return "Hello @ " + Instant.now();
    }

    @Scheduled(initialDelay = 5_000, fixedDelay = 1_000)
    void sendMessage() throws PulsarClientException {
        String msg =  "Hello_" + sendCounter;
        pulsarTemplate.send(this.demoProperties.topicName(), msg);
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

    @PulsarListener(subscriptionName = "sp-gae-demo-subscription", topics = "${demo.topic-name}")
    void receiveMessage(String msg) {
        long count = Long.parseLong(msg.substring("Hello_".length()));
        if (count % 5 == 0) {
            this.logger.info("*** Received {}", msg);
        }
    }

    @ConfigurationProperties("demo")
    public record DemoProperties(String tenant, String namespace, String topicName) {
    }
}
