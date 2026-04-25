package com.setka.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.setka.data.db.dao.*
import com.setka.data.model.*

@Database(
    entities = [
        User::class,
        Contact::class,
        Chat::class,
        Message::class,
        ChatMember::class,
        MessageRoute::class,
        UserStats::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SetkaDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun contactDao(): ContactDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun chatMemberDao(): ChatMemberDao
    abstract fun statsDao(): StatsDao

    companion object {
        private const val DB_NAME = "setka.db"

        @Volatile
        private var INSTANCE: SetkaDatabase? = null

        fun getInstance(context: Context): SetkaDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    SetkaDatabase::class.java,
                    DB_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
