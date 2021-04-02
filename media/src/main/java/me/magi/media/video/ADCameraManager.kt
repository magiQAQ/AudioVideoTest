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

    @SuppressLint("MissingPermission")
    fun openCamera() {
        // 优先开启后置摄像头
        if (mBackCameraIds.isEmpty() && mFrontCameraIds.isEmpty()) {
            throw RuntimeException("没有找到可用的摄像头")
        }
        val cameraId = mBackCameraIds.getOrNull(0) ?: mFrontCameraIds[0]
        manager.openCamera(cameraId, CameraStateCallback(), handler)
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