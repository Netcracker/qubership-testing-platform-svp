/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is provided "AS IS", without warranties
 * or conditions of any kind, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.atp.svp.kafka;

import static org.mockito.ArgumentMatchers.any;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import org.qubership.atp.svp.model.api.kafka.ProjectEvent;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles({"IntegrationTest"})
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties", properties = {
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
public class ProjectEventKafkaListenerTest {

    private final CountDownLatch latch = new CountDownLatch(1);
    private final int delayForReceiveNotification = 5;

    @Value("${kafka.project.event.consumer.topic.name}")
    private String catalogProject;

    @SpyBean
    private ProjectEventKafkaListener projectEventKafkaListener;

    @Autowired
    public KafkaTemplate<String, String> kafkaTemplate;

    @ClassRule
    public static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));

    @Test
    public void whenWeGotCatalogProjectKafkaEvent_thenCalledMethodListenOnProjectEventKafkaListenerWithCorrectPayload() {
        UUID projectId = UUID.randomUUID();
        String message = "Sending with default template";
        //Action
        kafkaTemplate.send(catalogProject, projectId.toString(), message);
        //        latch.await(delayForReceiveNotification, TimeUnit.SECONDS);
        Mockito.verify(projectEventKafkaListener, Mockito.timeout(5000)).listen(any(ProjectEvent.class));
    }

    @TestConfiguration
    static class KafkaTestContainersConfiguration {

        @Bean
        public String getKafkaServerAddress() {
            return kafka.getBootstrapServers();
        }

        @Bean
        public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }

        @Bean
        public ProducerFactory<String, String> producerFactory() {
            Map<String, Object> configProps = new HashMap<>();
            configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
            configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            return new DefaultKafkaProducerFactory<>(configProps);
        }
    }
}
