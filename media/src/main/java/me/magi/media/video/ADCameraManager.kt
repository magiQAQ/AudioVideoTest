package me.magi.media.video

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import me.magi.media.utils.getApp
import java.lang.RuntimeException


object ADCameraManager {
    private val TAG = this::class.simpleName

    private val manager by lazy { getApp().getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var mFrontCameraId: String? = null
    private var mFrontCameraCharacteristics: CameraCharacteristics? = null
    private var mBackCameraId: String? = null
    private var mBackCameraCharacteristics: CameraCharacteristics? = null
    private var mCameraDevice: CameraDevice? = null

    private var mCallback: ADCameraCallback? = null

    init {
        val cameraIdList = manager.cameraIdList
        Log.d(TAG, "cameraDeviceCount: ${cameraIdList.size}")
        cameraIdList.forEach { cameraId ->
            val cameraCharacteristics = manager.getCameraCharacteristics(cameraId)
            if (cameraCharacteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_BACK) {
                mBackCameraId = cameraId
                mBackCameraCharacteristics = cameraCharacteristics
            } else if (cameraCharacteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_FRONT) {
                mFrontCameraId = cameraId
                mFrontCameraCharacteristics = cameraCharacteristics
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera() {
        // 优先开启后置摄像头
        val cameraId = mBackCameraId ?: mFrontCameraId
        if (cameraId != null) {
            manager.openCamera(cameraId, CameraStateCallback(), handler)
        } else {
            throw RuntimeException("没有找到可用的摄像头")
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