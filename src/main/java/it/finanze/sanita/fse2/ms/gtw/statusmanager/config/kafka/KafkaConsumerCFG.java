/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtw.statusmanager.config.kafka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class KafkaConsumerCFG {

	/**
	 *	Kafka consumer properties.
	 */
	@Autowired
	private KafkaConsumerPropertiesCFG kafkaConsumerPropCFG;
	
	@Autowired
	private KafkaTopicCFG topicCFG;

	/**
	 * Configurazione consumer.
	 * 
	 * @return	configurazione consumer
	 */
	@Bean
	public Map<String, Object> consumerConfigs() {
		Map<String, Object> props = new HashMap<>();
		
		log.info("CLIENT ID:" + kafkaConsumerPropCFG.getClientId());
		props.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaConsumerPropCFG.getClientId());
		log.info("BOOTSTRAP SERVER:" + kafkaConsumerPropCFG.getConsumerBootstrapServers());
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConsumerPropCFG.getConsumerBootstrapServers());
		log.info("CONSUMER GROUP ID:" + kafkaConsumerPropCFG.getConsumerGroupId());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerPropCFG.getConsumerGroupId());
		log.info("KEY SERIALIZER:" + kafkaConsumerPropCFG.getConsumerKeyDeserializer());
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaConsumerPropCFG.getConsumerKeyDeserializer());
		log.info("VALUE SERIALIZER:" + kafkaConsumerPropCFG.getConsumerValueDeserializer());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaConsumerPropCFG.getConsumerValueDeserializer());
		log.info("ISOLATION:" + kafkaConsumerPropCFG.getIsolationLevel());
		props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, kafkaConsumerPropCFG.getIsolationLevel());
		log.info("AUTOCOMMIT:" + kafkaConsumerPropCFG.getAutoCommit());
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaConsumerPropCFG.getAutoCommit());
		log.info("AUTO OFFSET:" + kafkaConsumerPropCFG.getAutoOffsetReset());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaConsumerPropCFG.getAutoOffsetReset());
		
		//SSL
		if (kafkaConsumerPropCFG.isEnableSsl()) { 
			log.info("SECURITY PROTOCOL:" + kafkaConsumerPropCFG.getProtocol());
			props.put("security.protocol", kafkaConsumerPropCFG.getProtocol());
			log.info("MECHANISM:" + kafkaConsumerPropCFG.getMechanism());
			props.put("sasl.mechanism", kafkaConsumerPropCFG.getMechanism());
			log.info("JAAS:" + kafkaConsumerPropCFG.getConfigJaas());
			props.put("sasl.jaas.config", kafkaConsumerPropCFG.getConfigJaas());
			log.info("TRUSTORE:" + kafkaConsumerPropCFG.getTrustoreLocation());
			props.put("ssl.truststore.location", kafkaConsumerPropCFG.getTrustoreLocation());
			log.info("TRUSTORE:" + kafkaConsumerPropCFG.getTrustoreLocation());
			props.put("ssl.truststore.password", String.valueOf(kafkaConsumerPropCFG.getTrustorePassword()));
		}
		return props;
	}


	/**
	 * Consumer factory.
	 * 
	 * @return	factory
	 */
	@Bean
	public ConsumerFactory<String, String> consumerFactory() {
		return new DefaultKafkaConsumerFactory<>(consumerConfigs());
	}

	/**
	 * Factory with dead letter configuration.
	 * 
	 * @param deadLetterKafkaTemplate
	 * @return	factory
	 */
	@Bean
	public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerDeadLetterContainerFactory(final @Qualifier("notxkafkadeadtemplate") KafkaTemplate<Object, Object> deadLetterKafkaTemplate) {
		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		
		DeadLetterPublishingRecoverer dlpr = new DeadLetterPublishingRecoverer(deadLetterKafkaTemplate, (consumerRecord, ex) -> new TopicPartition(topicCFG.getStatusManagerTopicDlt(), -1));
		// Set classificazione errori da gestire per la deadLetter.
		DefaultErrorHandler sceh = new DefaultErrorHandler(dlpr, new FixedBackOff(FixedBackOff.DEFAULT_INTERVAL, FixedBackOff.UNLIMITED_ATTEMPTS));
		
		log.debug("Setting dead letter classification");
		setClassification(sceh);
		
		// da eliminare se non si volesse gestire la dead letter
		factory.setCommonErrorHandler(sceh); 

		return factory;
	}
	
	@Bean
	public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerDeadLetterContainerFactoryEds(final @Qualifier("notxkafkadeadtemplate") KafkaTemplate<Object, Object> deadLetterKafkaTemplate) {
		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		
		DeadLetterPublishingRecoverer dlpr = new DeadLetterPublishingRecoverer(deadLetterKafkaTemplate, (consumerRecord, ex) -> new TopicPartition(topicCFG.getStatusManagerEdsTopicDlt(), -1));
		// Set classificazione errori da gestire per la deadLetter.
		DefaultErrorHandler sceh = new DefaultErrorHandler(dlpr, new FixedBackOff(FixedBackOff.DEFAULT_INTERVAL, FixedBackOff.UNLIMITED_ATTEMPTS));
		
		log.debug("Setting dead letter classification");
		setClassification(sceh);
		
		// da eliminare se non si volesse gestire la dead letter
		factory.setCommonErrorHandler(sceh); 

		return factory;
	}
	
	private void setClassification(final DefaultErrorHandler sceh) {
		List<Class<? extends Exception>> out = getExceptionsConfig();

		for (Class<? extends Exception> ex : out) {
			log.warn("Found a non retryable exception: {}", ex.getCanonicalName());
			sceh.addNotRetryableExceptions(ex);
		}
		
	}

	/**
	 * @return	exceptions list
	 */
	@SuppressWarnings("unchecked")
	private List<Class<? extends Exception>> getExceptionsConfig() {
		List<Class<? extends Exception>> out = new ArrayList<>();
		String temp = null;
		try {
			for (String excs : kafkaConsumerPropCFG.getDeadLetterExceptions()) {
				temp = excs;
				Class<? extends Exception> s = (Class<? extends Exception>) Class.forName(excs, false, Thread.currentThread().getContextClassLoader());
				out.add(s);
			}
		} catch (Exception e) {
			log.error("Error retrieving the exception with fully qualified name: <{}>", temp);
			log.error("Error : ", e);
		}
		
		return out;
	}
	
	/**
	 * Default Container factory.
	 * 
	 * @param kafkaTemplate	templete
	 * @return				factory
	 */
	@Bean
	public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerContainerFactory(final @Qualifier("notxkafkatemplate") KafkaTemplate<String, String> kafkaTemplate) {
		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		
		return factory;
	}
}
