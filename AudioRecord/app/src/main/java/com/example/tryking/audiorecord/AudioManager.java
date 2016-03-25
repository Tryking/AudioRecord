package com.example.tryking.audiorecord;

import android.media.MediaRecorder;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by Tryking on 2016/3/24.
 */
public class AudioManager {
    private String mDirString;

    private boolean isPrepared;//Audio是否准备好
    private String mCurrentFilePath;
    private MediaRecorder mRecorder;


    /**
     * 单例模式：获得AudioManager实例
     *
     * @param dir 存储路径
     * @return 返回实例
     */
    private static AudioManager mInstance;

    private AudioManager(String dir) {
        mDirString = dir;
    }

    public static AudioManager getInstance(String dir) {
        if (mInstance == null) {
            synchronized (AudioManager.class) {
                if (mInstance == null) {
                    mInstance = new AudioManager(dir);
                }
            }
        }
        return mInstance;
    }

    /**
     * 回调函数：准备完毕，准备好后，需要做的事：比如录音button显示
     */
    public interface AudioStageListener {
        void wellPrepared();
    }

    private AudioStageListener mListener;

    public void setOnAudioStageListener(AudioStageListener listener) {
        mListener = listener;
    }

    /**
     * 准备Audio
     */
    public void prepareAudio() {
        try {
            isPrepared = false;

            // path - the path to be used for the file.
            File dir = new File(mDirString);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = generalFileName();
            File file = new File(dir, fileName);

            mCurrentFilePath = file.getAbsolutePath();

            mRecorder = new MediaRecorder();
            //设置声音来源
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //设置音频输出格式（必须在设置声音编码格式之前设置，否则抛出IllegalStateException异常）
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            //设置编码格式
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            //设置输出文件
            mRecorder.setOutputFile(mCurrentFilePath);
            mRecorder.prepare();
            mRecorder.start();

            isPrepared = true;
            //已经准备好，可以开始录制了
            if (mListener != null) {
                mListener.wellPrepared();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
     随机生成文件的名称
     */
    private String generalFileName() {
        return UUID.randomUUID().toString() + ".amr";
    }

    /**
     * 获取声音的等级
     *
     * @param maxLevel 自己设定的最大值
     * @return
     */
    public int getVoiceLevel(int maxLevel) {
        if (isPrepared) {
            try {
                // mRecorder.getMaxAmplitude()，值域是1-32767
                // 不加1取不到最大值
                return maxLevel * mRecorder.getMaxAmplitude() / 32768 + 1;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 1;
    }

    /**
     * 释放资源
     */
    public void release() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    /**
     * 取消，因为prepare产生了一个文件，需要删除文件
     */
    public void cancel() {
        release();
        if (mCurrentFilePath != null) {
            File file = new File(mCurrentFilePath);
            file.delete();
            mCurrentFilePath = null;
        }
    }

    /**
     * 获取录音文件存储路径
     *
     * @return
     */
    public String getCurrentFilePath() {
        return mCurrentFilePath;
    }


}
