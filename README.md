Here’s a **comprehensive README** for your dual-camera app! 🚀  

---

# **Multi Camera Preview & Capture App**

This Android application provides a multi-camera experience, allowing users to preview cameras simultaneously. It also includes a capture button to take snapshots from both cameras. 

The app is designed to check if the device hardware supports concurrent camera usage and gracefully handle devices that don’t. 

## 📸 **Features**

- **Multi Camera Preview:** View live feeds from both the front and back cameras at the same time.  
- **Camera Concurrency Check:** Automatically checks if the device supports multi-camera concurrency.  
- **UI Controls:**  
  - **Start Front Camera Button:** Starts the front camera preview.  
  - **Start Back Camera Button:** Starts the back camera preview.  
  - **Capture Button:** Captures images from both cameras simultaneously (if supported).  
- **Error Handling:** Displays a toast message if the device doesn’t support concurrent camera usage.

---

## 🏗️ **Project Structure**

```
├── res/
│   ├── drawable/                    # Icons, button backgrounds
│   ├── layout/                      # XML layout files
│   │   └── activity_main.xml        # Main layout with camera previews and buttons
│   └── values/                      # Strings, dimensions, colors
│
├── MainActivity.kt                  # Main activity with camera logic and concurrency check
└── AndroidManifest.xml              # App permissions and camera features
```

---

## 🛠️ **Tech Stack**

- **Language:** Kotlin  
- **UI Framework:** Android XML Layouts  
- **Camera API:** Camera2 API  

---

## 🚨 **Permissions**  

Add the following permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera.any" android:required="true" />
```

These permissions allow the app to access the device's cameras.

---

## 📲 **UI Layout (activity_main.xml)**

The layout consists of:  
- **2 `TextureView` elements:** For front and back camera previews.  
- **3 Buttons:** To start the front/back cameras and capture an image.  

Example layout snippet:  

```xml
<TextureView
    android:id="@+id/previewView"
    android:layout_width="300px"
    android:layout_height="400px"
    android:layout_alignParentTop="true"
    android:layout_centerHorizontal="true"
/>

<Button
    android:id="@+id/startFrontCameraButton"
    android:layout_width="56dp"
    android:layout_height="56dp"
    android:layout_alignParentTop="true"
    android:layout_alignParentStart="true"
    android:contentDescription="Start Front Camera"
/>
```

---

## 🧠 **Main Logic (MainActivity.kt)**

The main functionality lives in `MainActivity`.  

1. **Camera Concurrency Check:**  
   On app launch, the app checks whether the device supports concurrent camera usage:  

```kotlin
private fun checkCameraConcurrency() {
    val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraIds = cameraManager.cameraIdList
    var supportsConcurrent = false

    for (cameraId in cameraIds) {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val capability = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        
        if (capability?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) == true) {
            supportsConcurrent = true
            break
        }
    }

    if (!supportsConcurrent) {
        Toast.makeText(this, "Camera concurrency not supported on this device", Toast.LENGTH_LONG).show()
    }
}
```

2. **Camera Previews:**  
   Starts front and back camera previews when the corresponding buttons are clicked.  

3. **Capture Functionality:**  
   The capture button grabs images from both previews (if concurrency is supported) and processes them.  

---

## ✅ **Concurrency Support**  

This app relies on Android’s `Camera2` API and checks for the `LOGICAL_MULTI_CAMERA` capability:  

- ✅ **Supported:** Dual previews and captures work seamlessly.  
- ❌ **Not Supported:** The app shows a toast message, and you can choose to either disable the second preview or guide users accordingly.  

---

## 🚀 **How to Run the App**

1. **Clone the repository:**  
   ```sh
   git clone <repo-url>
   cd dual-camera-app
   ```

2. **Open in Android Studio:**  
   Import the project into Android Studio.  

3. **Build & Run:**  
   - Connect a physical Android device (emulators may not support concurrent cameras).  
   - Click **Run** or press **Shift + F10**.  

4. **Test Camera Concurrency:**  
   - If supported: Both camera previews appear, and capture works.  
   - If not supported: A toast appears, and you can decide how to handle the limitation.

---

## 🛡️ **Error Handling**  

- **Camera Permissions Denied:** The app requests permissions at runtime.  
- **Concurrency Unsupported:** Shows a toast and optionally disables the second preview.  
- **Camera Initialization Issues:** Handles exceptions when accessing cameras.  

---

## 🛠️ **Customization Options**

- **Preview Size:** Adjust `layout_width` and `layout_height` for each `TextureView`.  
- **Capture Logic:** Save images, combine them, or handle captures differently.  
- **UI Design:** Modify buttons, colors, or add animations.  

---

## 📘 **Future Improvements**  

- Add video recording for both cameras.  
- Implement picture-in-picture or split-screen layouts.  
- Provide user feedback or alternative features for unsupported devices.  

---

