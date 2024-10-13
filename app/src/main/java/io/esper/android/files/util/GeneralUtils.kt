package io.esper.android.files.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.esper.android.files.BuildConfig
import io.esper.android.files.R
import io.esper.android.files.app.application
import io.esper.android.files.util.Constants.GeneralUtilsTag
import io.esper.devicesdk.EsperDeviceSDK
import io.esper.devicesdk.models.EsperDeviceInfo
import io.esper.devicesdk.models.ProvisionInfo
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GeneralUtils {
    fun hideKeyboard(context: Context) {
        val imm =
            context.activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = context.activity?.currentFocus ?: View(context.activity)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun setFadeAnimation(view: View, duration: Long = 300L) {
        val anim = AlphaAnimation(0.0f, 1.0f)
        anim.duration = duration
        view.startAnimation(anim)
    }

    fun hasActiveInternetConnection(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun calculateNoOfColumns(context: Context, columnWidthDp: Float): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        return (screenWidthDp / columnWidthDp + 0.5).toInt()
    }

    fun getCurrentDateTime(pattern: String = "ddMMyyHHmm"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
    }

    private fun startEsperSDKActivation(
        context: Context, sharedPrefManaged: SharedPreferences, triggeredFromService: Boolean
    ) {
        val token = sharedPrefManaged.getString(
            Constants.SHARED_MANAGED_CONFIG_API_KEY, null
        )
        val existingDeviceName = getDeviceName(context)
        if (!token.isNullOrEmpty() && existingDeviceName.isNullOrEmpty()) {
            val sdk = getEsperSDK(context)
            sdk.activateSDK(token, object : EsperDeviceSDK.Callback<Void?> {
                override fun onResponse(response: Void?) {
                    getDeviceDetails(sdk, sharedPrefManaged)
                    getProvisioningInfo(context, sdk, sharedPrefManaged, triggeredFromService)
                }

                override fun onFailure(t: Throwable) {
                    Log.e(GeneralUtilsTag, "SDK activation failed: ${t.message}", t)
                }
            })
        }
    }

    fun isAddingStorageAllowed(context: Context): Boolean {
        return getBooleanPreference(
            context, Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ADD_STORAGE, false
        )
    }

    fun isDlcAllowed(context: Context): Boolean {
        return getBooleanPreference(
            context, Constants.SHARED_MANAGED_CONFIG_ON_DEMAND_DOWNLOAD, false
        )
    }

    fun isEsperAppStoreVisible(context: Context): Boolean {
        return getBooleanPreference(
            context, Constants.SHARED_MANAGED_CONFIG_ESPER_APP_STORE_VISIBILITY, false
        )
    }

    fun isFtpServerAllowed(context: Context): Boolean {
        return getBooleanPreference(
            context, Constants.SHARED_MANAGED_CONFIG_EXTERNAL_FTP_ALLOWED, false
        )
    }

    fun isNetworkTesterVisible(context: Context): Boolean {
        return getBooleanPreference(
            context, Constants.SHARED_MANAGED_CONFIG_NETWORK_TESTER_VISIBILITY, false
        )
    }

    fun showDeviceDetails(): Boolean {
        return getBooleanPreference(
            application, Constants.SHARED_MANAGED_CONFIG_SHOW_DEVICE_DETAILS, false
        )
    }

    private fun getProvisioningInfo(
        context: Context,
        sdk: EsperDeviceSDK,
        sharedPrefManaged: SharedPreferences,
        triggeredFromService: Boolean
    ) {
        sdk.getProvisionInfo(object : EsperDeviceSDK.Callback<ProvisionInfo> {
            override fun onResponse(response: ProvisionInfo?) {
                response?.let {
                    sharedPrefManaged.edit().apply {
                        putString(Constants.SHARED_MANAGED_CONFIG_TENANT, it.apiEndpoint)
                        putString(Constants.SHARED_MANAGED_CONFIG_ENTERPRISE_ID, it.tenantUUID)
                        apply()
                    }
                }
                if (!triggeredFromService) {
                    triggerRebirth(context)
                }
            }

            override fun onFailure(t: Throwable) {
                Log.e(GeneralUtilsTag, "Failed to get provisioning info: ${t.message}", t)
            }
        })
    }

    private fun getDeviceDetails(sdk: EsperDeviceSDK, sharedPrefManaged: SharedPreferences) {
        sdk.getEsperDeviceInfo(object : EsperDeviceSDK.Callback<EsperDeviceInfo> {
            override fun onFailure(t: Throwable) {
                Log.e(GeneralUtilsTag, "Failed to get device info: ${t.message}", t)
            }

            override fun onResponse(esperDeviceInfo: EsperDeviceInfo?) {
                esperDeviceInfo?.let {
                    sharedPrefManaged.edit().apply {
                        putString(Constants.ESPER_DEVICE_NAME, it.deviceId)
                        putString(Constants.ESPER_DEVICE_SERIAL, it.serialNo)
                        putString(Constants.ESPER_DEVICE_IMEI1, it.imei1)
                        putString(Constants.ESPER_DEVICE_IMEI2, it.imei2)
                        putString(Constants.ESPER_DEVICE_UUID, it.uuid)
                        apply()
                    }
                }
            }
        })
    }

    fun getExternalStoragePath(context: Context): String? {
        return getStringPreference(
            context,
            Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ROOT_PATH,
            Constants.ExternalRootFolderExt
        )
    }

    fun setExternalStoragePath(sharedPrefManaged: SharedPreferences, path: String) {
        sharedPrefManaged.edit().putString(Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ROOT_PATH, path)
            .apply()
    }

    fun removeExternalStoragePath(sharedPrefManaged: SharedPreferences) {
        sharedPrefManaged.edit().remove(Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ROOT_PATH).apply()
    }

    fun getInternalStoragePath(context: Context): String? {
        return getStringPreference(
            context,
            Constants.SHARED_MANAGED_CONFIG_INTERNAL_ROOT_PATH,
            Constants.InternalRootFolder
        )
    }

    fun setInternalStoragePath(context: Context, path: String) {
        context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).edit().putString(Constants.SHARED_MANAGED_CONFIG_INTERNAL_ROOT_PATH, path).apply()
    }

    fun removeInternalStoragePath(context: Context) {
        context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).edit().remove(Constants.SHARED_MANAGED_CONFIG_INTERNAL_ROOT_PATH).apply()
    }

    fun initNetworkConfigs() {
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
    }

    fun restart(context: Context) {
        context.activity?.finish()
        val intent = context.activity?.intent
        context.activity?.startActivity(intent)
        context.activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

//    fun triggerRebirth(context: Context) {
//        Log.i(GeneralUtilsTag, "triggerRebirth: Restarting app")
//        val packageManager = context.packageManager
//        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
//        val componentName = intent?.component
//        val mainIntent = Intent.makeRestartActivityTask(componentName).apply {
//            setPackage(context.packageName)
//        }
//        context.startActivity(mainIntent)
//        Runtime.getRuntime().exit(0)
//    }

    fun triggerRebirth(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }

    fun initSDK(
        sharedPrefManaged: SharedPreferences,
        context: Context,
        triggeredFromService: Boolean = false
    ) {
        startEsperSDKActivation(context, sharedPrefManaged, triggeredFromService)
    }

    fun getEsperSDK(context: Context): EsperDeviceSDK {
        return EsperDeviceSDK.getInstance(context)
    }

    fun isEsperDeviceSDKActivated(context: Context, callback: (Boolean) -> Unit) {
        getEsperSDK(context).isActivated(object : EsperDeviceSDK.Callback<Boolean> {
            override fun onResponse(response: Boolean?) {
                callback(response == true)
            }

            override fun onFailure(t: Throwable) {
                Log.e(GeneralUtilsTag, "SDK activation check failed: ${t.message}", t)
                callback(false)
            }
        })
    }

    fun getDeviceName(context: Context): String? {
        return getStringPreference(
            context, Constants.ESPER_DEVICE_NAME, null
        )
    }

    fun getDeviceSerial(context: Context): String? {
        return getStringPreference(
            context, Constants.ESPER_DEVICE_SERIAL, null
        )
    }

    fun getApiKey(context: Context): String? {
        return getStringPreference(
            context, Constants.SHARED_MANAGED_CONFIG_API_KEY, null
        )
    }

    fun getEnterpriseId(context: Context): String? {
        return getStringPreference(
            context, Constants.SHARED_MANAGED_CONFIG_ENTERPRISE_ID, null
        )
    }

    fun getTenant(context: Context): String? {
        return getStringPreference(
            context, Constants.SHARED_MANAGED_CONFIG_TENANT, null
        )
    }

    fun getDeviceUUID(context: Context): String? {
        return getStringPreference(
            context, Constants.ESPER_DEVICE_UUID, null
        )
    }

    fun showNoInternetDialog(context: Context, finishActivity: Boolean = false) {
        MaterialAlertDialogBuilder(context).setTitle("No Internet Connection")
            .setMessage("Please check your internet connection and try again.").setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                if (finishActivity) {
                    (context as? Activity)?.finish()
                }
            }.show()
    }

    fun showTenantDialog(context: Context, onInputReceived: (String, Boolean) -> Unit) {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 0, 20, 0)
        }
        val textInputLayout = TextInputLayout(context).apply {
            hint = "Please enter your tenant name"
        }
        val editText = TextInputEditText(context)
        textInputLayout.addView(editText)
        val checkBox = CheckBox(context).apply {
            text = "Is streamer service active?"
        }
        layout.addView(textInputLayout)
        layout.addView(checkBox)

        MaterialAlertDialogBuilder(context).setTitle("Tenant Details").setView(layout)
            .setCancelable(false).setPositiveButton("OK") { dialog, _ ->
                val tenantInput = editText.text.toString()
                val isChecked = checkBox.isChecked
                onInputReceived(tenantInput, isChecked)
                dialog.dismiss()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                (context as? Activity)?.finish()
            }.show()
    }

    fun saveTenantForNetworkTester(context: Context, tenantInput: String) {
        context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).edit().putString(Constants.SHARED_MANAGED_CONFIG_TENANT_FOR_NETWORK_TESTER, tenantInput)
            .apply()
    }

    fun getTenantForNetworkTester(context: Context): String? {
        return getStringPreference(
            context, Constants.SHARED_MANAGED_CONFIG_TENANT_FOR_NETWORK_TESTER, null
        )
    }

    fun getTargetFromUrl(url: String): String? {
        return url.split(".").firstOrNull()?.replace("https://", "")?.split("-")?.firstOrNull()
    }

    fun setStreamerAvailability(context: Context, isAvailable: Boolean) {
        setBooleanPreference(
            context, Constants.SHARED_MANAGED_CONFIG_STREAMER_FOR_NETWORK_TESTER, isAvailable
        )
    }

    fun isStreamerAvailable(context: Context): Boolean {
        return getBooleanPreference(
            context, Constants.SHARED_MANAGED_CONFIG_STREAMER_FOR_NETWORK_TESTER, false
        )
    }

    fun setBaseStackName(context: Context, baseStackName: String?) {
        context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).edit().putString(
            Constants.SHARED_MANAGED_CONFIG_BASE_STACK_FOR_NETWORK_TESTER,
            baseStackName?.lowercase()
        ).apply()
    }

    fun getBaseStackName(context: Context): String? {
        return getStringPreference(
            context, Constants.SHARED_MANAGED_CONFIG_BASE_STACK_FOR_NETWORK_TESTER, null
        )
    }

    fun fetchAndStoreBaseStackName(
        context: Context, tenantInput: String, callback: BaseStackNameCallback
    ) {
        val url =
            "https://mission-control-api.esper.cloud/api/06-2020/mission-control/companies/?endpoint=$tenantInput"
        val client = OkHttpClient()
        val request = Request.Builder().url(url)
            .addHeader("authorization", BuildConfig.MISSION_CONTROL_API_KEY).build()

        val progressDialog = showMaterialLoadingDialog(context)

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                dismissMaterialLoadingDialog(progressDialog)
                callback.onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                dismissMaterialLoadingDialog(progressDialog)
                if (!response.isSuccessful) {
                    callback.onError(IOException("Unexpected code $response"))
                    return
                }

                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    val dataArray = jsonObject.getJSONArray("data")
                    if (dataArray.length() > 0) {
                        val firstObject = dataArray.getJSONObject(0)
                        val baseStackObject = firstObject.getJSONObject("baseStack")
                        val baseStackName = baseStackObject.getString("name")

                        setBaseStackName(context, baseStackName)
                        callback.onBaseStackNameFetched(baseStackName)
                    } else {
                        callback.onError(IOException("No data found"))
                    }
                } else {
                    callback.onError(IOException("Empty response body"))
                }
            }
        })
    }

    fun showStackDialog(context: Context, onInputReceived: (String?) -> Unit) {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 0, 20, 0)
        }
        val textInputLayout = TextInputLayout(context).apply {
            hint = "Please enter your stack name"
        }
        val editText = TextInputEditText(context)
        textInputLayout.addView(editText)
        layout.addView(textInputLayout)

        MaterialAlertDialogBuilder(context).setTitle("Stack Details")
            .setMessage("Please enter your tenant's stack name (if you know), else press Cancel to skip.")
            .setView(layout).setCancelable(false).setPositiveButton("OK") { dialog, _ ->
                val tenantInput = editText.text.toString()
                onInputReceived(if (tenantInput.isNotEmpty()) tenantInput else null)
                dialog.dismiss()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                onInputReceived(null)
            }.show()
    }

    fun showMaterialLoadingDialog(context: Context): AlertDialog {
        val progressDialogView = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
        return MaterialAlertDialogBuilder(context).setView(progressDialogView).setCancelable(false)
            .show()
    }

    fun dismissMaterialLoadingDialog(dialog: AlertDialog) {
        dialog.dismiss()
    }

    fun useCustomTenantForNetworkTester(context: Context): Boolean {
        return getBooleanPreference(
            context, Constants.SHARED_MANAGED_CONFIG_USE_CUSTOM_TENANT_FOR_NETWORK_TESTER, false
        )
    }

    fun isDeletionAllowed(): Boolean {
        return getBooleanPreference(
            application, Constants.SHARED_MANAGED_CONFIG_DELETION_ALLOWED, false
        )
    }

    fun isArchiveAllowed(): Boolean {
        return getBooleanPreference(
            application, Constants.SHARED_MANAGED_CONFIG_ARCHIVE_ALLOWED, false
        )
    }

    fun isRenameAllowed(): Boolean {
        return getBooleanPreference(
            application, Constants.SHARED_MANAGED_CONFIG_RENAME_ALLOWED, false
        )
    }

    fun isSharingAllowed(): Boolean {
        return getBooleanPreference(
            application, Constants.SHARED_MANAGED_CONFIG_SHARING_ALLOWED, false
        )
    }

    fun isUploadContentAllowed(): Boolean {
        return getBooleanPreference(
            application, Constants.SHARED_MANAGED_CONFIG_UPLOAD_CONTENT, false
        )
    }

    fun isCutCopyAllowed(): Boolean {
        return getBooleanPreference(
            application, Constants.SHARED_MANAGED_CONFIG_CUT_COPY_ALLOWED, false
        )
    }

    interface BaseStackNameCallback {
        fun onBaseStackNameFetched(baseStackName: String)
        fun onError(e: Exception)
    }

    private fun getBooleanPreference(
        context: Context, key: String, defaultValue: Boolean
    ): Boolean {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getBoolean(key, defaultValue)
    }

    private fun setBooleanPreference(
        context: Context, key: String, value: Boolean
    ) {
        context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).edit().putBoolean(key, value).apply()
    }

    private fun getStringPreference(
        context: Context, key: String, defaultValue: String?
    ): String? {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getString(key, defaultValue)
    }
}
