package me.magi.media.video

import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.CameraAccessException.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.*
import android.util.Log
import android.view.Surface
import me.magi.media.utils.ADAppUtil
import me.magi.media.video.ADCameraConstant.*


object ADCameraManager {
    private val TAG = this::class.simpleName

    private val manager by lazy { ADAppUtil.cameraManager }
    private val mHandlerThread by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HandlerThread("CameraThread", Process.THREAD_PRIORITY_VIDEO)
        } else {
            HandlerThread("CameraThread")
        }
    }
    private val mCameraHandler by lazy { Handler(mHandlerThread.looper) }
    private var mFrontCameraIds = mutableListOf<String>()
    private var mBackCameraIds = mutableListOf<String>()
    private var mCameraInfoMap = hashMapOf<String, ADCameraInfo>()
    // 当前正在使用的摄像头
    private var mCameraDevice: CameraDevice? = null
    // 当前摄像头开启的会话
    private var mCameraSession: CameraCaptureSession? = null
    // 摄像头的录制强求
    private var mCaptureRequest: CaptureRequest? = null
    // 对外的回调
    private var mCallback: ADCameraCallback? = null
    // 外部的预览输出缓冲区
    private var mPreviewSurface: Surface? = null
    // 图像处理的输出缓冲区
    private var mImageReaderSurface: Surface? = null
    // 图像采样器
    private var mImageReader: ImageReader? = null


    init {
        val cameraIdList = manager.cameraIdList
        Log.d(TAG, "cameraDeviceCount: ${cameraIdList.size}")
        for (cameraId in cameraIdList) {
            val cameraCharacteristics = manager.getCameraCharacteristics(cameraId)
            // 如果摄像头不支持YUV_420_888，也排除掉
            cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.getOutputSizes(ImageFormat.YUV_420_888)?:continue
            // 获取前置后置摄像头, 摄像头会有多个, 可以用列表保存, 方便切换
            when (cameraCharacteristics[CameraCharacteristics.LENS_FACING]) {
                CameraCharacteristics.LENS_FACING_FRONT -> {
                    mFrontCameraIds.add(cameraId)
                    mCameraInfoMap[cameraId] = ADCameraInfo(cameraId)
                }
                CameraCharacteristics.LENS_FACING_BACK -> {
                    mBackCameraIds.add(cameraId)
                    mCameraInfoMap[cameraId] = ADCameraInfo(cameraId)
                }
            }
        }
    }

    fun setCameraCallback(callback: ADCameraCallback) {
        mCallback = callback
    }

    fun setPreviewSurface(previewSurface: Surface?) {
        mPreviewSurface = previewSurface
    }

    fun getFrontCameraCount() = mFrontCameraIds.size

    fun getBackCameraCount() = mBackCameraIds.size

    fun openCamera(@ADCameraConstant.ADFacingDef cameraFacing: Int, index: Int = 0) {
        val previewSurface = mPreviewSurface
        if (previewSurface == null || !previewSurface.isValid) {
            return
        }

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
        try {
            manager.openCamera(cameraId, mCameraStateCallback, mCameraHandler)
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

    fun closeCamera() {
        mCameraDevice?.close()
        mCameraDevice = null
    }

    private fun getImageReader(width: Int, height: Int): ImageReader {
        return ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2)
    }

    private val mCameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "camera ${camera.id} onOpened")
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
            Log.e(TAG, "camera ${camera.id} onDisconnected")
            camera.close()
            mCameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(TAG, "camera ${camera.id} onError,errorCode: $error")
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
            Log.d(TAG, "camera ${camera.id} onClosed")
            mCaptureRequest = null
            mCameraSession?.close()
            mImageReaderSurface?.release()
            mImageReader?.close()
            mImageReaderSurface = null
            mImageReader = null
            super.onClosed(camera)
            mCameraDevice = null
        }
    }

    private val mSessionStateCallback = object : CameraCaptureSession.StateCallback(){

        override fun onConfigured(session: CameraCaptureSession) {
            mCameraSession = session
            val cameraDevice = session.device
            val previewSurface = mPreviewSurface?:return
            val requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            requestBuilder.addTarget(previewSurface)

            requestBuilder.build()



        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            mCallback?.onError(ERROR_SESSION_CONFIGURE_FAILED, "this session configure failed")
            mCameraDevice?.close()
        }

        override fun onClosed(session: CameraCaptureSession) {
            super.onClosed(session)
            mCaptureRequest = null
        }

    }

}