package com.example.demo.longevity;

import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.reactive.config.annotation.ReactivePulsarListener;
import org.springframework.pulsar.reactive.core.ReactiveMessageConsumerBuilderCustomizer;
import org.springframework.pulsar.reactive.core.ReactivePulsarTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootApplication
@EnableScheduling
@RestController
public class ReactiveSpringPulsarLongevityDemoApplication {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String topic;
    private final ReactivePulsarTemplate<String> pulsarTemplate;
    private long sendCounter = 0L;

    public ReactiveSpringPulsarLongevityDemoApplication(@Value("${demo-topic-name}") String topic,
                                                        ReactivePulsarTemplate<String> pulsarTemplate) {
        this.topic = topic;
        this.pulsarTemplate = pulsarTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(ReactiveSpringPulsarLongevityDemoApplication.class, args);
    }

    @GetMapping("/")
    String hello() {
        return "Hello";
    }

    @Scheduled(initialDelay = 10_000, fixedDelay = 1_000)
    void sendMessage() {
        String msg =  "Hello_" + sendCounter;
        pulsarTemplate.send(topic, msg).subscribe();
        if (sendCounter++ % 5 == 0) {
            this.logger.info("*** Reactive template sent {}", msg);
        }
        if (sendCounter % 10 == 0) {
            try {
                Thread.sleep(15_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @ReactivePulsarListener(subscriptionName = "sp-gae-reactive-demo-subscription",
            topics = "${demo-topic-name}", consumerCustomizer = "subscriptionInitialPositionEarliest")
    Mono<Void> receiveMessage(String msg) {
        long count = Long.parseLong(msg.substring("Hello_".length()));
        if (count % 5 == 0) {
            this.logger.info("*** Reactive listener received {}", msg);
        }
        return Mono.empty();
    }

    @Configuration(proxyBeanMethods = false)
    static class ConsumerCustomizerConfig {

        @Bean
        ReactiveMessageConsumerBuilderCustomizer<String> subscriptionInitialPositionEarliest() {
            return b -> b.subscriptionInitialPosition(SubscriptionInitialPosition.Earliest);
        }

    }
}
