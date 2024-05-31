@file:Suppress("DEPRECATION")

package io.esper.android.files.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.text.TextUtils
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
import io.esper.android.files.filelist.FileListActivity
import io.esper.android.files.filelist.FileListFragment
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
import java.io.File
import java.io.IOException
import java.lang.Thread.sleep
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date


object GeneralUtils {
    fun createDir(mCurrentPath: String) {
        val fileDirectory = File(mCurrentPath)
        if (!fileDirectory.exists()) fileDirectory.mkdir()
    }

    fun deleteDir(mCurrentPath: String) {
        Log.i(GeneralUtilsTag, "deleteDir: Deleting directory")
        val fileDirectory = File(mCurrentPath)
        if (fileDirectory.exists()) fileDirectory.deleteRecursively()
    }

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) view = View(activity)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun hideKeyboard(context: Context) {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = context.activity?.currentFocus
        if (view == null) view = View(context)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun setFadeAnimation(view: View) {
        val anim = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 300
        view.startAnimation(anim)
    }

    fun hasActiveInternetConnection(context: Context): Boolean {
        var isConnected = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        if (activeNetwork != null && activeNetwork.isConnected) {
            isConnected = true
        }
        return isConnected
    }

    fun calculateNoOfColumns(
        context: Context, columnWidthDp: Float
    ): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        return (screenWidthDp / columnWidthDp + 0.5).toInt()
    }

    @SuppressLint("SimpleDateFormat")
    fun getCurrentDateTime(): String? {
        val dateFormat: DateFormat = SimpleDateFormat("ddMMyyHHmm")
        val date = Date()
        return dateFormat.format(date)
    }

    private fun startEsperSDKActivation(context: Context, sharedPrefManaged: SharedPreferences?) {
        val token = sharedPrefManaged!!.getString(
            Constants.SHARED_MANAGED_CONFIG_API_KEY, null
        )
        val existingDeviceName = getDeviceName(context)
        if (token != null && TextUtils.isEmpty(existingDeviceName)) {
            Log.i(GeneralUtilsTag, "initSDK: Initializing SDK")
            val sdk = getEsperSDK(context)
            sdk.activateSDK(token, object : EsperDeviceSDK.Callback<Void?> {
                override fun onResponse(response: Void?) {
                    getDeviceDetails(sdk, sharedPrefManaged)
                    getProvisioningInfo(context, sdk, sharedPrefManaged)
                }

                override fun onFailure(t: Throwable) {
                    Log.d(
                        GeneralUtilsTag,
                        "activateSDK: Callback.onFailure: message : " + t.message
                    )
                }
            })
        }
    }

    fun isAddingStorageAllowed(context: Context): Boolean {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getBoolean(Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ADD_STORAGE, false)
    }

    fun isDlcAllowed(context: Context): Boolean {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getBoolean(Constants.SHARED_MANAGED_CONFIG_ON_DEMAND_DOWNLOAD, false)
    }

    fun isEsperAppStoreVisible(context: Context): Boolean {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getBoolean(Constants.SHARED_MANAGED_CONFIG_ESPER_APP_STORE_VISIBILITY, false)
    }

    fun isFtpServerAllowed(context: Context): Boolean {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getBoolean(Constants.SHARED_MANAGED_CONFIG_EXTERNAL_FTP_ALLOWED, false)
    }

    fun isNetworkTesterVisible(context: Context): Boolean {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getBoolean(Constants.SHARED_MANAGED_CONFIG_NETWORK_TESTER_VISIBILITY, false)
    }

    fun showDeviceDetails(context: Context): Boolean {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getBoolean(Constants.SHARED_MANAGED_CONFIG_SHOW_DEVICE_DETAILS, false)
    }

    private fun getProvisioningInfo(
        context: Context, sdk: EsperDeviceSDK, sharedPrefManaged: SharedPreferences
    ) {
        sdk.getProvisionInfo(object : EsperDeviceSDK.Callback<ProvisionInfo> {
            override fun onResponse(response: ProvisionInfo?) {
                if (!response?.apiEndpoint.isNullOrEmpty()) {
                    val tenant = response?.apiEndpoint
                    sharedPrefManaged.edit()
                        .putString(Constants.SHARED_MANAGED_CONFIG_TENANT, tenant).apply()
                }
                if (!response?.tenantUUID.isNullOrEmpty()) {
                    val enterpriseId = response?.tenantUUID
                    sharedPrefManaged.edit()
                        .putString(Constants.SHARED_MANAGED_CONFIG_ENTERPRISE_ID, enterpriseId)
                        .apply()
                    triggerRebirth(context)
                }
                triggerRebirth(context)
            }

            override fun onFailure(t: Throwable) {
                Log.d(
                    GeneralUtilsTag,
                    "getProvisionInfo: Callback.onFailure: message : " + t.message
                )
            }
        })
    }

    private fun getDeviceDetails(sdk: EsperDeviceSDK, sharedPrefManaged: SharedPreferences?) {
        sdk.getEsperDeviceInfo(object : EsperDeviceSDK.Callback<EsperDeviceInfo> {

            override fun onFailure(t: Throwable) {
                Log.d(
                    GeneralUtilsTag,
                    "getEsperDeviceInfo: Callback.onFailure: message : " + t.message
                )
            }

            override fun onResponse(esperDeviceInfo: EsperDeviceInfo?) {
                if (!esperDeviceInfo?.deviceId.isNullOrEmpty()) {
                    val deviceId = esperDeviceInfo?.deviceId
                    sharedPrefManaged?.edit()?.putString(
                        Constants.ESPER_DEVICE_NAME, deviceId
                    )?.apply()
                }
                if (!esperDeviceInfo?.serialNo.isNullOrEmpty()) {
                    val serialNo = esperDeviceInfo?.serialNo
                    sharedPrefManaged?.edit()?.putString(
                        Constants.ESPER_DEVICE_SERIAL, serialNo
                    )?.apply()
                }
                if (!esperDeviceInfo?.imei1.isNullOrEmpty()) {
                    val imei1 = esperDeviceInfo?.imei1
                    sharedPrefManaged?.edit()?.putString(
                        Constants.ESPER_DEVICE_IMEI1, imei1
                    )?.apply()
                }
                if (!esperDeviceInfo?.imei2.isNullOrEmpty()) {
                    val deviceId = esperDeviceInfo?.imei2
                    sharedPrefManaged?.edit()?.putString(
                        Constants.ESPER_DEVICE_IMEI2, deviceId
                    )?.apply()
                }
                if (!esperDeviceInfo?.uuid.isNullOrEmpty()) {
                    val deviceId = esperDeviceInfo?.uuid
                    sharedPrefManaged?.edit()?.putString(
                        Constants.ESPER_DEVICE_UUID, deviceId
                    )?.apply()
                }
            }
        })
    }

    fun getExternalStoragePath(sharedPrefManaged: SharedPreferences): String? {
        return sharedPrefManaged.getString(
            Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ROOT_PATH, Constants.ExternalRootFolder
        )!!
    }

    fun setExternalStoragePath(sharedPrefManaged: SharedPreferences, path: String) {
        sharedPrefManaged.edit().putString(Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ROOT_PATH, path)
            .apply()
    }

    fun removeExternalStoragePath(sharedPrefManaged: SharedPreferences) {
        sharedPrefManaged.edit().remove(Constants.SHARED_MANAGED_CONFIG_EXTERNAL_ROOT_PATH).apply()
    }

    fun getInternalStoragePath(context: Context): String? {
        return getInternalStoragePath(
            context.getSharedPreferences(
                Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
            )
        )
    }

    fun getInternalStoragePath(sharedPrefManaged: SharedPreferences): String? {
        return sharedPrefManaged.getString(
            Constants.SHARED_MANAGED_CONFIG_INTERNAL_ROOT_PATH, Constants.InternalRootFolder
        )
    }

    fun setInternalStoragePath(sharedPrefManaged: SharedPreferences, path: String) {
        sharedPrefManaged.edit().putString(Constants.SHARED_MANAGED_CONFIG_INTERNAL_ROOT_PATH, path)
            .apply()
    }

    fun removeInternalStoragePath(sharedPrefManaged: SharedPreferences) {
        sharedPrefManaged.edit().remove(Constants.SHARED_MANAGED_CONFIG_INTERNAL_ROOT_PATH).apply()
    }

    fun initNetworkConfigs() {
        //Used for Glide Image Loader
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
    }

    fun restart(context: Context) {
        context.activity?.finish()
        val intent = context.activity?.intent?.let {
            FileListFragment.Args(
                it
            )
        }?.let {
            FileListActivity::class.createIntent().putArgs(
                it
            )
        }
        intent?.let { context.startActivitySafe(it) }
        context.activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    fun triggerRebirth(context: Context) {
        Log.i(GeneralUtilsTag, "triggerRebirth: Restarting app")
        sleep(1000)
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        // Required for API 34 and later
        // Ref: https://developer.android.com/about/versions/14/behavior-changes-14#safer-intents
        mainIntent.setPackage(context.packageName)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }


    private fun clearCache(context: Context) {
        // Clear cache directory
        val cacheDir = context.cacheDir
        cacheDir?.let { deleteDir(it) }
    }

    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            val children = dir.list()
            children?.forEach { fileName ->
                val success = deleteDir(File(dir, fileName))
                if (!success) {
                    return false
                }
            }
        }
        return dir.delete()
    }

    fun initSDK(sharedPrefManaged: SharedPreferences, context: Context) {
        startEsperSDKActivation(context, sharedPrefManaged)
    }

    fun getEsperSDK(context: Context): EsperDeviceSDK {
        return EsperDeviceSDK.getInstance(context)
    }

    fun isEsperDeviceSDKActivated(context: Context, callback: (Boolean) -> Unit) {
        getEsperSDK(context).isActivated(object : EsperDeviceSDK.Callback<Boolean> {
            override fun onResponse(response: Boolean?) {
                response?.let {
                    callback(it)
                }
            }

            override fun onFailure(t: Throwable) {
                Log.d(
                    GeneralUtilsTag,
                    "activateSDK: Callback.onFailure: message : " + t.message
                )
                // Call the callback with a default value or handle failure case as needed
                callback(false)
            }
        })
    }

    fun getDeviceName(context: Context): String? {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getString(Constants.ESPER_DEVICE_NAME, null)
    }

    fun getDeviceSerial(context: Context): String? {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getString(Constants.ESPER_DEVICE_SERIAL, null)
    }

    fun getApiKey(context: Context): String? {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getString(Constants.SHARED_MANAGED_CONFIG_API_KEY, null)
    }

    fun getEnterpriseId(context: Context): String? {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getString(Constants.SHARED_MANAGED_CONFIG_ENTERPRISE_ID, null)
    }

    fun getTenant(context: Context): String? {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getString(Constants.SHARED_MANAGED_CONFIG_TENANT, null)
    }

    fun getDeviceUUID(context: Context): String? {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getString(Constants.ESPER_DEVICE_UUID, null)
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
                // Pass the input to the callback
                onInputReceived(tenantInput, isChecked)
                dialog.dismiss()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                context.activity?.finish()
            }.show()
    }

    fun saveTenantForNetworkTester(context: Context, tenantInput: String) {
        context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).edit().putString(Constants.SHARED_MANAGED_CONFIG_TENANT_FOR_NETWORK_TESTER, tenantInput)
            .apply()
    }

    fun getTenantForNetworkTester(context: Context): String? {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getString(Constants.SHARED_MANAGED_CONFIG_TENANT_FOR_NETWORK_TESTER, null)
    }

    fun getTargetFromUrl(url: String): String? {
        val parts = url.split(".")
        if (parts.isNotEmpty()) {
            val subdomain = parts[0].replace("https://", "")
            if (subdomain.contains("-")) {
                return subdomain.split("-")[0]
            }
            return subdomain
        }
        return null
    }

    fun setStreamerAvailability(context: Context, isAvailable: Boolean) {
        context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).edit()
            .putBoolean(Constants.SHARED_MANAGED_CONFIG_STREAMER_FOR_NETWORK_TESTER, isAvailable)
            .apply()
    }

    fun isStreamerAvailable(context: Context): Boolean {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getBoolean(Constants.SHARED_MANAGED_CONFIG_STREAMER_FOR_NETWORK_TESTER, false)
    }

    fun setBaseStackName(requireContext: Context, baseStackName: String?) {
        requireContext.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).edit()
            .putString(Constants.SHARED_MANAGED_CONFIG_BASE_STACK_FOR_NETWORK_TESTER, baseStackName)
            .apply()
    }

    fun getBaseStackName(context: Context): String? {
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getString(Constants.SHARED_MANAGED_CONFIG_BASE_STACK_FOR_NETWORK_TESTER, null)
    }

    fun fetchAndStoreBaseStackName(
        context: Context,
        tenantInput: String,
        callback: BaseStackNameCallback
    ) {
        val url =
            "https://mission1-control-api.esper.cloud/api/06-2020/mission-control/companies/?endpoint=$tenantInput"
        val client = OkHttpClient()
        val request = Request.Builder().url(url)
            .addHeader("authorization", BuildConfig.MISSION_CONTROL_API_KEY).build()

        // Show loading dialog using MaterialAlertDialogBuilder
        val progressDialog = showMaterialLoadingDialog(context)

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    dismissMaterialLoadingDialog(progressDialog)
                    callback.onError(e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    Handler(Looper.getMainLooper()).post {
                        dismissMaterialLoadingDialog(progressDialog)
                    }

                    if (!response.isSuccessful) {
                        Handler(Looper.getMainLooper()).post {
                            callback.onError(IOException("Unexpected code $response"))
                        }
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

                            // Store the base stack name in shared preferences
                            setBaseStackName(context, baseStackName.toLowerCase())

                            // Notify the fragment with the fetched base stack name
                            Handler(Looper.getMainLooper()).post {
                                callback.onBaseStackNameFetched(baseStackName)
                            }
                        } else {
                            Handler(Looper.getMainLooper()).post {
                                callback.onError(IOException("No data found"))
                            }
                        }
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            callback.onError(IOException("Empty response body"))
                        }
                    }
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
                // Pass the input to the callback
                if (tenantInput.isNotEmpty()) {
                    onInputReceived(tenantInput)
                } else {
                    onInputReceived(null)
                }
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
        return context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        ).getBoolean(Constants.SHARED_MANAGED_CONFIG_USE_CUSTOM_TENANT_FOR_NETWORK_TESTER, false)
    }

    fun createDirectory(path: String) {
        try {
            val fileDirectory = File(path)
            if (!fileDirectory.exists()) {
                fileDirectory.mkdir()
            }
        } catch (e: Exception) {
            Log.e(GeneralUtilsTag, "Error creating directory: $path", e)
        }
    }

    interface BaseStackNameCallback {
        fun onBaseStackNameFetched(baseStackName: String)
        fun onError(e: Exception)
    }
}