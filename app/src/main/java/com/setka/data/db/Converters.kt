package com.setka.data.db

import androidx.room.TypeConverter
import com.setka.data.model.*

class Converters {

    @TypeConverter fun fromMessageType(v: MessageType) = v.name
    @TypeConverter fun toMessageType(v: String) = MessageType.valueOf(v)

    @TypeConverter fun fromMessageStatus(v: MessageStatus) = v.name
    @TypeConverter fun toMessageStatus(v: String) = MessageStatus.valueOf(v)

    @TypeConverter fun fromTransportType(v: TransportType) = v.name
    @TypeConverter fun toTransportType(v: String) = TransportType.valueOf(v)

    @TypeConverter fun fromChatType(v: ChatType) = v.name
    @TypeConverter fun toChatType(v: String) = ChatType.valueOf(v)

    @TypeConverter fun fromMemberRole(v: MemberRole) = v.name
    @TypeConverter fun toMemberRole(v: String) = MemberRole.valueOf(v)
}
