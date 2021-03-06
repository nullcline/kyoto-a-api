package com.example.apiSample.service

import com.example.apiSample.controller.BadRequestException
import com.example.apiSample.mapper.MessageMapper
import com.example.apiSample.model.EventTypes
import com.example.apiSample.model.Message
import com.example.apiSample.model.MessageForMapping
import com.example.apiSample.model.NonUidUser
import org.springframework.stereotype.Service

data class InsertMessage (
        var id: Long = 0,
        var room_id: Long,
        var user_id: Long,
        var text: String
)

@Service
class MessageService(private val messageMapper: MessageMapper, private val eventService: EventService) {
    fun getMessagesFromRoomId(roomId: Long, sinceId: Long = 0, limit: Int = 50): ArrayList<Message> {
        val messages_for_mapping = messageMapper.findByRoomId(roomId, sinceId, limit)
        val mutable_messages = mutableListOf<Message>()
        for (ms_f_m in messages_for_mapping) {
            mutable_messages.add(messageFromMessageForMapping(ms_f_m))
        }
        val messages: ArrayList<Message> = mutable_messages as ArrayList<Message>
        return messages
    }

    fun getMessageFromId(messageId: Long): Message {
        val message_for_mapping: MessageForMapping? = messageMapper.findById(messageId)
        message_for_mapping ?: throw BadRequestException("There are no message")
        val message = messageFromMessageForMapping(message_for_mapping)
        return message
    }

    fun createMessage(roomId: Long, userId: Long, text: String): Message {
        if (text.isBlank()){
            throw BadRequestException("text must not be blank")
        }
        var messageInserted = InsertMessage(
                room_id = roomId,
                user_id = userId,
                text = text
        )
        messageMapper.createMessage(messageInserted)
        eventService.createEvent(EventTypes.MESSAGE_SENT.ordinal, roomId, userId, messageInserted.id)
        eventService.createEvent(EventTypes.ROOM_UPDATED.ordinal, roomId, userId, messageInserted.id)
        val message = this.getMessageFromId(messageInserted.id)
        return message
    }

    fun updateMessage(userId: Long, messageId: Long, text: String): Message {
        if (text.isBlank()){
            throw BadRequestException("text must not be blank")
        }
        val message = this.getMessageFromId(messageId)
        if (message.user_id == userId) {
            messageMapper.updateMessage(messageId, text)
            eventService.createEvent(EventTypes.MESSAGE_UPDATED.ordinal, message.room_id, userId, messageId)
            return this.getMessageFromId(messageId)
        }
        throw BadRequestException("message creator only can update message")
    }

    fun deleteMessage(userId: Long, messageId: Long): Boolean {
        val message: Message = this.getMessageFromId(messageId)
        if (message.user_id == userId) {
            eventService.createEvent(EventTypes.MESSAGE_DELETED.ordinal, message.room_id, userId, messageId)
            return messageMapper.deleteMessage(messageId)
        }
        throw BadRequestException("message creator only can delete message")
    }

    fun messageFromMessageForMapping(message_for_mapping: MessageForMapping): Message {
        return Message(
                id = message_for_mapping.message_id,
                room_id = message_for_mapping.room_id,
                user_id = message_for_mapping.user_id,
                user_name = message_for_mapping.user_name,
                text = message_for_mapping.text,
                createdAt = message_for_mapping.message_created_at,
                updatedAt = message_for_mapping.message_updated_at
        )
    }
}
