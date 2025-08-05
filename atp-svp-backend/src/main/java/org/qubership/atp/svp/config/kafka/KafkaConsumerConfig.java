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

package org.qubership.atp.svp.config.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.qubership.atp.svp.model.api.kafka.ProjectEvent;
import org.qubership.atp.svp.model.kafka.LogCollectorKafkaMessage;
import org.qubership.atp.svp.model.kafka.SvpKafkaMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListenerConfigurer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;


@EnableKafka
@Configuration
public class KafkaConsumerConfig implements KafkaListenerConfigurer {

    private final LocalValidatorFactoryBean validator;

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaServer;

    @Value("${spring.kafka.consumer.group-id}")
    private String kafkaGroupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetResetConfig;

    private static final String TOOL_NAME = "atp-svp";

    @Autowired
    public KafkaConsumerConfig(LocalValidatorFactoryBean validator) {
        this.validator = validator;
    }

    @Override
    public void configureKafkaListeners(KafkaListenerEndpointRegistrar registrar) {
        registrar.setValidator(this.validator);
    }

    /**
     * Bean for creating ConcurrentKafkaListenerContainerFactory, used for init Kafka listeners.
     *
     * @return {@link ConcurrentKafkaListenerContainerFactory} type of key {@link String},
     *         type of value {@link String}.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    stringStringConcurrentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(concurrentKafkaListenerContainerFactory(String.class));
        return factory;
    }

    /**
     * Bean for creating ConcurrentKafkaListenerContainerFactory by LogCollector, used for init Kafka listeners.
     *
     * @return {@link ConcurrentKafkaListenerContainerFactory} type of key {@link String},
     *         type of value {@link LogCollectorKafkaMessage}.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LogCollectorKafkaMessage>
    stringLogCollectorEventConcurrentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, LogCollectorKafkaMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(concurrentKafkaListenerContainerFactory(LogCollectorKafkaMessage.class));
        factory.setRecordFilterStrategy(consumerRecord -> Objects.isNull(consumerRecord.value())
                || !TOOL_NAME.equals(consumerRecord.value().getRequestTool()));
        return factory;
    }

    /** Bean for creating ConcurrentKafkaListenerContainerFactory by Project, used for init Kafka listeners.
     *
     * @return {@link ConcurrentKafkaListenerContainerFactory} type of key {@link String},
     *          type of value {@link ProjectEvent}.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProjectEvent>
    stringProjectEventConcurrentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ProjectEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(concurrentKafkaListenerContainerFactory(ProjectEvent.class));
        return factory;
    }

    /**
     * Bean for creating ConcurrentKafkaListenerContainerFactory, used for init Kafka listeners.
     *
     * @return {@link ConcurrentKafkaListenerContainerFactory} type of key {@link UUID},
     *         type of value {@link SvpKafkaMessage}.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, SvpKafkaMessage>
    stringSvpKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, SvpKafkaMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    private ConsumerFactory consumerFactory() {
        return new DefaultKafkaConsumerFactory(consumerConfigs(), new UUIDDeserializer(),
                generateJsonDeserializer(SvpKafkaMessage.class, false, true));
    }

    private Properties consumerConfigs() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaServerAddress());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetResetConfig);
        return props;
    }

    @Bean
    public String getKafkaServerAddress() {
        return this.kafkaServer;
    }

    private <T> ConsumerFactory<String, T> concurrentKafkaListenerContainerFactory(Class<T> clazz) {
        Map<String, Object> configProps = buildConfigProps();
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetResetConfig);
        return buildKafkaConsumerFactory(configProps, clazz);
    }

    private Map<String, Object> buildConfigProps() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaServerAddress());
        return configProps;
    }

    private <T> DefaultKafkaConsumerFactory<String, T> buildKafkaConsumerFactory(
            Map<String, Object> properties, Class<T> clazz) {
        StringDeserializer keyDeserializer = new StringDeserializer();
        Deserializer<?> valueDeserializer;
        if ("String".equals(clazz.getSimpleName())) {
            valueDeserializer = new StringDeserializer();
        } else {
            valueDeserializer = generateJsonDeserializer(clazz, false, true);
        }
        return new DefaultKafkaConsumerFactory(properties, keyDeserializer, valueDeserializer);
    }

    private <T> JsonDeserializer generateJsonDeserializer(Class<T> clazz, boolean removeTypeHeaders,
                                                          boolean useTypeMapperForKey) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonDeserializer deserializer = new JsonDeserializer(clazz, objectMapper);
        deserializer.addTrustedPackages("*");
        deserializer.setRemoveTypeHeaders(removeTypeHeaders);
        deserializer.setUseTypeMapperForKey(useTypeMapperForKey);
        return deserializer;
    }
}
