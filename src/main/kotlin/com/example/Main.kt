package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.R

class Main : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager

    private var backCameraDevice: CameraDevice? = null
    private var backCaptureSession: CameraCaptureSession? = null

    private lateinit var backHandler: Handler
    private lateinit var backThread: HandlerThread

    private lateinit var backPreview: TextureView
    private lateinit var frontPreview: TextureView

    private lateinit var startBackCameraButton: Button
    private lateinit var startFrontCameraButton: Button
    private lateinit var captureButton: Button

    private lateinit var imageReader: ImageReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        backPreview = findViewById(R.id.previewView)
        frontPreview = findViewById(R.id.previewView2)
        startBackCameraButton = findViewById(R.id.startBackCameraButton)
        startFrontCameraButton = findViewById(R.id.startFrontCameraButton)
        captureButton = findViewById(R.id.captureButton)

        startBackgroundThreads()

        if (checkPermissions()) {
            checkCameraConcurrency()
            setupPreview(backPreview)
            setupPreview(frontPreview)
        } else {
            requestPermissions()
        }

        startBackCameraButton.setOnClickListener {
            startCamera(CameraCharacteristics.LENS_FACING_BACK, backPreview)
        }

        startFrontCameraButton.setOnClickListener {
            startCamera(CameraCharacteristics.LENS_FACING_FRONT, frontPreview)
        }

        captureButton.setOnClickListener {
            capturePicture()
        }
    }

    private fun setupPreview(previewView: TextureView) {
        previewView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(texture: android.graphics.SurfaceTexture, width: Int, height: Int) {
                Log.d(TAG, "TextureView ready")
            }

            override fun onSurfaceTextureSizeChanged(texture: android.graphics.SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(texture: android.graphics.SurfaceTexture): Boolean {
                closeCamera()
                return true
            }

            override fun onSurfaceTextureUpdated(texture: android.graphics.SurfaceTexture) {}
        }
    }

    private fun setupImageReader() {
        imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 2)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                saveImage(it)
                image.close()
            }
        }, backHandler)
    }

    private fun capturePicture() {
        backCameraDevice?.let { camera ->
            try {
                val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                captureRequestBuilder.addTarget(imageReader.surface)
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

                backCaptureSession?.capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                        super.onCaptureCompleted(session, request, result)
                        Toast.makeText(this@Main, "Image captured!", Toast.LENGTH_SHORT).show()
                    }
                }, backHandler)
            } catch (e: CameraAccessException) {
                Log.e(TAG, "Failed to capture picture", e)
            }
        }
    }

    private fun saveImage(image: android.media.Image) {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = File(picturesDir, "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")

        try {
            FileOutputStream(file).use { output ->
                output.write(bytes)
                Toast.makeText(this, "Image saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save image", e)
        } finally {
            image.close()
        }
    }


    private fun checkCameraConcurrency() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // List all available cameras
        val cameraIds = cameraManager.cameraIdList
        var supportsConcurrent = false

        // Check if at least two cameras support concurrent operation
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

    private fun startCamera(lensFacing: Int, previewView: TextureView) {
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == lensFacing) {
                try {
                    cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            backCameraDevice = camera
                            setupImageReader()
                            createCameraPreviewSession(camera, previewView)
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            camera.close()
                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            camera.close()
                            Log.e(TAG, "Camera error: $error")
                        }
                    }, backHandler)
                } catch (e: CameraAccessException) {
                    Log.e(TAG, "Failed to open camera", e)
                }
                break
            }
        }
    }

    private fun createCameraPreviewSession(camera: CameraDevice, previewView: TextureView) {
        try {
            val texture = previewView.surfaceTexture
            texture?.setDefaultBufferSize(1920, 1080)
            val surface = Surface(texture)

            val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)

            camera.createCaptureSession(listOf(surface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    backCaptureSession = session
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    session.setRepeatingRequest(previewRequestBuilder.build(), null, backHandler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(this@Main, "Failed to configure camera", Toast.LENGTH_SHORT).show()
                }
            }, backHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create camera preview session", e)
        }
    }

    private fun startBackgroundThreads() {
        backThread = HandlerThread("BackCameraBackground").apply { start() }
        backHandler = Handler(backThread.looper)
    }

    private fun closeCamera() {
        backCaptureSession?.close()
        backCameraDevice?.close()
        backCaptureSession = null
        backCameraDevice = null
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_PERMISSIONS)
    }

    companion object {
        private const val TAG = "MultiCamera"
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}
