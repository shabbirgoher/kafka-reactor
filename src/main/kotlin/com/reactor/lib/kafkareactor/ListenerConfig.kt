package com.reactor.lib.kafkareactor

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig

object ListenerConfig {
    fun defaultConsumerConfig(
            bootstrapServers: String,
            schemaRegistryUrl: String,
            consumerGroup: String
    ): Map<String, Any> =
            mapOf(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                    "schema.registry.url" to schemaRegistryUrl,
                    ConsumerConfig.GROUP_ID_CONFIG to consumerGroup,
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                    KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true
            )
}