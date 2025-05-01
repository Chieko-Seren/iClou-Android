# iCloud for Android

Access your iCloud content directly from your Android device.

## Features

- Access iCloud Mail, Calendar, Photos, and more
- Seamless synchronization with your Apple devices
- User-friendly interface
- File downloads with a built-in download manager
- Support for various file types (PDF, Office documents, etc.)
- Apple third-party login support

## Permissions

This app requires the following permissions:

- `INTERNET`: To access iCloud services
- `ACCESS_NETWORK_STATE`: To monitor network connectivity
- `POST_NOTIFICATIONS`: To display mail and download notifications
- `RECEIVE_BOOT_COMPLETED`: To start services when device boots
- `WRITE_EXTERNAL_STORAGE`: To save downloaded files (Android 9 and below)

## Getting Started

### Prerequisites

- Android Studio
- Android SDK
- JDK 11 or higher

### Installation

1. Clone this repository
   ```bash
   git clone https://github.com/Chieko-Seren/iCloud-Android.git
   ```
2. Open the project in Android Studio
3. Build and run the app on your device

## Using the Download Manager

1. Tap on any downloadable file link (PDF, documents, etc.)
2. The app will automatically start downloading the file
3. Access all your downloads by tapping the download icon in the toolbar
4. From the Download Manager, you can:
   - Track download progress
   - Open completed downloads
   - Cancel ongoing downloads
   - Remove downloaded files

## Apple Third-Party Login

1. When you see an "Sign in with Apple" button in iCloud, the app will automatically detect it
2. Tapping the button will open an external browser window for secure authentication
3. After successful authentication, you'll be redirected back to the app
4. Your authentication will be securely stored for future sessions

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## License

This project is licensed under the GPL-3.0 License - see the LICENSE file for details. 
