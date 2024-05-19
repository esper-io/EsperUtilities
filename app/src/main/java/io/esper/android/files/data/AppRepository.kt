package io.esper.android.files.data

import io.esper.appstore.model.AllApps
import io.esper.appstore.model.AppData1

object AppRepository {

    // Check if AllApps have changed
    fun hasAllAppsChanged(
        currentAllApps: MutableList<AllApps>,
        newAllApps: MutableList<AllApps>
    ): Boolean {
        // Check if the size of lists has changed
        if (currentAllApps.size != newAllApps.size) {
            return true
        }

        // Check for changes in AllApps table
        for (i in currentAllApps.indices) {
            if (currentAllApps[i].package_name != newAllApps[i].package_name) {
                return true
            }
        }
        return false
    }

    // Check if AppData1 have changed
    fun hasInstalledAppsChanged(
        currentInstalledApps: MutableList<AppData1>, newInstalledApps: MutableList<AppData1>
    ): Boolean {
        // Check if the size of lists has changed
        if (currentInstalledApps.size != newInstalledApps.size) {
            return true
        }

        // Check for changes in AppData1 table
        for (i in currentInstalledApps.indices) {
            if (currentInstalledApps[i].package_name != newInstalledApps[i].package_name) {
                return true
            }
        }
        return false
    }
}
