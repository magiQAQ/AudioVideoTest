package me.magi.media.utils;

import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class ADLiveConstant {

    // 前置摄像头
    public static final int CAMERA_FACING_FRONT = 0;
    // 后置摄像头
    public static final int CAMERA_FACING_BACK = 1;
    @IntDef({CAMERA_FACING_FRONT, CAMERA_FACING_BACK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ADFacingDef {}

    // 摄像头分辨率
    public static final int RESOLUTION_640_360 = 5;
    public static final int RESOLUTION_1280_720 = 6;
    public static final int RESOLUTION_1920_1080 = 7;
    @IntDef({
            RESOLUTION_640_360,
            RESOLUTION_1280_720,
            RESOLUTION_1920_1080
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ADResolutionDef{}

    // 画面预览模式
    public static final int MODE_FIT_XY = 0;
    public static final int MODE_INSIDE = 1;
    public static final int MODE_OUTSIDE = 2;
    @IntDef({
            MODE_FIT_XY,
            MODE_INSIDE,
            MODE_OUTSIDE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ADPreviewModeDef{}

    // 横屏还是竖屏
    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;
    @IntDef({
            ORIENTATION_PORTRAIT,
            ORIENTATION_LANDSCAPE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ADOrientationDef{}

    // 声道
    public static final int AUDIO_CHANNEL_MONO = 1;
    public static final int AUDIO_CHANNEL_STEREO = 2;
    @IntDef({
            AUDIO_CHANNEL_MONO,
            AUDIO_CHANNEL_STEREO
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ADAudioChannelDef{}

    public static final int FLAG_DIRECTION_FLIP_HORIZONTAL = 0x01;
    public static final int FLAG_DIRECTION_FLIP_VERTICAL = 0x02;
    public static final int FLAG_DIRECTION_ROTATION_0 = 0x10;
    public static final int FLAG_DIRECTION_ROTATION_90 = 0x20;
    public static final int FLAG_DIRECTION_ROTATION_180 = 0x40;
    public static final int FLAG_DIRECTION_ROTATION_270 = 0x80;

    public static final int ERROR_UNKNOWN = -1000;
    public static final int ERROR_CAMERA_DISABLED = -1001;
    public static final int ERROR_CAMERA_DISCONNECTED = -1002;
    public static final int ERROR_CAMERA_WRONG_STATUS = -1003;
    public static final int ERROR_CAMERA_IN_USE = -1004;
    public static final int ERROR_CAMERA_MAX_USE_COUNT = -1005;
    public static final int ERROR_NO_THIS_CAMERA = -1006;
    public static final int ERROR_NO_PERMISSION = -1007;
    public static final int ERROR_CAMERA_DEVICE = -1008;
    public static final int ERROR_CAMERA_SERVICE = -1009;
    public static final int ERROR_SESSION_CONFIGURE_FAILED = -1010;
    public static final int ERROR_CAMERA_NOT_SUPPORT_RECORD = -1011;
    public static final int ERROR_CAMERA_CLOSED = -1012;
    public static final int ERROR_SESSION_INVALID = -1013;
    public static final int ERROR_SURFACE_INVALID = -1014;


}
