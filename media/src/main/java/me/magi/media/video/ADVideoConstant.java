package me.magi.media.video;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class ADVideoConstant {

    // 前置摄像头
    public static final int CAMERA_FACING_FRONT = 0;
    // 后置摄像头
    public static final int CAMERA_FACING_BACK = 1;

    public static final int ERROR_UNKNOWN = -1000;
    public static final int ERROR_CAMERA_DISABLED = -1001;
    public static final int ERROR_CAMERA_DISCONNECTED = -1002;
    public static final int ERROR_CAMERA_WRONG_STATUS = -1003;
    public static final int ERROR_CAMERA_IN_USE = -1004;
    public static final int ERROR_CAMERA_MAX_USE_COUNT = -1005;
    public static final int ERROR_NO_THIS_CAMERA = -1006;
    public static final int ERROR_NO_PERMISSION = -1007;



    @IntDef({CAMERA_FACING_FRONT, CAMERA_FACING_BACK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ADFacingDef{}

}
