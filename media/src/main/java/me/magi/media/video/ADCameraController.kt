package me.magi.media.video

import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.CameraAccessException.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.*
import android.util.Size
import android.view.Surface
import me.magi.media.utils.ADAppUtil
import me.magi.media.utils.ADLogUtil
import me.magi.media.video.ADCameraConstant.*
import java.util.*


internal object ADCameraController {

    private val mHandlerThread by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HandlerThread("CameraThread", Process.THREAD_PRIORITY_VIDEO)
        } else {
            HandlerThread("CameraThread")
        }
    }
    private val mCameraHandler by lazy {
        mHandlerThread.start()
        Handler(mHandlerThread.looper)
    }
    private var mFrontCameraIds = mutableListOf<String>()
    private var mBackCameraIds = mutableListOf<String>()
    private var mCameraInfoMap = hashMapOf<String, ADCameraInfo>()
    // 当前正在使用的摄像头
    private var mCameraDevice: CameraDevice? = null
    // 当前摄像头开启的会话
    private var mCameraSession: CameraCaptureSession? = null
    // 摄像头的录制请求
    private var mCaptureRequestBuilder: CaptureRequest.Builder? = null
    // 对外的回调
    private var mCallback: ADCameraCallback? = null
    // 外部的预览输出缓冲区
    private var mPreviewSurface: Surface? = null
    // 图像采样器
    private var mImageReader: ImageReader? = null

    private var outputSize = Size(1280, 720)


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

    fun setCameraCallback(callback: ADCameraCallback) {
        mCallback = callback
    }

    fun getFrontCameraCount() = mFrontCameraIds.size

    fun getBackCameraCount() = mBackCameraIds.size

    fun openCamera(@ADCameraConstant.ADFacingDef cameraFacing: Int, index: Int = 0, previewWidth: Int, previewHeight: Int) {
        // 优先开启后置摄像头
        val cameraId = if (cameraFacing == CAMERA_FACING_FRONT) {
            mFrontCameraIds.getOrNull(index)
        } else {
            mBackCameraIds.getOrNull(index)
        }
        if (cameraId == null) {
            mCallback?.onError(
                ERROR_NO_THIS_CAMERA,
                "no this camera device with cameraFacing: $cameraFacing,index: $index"
            )
            return
        }
        mCameraHandler.post {
            setupCameraOutput(cameraId, outputSize.width, outputSize.height)
            try {
                ADAppUtil.cameraManager.openCamera(cameraId, mCameraStateCallback, mCameraHandler)
            } catch (e: CameraAccessException) {
                when(e.reason) {
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
                    else -> mCallback?.onError(ERROR_UNKNOWN, "appear unknown error with open camera")
                }
            } catch (e: IllegalArgumentException) {
                mCallback?.onError(ERROR_NO_THIS_CAMERA, "this cameraId not in cameraIdsList")
            } catch (e: SecurityException) {
                mCallback?.onError(ERROR_NO_PERMISSION, "application has not camera permission")
            }
        }
    }

    fun closeCamera() {
        mCameraHandler.post {
            mCameraSession?.stopRepeating()
            mCameraDevice?.close()
            mCameraDevice = null
        }
    }

    private fun setupCameraOutput(cameraId: String, width: Int, height: Int) {
        val cameraInfo = getCameraInfo(cameraId)?:return
        val sizeArray = cameraInfo.getOutputSize()?:return

    }

    private fun getImageReader(width: Int, height: Int): ImageReader {
        return ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2)
    }

    private fun getCameraInfo(cameraId: String): ADCameraInfo? {
        return mCameraInfoMap[cameraId]
    }

    private val mCameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            ADLogUtil.d("camera ${camera.id} onOpened")
            mCameraDevice = camera
            val previewSurface = mPreviewSurface
            if (previewSurface == null) {
                camera.close()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val outputConfig = OutputConfiguration(previewSurface)
                    val sessionConfiguration = SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR, listOf(
                            outputConfig
                        ), ADAppUtil.executor, mSessionStateCallback
                    )
                    camera.createCaptureSession(sessionConfiguration)
                } else {
                    camera.createCaptureSession(
                        listOf(previewSurface),
                        mSessionStateCallback,
                        mCameraHandler
                    )
                }
            }
//            mImageReader = getImageReader(1280, 720)
//            mImageReaderSurface = mImageReader?.surface

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
            when(error) {
                ERROR_CAMERA_IN_USE -> mCallback?.onError(
                    ADCameraConstant.ERROR_CAMERA_IN_USE,
                    "this camera in use by other"
                )
                ERROR_MAX_CAMERAS_IN_USE -> mCallback?.onError(
                    ERROR_CAMERA_MAX_USE_COUNT,
                    "current device not support open together"
                )
                ERROR_CAMERA_DISABLED -> mCallback?.onError(
                    ADCameraConstant.ERROR_CAMERA_DISABLED,
                    "this camera device disable"
                )
                ERROR_CAMERA_DEVICE -> mCallback?.onError(
                    ADCameraConstant.ERROR_CAMERA_DEVICE,
                    "camera device error, maybe need reopen"
                )
                ERROR_CAMERA_SERVICE -> mCallback?.onError(
                    ADCameraConstant.ERROR_CAMERA_SERVICE,
                    "camera service error, maybe need reboot Android device"
                )
                else -> mCallback?.onError(ERROR_UNKNOWN, "appear unknown error with open camera")
            }
        }

        override fun onClosed(camera: CameraDevice) {
            super.onClosed(camera)
            ADLogUtil.d("camera ${camera.id} onClosed")
            mImageReader?.close()
            mImageReader = null
            mCameraDevice = null
        }
    }

    private val mSessionStateCallback = object : CameraCaptureSession.StateCallback(){

        override fun onConfigured(session: CameraCaptureSession) {
            ADLogUtil.d("camera session onConfigured")
            mCameraSession = session
            val cameraDevice = session.device
            val previewSurface = mPreviewSurface?:return
            try {
                val requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                // 运行时参数
                requestBuilder.addTarget(previewSurface)
                // 自动聚焦
                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                // 自动曝光，但是不自动开启闪光灯
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                mCaptureRequestBuilder = requestBuilder
                ADLogUtil.d("camera requestBuilder generated")
            } catch (e: Exception) {
                when (e) {
                    is IllegalArgumentException -> mCallback?.onError(ERROR_CAMERA_NOT_SUPPORT_RECORD, "this camera not support record")
                    is CameraAccessException -> mCallback?.onError(ERROR_CAMERA_DISCONNECTED, "camera disconnect")
                    is IllegalStateException -> mCallback?.onError(ERROR_CAMERA_CLOSED, "camera has closed")
                }
            }
            try {
                mCaptureRequestBuilder?.let { session.setRepeatingRequest(it.build(), null, null) }
                ADLogUtil.d("camera request build")
            } catch (e: Exception) {
                when (e) {
                    is CameraAccessException -> mCallback?.onError(ERROR_CAMERA_DISCONNECTED, "camera disconnect")
                    is IllegalStateException -> mCallback?.onError(ERROR_SESSION_INVALID, "camera session invalid")
                    is IllegalArgumentException -> mCallback?.onError(ERROR_SURFACE_INVALID, "surface not config or invalid")
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

    /**
     * 录制设置自动对焦
     *
     * @param state 是否开启自动对焦
     */
    fun setAutoFocus(state: Boolean) {
        val requestBuilder = mCaptureRequestBuilder
        val session = mCameraSession
        if (requestBuilder == null || session == null) return
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
                is CameraAccessException -> mCallback?.onError(ERROR_CAMERA_DISCONNECTED, "camera disconnect")
                is IllegalStateException -> mCallback?.onError(ERROR_SESSION_INVALID, "camera session invalid")
                is IllegalArgumentException -> mCallback?.onError(ERROR_SURFACE_INVALID, "surface not config or invalid")
            }
        }
    }

    /**
     * 录制开关闪光灯
     *
     * @param state 是否开启闪光灯
     */
    fun setFlashState(state: Boolean) {
        val requestBuilder = mCaptureRequestBuilder
        val session = mCameraSession
        if (requestBuilder == null || session == null) return
        // 检查闪光灯是否可用
        val cameraInfo = getCameraInfo(session.device.id)
        if (cameraInfo?.isSupportFlash() != true) return
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
                is CameraAccessException -> mCallback?.onError(ERROR_CAMERA_DISCONNECTED, "camera disconnect")
                is IllegalStateException -> mCallback?.onError(ERROR_SESSION_INVALID, "camera session invalid")
                is IllegalArgumentException -> mCallback?.onError(ERROR_SURFACE_INVALID, "surface not config or invalid")
            }
        }
    }

}