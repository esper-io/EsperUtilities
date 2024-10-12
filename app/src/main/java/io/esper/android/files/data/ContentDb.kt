package io.esper.android.files.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.esper.android.files.model.AllContent

@Database(entities = [AllContent::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class ContentDb : RoomDatabase() {

    abstract fun contentDao(): ContentDao

    companion object {
        @Volatile
        private var INSTANCE: ContentDb? = null

        fun getDatabase(context: Context): ContentDb {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): ContentDb {
            return Room.databaseBuilder(
                context.applicationContext, ContentDb::class.java, "ContentDb"
            ).build()
        }
    }
}
