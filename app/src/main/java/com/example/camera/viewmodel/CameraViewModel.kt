package com.example.camera.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.*
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import androidx.core.net.toUri
import androidx.core.view.MotionEventCompat
import androidx.lifecycle.AndroidViewModel
import com.example.camera.util.BitmapUtil.mirror
import com.example.camera.util.BitmapUtil.rotate
import com.example.camera.util.BitmapUtil.toBitmap
import com.example.camera.util.BitmapUtil.toByteArray
import com.example.camera.util.FileUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt


class CameraViewModel(private val app: Application): AndroidViewModel(app) {
    private val TAG = "###"

    private lateinit var imageReader: ImageReader
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)
    private var frontCameraId: String? = null
    private lateinit var frontCameraCharacteristics: CameraCharacteristics
    private var backCameraId: String? = null
    private lateinit var backCameraCharacteristics: CameraCharacteristics
    private lateinit var cameraDevice: CameraDevice
    private var cameraId: String? = null
    private lateinit var cameraCharacteristics: CameraCharacteristics
    private lateinit var captureRequest: CaptureRequest.Builder
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var previewSurface: Surface
    private lateinit var previewSize: Size
    private var orientation = 0
    private lateinit var mediaRecorder: MediaRecorder
    private val path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/MyCamera"
    private lateinit var displayName: String
    private var zoom = 1
    private lateinit var zoomRect: Rect

    fun initCameraInfo(surfaceTexture: SurfaceTexture, ratio: String) {
        if (cameraId == null) {
            val cameraManager = app.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIdList = cameraManager.cameraIdList
            cameraIdList.forEach { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                if (characteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_FRONT && frontCameraId == null) {
                    frontCameraId = cameraId
                    frontCameraCharacteristics = characteristics
                } else if (characteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_BACK && backCameraId == null) {
                    backCameraId = cameraId
                    backCameraCharacteristics = characteristics
                }
            }
            cameraId = backCameraId
            cameraCharacteristics = backCameraCharacteristics

        } else {
            cameraCharacteristics = when (cameraId) {
                frontCameraId -> frontCameraCharacteristics
                else -> backCameraCharacteristics
            }
        }
        zoom = 1
        File(path).mkdir()
        setPreviewSize(ratio)
        previewSurface = Surface(surfaceTexture)
        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        openCamera()
    }

    private fun setPreviewSize(ratio: String) {
        val aspectRatio: Float = if (ratio == "3:4") {
            4.toFloat() / 3
        } else {
            16.toFloat() / 9
        }
        val sizes = cameraCharacteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            .getOutputSizes(SurfaceTexture::class.java)
        if (sizes != null) {
            for (size in sizes) {
                if (size.width.toFloat() / size.height == aspectRatio) {
                    previewSize = size
                    return
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val cameraManager = app.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.openCamera(cameraId!!, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                cameraDevice = device
                createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            }
            override fun onDisconnected(cameraDevice: CameraDevice) {
                Log.e(TAG, "onDisconnected ${cameraDevice.id} ")
            }
            override fun onError(cameraDevice: CameraDevice, error: Int) {
                Log.e(TAG, "onError ${cameraDevice.id} ")
            }
        }, cameraHandler)
    }

    private fun createCaptureRequest(templateType: Int) {
        imageReader = ImageReader.newInstance(800, 600, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireNextImage()
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            savePicture(bytes)
        }, cameraHandler)

        var target = listOf(previewSurface, imageReader.surface)
        when (templateType) {
            CameraDevice.TEMPLATE_PREVIEW -> {
                captureRequest = cameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(previewSurface) }
            }
            CameraDevice.TEMPLATE_STILL_CAPTURE -> {
                captureRequest = cameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE).apply { addTarget(imageReader.surface) }
            }
            CameraDevice.TEMPLATE_RECORD -> {
                setMediaRecorder()
                captureRequest = cameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_RECORD).apply {
                        addTarget(previewSurface)
                        addTarget(mediaRecorder.surface)
                    }
                captureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                target = listOf(previewSurface, mediaRecorder.surface)
            }
        }
        if (zoom != 1) {
            captureRequest.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
        }
        cameraDevice.createCaptureSession(target, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                createCaptureSession(templateType)
            }
            override fun onConfigureFailed(p0: CameraCaptureSession) {
                Log.e(TAG, "onConfigureFailed")
            }
        }, cameraHandler)
    }

    private fun createCaptureSession(templateType: Int) {
        when (templateType) {
            CameraDevice.TEMPLATE_STILL_CAPTURE -> {
                captureSession.capture(captureRequest.build(), null, cameraHandler)
            }
            else -> {
                captureSession.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
            }
        }
    }

    fun takePicture() {
        createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        cameraHandler.postDelayed({
            createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        }, 500)
    }

    fun videoStart() {
        captureSession.close()
        createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        mediaRecorder.start()
    }

    fun videoStop() {
        mediaRecorder.stop()
        FileUtil.addFile(app, "${path}/${displayName}", "mp4")
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, File("${path}/${displayName}").toUri())
        app.sendBroadcast(mediaScanIntent)
        createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
    }

    fun closeCamera() {
        captureSession.close()
        cameraDevice.close()
        imageReader.close()
    }

    fun releaseThread() {
        cameraThread.quitSafely()
    }

    fun changeCamera(surfaceTexture: SurfaceTexture, ratio: String) {
        cameraId = when (cameraId) {
            frontCameraId -> backCameraId
            backCameraId -> frontCameraId
            else -> null
        }
        cameraDevice.close()
        initCameraInfo(surfaceTexture, ratio)
    }

    private fun setMediaRecorder() {
        mediaRecorder = MediaRecorder()
        mediaRecorder.reset()
        mediaRecorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(8*previewSize.width*previewSize.height)
            setVideoFrameRate(30)
            setOrientationHint(orientation)
            setPreviewDisplay(previewSurface)
            setVideoSize(previewSize.width, previewSize.height)
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            displayName = "VID_${sdf.format(Date())}.mp4"
            setOutputFile("${path}/${displayName}")
        }
        mediaRecorder.prepare()
    }

    private fun savePicture(bytes: ByteArray) {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val displayName = "IMG_${sdf.format(Date())}.jpg"
        val file = File("${path}/${displayName}")

        val bitmap = bytes.toBitmap().rotate(orientation)
        val resultBytes: ByteArray = if (cameraId == frontCameraId) {
            bitmap.mirror().toByteArray()
        } else {
            bitmap.toByteArray()
        }
        file.writeBytes(resultBytes)
        FileUtil.addFile(app, file.path, "jpg")
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, file.toUri())
        app.sendBroadcast(mediaScanIntent)
    }

    private var oldDis = 1
    fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount == 2) {
            when (MotionEventCompat.getActionMasked(event)) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    val x = event.getX(0) - event.getX(1)
                    val y = event.getY(0) - event.getY(1)
                    oldDis = sqrt((x * x + y * y).toDouble()).toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    val x = event.getX(0) - event.getX(1)
                    val y = event.getY(0) - event.getY(1)
                    val newDis = sqrt((x * x + y * y).toDouble()).toInt()
                    if (newDis > oldDis) {
                        handleZoom(true)
                    } else {
                        handleZoom(false)
                    }
                    oldDis = newDis
                }
            }
        }
        return true
    }

    private fun handleZoom(isZoomIn: Boolean) {
        val maxZoom = cameraCharacteristics[CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM]!!
        val factor = 150
        if (isZoomIn && zoom < factor) {
            zoom++
        } else if (zoom > 0) {
            zoom--
        }
        val rect = cameraCharacteristics[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]!!
        val minW = ((rect.width() - rect.width() / maxZoom) / (2 * factor)).toInt()
        val minH = ((rect.height() - rect.height() / maxZoom) / (2 * factor)).toInt()
        val cropW = minW * zoom
        val cropH = minH * zoom
        zoomRect = Rect(rect.left + cropW, rect.top + cropH, rect.right - cropW, rect.bottom - cropH)
        captureRequest.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
        captureSession.stopRepeating()
        captureSession.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
    }

    fun setOrientation(deviceOrientation: Int) {
        var mDeviceOrientation = (deviceOrientation + 45) / 90 * 90
        val sensorOrientation = cameraCharacteristics[CameraCharacteristics.SENSOR_ORIENTATION]!!
        if (cameraId == frontCameraId) {
            mDeviceOrientation = -mDeviceOrientation
        }
        orientation = (sensorOrientation + mDeviceOrientation + 360) % 360
    }
}