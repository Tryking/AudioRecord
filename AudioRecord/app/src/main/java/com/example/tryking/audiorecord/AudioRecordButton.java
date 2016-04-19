package com.mmbao.maibei.widget.record;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.mmbao.maibei.R;
import com.mmbao.maibei.global.ApplicationGlobal;

/**
 * Created by Administrator on 2016/4/14.
 */
public class AudioRecordButton extends Button implements AudioManager.AudioStageListener {
    private static final int STATE_NORMAL = 1;
    private static final int STATE_RECORDING = 2;
    private static final int STATE_WANT_TO_CANCEL = 3;
    private static final int DISTANCE_Y_CANCEL = 50;
    private int mCurrentState = STATE_NORMAL;
    // 已经开始录音
    private boolean isRecording = false;
    private DialogManager mDialogManager;
    private AudioManager mAudioManager;
    private float mTime = 0;
    // 是否触发了onLongClick，准备好了
    private boolean mReady;
    private Vibrator vibrator;//振动

    /**
     * 先实现两个参数的构造方法，布局会默认引用这个构造方法， 用一个 构造参数的构造方法来引用这个方法 * @param context
     */
    public AudioRecordButton(Context context) {
        this(context, null);
    }

    public AudioRecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDialogManager = new DialogManager(getContext());

        // 这里没有判断储存卡是否存在，有空要判断
        String dir = ApplicationGlobal.basePath + "MaiBei_record_audios";
        mAudioManager = AudioManager.getInstance(dir);
        mAudioManager.setOnAudioStageListener(this);

        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startRecording();
                return false;
            }
        });
    }

    //开始录音
    private void startRecording() {
        try {
            mAudioManager.startRecording();
            mReady = true;
            vibrator.vibrate(50);
        } catch (Exception e) {
            if (mAudioManager != null) {
                mAudioManager.discardRecording();
                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                        .setTitle("开启录音权限")
                        .setMessage("检测到录音失败，请尝试按以下路径开启录音权限:\n\n" + "方式一：在弹出的权限选择框中勾中以后不再提醒，并选择允许\n\n" +
                                "方式二：360卫士 -> 软件管理 -> 权限管理 -> 微信 -> 使用话筒录音/通话录音 -> 允许")
                        .setNegativeButton("我知道了", null);
                builder.create().show();
            }
        }
    }

    /**
     * 录音完成后的回调，回调给activity，可以获得time和文件的路径
     */
    public interface AudioFinishRecorderListener {
        void onFinished(float seconds, String filePath);
    }

    private AudioFinishRecorderListener mListener;

    /**
     * 设置录音成功调用的接口
     *
     * @param listener
     */
    public void setAudioFinishRecorderListener(AudioFinishRecorderListener listener) {
        mListener = listener;
    }

    // 获取音量大小的runnable
    private Runnable mGetVoiceLevelRunnable = new Runnable() {

        @Override
        public void run() {
            while (isRecording) {
                try {
                    Thread.sleep(100);
                    mTime += 0.1f;
                    mHandler.sendEmptyMessage(MSG_VOICE_CHANGE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    // 准备三个常量
    private static final int MSG_AUDIO_PREPARED = 0X110;
    private static final int MSG_VOICE_CHANGE = 0X111;
    private static final int MSG_DIALOG_DISMISS = 0X112;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_AUDIO_PREPARED:
                    // 显示应该是在audio end prepare之后回调
                    mDialogManager.showRecordingDialog();
                    isRecording = true;
                    // 需要开启一个线程来变换音量
                    new Thread(mGetVoiceLevelRunnable).start();
                    break;
                case MSG_VOICE_CHANGE:
                    mDialogManager.updateVoiceLevel(mAudioManager.getVoiceLevel(7));
                    break;
                case MSG_DIALOG_DISMISS:
                    mDialogManager.dismissDialog();
                    break;
            }
        }
    };

    // 在这里面发送一个handler的消息
    @Override
    public void wellPrepared() {
        mHandler.sendEmptyMessage(MSG_AUDIO_PREPARED);
    }

    /**
     * 直接复写这个监听函数
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                changeState(STATE_RECORDING);
                break;
            case MotionEvent.ACTION_MOVE:
                if (isRecording) {
                    // 根据x，y来判断用户是否想要取消
                    if (wantToCancel(x, y)) {
                        changeState(STATE_WANT_TO_CANCEL);
                    } else {
                        changeState(STATE_RECORDING);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                // 首先判断是否有触发onLongClick事件，没有的话直接返回reset
                if (!mReady) {
                    reset();
                    return super.onTouchEvent(event);
                }
                // 如果按的时间太短，还没准备好或者时间录制太短，就离开了，则显示这个dialog
                if (!isRecording || mTime < 0.6f) {
                    mDialogManager.tooShort();
                    mAudioManager.discardRecording();
                    mHandler.sendEmptyMessageDelayed(MSG_DIALOG_DISMISS, 1300);// 持续1.3s
                } else if (mCurrentState == STATE_RECORDING) {//正常录制结束
                    mAudioManager.stopRecoding();
                    if (mListener != null) {
                        mListener.onFinished(mTime, mAudioManager.getCurrentFilePath());
                    }
                } else if (mCurrentState == STATE_WANT_TO_CANCEL) {
                    // cancel
                    mAudioManager.discardRecording();
                }
                reset();// 恢复标志位
                break;
        }
        return super.onTouchEvent(event);
    }

    /*
     * 恢复标志位以及状态
     */
    private void reset() {
        isRecording = false;
        mDialogManager.dismissDialog();
        changeState(STATE_NORMAL);
        mReady = false;
        mTime = 0;
    }

    private boolean wantToCancel(int x, int y) {
        if (x < 0 || x > getWidth()) {// 判断是否在左边，右边，上边，下边
            return true;
        }
        if (y < -DISTANCE_Y_CANCEL || y > getHeight() + DISTANCE_Y_CANCEL) {
            return true;
        }
        return false;
    }

    //改变状态
    private void changeState(int state) {
        if (mCurrentState != state) {
            mCurrentState = state;
            switch (mCurrentState) {
                case STATE_NORMAL:
                    this.setBackgroundResource(R.drawable.common_btn_yellow_dash_frame);
                    this.setText("按住 说话");
                    break;
                case STATE_RECORDING:
                    this.setBackgroundResource(R.drawable.common_btn_yellow_solid);
                    this.setText("松开 结束");
                    if (isRecording) {
                        mDialogManager.recording();
                    }
                    break;
                case STATE_WANT_TO_CANCEL:
                    this.setBackgroundResource(R.drawable.common_btn_yellow_solid);
                    this.setText("松开手指，取消录音");
                    // dialog want to cancel
                    mDialogManager.wantToCancel();
                    break;
            }
        }
    }

    @Override
    public boolean onPreDraw() {
        return false;
    }
}
