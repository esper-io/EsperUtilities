package io.esper.android.files.data

import androidx.room.Database
import androidx.room.RoomDatabase
import io.esper.android.files.model.AllContent

@Database(entities = [(AllContent::class)], version = 1, exportSchema = false)
abstract class ContentDb : RoomDatabase() {
    abstract fun contentDao(): ContentDao
}