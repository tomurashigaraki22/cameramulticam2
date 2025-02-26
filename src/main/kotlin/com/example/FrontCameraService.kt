package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.service.notification.NotificationListenerService
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class FrontCameraService : NotificationListenerService() {
    private val CHANNEL_ID = "FrontCameraService"
    private val NOTIFICATION_ID = 2
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("FrontCameraThread").apply { start() }
        handler = Handler(handlerThread.looper)

        startCamera()
    }

    private fun startCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e("FrontCameraService", "Camera permission not granted")
            return
        }

        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            cameraDevice = camera
                            createCameraPreviewSession()
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            camera.close()
                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            camera.close()
                        }
                    }, handler)
                    break
                }
            }
        } catch (e: CameraAccessException) {
            Log.e("FrontCameraService", "Failed to open front camera", e)
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val textureView = TextureView(this)
            val surfaceTexture: SurfaceTexture = textureView.surfaceTexture!!
            val surface = Surface(surfaceTexture)

            val previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)

            cameraDevice!!.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    session.setRepeatingRequest(previewRequestBuilder.build(), null, handler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e("FrontCameraService", "Failed to configure camera preview")
                }
            }, handler)
        } catch (e: CameraAccessException) {
            Log.e("FrontCameraService", "Failed to create camera preview session", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Front Camera Service"
            val descriptionText = "Running front camera service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Front Camera Service")
            .setContentText("Running front camera service")
            .setSmallIcon(R.drawable.ic_stat_4k)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice?.close()
        captureSession?.close()
        handlerThread.quitSafely()
    }
}
