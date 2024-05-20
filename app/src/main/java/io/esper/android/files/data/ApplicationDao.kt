package io.esper.appstore.data

import androidx.room.*
import io.esper.appstore.model.AllApps
import io.esper.appstore.model.AppData1
import io.esper.appstore.model.InstalledApps

@Dao
interface ApplicationDao {

    @Query("SELECT * from AllApps")
    fun getApplications(): MutableList<AllApps>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(apps: AllApps)

    @Update(entity = AppData1::class)
    fun update(obj: InstalledApps)

    @Query("UPDATE AppData1 SET version_name=:version_name WHERE package_name = :packageName")
    fun updateInstalledOnlyVersion(packageName: String, version_name: String)

    @Query("SELECT * from AppData1 where package_name=:packageName")
    fun getInstalledOnlyVersion(packageName: String): AppData1

    @Query("SELECT * from AllApps where package_name=:packageName")
    fun getIdFromRoot(packageName: String): AllApps

    @Query("SELECT * from AppData1")
    fun getInstalledApplications(): MutableList<AppData1>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertInstalled(installedApps: AppData1)

    @Query("DELETE FROM AllApps")
    fun deleteAll()

    @Query("DELETE FROM AppData1")
    fun deleteAllInstalled()

    @Query("SELECT * FROM AllApps WHERE package_name = :packageName")
    fun getAppByPackageName(packageName: String): AllApps?
}