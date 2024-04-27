package io.esper.android.files.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.esper.android.files.model.AllContent

@Dao
interface ContentDao {

    @Query("SELECT * from AllContent")
    fun getAllContent(): MutableList<AllContent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(apps: AllContent)

    @Query("SELECT * from AllContent where name=:name")
    fun getContentWithName(name: String): AllContent

    @Query("DELETE FROM AllContent")
    fun deleteAll()
}