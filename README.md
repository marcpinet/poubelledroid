# Poubelldroid

## 📝 Description

Poubelldroid is a school project for the course *IHM* at Polytech Nice Sophia. an application that aims to facilitate the search and cleanup of abandoned waste.

## 🎥 Demo (censured my home for privacy concerns)

https://github.com/marcpinet/poubelledroid/assets/52708150/63dfb93b-5c0c-40c3-b67f-2074b7091c81

**NOTE:** Demo is not the latest version which includes better aestetic, visuals, settings and much more!

## 💡 How to use

### Prerequisites

* [Java JDK 8+](https://www.oracle.com/java/technologies/downloads/) *(tested on 8, 11, and 17)*
* [Android SDK (API 29+)](https://developer.android.com/studio) linked to an `ANDROID_HOME` environement variable *(when opened with Android Studio, the Android SDK is automatically set for you thanks to the `local.properties` auto-generated file)*

1. Get a copy of the Project. Assuming you have git installed, open your Terminal and enter:

    ```bash
    git clone 'https://github.com/marcpinet/poubelledroid'
    ```

2. Setting up your own backend

    2.1 Create a `.env` file at the root directory based on the `.env.template` file

    2.2 Firebase setup
        
    - Get your own `google-service.json`, set up your own Firebase instance, enable **Firestore**, **Storage**, **Functions** and **Cloud Messaging**
    
    - Fill the dedicated part in the `.env` file
    
    - Add the functions from the `firebase-functions` directory using `firebase init` and `firebase deploy` (install using `npm install -g firebase-tools`)

    2.3 Get a **Google Maps API Key** and fill the `.env` file

    2.4 Get a **Twitter API Key (the Bearer Token)** and fill the `.env` file

3. Go into the project's root directory and run:

    ```bash
    .\gradlew build
    ```

    *Note: On Linux, you'll need to use* `chmod +x gradlew` *and* `./gradlew build`

### Running

* If you want to run it in **Android Studio**:
    - Open the folder with Android Studio (choose API 29 if prompted to choose one)
    - Create a new virtual device (take whatever model you want, but we went for the Pixel 2)
    - Use API 29 (Android Q == Android 10)
    - Build and run!

* If you want to run it on **your phone**:
    - Connect your device via debugging mode
    - Paire device with Android Studio using either Wi-Fi debugging or USB Debugging

    OR
    
    - Build > Generate Signed APK and generate the APK with a certificate
    - Get the APK on your phone by using `adb` or by file transfer
    - Install and run!

## 📃 License

Distributed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details
