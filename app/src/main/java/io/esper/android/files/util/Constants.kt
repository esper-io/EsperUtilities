package io.esper.android.files.util

import android.os.Environment
import java.io.File

object Constants {

    var InternalRootFolder: String =
        Environment.getExternalStorageDirectory().path + File.separator + "esperfiles" + File.separator

    var ExternalRootFolder: String = "esperfiles" + File.separator

    var InternalScreenshotFolderDCIM: String =
        Environment.getExternalStorageDirectory().path + File.separator + "DCIM" + File.separator + "Screenshots" + File.separator
    var InternalScreenshotFolderPictures: String =
        Environment.getExternalStorageDirectory().path + File.separator + "Pictures" + File.separator + "Screenshots" + File.separator

    var EsperScreenshotFolder: String = InternalRootFolder + "Screenshots"

    //Tags
    const val FileUtilsTag = "FileUtils"
    const val UploadDownloadUtilsTag = "UploadDownloadUtils"
    const val ManagedConfigUtilsTag = "ManagedConfigUtils"
    const val GeneralUtilsTag = "GeneralUtils"
    const val FileListFragmentTag = "FileListFragment"
    const val PdfViewerFragmentTag = "PdfViewerFragment"
    const val DlcFragmentTag = "DlcFragment"
    const val AppStoreFragmentTag = "AppStoreFragment"
    const val NetworkTesterFragmentTag = "NetworkTesterFragment"

    const val SORT_ASCENDING = "ascending"

    // SharedPreference keys
    const val ORIGINAL_SCREENSHOT_STORAGE_VALUE = "OGScreenshotFolder"

    const val ESPER_DEVICE_NAME = "esperDeviceName"
    const val ESPER_DEVICE_SERIAL = "esperDeviceSerial"
    const val ESPER_DEVICE_IMEI1 = "esperDeviceIMEI1"
    const val ESPER_DEVICE_IMEI2 = "esperDeviceIMEI2"
    const val ESPER_DEVICE_UUID = "esperDeviceUUID"

    const val SHARED_MANAGED_CONFIG_VALUES = "ManagedConfig"
    const val SHARED_MANAGED_CONFIG_APP_NAME = "app_name"

    // only used locally for now
    const val SHARED_MANAGED_CONFIG_TENANT_FOR_NETWORK_TESTER = "tenant_for_network_tester"
    const val SHARED_MANAGED_CONFIG_STREAMER_FOR_NETWORK_TESTER = "streamer_for_network_tester"
    const val SHARED_MANAGED_CONFIG_BASE_STACK_FOR_NETWORK_TESTER = "base_stack_for_network_tester"

    const val SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS = "show_screenshots_folder"
    const val SHARED_MANAGED_CONFIG_DELETION_ALLOWED = "deletion_allowed"
    const val SHARED_MANAGED_CONFIG_ON_DEMAND_DOWNLOAD = "on_demand_download"
    const val SHARED_MANAGED_CONFIG_TENANT = "tenant"

    const val SHARED_MANAGED_CONFIG_ENTERPRISE_ID = "enterprise_id"
    const val SHARED_MANAGED_CONFIG_API_KEY = "api_key"
    const val SHARED_MANAGED_CONFIG_UPLOAD_CONTENT = "upload_content"
    const val SHARED_MANAGED_CONFIG_SHARING_ALLOWED = "sharing_allowed"
    const val SHARED_MANAGED_CONFIG_CREATION_ALLOWED = "creation_allowed"
    const val SHARED_MANAGED_CONFIG_INTERNAL_ROOT_PATH = "internal_root_path"
    const val SHARED_MANAGED_CONFIG_EXTERNAL_ROOT_PATH = "external_root_path"
    const val SHARED_MANAGED_CONFIG_EXTERNAL_ADD_STORAGE = "add_storage"
    const val SHARED_MANAGED_CONFIG_EXTERNAL_FTP_ALLOWED = "ftp_allowed"
    const val SHARED_MANAGED_CONFIG_ARCHIVE_ALLOWED = "archive_allowed"
    const val SHARED_MANAGED_CONFIG_RENAME_ALLOWED = "rename_allowed"
    const val SHARED_MANAGED_CONFIG_SHOW_DEVICE_DETAILS = "show_device_details"
    const val SHARED_MANAGED_CONFIG_ESPER_APP_STORE_VISIBILITY = "esper_app_store_visibility"
    const val SHARED_MANAGED_CONFIG_CONVERT_FILES_TO_APP_STORE = "convert_files_to_app_store"
    const val SHARED_MANAGED_CONFIG_NETWORK_TESTER_VISIBILITY = "network_tester_visibility"
    const val SHARED_MANAGED_CONFIG_CONVERT_FILES_TO_NETWORK_TESTER =
        "convert_files_to_network_tester"
    const val SHARED_MANAGED_CONFIG_USE_CUSTOM_TENANT_FOR_NETWORK_TESTER =
        "use_custom_tenant_for_network_tester"
}