package com.magi.adlive.encode.video

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.util.Size
import android.view.Surface
import com.magi.adlive.model.Facing
import com.magi.adlive.util.ADLogUtil
import java.util.concurrent.Semaphore
import kotlin.math.abs

internal class ADCameraController(context: Context) : CameraDevice.StateCallback() {

    private val TAG = "CameraController"

    private var cameraDevice: CameraDevice? = null
    private var surface: Surface? = null
    private val cameraManager: CameraManager = context.getSystemService(CameraManager::class.java)
    private var cameraHandler: Handler? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    var isPrepared = false
        private set
    var isRunning = false
        private set
    private var currentCameraId = "0"
    private var facing = Facing.BACK
    private var requestBuilder: CaptureRequest.Builder? = null
    private var fingerSpacing = 0
    private var zoomLevel = 0f
    private var flashEnable = false
    private var autoFocusEnable = true
    private var fps = 30
    private var semaphore = Semaphore(0)
    private var cameraCallback: ADCameraCallback? = null

    fun getCameraResolutions(facing: Facing): Array<Size> {
        return try {
            val cameraId = getCameraIdFromFacing(facing)
            val characteristics = getCameraCharacteristics(cameraId)
            characteristics
                ?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.getOutputSizes(SurfaceTexture::class.java)?: emptyArray()
        } catch (e: CameraAccessException) {
            ADLogUtil.logE(TAG, " Error", e)
            emptyArray()
        }
    }

    fun prepareCamera(surfaceTexture: SurfaceTexture, width: Int, height: Int, fps: Int) {
        surfaceTexture.setDefaultBufferSize(width, height)
        surface = Surface(surfaceTexture)
        this.fps = fps
        isPrepared = true
    }

    fun openLastCamera() {
        openCameraId(currentCameraId)
    }

    private fun startPreview(cameraDevice: CameraDevice) {
        try {
            val listSurfaces = ArrayList<Surface>()
            surface?.let { listSurfaces.add(it) }

            cameraDevice.createCaptureSession(listSurfaces, object :CameraCaptureSession.StateCallback() {

                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSession = session
                    try {
                        val captureRequest = createCaptureRequest(listSurfaces)
                        captureRequest?.let { session.setRepeatingRequest(it, null, cameraHandler) }
                    } catch (e : CameraAccessException) {
                        ADLogUtil.logE(TAG, "Camera Error", e)
                    } catch (e: IllegalStateException) {
                        reOpenCamera(currentCameraId)
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    session.close()
                    cameraCallback?.onCameraError("CameraCaptureSession configuration failed")
                }
            }, cameraHandler)
        } catch (e: CameraAccessException) {
            cameraCallback?.onCameraError("Create capture session failed: ${e.message}")
            ADLogUtil.logE(TAG, "Error", e)
        } catch (e: IllegalArgumentException) {
            cameraCallback?.onCameraError("Create capture session failed: ${e.message}")
            ADLogUtil.logE(TAG, "Error", e)
        } catch (e: IllegalStateException) {
            reOpenCamera(currentCameraId)
        }
    }

    private fun createCaptureRequest(surfaces: List<Surface>): CaptureRequest? {
        requestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)?:return null
        for (surface in surfaces) {
            requestBuilder!!.addTarget(surface)
        }
        adaptFpsRange()
        return requestBuilder?.build()
    }

    private fun setCameraCallback(callback: ADCameraCallback) {
        cameraCallback = callback
    }

    private fun adaptFpsRange() {
        val characteristics = getCameraCharacteristics(currentCameraId)
        val fpsRanges = characteristics?.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
        if (fpsRanges != null && fpsRanges.isNotEmpty()) {
            var closestRange: Range<Int> = fpsRanges.first()
            var delta = abs(closestRange.upper - fps) + abs(closestRange.lower - fps)
            for (fpsRange in fpsRanges) {
                if (fpsRange.lower <= fps && fpsRange.upper >= fps) {
                    val curDelta = abs(fpsRange.upper - fps) + abs(fpsRange.lower - fps)
                    if (curDelta < delta) {
                        closestRange = fpsRange
                        delta = curDelta
                    }
                }
            }
            ADLogUtil.logD(TAG, "choose fps range ${closestRange.lower} ~ ${closestRange.upper}")
            requestBuilder?.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, closestRange)
        }

    }

    private fun getCameraCharacteristics(cameraId: String): CameraCharacteristics? {
        return try {
            cameraManager.getCameraCharacteristics(cameraId)
        } catch (e: Exception) {
            ADLogUtil.logE(TAG, "Error", e)
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCameraId(cameraId: String) {
        currentCameraId = cameraId
        if (isPrepared) {
            val handlerThread = HandlerThread("${TAG}cameraId:$cameraId")
            handlerThread.start()
            cameraHandler = Handler(handlerThread.looper)
            try {
                semaphore.acquireUninterruptibly()
                cameraManager.openCamera(cameraId, this, cameraHandler)
                isRunning = true
                facing = getCameraFacingFromId(cameraId)
                cameraCallback?.onCameraChanged(facing)
            } catch (e: CameraAccessException) {
                cameraCallback?.onCameraError("Open camera $cameraId failed")
                ADLogUtil.logE(TAG, "Error", e)
            } catch (e: SecurityException) {
                cameraCallback?.onCameraError("Open camera $cameraId failed")
                ADLogUtil.logE(TAG, "Error", e)
            }
        } else {
            ADLogUtil.logE(TAG, "before openCamera need prepared")
        }
    }

    private fun switchCamera() {
        try {
            var newCameraId = if (cameraDevice == null || facing == Facing.FRONT) {
                getCameraIdFromFacing(Facing.BACK)
            } else {
                getCameraIdFromFacing(Facing.FRONT)
            }
            if (newCameraId.isEmpty()) newCameraId = "0"
            reOpenCamera(newCameraId)
        } catch (e: CameraAccessException) {
            ADLogUtil.logE(TAG, "Error", e)
        }
    }

    private fun reOpenCamera(cameraId: String) {
        if (cameraDevice!=null) {
            closeCamera(false)
            isPrepared = true
            openCameraId(cameraId)
        }
    }

    fun stopRepeating() {
        try {
            cameraCaptureSession?.stopRepeating()
        } catch (e: IllegalStateException) {
            ADLogUtil.logE(TAG, "Error", e)
        } catch (e: CameraAccessException) {
            ADLogUtil.logE(TAG, "Error", e)
        }
        surface = null
    }

    fun closeCamera(resetSurface: Boolean = true) {
        flashEnable = false
        zoomLevel = 1f
        cameraCaptureSession?.close()
        cameraCaptureSession = null
        cameraDevice?.close()
        cameraDevice = null
        cameraHandler?.looper?.quitSafely()
        cameraHandler = null
        if (resetSurface) {
            surface = null
            requestBuilder = null
        }
        isPrepared = false
        isRunning = false
    }

    override fun onOpened(camera: CameraDevice) {
        cameraDevice = camera
        startPreview(camera)
        semaphore.release()
        ADLogUtil.logD(TAG,"Camera Opened")
    }

    override fun onDisconnected(camera: CameraDevice) {
        camera.close()
        semaphore.release()
        ADLogUtil.logD(TAG, "Camera disconnected")
    }

    override fun onError(camera: CameraDevice, error: Int) {
        camera.close()
        semaphore.release()
        cameraCallback?.onCameraError("Open camera failed")
        ADLogUtil.logE(TAG, "Camera open failed: $error")
    }

    private fun getCameraIdFromFacing(facing: Facing): String {
        val selectFacing = when (facing) {
            Facing.BACK -> CameraCharacteristics.LENS_FACING_BACK
            Facing.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
        }
        for (cameraId in cameraManager.cameraIdList) {
            val cc = getCameraCharacteristics(cameraId)
            if (cc!=null && cc.get(CameraCharacteristics.LENS_FACING) == selectFacing
                && cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.getOutputSizes(SurfaceTexture::class.java)!=null) {
                return cameraId
            }
        }
        ADLogUtil.logE(TAG, "Not found cameraId by facing:$facing")
        return ""
    }

    private fun getCameraFacingFromId(cameraId: String): Facing {
        val characteristics = getCameraCharacteristics(cameraId)
        return when(characteristics?.get(CameraCharacteristics.LENS_FACING)) {
            CameraCharacteristics.LENS_FACING_BACK -> Facing.BACK
            CameraCharacteristics.LENS_FACING_FRONT -> Facing.FRONT
            else -> Facing.BACK
        }
    }

}