package com.reactor.lib.kafkareactor

import org.apache.kafka.common.TopicPartition
import reactor.kafka.receiver.ReceiverOffset
import java.util.concurrent.ConcurrentHashMap

class TopicPartitionTracker {
    private val committedOffsetMap = ConcurrentHashMap<TopicPartition, Long>()
    fun registerTopicPartition(topicPartitions: Collection<TopicPartition>) {
        topicPartitions.forEach {
            committedOffsetMap[it] = -1L
        }
    }

    fun unregisterTopicPartition(topicPartitions: Collection<TopicPartition>) {
        topicPartitions.forEach {
            committedOffsetMap.remove(it)
        }
    }

    private fun isAssignedPartition(receiveRecordTopicPartition: TopicPartition) =
            committedOffsetMap.keys.contains(receiveRecordTopicPartition)

    private fun isNewRecord(receiverOffset: ReceiverOffset): Boolean =
            receiverOffset.offset() > committedOffsetMap.getOrDefault(receiverOffset.topicPartition(), -1)

    fun isEligible(receiverOffset: ReceiverOffset): Boolean {
        return isAssignedPartition(receiverOffset.topicPartition()) &&
                isNewRecord(receiverOffset)
    }

    fun acknowledge(receiverOffset: ReceiverOffset) {
        if (isEligible(receiverOffset)) {
            receiverOffset.acknowledge()
            committedOffsetMap[receiverOffset.topicPartition()] = receiverOffset.offset()
        }
    }
}