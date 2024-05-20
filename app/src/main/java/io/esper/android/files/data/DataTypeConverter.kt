@file:Suppress("FunctionName")

package io.esper.appstore.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.esper.appstore.model.AppData
import java.lang.reflect.Type
import java.util.*


class DataTypeConverter {
    private val gson = Gson()

    @TypeConverter
    fun StringToMutableList(data: String?): MutableList<AppData> {
        if (data == null) {
            return Collections.emptyList()
        }
        val listType: Type = object : TypeToken<MutableList<AppData?>?>() {}.type
        return gson.fromJson(data, listType)
    }

    @TypeConverter
    fun MutableListToString(someObjects: MutableList<AppData?>?): String {
        return gson.toJson(someObjects)
    }
}

