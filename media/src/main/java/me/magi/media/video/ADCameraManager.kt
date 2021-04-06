package me.magi.media.video

import android.content.Context
import android.hardware.camera2.*
import android.hardware.camera2.CameraAccessException.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import me.magi.media.utils.getApp
import me.magi.media.video.ADVideoConstant.*


object ADCameraManager {
    private val TAG = this::class.simpleName

    private val manager by lazy { getApp().getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var mFrontCameraIds = mutableListOf<String>()
    private var mBackCameraIds = mutableListOf<String>()
    private var characteristicsKey = hashMapOf<String, List<CameraCharacteristics.Key<*>>>()
    // 当前正在使用的摄像头
    private var mCameraDevice: CameraDevice? = null

    private var mCallback: ADCameraCallback? = null

    init {
        val cameraIdList = manager.cameraIdList
        Log.d(TAG, "cameraDeviceCount: ${cameraIdList.size}")
        cameraIdList.forEach { cameraId ->
            val cameraCharacteristics = manager.getCameraCharacteristics(cameraId)
            // 获取前置后置摄像头, 摄像头会有多个, 可以用列表保存, 方便切换
            when (cameraCharacteristics[CameraCharacteristics.LENS_FACING]) {
                CameraCharacteristics.LENS_FACING_FRONT -> {
                    mFrontCameraIds.add(cameraId)
                    characteristicsKey[cameraId] = cameraCharacteristics.keys
                }
                CameraCharacteristics.LENS_FACING_BACK -> {
                    mBackCameraIds.add(cameraId)
                    characteristicsKey[cameraId] = cameraCharacteristics.keys
                }
            }
        }
    }

    private fun getCameraOrientation(cameraId: String): Int {
        return manager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SENSOR_ORIENTATION)?:-1
    }

    fun openCamera(@ADVideoConstant.ADFacingDef cameraFacing: Int, index: Int = 0) {
        // 优先开启后置摄像头
        val cameraId = if (cameraFacing == CAMERA_FACING_FRONT) {
            mFrontCameraIds.getOrNull(index)
        } else {
            mBackCameraIds.getOrNull(index)
        }
        if (cameraId == null) {
            mCallback?.onError(ERROR_NO_THIS_CAMERA,"no this camera device with cameraFacing: $cameraFacing,index: $index")
            return
        }
        try {
            manager.openCamera(cameraId, CameraStateCallback(), handler)
        } catch (e: CameraAccessException) {
            when(e.reason) {
                CAMERA_DISABLED -> mCallback?.onError(ERROR_CAMERA_DISABLED, "this camera device disable")
                CAMERA_DISCONNECTED -> mCallback?.onError(ERROR_CAMERA_DISCONNECTED,"can not connect this camera device")
                CAMERA_ERROR -> mCallback?.onError(ERROR_CAMERA_WRONG_STATUS, "this camera device in wrong status")
                CAMERA_IN_USE -> mCallback?.onError(ERROR_CAMERA_IN_USE, "this camera in use by other")
                MAX_CAMERAS_IN_USE -> mCallback?.onError(ERROR_CAMERA_MAX_USE_COUNT, "current device not support open together")
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
    }



    private class CameraStateCallback: CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            Log.d(TAG, "相机已开启")
        }

        override fun onDisconnected(camera: CameraDevice) {

        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            mCameraDevice = null
            mCallback?.onError(30, handleError(error))
        }

        private fun handleError(error: Int): String {
            return when (error) {
                else -> ""
            }
        }

        override fun onClosed(camera: CameraDevice) {
            super.onClosed(camera)
            mCameraDevice = null
            Log.d(TAG, "相机已关闭")
        }
    }

}