package com.reactor.lib.kafkareactor

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.kafka.test.EmbeddedKafkaBroker
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

class ListenerTest {
    companion object {
        private const val topic = "test-topic"
        private const val partitions = 10
        private lateinit var sender: KafkaSender<String, String>
        private lateinit var service: Service
        private val kafkaBroker: EmbeddedKafkaBroker = EmbeddedKafkaBroker(1, false, partitions, topic)
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            kafkaBroker.afterPropertiesSet()
            val senderConfigs = mapOf<String, Any>(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBroker.brokersAsString,
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java
            )
            sender = KafkaSender.create(SenderOptions.create<String, String>(senderConfigs))

            service = Service()
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            sender.close()
            kafkaBroker.destroy()
        }
    }


    @Test
    fun `kafka is running`() {
        val ints = Array(10000) { it }
        val events = Flux.fromArray(ints)
                .map { ProducerRecord(topic, it % partitions, it.toString(), it.toString()) }

        sender.createOutbound()
                .send(events)
                .then().block()
        val consumerConfig = ListenerConfig.defaultConsumerConfig(kafkaBroker.brokersAsString, "", "test-consumer")
                .plus(arrayOf(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java))
        val listener1 = Listener.listen<String, String>(consumerConfig, listOf(topic), javaClass) { record ->
            service.doSomeStuff("listener1", record.value())
        }
        Thread.sleep(2000)
        val listener2 = Listener.listen<String, String>(consumerConfig, listOf(topic), javaClass) { record ->
            service.doSomeStuff("listener2", record.value())
        }
        Thread.sleep(2000)
        val listener3 = Listener.listen<String, String>(consumerConfig, listOf(topic), javaClass) { record ->
            service.doSomeStuff("listener3", record.value())
        }
        Thread.sleep(30000)
        listener1.dispose()
        listener2.dispose()
        listener3.dispose()
        ints.forEach {
            Assertions.assertEquals(1, service.map.getOrDefault(it.toString(), emptyList()).size,
                    "Invalid list for $it ${service.map.getOrDefault(it.toString(), emptyList())}")
        }
        println("verification complete ${LocalDateTime.now()}")
    }

    class Service {
        val map: ConcurrentHashMap<String, Collection<String>> = ConcurrentHashMap()
        fun doSomeStuff(consumer: String, value: String): Mono<Unit> {
            val collection = map.getOrDefault(value, emptyList()).plus(consumer)
            map[value] = collection
            return Mono.just(1)
                    .map {
                        Thread.sleep(5)
                        Unit
                    }
        }
    }
}