package io.esper.android.files.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.esper.android.files.model.AllContent

@Dao
interface ContentDao {

    @Query("SELECT * FROM allcontent")
    fun getAllContentLive(): LiveData<List<AllContent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contentList: List<AllContent>)

    @Query("DELETE FROM allcontent")
    suspend fun deleteAll()
}
