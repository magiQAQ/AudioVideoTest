package com.magi.adlive.encode.video

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.util.Range
import android.view.Surface
import com.magi.adlive.model.Facing
import com.magi.adlive.util.ADLogUtil
import java.util.concurrent.Semaphore
import kotlin.math.abs

internal class ADCameraController(context: Context) : CameraDevice.StateCallback() {

    private val TAG = "CameraController"

    companion object {

    }

    private var cameraDevice: CameraDevice? = null
    private lateinit var surface: Surface
    private val cameraManager: CameraManager = context.getSystemService(CameraManager::class.java)
    private var cameraHandler: Handler? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    var isPrepared = false
        private set
    private var cameraId = "0"
    private var facing = Facing.BACK
    private var requestBuilder: CaptureRequest.Builder? = null
    private var fingerSpacing = 0
    private var zoomLevel = 0f
    private var flashEnable = false
    private var autoFocusEnable = true
    private var running = false
    private var fps = 30
    private var semaphore = Semaphore(0)
    private var cameraCallback: ADCameraCallback? = null

    fun prepareCamera(surfaceTexture: SurfaceTexture, width: Int, height: Int, fps: Int) {
        surfaceTexture.setDefaultBufferSize(width, height)
        surface = Surface(surfaceTexture)
        this.fps = fps
        isPrepared = true
    }

    fun openLastCamera() {
        openCameraId(cameraId)
    }

    private fun startPreview(cameraDevice: CameraDevice) {
        try {
            val listSurfaces = ArrayList<Surface>()
            listSurfaces.add(surface)

            cameraDevice.createCaptureSession(listSurfaces,object :CameraCaptureSession.StateCallback() {

                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSession = session
                    try {
                        val captureRequest = createCaptureRequest(listSurfaces)
                        captureRequest?.let { session.setRepeatingRequest(it, null, cameraHandler) }
                    } catch (e : CameraAccessException) {
                        ADLogUtil.logE(TAG, "Camera Error", e)
                    } catch (e: IllegalStateException) {
                        reOpenCamera(cameraId)
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
            reOpenCamera(cameraId)
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

    private fun adaptFpsRange() {
        val characteristics = getCameraCharacteristics()
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

    private fun getCameraCharacteristics(): CameraCharacteristics? {
        return try {
            cameraManager.getCameraCharacteristics(cameraId)
        } catch (e: Exception) {
            ADLogUtil.logE(TAG, "Error", e)
            null
        }
    }

    private fun openCameraId(cameraId: String) {

    }

    private fun reOpenCamera(cameraId: String) {

    }
}