package com.reactor.lib.kafkareactor

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import java.time.Duration

object Listener {
    fun <K, V> listen(
            consumerConfig: Map<String, Any>,
            topics: Collection<String>,
            clazz: Class<*>,
            block: (ReceiverRecord<K, V>) -> Mono<Unit>
    ): Disposable {
        val logger: Logger = LoggerFactory.getLogger("[Listener]-${clazz.name}")
        val scheduler = Schedulers.newSingle(clazz.name)
        val topicPartitionTracker = TopicPartitionTracker()
        val receiverOptions = receiverOptions<K, V>(consumerConfig, topics, logger, topicPartitionTracker)

        return KafkaReceiver.create<K, V>(receiverOptions)
                .receive()
                .publishOn(scheduler)
                .filter { topicPartitionTracker.isEligible(it.receiverOffset()) }
                .concatMap {
                    Mono.just(it)
                            .flatMap { record ->
                                block(record)
                            }
                            .thenReturn(topicPartitionTracker.acknowledge(it.receiverOffset()))
                            .retryBackoff(2, Duration.ofSeconds(1), Duration.ofSeconds(5))
                }
                .subscribe()
    }

    private fun <K, V> receiverOptions(
            consumerConfig: Map<String, Any>,
            topics: Collection<String>,
            logger: Logger,
            topicPartitionTracker: TopicPartitionTracker
    ): ReceiverOptions<K, V> =
            ReceiverOptions.create<K, V>(consumerConfig)
                    .subscription(topics)
                    .addAssignListener { partitions ->
                        logger.info("Partition assigned $partitions")
                        val topicPartitions = partitions.map { it.topicPartition() }
                        topicPartitionTracker.registerTopicPartition(topicPartitions)
                    }
                    .addRevokeListener { partitions ->
                        logger.info("Partition revoked $partitions")
                        val topicPartitions = partitions.map { it.topicPartition() }
                        topicPartitionTracker.unregisterTopicPartition(topicPartitions)
                    }
}