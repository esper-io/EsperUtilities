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

    // Sample Json
    /**
     * {
     *   "api_key": "mr915DuuSsM26U5hTrsxMwcNWqcf3G",
     *   "external_root_path": "/esperfiles/",
     *   "internal_root_path": "/storage/emulated/0/esperfiles/",
     *   "app_name": "Files",
     *   "add_storage": false,
     *   "ftp_allowed": false,
     *   "rename_allowed": false,
     *   "upload_content": false,
     *   "archive_allowed": false,
     *   "sharing_allowed": false,
     *   "creation_allowed": false,
     *   "cut_copy_allowed": false,
     *   "deletion_allowed": false,
     *   "on_demand_download": false,
     *   "show_device_details": false,
     *   "show_screenshots_folder": false,
     *   "network_tester_visibility": false,
     *   "convert_files_to_app_store": false,
     *   "esper_app_store_visibility": false,
     *   "convert_files_to_network_tester": false,
     *   "use_custom_tenant_for_network_tester": false
     * }
     */

    // 2nd Sample Json
    /**
     * {
     *   "api_key": "mr915DuuSsM26U5hTrsxMwcNWqcf3G",
     *   "external_root_path": "/esperfiles/",
     *   "internal_root_path": "/storage/emulated/0/esperfiles/",
     *   "app_name": "Files",
     *   "add_storage": true,
     *   "ftp_allowed": true,
     *   "rename_allowed": true,
     *   "upload_content": true,
     *   "archive_allowed": true,
     *   "sharing_allowed": true,
     *   "creation_allowed": true,
     *   "cut_copy_allowed": true,
     *   "deletion_allowed": true,
     *   "on_demand_download": true,
     *   "show_device_details": true,
     *   "show_screenshots_folder": true,
     *   "network_tester_visibility": true,
     *   "convert_files_to_app_store": false,
     *   "esper_app_store_visibility": true,
     *   "convert_files_to_network_tester": false,
     *   "use_custom_tenant_for_network_tester": false
     * }
     */

    // Time-based guard to prevent continuous restarts
    private const val RESTART_DELAY_MS = 5000L
    private var isReceiverRegistered = false

    @JvmStatic
    fun getManagedConfigValues(
        context: Context, wasForceRefresh: Boolean = false, triggeredFromService: Boolean = false
    ) {
        Log.i(Constants.ManagedConfigUtilsTag, "Getting Managed Config Values")
        val sharedPrefManaged = context.applicationContext.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val restrictionsBundle =
            userManager.getApplicationRestrictions(context.packageName) ?: Bundle()

        applyManagedConfig(
            context, restrictionsBundle, sharedPrefManaged, wasForceRefresh, triggeredFromService
        )

        if (!triggeredFromService && !isReceiverRegistered) {
            startManagedConfigValuesReceiver(context, sharedPrefManaged)
        }
    }

    private fun startManagedConfigValuesReceiver(
        context: Context, sharedPrefManaged: SharedPreferences
    ) {
        val restrictionsMgr =
            context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        val restrictionsFilter = IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)

        val restrictionsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(
                    Constants.ManagedConfigUtilsTag,
                    "Managed Config Values Changed (Broadcast Received)"
                )
                val appRestrictions = restrictionsMgr.applicationRestrictions ?: Bundle()
                applyManagedConfig(
                    context, appRestrictions, sharedPrefManaged, wasForceRefresh = false
                )
            }
        }
        context.registerReceiver(restrictionsReceiver, restrictionsFilter)
        isReceiverRegistered = true
    }

    private fun applyManagedConfig(
        context: Context,
        appRestrictions: Bundle,
        sharedPrefManaged: SharedPreferences,
        wasForceRefresh: Boolean,
        triggeredFromService: Boolean = false
    ) {
        var requiresRestart = false
        var requiresRebirth = false

        val configList = getConfigurations(context, sharedPrefManaged, triggeredFromService)

        for (config in configList) {
            val changeOccurred = config.apply(appRestrictions, sharedPrefManaged)
            if (changeOccurred) {
                if (config.requiresRebirth) {
                    requiresRebirth = true
                } else {
                    requiresRestart = true
                }
            }
        }

        // Persist lastRestartTime in SharedPreferences
        val currentTime = System.currentTimeMillis()
        val lastRestartTime = sharedPrefManaged.getLong("lastRestartTime", 0L)

        if (requiresRebirth && !triggeredFromService) {
            if (currentTime - lastRestartTime > RESTART_DELAY_MS) {
                sharedPrefManaged.edit().putLong("lastRestartTime", currentTime).apply()
                Log.i(Constants.ManagedConfigUtilsTag, "Configurations changed, triggering rebirth")
                GeneralUtils.triggerRebirth(context)
            } else {
                Log.w(Constants.ManagedConfigUtilsTag, "Rebirth prevented to avoid loop")
            }
        } else if (requiresRestart) {
            if (currentTime - lastRestartTime > RESTART_DELAY_MS) {
                sharedPrefManaged.edit().putLong("lastRestartTime", currentTime).apply()
                Log.i(Constants.ManagedConfigUtilsTag, "Configurations changed, restarting app")
                GeneralUtils.restart(context)
            } else {
                Log.w(Constants.ManagedConfigUtilsTag, "Restart prevented to avoid loop")
            }
        }
    }

    private fun getConfigurations(
        context: Context, sharedPrefManaged: SharedPreferences, triggeredFromService: Boolean
    ): List<ManagedConfig> {
        return listOf(
            // String Configurations
            StringConfig(
                key = Constants.SHARED_MANAGED_CONFIG_APP_NAME,
                defaultValue = context.getString(R.string.app_name),
                requiresRebirth = false
            ),
            StringConfig(
                key = Constants.SHARED_MANAGED_CONFIG_API_KEY,
                defaultValue = null,
                requiresRebirth = false,
                onChange = { newValue ->
                    val editor = sharedPrefManaged.edit()
                    if (newValue.isNullOrEmpty()) {
                        editor.remove(Constants.SHARED_MANAGED_CONFIG_API_KEY)
                        Log.i(Constants.ManagedConfigUtilsTag, "API Key removed")
                    } else {
                        editor.putString(Constants.SHARED_MANAGED_CONFIG_API_KEY, newValue)
                        Log.i(Constants.ManagedConfigUtilsTag, "API Key updated")
                    }
                    editor.apply()
                    // Initialize SDK with the new API key
                    GeneralUtils.initSDK(sharedPrefManaged, context, triggeredFromService)
                }
            ),
            StringConfig(
                key = Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ROOT_PATH,
                defaultValue = null,
                requiresRebirth = true,
                onChange = { newValue ->
                    val editor = sharedPrefManaged.edit()
                    if (newValue.isNullOrEmpty()) {
                        editor.remove(Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ROOT_PATH)
                        GeneralUtils.removeExternalStoragePath(sharedPrefManaged)
                        Log.i(Constants.ManagedConfigUtilsTag, "External Root Path Removed")
                    } else {
                        editor.putString(
                            Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ROOT_PATH, newValue
                        )
                        GeneralUtils.setExternalStoragePath(sharedPrefManaged, newValue)
                        Log.i(
                            Constants.ManagedConfigUtilsTag,
                            "External Root Path Changed to: $newValue"
                        )
                    }
                    editor.apply()
                }
            ),
            StringConfig(
                key = Constants.SHARED_MANAGED_CONFIG_INTERNAL_ROOT_PATH,
                defaultValue = null,
                requiresRebirth = true,
                onChange = { newValue ->
                    if (newValue.isNullOrEmpty()) {
                        GeneralUtils.removeInternalStoragePath(context)
                        Log.i(Constants.ManagedConfigUtilsTag, "Internal Root Path removed")
                    } else {
                        GeneralUtils.setInternalStoragePath(context, newValue)
                        Log.i(
                            Constants.ManagedConfigUtilsTag,
                            "Internal Root Path updated to: $newValue"
                        )
                    }
                }
            ),
            // Boolean Configurations
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS,
                defaultValue = false,
                requiresRebirth = false,
                onChange = { _ ->
                    FileUtils.startScreenShotMove(context)
                }
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_DELETION_ALLOWED,
                defaultValue = false,
                requiresRebirth = false
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_ARCHIVE_ALLOWED,
                defaultValue = false,
                requiresRebirth = false
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_RENAME_ALLOWED,
                defaultValue = false,
                requiresRebirth = false
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_CUT_COPY_ALLOWED,
                defaultValue = false,
                requiresRebirth = false
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_ON_DEMAND_DOWNLOAD,
                defaultValue = false,
                requiresRebirth = true
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_UPLOAD_CONTENT,
                defaultValue = false,
                requiresRebirth = false
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_SHARING_ALLOWED,
                defaultValue = false,
                requiresRebirth = false
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_CREATION_ALLOWED,
                defaultValue = false,
                requiresRebirth = false
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ADD_STORAGE,
                defaultValue = false,
                requiresRebirth = true
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_EXTERNAL_FTP_ALLOWED,
                defaultValue = false,
                requiresRebirth = true
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_SHOW_DEVICE_DETAILS,
                defaultValue = false,
                requiresRebirth = true
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_ESPER_APP_STORE_VISIBILITY,
                defaultValue = false,
                requiresRebirth = true
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_CONVERT_FILES_TO_APP_STORE,
                defaultValue = false,
                requiresRebirth = true
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_NETWORK_TESTER_VISIBILITY,
                defaultValue = false,
                requiresRebirth = true
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_CONVERT_FILES_TO_NETWORK_TESTER,
                defaultValue = false,
                requiresRebirth = true
            ),
            BooleanConfig(
                key = Constants.SHARED_MANAGED_CONFIG_USE_CUSTOM_TENANT_FOR_NETWORK_TESTER,
                defaultValue = false,
                requiresRebirth = true
            )
            // Add any additional configurations here
        )
    }

    // Abstract base class for managed configurations
    sealed class ManagedConfig {
        abstract val key: String
        abstract val requiresRebirth: Boolean
        abstract fun apply(appRestrictions: Bundle, sharedPrefManaged: SharedPreferences): Boolean
    }

    // Configuration class for Boolean values
    data class BooleanConfig(
        override val key: String,
        val defaultValue: Boolean,
        override val requiresRebirth: Boolean,
        val onChange: ((Boolean) -> Unit)? = null
    ) : ManagedConfig() {
        override fun apply(appRestrictions: Bundle, sharedPrefManaged: SharedPreferences): Boolean {
            val newValue = appRestrictions.getBoolean(key, defaultValue)
            val currentValue = sharedPrefManaged.getBoolean(key, defaultValue)
            Log.i(
                Constants.ManagedConfigUtilsTag,
                "Applying $key: newValue=$newValue, currentValue=$currentValue"
            )
            if (newValue != currentValue) {
                sharedPrefManaged.edit().putBoolean(key, newValue).commit()
                Log.i(Constants.ManagedConfigUtilsTag, "$key updated to: $newValue")
                onChange?.invoke(newValue)
                return true
            }
            return false
        }
    }

    // Configuration class for String values
    data class StringConfig(
        override val key: String,
        val defaultValue: String?,
        override val requiresRebirth: Boolean,
        val onChange: ((String?) -> Unit)? = null
    ) : ManagedConfig() {
        override fun apply(appRestrictions: Bundle, sharedPrefManaged: SharedPreferences): Boolean {
            val newValue = appRestrictions.getString(key, defaultValue)
            val currentValue = sharedPrefManaged.getString(key, defaultValue)
            Log.i(
                Constants.ManagedConfigUtilsTag,
                "Applying $key: newValue='$newValue', currentValue='$currentValue'"
            )
            if (newValue != currentValue) {
                if (newValue == null) {
                    sharedPrefManaged.edit().remove(key).commit()
                } else {
                    sharedPrefManaged.edit().putString(key, newValue).commit()
                }
                Log.i(Constants.ManagedConfigUtilsTag, "$key updated to: $newValue")
                onChange?.invoke(newValue)
                return true
            }
            return false
        }
    }
}
