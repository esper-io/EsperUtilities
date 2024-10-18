# Esper Utilities [![Android CI & Release](https://github.com/esper-io/EsperUtilities/actions/workflows/android-ci.yml/badge.svg)](https://github.com/esper-io/EsperUtilities/actions/workflows/android-ci.yml)

**Powering Your World with a Unified Toolset**

Welcome to **Esper Utilities**, an all-in-one utility app designed to streamline your digital management experience. With an integrated file manager, network tester, downloadable content manager, and app store, Esper Utilities provides a comprehensive toolkit within a single app interface. 

## Key Features

- **File Manager**: Organize, view, and manage files with powerful features like root access, breadcrumbs navigation, and archive support.
- **Network Checker**: Diagnose network connectivity issues and monitor your connection’s health.
- **Content Manager**: Easily download and manage your media and files.
- **App Store**: Discover, download, and manage applications seamlessly.

### Additional Functionalities

- **Open Source**: Lightweight, clean, and secure for dependable performance.
- **Material Design**: A clean and intuitive UI that adheres to Material Design principles.
- **Root Support**: Access and manage files requiring root privileges.
- **NAS Support**: Connect to remote servers via FTP, SFTP, SMB, and WebDAV.
- **Archive Support**: Extract and create compressed files such as .zip and .rar.
- **Theme Customization**: Choose UI colors, including a night mode with an optional true black theme.

Built with modern **ViewModel** and **LiveData** architecture, the frontend is responsive and optimized for rotation and error handling, ensuring a smooth user experience.

## Managed Configuration & Esper MDM Compatibility

Esper Utilities integrates seamlessly with the Esper MDM solution, allowing complete management and configuration. This makes it ideal for enterprise environments that need control over file access, network functionality, and security options. 

### Configuration Example
Below are examples of JSON configuration settings for customizable usage control:

- **First Sample JSON** (Basic File Manager availability)
    ```json
    {
      "api_key": "<api_key>",
      "external_root_path": "/esperfiles/",
      "internal_root_path": "/storage/emulated/0/esperfiles/",
      "app_name": "Files",
      "add_storage": false,
      "ftp_allowed": false,
      "rename_allowed": false,
      "upload_content": false,
      "archive_allowed": false,
      "sharing_allowed": false,
      "creation_allowed": false,
      "cut_copy_allowed": false,
      "deletion_allowed": false,
      "on_demand_download": false,
      "show_device_details": false,
      "show_screenshots_folder": false,
      "network_tester_visibility": false,
      "convert_files_to_app_store": false,
      "esper_app_store_visibility": false,
      "convert_files_to_network_tester": false,
      "use_custom_tenant_for_network_tester": false
    }
    ```

- **Second Sample JSON** (Full Accesss - All features)
    ```json
    {
      "api_key": "<api_key>",
      "external_root_path": "/esperfiles/",
      "internal_root_path": "/storage/emulated/0/esperfiles/",
      "app_name": "Files",
      "add_storage": true,
      "ftp_allowed": true,
      "rename_allowed": true,
      "upload_content": true,
      "archive_allowed": true,
      "sharing_allowed": true,
      "creation_allowed": true,
      "cut_copy_allowed": true,
      "deletion_allowed": true,
      "on_demand_download": true,
      "show_device_details": true,
      "show_screenshots_folder": true,
      "network_tester_visibility": true,
      "convert_files_to_app_store": false,
      "esper_app_store_visibility": true,
      "convert_files_to_network_tester": false,
      "use_custom_tenant_for_network_tester": false
    }
    ```

## Installation

1. **Download the App**: Available on [Release Page](https://github.com/esper-io/EsperUtilities/releases/).
2. **Configure Settings**: Set up Managed Config JSON configuration to customize your app experience.
3. **Start Managing**: Begin managing files, network, downloads, and apps directly through Esper Utilities.

## Contact & Support

For issues or inquiries, please reach out to our [Karthik](mailto:karthik@esper.io). 

---

