package io.esper.android.files.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.RestrictionsManager
import android.content.SharedPreferences
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import io.esper.android.files.R

object ManagedConfigUtils {

//    Managed Config Example Values
//    {
//        //(default: Files)
//        "app_name": "Company Name",
//        //(default: false)
//        "show_screenshots_folder": true,
//        //(default: false)
//        "deletion_allowed": true,
//        //(default: false)
//        "on_demand_download": true,
//        //(default: null)
//        "api_key": "dummy",
//        //(default: false)
//        "upload_content": true,
//        //(default: InternalRootFolder (/storage/emulated/0/esperfiles)
//        "internal_root_path": "/storage/emulated/0/Download",
//        //(default: ExternalRootFolder (/storage/SD-CARD/esperfiles/)
//        "external_root_path": "Download/",
//        //(default: false)
//        "sharing_allowed": true
//        //(default: false)
//        "creation_allowed": true
//        //(default: false)
//        "add_storage": true
//        //(default: false)
//        "ftp_allowed": true
//        //(default: false)
//        "show_device_details": true
//        //(default: false)
//        "esper_app_store_visibility": true
//        //(default: false)
//        "convert_files_to_app_store": true
//    }

    private fun startManagedConfigValuesReceiver(
        context: Context, sharedPrefManaged: SharedPreferences
    ) {
        val myRestrictionsMgr =
            context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        val restrictionsFilter = IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)

        val restrictionsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(
                    Constants.ManagedConfigUtilsTag,
                    "Getting Managed Config Values (Receiver Triggered)"
                )
                val appRestrictions = myRestrictionsMgr.applicationRestrictions
                mainFunction(appRestrictions, context, sharedPrefManaged, false)
            }
        }
        context.registerReceiver(restrictionsReceiver, restrictionsFilter)
    }

    private fun mainFunction(
        appRestrictions: Bundle,
        context: Context,
        sharedPrefManaged: SharedPreferences,
        wasIsItAForceRefresh: Boolean
    ) {

        val appNameChange = appNameManagedConfig(context, appRestrictions, sharedPrefManaged)
        val internalRootPathChange =
            internalRootPathManagedConfig(context, appRestrictions, sharedPrefManaged)
        val externalRootPathChange =
            externalRootPathManagedConfig(context, appRestrictions, sharedPrefManaged)
        val showScreenshotsFolderChange =
            showScreenshotsFolderManagedConfig(context, appRestrictions, sharedPrefManaged)
        val deletionAllowedChange =
            deletionAllowedManagedConfig(context, appRestrictions, sharedPrefManaged)
        val onDemandDownloadChange =
            onDemandDownloadManagedConfig(context, appRestrictions, sharedPrefManaged)
        val esperAppStoreVisibility =
            esperAppStoreVisibilityManagedConfig(context, appRestrictions, sharedPrefManaged)
        val apiKeyChange = apiKeyManagedConfig(context, appRestrictions, sharedPrefManaged)
        val uploadContentChange =
            uploadContentManagedConfig(context, appRestrictions, sharedPrefManaged)
        val shareAllowedChange =
            shareAllowedManagedConfig(context, appRestrictions, sharedPrefManaged)
        val creationAllowedChange =
            creationAllowedManagedConfig(context, appRestrictions, sharedPrefManaged)
        val addStorageChange = addStorageManagedConfig(context, appRestrictions, sharedPrefManaged)
        val ftpAllowedChange = ftpAllowedManagedConfig(context, appRestrictions, sharedPrefManaged)
        val showDeviceDetails =
            showDeviceDetailsManagedConfig(context, appRestrictions, sharedPrefManaged)
        val convertFilesToAppStore = convertFilesToAppStoreManagedConfig(
            context, appRestrictions, sharedPrefManaged
        )

        if (appNameChange || showScreenshotsFolderChange || deletionAllowedChange || apiKeyChange || uploadContentChange || shareAllowedChange || wasIsItAForceRefresh || creationAllowedChange) {
            Log.i(Constants.ManagedConfigUtilsTag, "Managed Config Values Changed")
            GeneralUtils.restart(context)
        }
        if (internalRootPathChange || externalRootPathChange || addStorageChange || ftpAllowedChange || onDemandDownloadChange || showDeviceDetails || esperAppStoreVisibility || convertFilesToAppStore) {
            if (!apiKeyChange) {
                Log.i(Constants.ManagedConfigUtilsTag, "Root Path Changed, Restart App")
                GeneralUtils.triggerRebirth(context)
            } else {
                Log.i(
                    Constants.ManagedConfigUtilsTag,
                    "Root Path Changed along with SDK ApiKey, so SDK will restart the "
                )
            }
        }
    }

    @JvmStatic
    fun getManagedConfigValues(
        context: Context, wasIsItAForceRefresh: Boolean = false
    ) {
        Log.i(Constants.ManagedConfigUtilsTag, "Getting Managed Config Values (Manually Triggered)")
        var restrictionsBundle: Bundle?
        val sharedPrefManaged = context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        restrictionsBundle = userManager.getApplicationRestrictions(context.packageName)
        if (restrictionsBundle == null) {
            restrictionsBundle = Bundle()
        }

        mainFunction(restrictionsBundle, context, sharedPrefManaged, wasIsItAForceRefresh)
        startManagedConfigValuesReceiver(context, sharedPrefManaged)
    }

    private fun appNameManagedConfig(
        context: Context, appRestrictions: Bundle, sharedPrefManaged: SharedPreferences
    ): Boolean {
        var result = false
        val newAppName =
            if (appRestrictions.containsKey(Constants.SHARED_MANAGED_CONFIG_APP_NAME)) appRestrictions.getString(
                Constants.SHARED_MANAGED_CONFIG_APP_NAME
            ).toString() else context.getString(R.string.app_name)
        val changeInValue = newAppName != sharedPrefManaged.getString(
            Constants.SHARED_MANAGED_CONFIG_APP_NAME, context.getString(R.string.app_name)
        )
        if (changeInValue && newAppName.isNotEmpty()) {
            sharedPrefManaged.edit().putString(Constants.SHARED_MANAGED_CONFIG_APP_NAME, newAppName)
                .apply()
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "App Name Changed")
        } else if (changeInValue && newAppName.isEmpty()) {
            sharedPrefManaged.edit().remove(Constants.SHARED_MANAGED_CONFIG_APP_NAME).apply()
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "App Name Removed")
        }
        return result
    }

    private fun internalRootPathManagedConfig(
        context: Context, appRestrictions: Bundle, sharedPrefManaged: SharedPreferences
    ): Boolean {
        var result = false
        val internalRootPath =
            if (appRestrictions.containsKey(Constants.SHARED_MANAGED_CONFIG_INTERNAL_ROOT_PATH)) appRestrictions.getString(
                Constants.SHARED_MANAGED_CONFIG_INTERNAL_ROOT_PATH
            ).toString() else Constants.InternalRootFolder
        val changeInValue =
            internalRootPath != GeneralUtils.getInternalStoragePath(sharedPrefManaged)
        if (changeInValue && internalRootPath.isEmpty()) {
            GeneralUtils.removeInternalStoragePath(sharedPrefManaged)
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "Internal Root Path Removed")
        } else if (changeInValue) {
            GeneralUtils.setInternalStoragePath(sharedPrefManaged, internalRootPath)
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "Internal Root Path Changed: $internalRootPath")
        }
        return result
    }

    private fun externalRootPathManagedConfig(
        context: Context, appRestrictions: Bundle, sharedPrefManaged: SharedPreferences
    ): Boolean {
        var result = false
        val externalRootPath =
            if (appRestrictions.containsKey(Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ROOT_PATH)) appRestrictions.getString(
                Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ROOT_PATH
            ).toString() else Constants.ExternalRootFolder
        val changeInValue = externalRootPath != sharedPrefManaged.getString(
            Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ROOT_PATH, Constants.ExternalRootFolder
        )
        if (changeInValue && externalRootPath.isEmpty()) {
            GeneralUtils.removeExternalStoragePath(sharedPrefManaged)
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "External Root Path Removed")
        } else if (changeInValue) {
            GeneralUtils.setExternalStoragePath(sharedPrefManaged, externalRootPath)
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "External Root Path Changed")
        }
        return result
    }

    private fun showScreenshotsFolderManagedConfig(
        context: Context, appRestrictions: Bundle, sharedPrefManaged: SharedPreferences
    ): Boolean {
        var result = false
        val showScreenshotsFolder =
            if (appRestrictions.containsKey(Constants.SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS)) appRestrictions.getBoolean(
                Constants.SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS
            ) else false
        val changeInValue = showScreenshotsFolder != sharedPrefManaged.getBoolean(
            Constants.SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS, false
        )
        if (changeInValue) {
            sharedPrefManaged.edit()
                .putBoolean(Constants.SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS, showScreenshotsFolder)
                .apply()
            FileUtils.startScreenShotMove(context)
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "Show Screenshots Folder Changed")
        }
        return result
    }

    private fun deletionAllowedManagedConfig(
        context: Context, appRestrictions: Bundle, sharedPrefManaged: SharedPreferences
    ): Boolean {
        var result = false
        val deletionAllowed =
            if (appRestrictions.containsKey(Constants.SHARED_MANAGED_CONFIG_DELETION_ALLOWED)) appRestrictions.getBoolean(
                Constants.SHARED_MANAGED_CONFIG_DELETION_ALLOWED
            ) else false
        val changeInValue = deletionAllowed != sharedPrefManaged.getBoolean(
            Constants.SHARED_MANAGED_CONFIG_DELETION_ALLOWED, false
        )
        if (changeInValue) {
            sharedPrefManaged.edit()
                .putBoolean(Constants.SHARED_MANAGED_CONFIG_DELETION_ALLOWED, deletionAllowed)
                .apply()
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "Deletion Allowed Changed")
        }
        return result
    }

    private fun onDemandDownloadManagedConfig(
        context: Context, appRestrictions: Bundle, sharedPrefManaged: SharedPreferences
    ): Boolean {
        var result = false
        val onDemandDownload =
            if (appRestrictions.containsKey(Constants.SHARED_MANAGED_CONFIG_ON_DEMAND_DOWNLOAD)) appRestrictions.getBoolean(
                Constants.SHARED_MANAGED_CONFIG_ON_DEMAND_DOWNLOAD
            ) else false
        val changeInValue = onDemandDownload != sharedPrefManaged.getBoolean(
            Constants.SHARED_MANAGED_CONFIG_ON_DEMAND_DOWNLOAD, false
        )
        if (changeInValue) {
            sharedPrefManaged.edit()
                .putBoolean(Constants.SHARED_MANAGED_CONFIG_ON_DEMAND_DOWNLOAD, onDemandDownload)
                .apply()
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "On Demand Download Changed")
        }
        return result
    }

    private fun esperAppStoreVisibilityManagedConfig(
        context: Context, appRestrictions: Bundle, sharedPrefManaged: SharedPreferences
    ): Boolean {
        var result = false
        val onDemandDownload =
            if (appRestrictions.containsKey(Constants.SHARED_MANAGED_CONFIG_ESPER_APP_STORE_VISIBILITY)) appRestrictions.getBoolean(
                Constants.SHARED_MANAGED_CONFIG_ESPER_APP_STORE_VISIBILITY
            ) else false
        val changeInValue = onDemandDownload != sharedPrefManaged.getBoolean(
            Constants.SHARED_MANAGED_CONFIG_ESPER_APP_STORE_VISIBILITY, false
        )
        if (changeInValue) {
            sharedPrefManaged.edit().putBoolean(
                Constants.SHARED_MANAGED_CONFIG_ESPER_APP_STORE_VISIBILITY, onDemandDownload
            ).apply()
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "Esper App Store Visibility Changed")
        }
        return result
    }

    private fun apiKeyManagedConfig(
        context: Context, appRestrictions: Bundle, sharedPrefManaged: SharedPreferences
    ): Boolean {
        var result = false
        val apiKey =
            if (appRestrictions.containsKey(Constants.SHARED_MANAGED_CONFIG_API_KEY)) appRestrictions.getString(
                Constants.SHARED_MANAGED_CONFIG_API_KEY
            ) else null

        val changeInValue =
            apiKey != sharedPrefManaged.getString(Constants.SHARED_MANAGED_CONFIG_API_KEY, null)
        if (changeInValue && apiKey.isNullOrEmpty()) {
            sharedPrefManaged.edit().remove(Constants.SHARED_MANAGED_CONFIG_API_KEY).apply()
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "API Key Removed")
        } else if (changeInValue) {
            sharedPrefManaged.edit().putString(Constants.SHARED_MANAGED_CONFIG_API_KEY, apiKey)
                .apply()
            result = true
            GeneralUtils.initSDK(sharedPrefManaged, context)
            Log.i(Constants.ManagedConfigUtilsTag, "API Key Changed")
        }
        return result
    }

    private fun uploadContentManagedConfig(
        context: Context, appRestrictions: Bundle, sharedPrefManaged: SharedPreferences
    ): Boolean {
        var result = false
        val uploadContent =
            if (appRestrictions.containsKey(Constants.SHARED_MANAGED_CONFIG_UPLOAD_CONTENT)) appRestrictions.getBoolean(
                Constants.SHARED_MANAGED_CONFIG_UPLOAD_CONTENT
            ) else false
        val changeInValue = uploadContent != sharedPrefManaged.getBoolean(
            Constants.SHARED_MANAGED_CONFIG_UPLOAD_CONTENT, false
        )
        if (changeInValue) {
            sharedPrefManaged.edit()
                .putBoolean(Constants.SHARED_MANAGED_CONFIG_UPLOAD_CONTENT, uploadContent).apply()
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "Upload Content Changed")
        }
        return result
    }

    private fun shareAllowedManagedConfig(
        context: Context, appRestrictions: Bundle, sharedPrefManaged: SharedPreferences
    ): Boolean {
        var result = false
        val shareAllowed =
            if (appRestrictions.containsKey(Constants.SHARED_MANAGED_CONFIG_SHARING_ALLOWED)) appRestrictions.getBoolean(
                Constants.SHARED_MANAGED_CONFIG_SHARING_ALLOWED
            ) else false
        val changeInValue = shareAllowed != sharedPrefManaged.getBoolean(
            Constants.SHARED_MANAGED_CONFIG_SHARING_ALLOWED, false
        )
        if (changeInValue) {
            sharedPrefManaged.edit()
                .putBoolean(Constants.SHARED_MANAGED_CONFIG_SHARING_ALLOWED, shareAllowed).apply()
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "Share Allowed Changed")
        }
        return result
    }

    private fun creationAllowedManagedConfig(
        context: Context, appRestrictions: Bundle, sharedPrefManaged: SharedPreferences
    ): Boolean {
        var result = false
        val creationAllowed =
            if (appRestrictions.containsKey(Constants.SHARED_MANAGED_CONFIG_CREATION_ALLOWED)) appRestrictions.getBoolean(
                Constants.SHARED_MANAGED_CONFIG_CREATION_ALLOWED
            ) else false
        val changeInValue = creationAllowed != sharedPrefManaged.getBoolean(
            Constants.SHARED_MANAGED_CONFIG_CREATION_ALLOWED, false
        )
        if (changeInValue) {
            sharedPrefManaged.edit()
                .putBoolean(Constants.SHARED_MANAGED_CONFIG_CREATION_ALLOWED, creationAllowed)
                .apply()
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "Creation Allowed Changed")
        }
        return result
    }

    private fun addStorageManagedConfig(
        context: Context, appRestrictions: Bundle, sharedPrefManaged: SharedPreferences
    ): Boolean {
        var result = false
        val shareAllowed =
            if (appRestrictions.containsKey(Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ADD_STORAGE)) appRestrictions.getBoolean(
                Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ADD_STORAGE
            ) else false
        val changeInValue = shareAllowed != sharedPrefManaged.getBoolean(
            Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ADD_STORAGE, false
        )
        if (changeInValue) {
            sharedPrefManaged.edit()
                .putBoolean(Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ADD_STORAGE, shareAllowed)
                .apply()
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "Add Storage Changed")
        }
        return result
    }

    private fun ftpAllowedManagedConfig(
        context: Context, appRestrictions: Bundle, sharedPrefManaged: SharedPreferences
    ): Boolean {
        var result = false
        val shareAllowed =
            if (appRestrictions.containsKey(Constants.SHARED_MANAGED_CONFIG_EXTERNAL_FTP_ALLOWED)) appRestrictions.getBoolean(
                Constants.SHARED_MANAGED_CONFIG_EXTERNAL_FTP_ALLOWED
            ) else false
        val changeInValue = shareAllowed != sharedPrefManaged.getBoolean(
            Constants.SHARED_MANAGED_CONFIG_EXTERNAL_FTP_ALLOWED, false
        )
        if (changeInValue) {
            sharedPrefManaged.edit()
                .putBoolean(Constants.SHARED_MANAGED_CONFIG_EXTERNAL_FTP_ALLOWED, shareAllowed)
                .apply()
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "FTP Allowed Changed")
        }
        return result
    }

    private fun showDeviceDetailsManagedConfig(
        context: Context, appRestrictions: Bundle, sharedPrefManaged: SharedPreferences
    ): Boolean {
        var result = false
        val showDeviceDetails =
            if (appRestrictions.containsKey(Constants.SHARED_MANAGED_CONFIG_SHOW_DEVICE_DETAILS)) appRestrictions.getBoolean(
                Constants.SHARED_MANAGED_CONFIG_SHOW_DEVICE_DETAILS
            ) else false
        val changeInValue = showDeviceDetails != sharedPrefManaged.getBoolean(
            Constants.SHARED_MANAGED_CONFIG_SHOW_DEVICE_DETAILS, false
        )
        if (changeInValue) {
            sharedPrefManaged.edit()
                .putBoolean(Constants.SHARED_MANAGED_CONFIG_SHOW_DEVICE_DETAILS, showDeviceDetails)
                .apply()
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "Show Device Details Allowed Changed")
        }
        return result
    }


    private fun convertFilesToAppStoreManagedConfig(
        context: Context, appRestrictions: Bundle, sharedPrefManaged: SharedPreferences
    ): Boolean {
        var result = false
        val convertFilesToAppStore =
            if (appRestrictions.containsKey(Constants.SHARED_MANAGED_CONFIG_CONVERT_FILES_TO_APP_STORE)) appRestrictions.getBoolean(
                Constants.SHARED_MANAGED_CONFIG_CONVERT_FILES_TO_APP_STORE
            ) else false
        val changeInValue = convertFilesToAppStore != sharedPrefManaged.getBoolean(
            Constants.SHARED_MANAGED_CONFIG_CONVERT_FILES_TO_APP_STORE,
            false
        )
        if (changeInValue) {
            sharedPrefManaged.edit().putBoolean(
                Constants.SHARED_MANAGED_CONFIG_CONVERT_FILES_TO_APP_STORE, convertFilesToAppStore
            ).apply()
            result = true
            Log.i(Constants.ManagedConfigUtilsTag, "Convert Files To App Store Changed")
        }
        return result
    }
}
