package me.magi.media;

import me.magi.media.utils.ADLiveConstant;
import me.magi.media.utils.ADLiveConstant.*;
import me.magi.media.video.ADCameraController;

import static me.magi.media.utils.ADLiveConstant.*;

public class ADLiveConfig {
    private int audioSample = 48000;
    private int audioChannel = AUDIO_CHANNEL_MONO;

    private int cameraFacing = CAMERA_FACING_BACK;
    private int cameraIndex = 0;
    private int videoResolution = RESOLUTION_1280_720;
    private int videoFPS = 30;
    private int videoBitrate = 4000;
    private int videoEncodeGOP = 3;
    private int orientation = ORIENTATION_LANDSCAPE;

    /**
     * @return 获取音频采样率
     * @see #setAudioSample(int)
     */
    public int getAudioSample() {
        return audioSample;
    }

    /**
     * 设置音频采样率
     * @param audioSample 采样率 一般采用44100或者48000
     */
    public void setAudioSample(int audioSample) {
        this.audioSample = audioSample;
    }

    /**
     * @return 获取声道数
     * @see #setAudioChannel(int)
     */
    public int getAudioChannel() {
        return audioChannel;
    }

    /**
     * 设置声道数
     * @param audioChannel 声道数
     *                     单声道 {@link ADLiveConstant#AUDIO_CHANNEL_MONO}
     *                     双声道 {@link ADLiveConstant#AUDIO_CHANNEL_STEREO}
     */
    public void setAudioChannel(@ADAudioChannelDef int audioChannel) {
        this.audioChannel = audioChannel;
    }

    /**
     * @return 获取摄像头朝向
     * @see #setCameraFacing(int)
     */
    public int getCameraFacing() {
        return cameraFacing;
    }

    /**
     * 设置摄像头朝向
     * @param cameraFacing 摄像头朝向
     *                     前置摄像头 {@link ADLiveConstant#CAMERA_FACING_FRONT}
     *                     后置摄像头 {@link ADLiveConstant#CAMERA_FACING_BACK}
     */
    public void setCameraFacing(@ADFacingDef int cameraFacing) {
        this.cameraFacing = cameraFacing;
    }

    /**
     * 获得在当前摄像头朝向下,设置的摄像头在前置摄像头列表或后置摄像头列表中的下角标
     * @return 摄像头下角标
     * @see #setCameraIndex(int)
     */
    public int getCameraIndex() {
        return cameraIndex;
    }

    /**
     * 设置在选择的摄像头朝向下,相关摄像头列表中的下角标
     * @param cameraIndex 如果当前选择前置摄像头,则取值应为 0~{@link #getFrontCameraCount() - 1},
     *                    如果当前选择后置摄像头,则取值应为 0~{@link #getBackCameraCount() - 1}
     */
    public void setCameraIndex(int cameraIndex) {
        this.cameraIndex = cameraIndex;
    }

    /**
     * @return 设置的屏幕分辨率代表的常量
     */
    public int getVideoResolution() {
        return videoResolution;
    }

    /**
     * 设置屏幕分辨率
     * @param videoResolution 屏幕分辨率所代表的常量,无论横竖屏,统一设置为长边*短边
     *                        {@link ADLiveConstant#RESOLUTION_640_360} 640*360
     *                        {@link ADLiveConstant#RESOLUTION_1280_720} 1280*720
     *                        {@link ADLiveConstant#RESOLUTION_1920_1080} 1920*1080
     *                        etc
     */
    public void setVideoResolution(@ADResolutionDef int videoResolution) {
        this.videoResolution = videoResolution;
    }

    /**
     * @return 获取设置的帧数
     */
    public int getVideoFPS() {
        return videoFPS;
    }

    /**
     * 设置直播帧数 每个摄像头有最大的支持帧数,建议设置在0~30,基本绝大部分摄像头都支持
     * 最高不要超过60,如果设置的帧数超过摄像头所支持的最大或最小帧数,则在实际启动时,
     * 会使用摄像头支持的最大或最小帧数
     * @param videoFPS 目标帧数
     */
    public void setVideoFPS(int videoFPS) {
        this.videoFPS = videoFPS;
    }

    /**
     * @return 设置的推流码率 单位(kbps)
     */
    public int getVideoBitrate() {
        return videoBitrate;
    }

    /**
     * 设置推流码率
     * @param videoBitrate 推流码率 单位(kbps)
     */
    public void setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    /**
     * @return 设置的GOP关键帧间隔 单位(s)
     */
    public int getVideoEncodeGOP() {
        return videoEncodeGOP;
    }

    /**
     * 设置关键帧间隔
     * @param videoEncodeGOP 关键帧间隔 单位(s)
     */
    public void setVideoEncodeGOP(int videoEncodeGOP) {
        this.videoEncodeGOP = videoEncodeGOP;
    }

    /**
     * @return 前置摄像头数量
     */
    public int getFrontCameraCount() {
        return ADCameraController.getFrontCameraCount();
    }

    /**
     * @return 后置摄像头数量
     */
    public int getBackCameraCount() {
        return ADCameraController.getBackCameraCount();
    }

    /**
     * @return 当前设置的屏幕方向
     */
    public int getOrientation() {
        return orientation;
    }

    /**
     * 设置当前的屏幕方向
     * @param orientation 屏幕方向的常量
     *                    {@link ADLiveConstant#ORIENTATION_PORTRAIT} 竖屏
     *                    {@link ADLiveConstant#ORIENTATION_LANDSCAPE} 横屏
     */
    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }
}
