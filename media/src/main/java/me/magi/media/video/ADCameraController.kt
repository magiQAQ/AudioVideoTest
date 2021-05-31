package me.magi.media.video

import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraAccessException.*
import android.os.*
import android.util.Range
import android.util.Size
import android.view.Surface
import me.magi.media.utils.ADAppUtil
import me.magi.media.utils.ADLiveConstant
import me.magi.media.utils.ADLogUtil
import me.magi.media.utils.ADLiveConstant.*
import java.lang.Long.signum
import kotlin.math.abs
import java.util.*

internal class ADCameraController {

    companion object {
        private var mFrontCameraIds = mutableListOf<String>()
        private var mBackCameraIds = mutableListOf<String>()
        private var mCameraInfoMap = hashMapOf<String, ADCameraInfo>()

        init {
            val cameraIdList = ADAppUtil.cameraManager.cameraIdList
            ADLogUtil.d("cameraDeviceCount: ${cameraIdList.size}")
            for (cameraId in cameraIdList) {
                val cameraCharacteristics = ADAppUtil.cameraManager.getCameraCharacteristics(cameraId)
                val cameraInfo = ADCameraInfo(cameraId)
                // 如果摄像头不支持YUV_420_888，也排除掉
                if (cameraInfo.getOutputSize() == null) {
                    continue
                }
                // 获取前置后置摄像头, 摄像头会有多个, 可以用列表保存, 方便切换
                when (cameraCharacteristics[CameraCharacteristics.LENS_FACING]) {
                    CameraCharacteristics.LENS_FACING_FRONT -> {
                        mFrontCameraIds.add(cameraId)
                        mCameraInfoMap[cameraId] = cameraInfo
                    }
                    CameraCharacteristics.LENS_FACING_BACK -> {
                        mBackCameraIds.add(cameraId)
                        mCameraInfoMap[cameraId] = cameraInfo
                    }
                }
            }
        }

        @JvmStatic
        fun getFrontCameraCount() = mFrontCameraIds.size

        @JvmStatic
        fun getBackCameraCount() = mBackCameraIds.size

        internal fun getCameraDirection(cameraId: String): Int {
            return mCameraInfoMap[cameraId]?.getCameraOrientation()?:0
        }

        internal fun getCameraFacing(cameraId: String): Int {
            return mCameraInfoMap[cameraId]?.getCameraFacing()?:CameraCharacteristics.LENS_FACING_EXTERNAL
        }
    }

    // 当前正在使用的摄像头
    private var mCameraDevice: CameraDevice? = null

    // 当前摄像头开启的会话
    private var mCameraSession: CameraCaptureSession? = null

    // 摄像头的录制请求
    private var mCaptureRequestBuilder: CaptureRequest.Builder? = null

    // 对外的回调
    private var mCallback: ADCameraCallback? = null

    // 外部的预览输出缓冲区
    private var mSurface: Surface? = null

    // 尺寸
    private var mOutputSize = Size(1280, 720)

    // 帧数
    private var mOutputFPS = Range(30,30)

    // 当前开启的摄像头Id
    private var currentCameraId: String = mBackCameraIds.getOrNull(0) ?: mFrontCameraIds.getOrNull(0) ?: ""

    // 摄像头线程
    private val mCameraThread: HandlerThread
    private val mCameraHandler: Handler


    internal fun setCameraCallback(callback: ADCameraCallback) {
        mCallback = callback
    }

    init {
        val cameraThread = HandlerThread("CameraThread")
        cameraThread.start()
        val cameraHandler = Handler(cameraThread.looper)
        mCameraThread = cameraThread
        mCameraHandler = cameraHandler
    }

    internal fun release() {
        mCameraHandler.removeCallbacksAndMessages(null)
        mCameraThread.quitSafely()
        mCameraThread.join()
    }

    private fun getCameraId(@ADFacingDef cameraFacing: Int, index: Int = 0): String? {
        return if (cameraFacing == CAMERA_FACING_FRONT) {
            mFrontCameraIds.getOrNull(index)
        } else {
            mBackCameraIds.getOrNull(index)
        }
    }

    private fun getOptimalSize(cameraId: String, targetWidth: Int, targetHeight: Int): Size {
        val nativeSizes = arrayListOf(*mCameraInfoMap[cameraId]!!.getOutputSize()!!)
        nativeSizes.sortWith { o1, o2 ->
            signum(o1.width.toLong() * o1.height - o2.width.toLong() * o2.height)
        }
        for (size in nativeSizes) {
            if (size.width >= targetWidth && size.height >= targetHeight) {
                return size
            }
        }
        return nativeSizes.last()
    }

    private fun getOptimalFpsRange(cameraId: String, targetFps: Int): Range<Int>{
        val nativeFps = arrayListOf(*mCameraInfoMap[cameraId]!!.getFpsRanges())
        nativeFps.sortWith { o1, o2 ->
            val r = abs(o1.lower - targetFps) + abs(o1.upper - targetFps)
            val l = abs(o2.lower - targetFps) + abs(o2.upper - targetFps)
            r.compareTo(l)
        }
        return nativeFps[0]
    }

    private fun getCameraInfo(cameraId: String): ADCameraInfo? {
        return mCameraInfoMap[cameraId]
    }

    private val mCameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            ADLogUtil.d("camera ${camera.id} onOpened")
            mCameraDevice = camera
            @Suppress("DEPRECATION")camera.createCaptureSession(listOf(mSurface), mSessionStateCallback, mCameraHandler)
        }

        override fun onDisconnected(camera: CameraDevice) {
            ADLogUtil.d("camera ${camera.id} onDisconnected")
            camera.close()
            mCameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            ADLogUtil.d("camera ${camera.id} onError,errorCode: $error")
            camera.close()
            mCameraDevice = null
            when (error) {
                ERROR_CAMERA_IN_USE -> mCallback?.onError(
                    ADLiveConstant.ERROR_CAMERA_IN_USE,
                    "this camera in use by other"
                )
                ERROR_MAX_CAMERAS_IN_USE -> mCallback?.onError(
                    ERROR_CAMERA_MAX_USE_COUNT,
                    "current device not support open together"
                )
                ERROR_CAMERA_DISABLED -> mCallback?.onError(
                    ADLiveConstant.ERROR_CAMERA_DISABLED,
                    "this camera device disable"
                )
                ERROR_CAMERA_DEVICE -> mCallback?.onError(
                    ADLiveConstant.ERROR_CAMERA_DEVICE,
                    "camera device error, maybe need reopen"
                )
                ERROR_CAMERA_SERVICE -> mCallback?.onError(
                    ADLiveConstant.ERROR_CAMERA_SERVICE,
                    "camera service error, maybe need reboot Android device"
                )
                else -> mCallback?.onError(ERROR_UNKNOWN, "appear unknown error with open camera")
            }
        }

        override fun onClosed(camera: CameraDevice) {
            super.onClosed(camera)
            try {
                mCameraSession?.stopRepeating()
                mCameraSession?.close()

            } catch (e: Exception) {
                e.printStackTrace()
            }
            mCameraSession = null
            ADLogUtil.d("camera ${camera.id} onClosed")
            mCameraDevice = null
        }
    }

    private val mSessionStateCallback = object : CameraCaptureSession.StateCallback() {

        override fun onConfigured(session: CameraCaptureSession) {
            ADLogUtil.d("camera session onConfigured")
            mCameraSession = session
            val cameraDevice = session.device
            val surface = mSurface?:return
            try {
                val requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                // 运行时参数
                requestBuilder.addTarget(surface)
                // 自动聚焦
                requestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                )
                // 自动曝光，但是不自动开启闪光灯
                requestBuilder.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
                // 设置帧数
                requestBuilder.set(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    mOutputFPS
                )
                mCaptureRequestBuilder = requestBuilder
                ADLogUtil.d("camera requestBuilder generated")
            } catch (e: Exception) {
                when (e) {
                    is IllegalArgumentException -> mCallback?.onError(
                        ERROR_CAMERA_NOT_SUPPORT_RECORD,
                        "this camera not support record"
                    )
                    is CameraAccessException -> mCallback?.onError(
                        ERROR_CAMERA_DISCONNECTED,
                        "camera disconnect"
                    )
                    is IllegalStateException -> mCallback?.onError(
                        ERROR_CAMERA_CLOSED,
                        "camera has closed"
                    )
                }
            }
            val request = mCaptureRequestBuilder?.build()?:return
            try{
                session.setRepeatingRequest(request, null, null)
                ADLogUtil.d("camera request build")
            } catch (e: Exception) {
                when (e) {
                    is CameraAccessException -> mCallback?.onError(
                        ERROR_CAMERA_DISCONNECTED,
                        "camera disconnect"
                    )
                    is IllegalStateException -> mCallback?.onError(
                        ERROR_SESSION_INVALID,
                        "camera session invalid"
                    )
                    is IllegalArgumentException -> mCallback?.onError(
                        ERROR_SURFACE_INVALID,
                        "surface not config or invalid"
                    )
                }
            }
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            ADLogUtil.d("camera session onConfigureFailed")
            session.device.close()
            mCameraSession = null
            mCaptureRequestBuilder = null
            mCallback?.onError(ERROR_SESSION_CONFIGURE_FAILED, "this session configure failed")
        }

        override fun onClosed(session: CameraCaptureSession) {
            ADLogUtil.d("camera session onClosed")
            super.onClosed(session)
            mCaptureRequestBuilder = null
            mCameraSession = null
        }

    }

    internal fun setCamera(@ADLiveConstant.ADFacingDef cameraFacing: Int, index: Int = 0) {
        val cameraId = getCameraId(cameraFacing, index)
        if (cameraId == null) {
            mCallback?.onError(
                ERROR_NO_THIS_CAMERA,
                "no this camera device with cameraFacing: $cameraFacing,index: $index"
            )
            return
        }
        currentCameraId = cameraId
    }

    internal fun setSize(width: Int, height: Int):Size {
        val optimalSize = getOptimalSize(currentCameraId, width, height)
        mOutputSize = optimalSize
        return Size(optimalSize.width, optimalSize.height)
    }

    internal fun setSurfaceTexture(surfaceTexture: SurfaceTexture) {
        surfaceTexture.setDefaultBufferSize(mOutputSize.width, mOutputSize.height)
        mSurface = Surface(surfaceTexture)
    }

    internal fun setFps(targetFPS: Int):Range<Int> {
        val optimalFps = getOptimalFpsRange(currentCameraId, targetFPS)
        mOutputFPS = optimalFps
        return Range(optimalFps.lower, optimalFps.upper)
    }

    internal fun openCamera() {
        mCameraHandler.post {
            try {
                ADAppUtil.cameraManager.openCamera(currentCameraId, mCameraStateCallback, mCameraHandler)
            } catch (e: CameraAccessException) {
                when (e.reason) {
                    CAMERA_DISABLED -> mCallback?.onError(
                        ERROR_CAMERA_DISABLED,
                        "this camera device disable"
                    )
                    CAMERA_DISCONNECTED -> mCallback?.onError(
                        ERROR_CAMERA_DISCONNECTED,
                        "can not connect this camera device"
                    )
                    CAMERA_ERROR -> mCallback?.onError(
                        ERROR_CAMERA_WRONG_STATUS,
                        "this camera device in wrong status"
                    )
                    CAMERA_IN_USE -> mCallback?.onError(
                        ERROR_CAMERA_IN_USE,
                        "this camera in use by other"
                    )
                    MAX_CAMERAS_IN_USE -> mCallback?.onError(
                        ERROR_CAMERA_MAX_USE_COUNT,
                        "current device not support open together"
                    )
                    else -> mCallback?.onError(
                        ERROR_UNKNOWN,
                        "appear unknown error with open camera"
                    )
                }
            } catch (e: IllegalArgumentException) {
                mCallback?.onError(ERROR_NO_THIS_CAMERA, "this cameraId not in cameraIdsList")
            } catch (e: SecurityException) {
                mCallback?.onError(ERROR_NO_PERMISSION, "application has not camera permission")
            }
        }
    }

    internal fun closeCamera() {
        mCameraHandler.post {
            mCameraSession?.stopRepeating()
            mCameraSession?.close()
            mCameraSession = null
            mCameraDevice?.close()
            mCameraDevice = null
        }
    }

    /**
     * 录制设置自动对焦
     *
     * @param state 是否开启自动对焦
     */
    fun setAutoFocus(state: Boolean) {
        mCameraHandler.post {
            val requestBuilder = mCaptureRequestBuilder
            val session = mCameraSession
            if (requestBuilder == null || session == null) return@post
            ADLogUtil.d("camera ${if (state) "open" else "close"} AF")
            requestBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                if (state) CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                else CaptureRequest.CONTROL_AF_MODE_OFF
            )
            try {
                session.stopRepeating()
                session.setRepeatingRequest(requestBuilder.build(), null, null)
            } catch (e: Exception) {
                when (e) {
                    is CameraAccessException -> mCallback?.onError(
                        ERROR_CAMERA_DISCONNECTED,
                        "camera disconnect"
                    )
                    is IllegalStateException -> mCallback?.onError(
                        ERROR_SESSION_INVALID,
                        "camera session invalid"
                    )
                    is IllegalArgumentException -> mCallback?.onError(
                        ERROR_SURFACE_INVALID,
                        "surface not config or invalid"
                    )
                }
            }
        }
    }

    /**
     * 录制开关闪光灯
     *
     * @param state 是否开启闪光灯
     */
    fun setFlashState(state: Boolean) {
        mCameraHandler.post {
            val requestBuilder = mCaptureRequestBuilder
            val session = mCameraSession
            if (requestBuilder == null || session == null) return@post
            // 检查闪光灯是否可用
            val cameraInfo = getCameraInfo(session.device.id)
            if (cameraInfo?.isSupportFlash() != true) return@post
            ADLogUtil.d("camera ${if (state) "open" else "close"} FlashLight")
            requestBuilder.set(
                CaptureRequest.FLASH_MODE,
                if (state) CaptureRequest.FLASH_MODE_TORCH
                else CaptureRequest.FLASH_MODE_OFF
            )
            try {
                session.stopRepeating()
                session.setRepeatingRequest(requestBuilder.build(), null, null)
            } catch (e: Exception) {
                when (e) {
                    is CameraAccessException -> mCallback?.onError(
                        ERROR_CAMERA_DISCONNECTED,
                        "camera disconnect"
                    )
                    is IllegalStateException -> mCallback?.onError(
                        ERROR_SESSION_INVALID,
                        "camera session invalid"
                    )
                    is IllegalArgumentException -> mCallback?.onError(
                        ERROR_SURFACE_INVALID,
                        "surface not config or invalid"
                    )
                }
            }
        }
    }

}