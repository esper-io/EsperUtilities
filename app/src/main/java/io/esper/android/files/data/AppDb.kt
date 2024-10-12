package io.esper.android.files.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.esper.appstore.data.ApplicationDao
import io.esper.appstore.model.AllApps
import io.esper.appstore.model.AppData
import io.esper.appstore.model.AppData1

@Database(
        entities = [(AllApps::class), (AppData::class), (AppData1::class)],
        version = 1,
        exportSchema = false
)

@TypeConverters(DataTypeConverter::class)
abstract class AppDb : RoomDatabase() {

    abstract fun appDao(): ApplicationDao
}