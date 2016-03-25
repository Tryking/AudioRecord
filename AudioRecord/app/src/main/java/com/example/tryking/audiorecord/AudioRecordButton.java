package com.example.tryking.audiorecord;

import android.app.Service;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

/**
 * 长按录音：按钮类
 * Created by Tryking on 2016/3/24.
 */
public class AudioRecordButton extends Button {

    // Handler用的标记常量
    private static final int MSG_AUDIO_PREPARED = 0X01;
    private static final int MSG_VOICE_CHANGE = 0X02;
    private static final int MSG_DIALOG_DISMISS = 0X03;

    private static final int STATE_RECORDING = 1;
    private static final int STATE_WANT_TO_CANCEL = 2;
    private static final int STATE_NORMAL = 3;

    private int mCurrentState = STATE_NORMAL;

    private static final int DISTANCE_Y_CANCEL = 50;

    private DialogManager mDialogManager;
    private AudioManager mAudioManager;
    private boolean isRecording = false;
    private float mTime;

    private boolean mReady;//触发LongClick，准备好了

    public AudioRecordButton(Context context) {
        this(context, null);
    }

    public AudioRecordButton(final Context context, AttributeSet attrs) {
        super(context, attrs);
        mDialogManager = new DialogManager(context);

        String dir = Environment.getExternalStorageDirectory() + "/recorder_audios";
        mAudioManager = AudioManager.getInstance(dir);
        mAudioManager.setOnAudioStageListener(new AudioManager.AudioStageListener() {
            @Override
            public void wellPrepared() {
                //准备好了，发送一个handler消息
                mHandler.sendEmptyMessage(MSG_AUDIO_PREPARED);
            }
        });

        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mAudioManager.prepareAudio();
                mReady = true;
                //调用手机震动器短震
                Vibrator vibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
                vibrator.vibrate(100);
                return false;
            }
        });
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_AUDIO_PREPARED:
                    mDialogManager.showRecordingDialog();
                    isRecording = true;

                    // 需要开启一个线程来变换音量
                    new Thread(mGetVoiceLevelRunnable).start();
                    break;
                case MSG_VOICE_CHANGE:
                    mDialogManager.updateVoiceLevel(mAudioManager.getVoiceLevel(7));
                    break;
                case MSG_DIALOG_DISMISS:
                    break;
            }
        }
    };

    /**
     * 录音完成后进行回调
     */
    public interface AudioFinishRecorderListener {
        /**
         * 录音成功后可以添加的事项
         *
         * @param seconds  录音时间
         * @param filePath 录音路径
         */
        void onFinished(float seconds, String filePath);
    }

    private AudioFinishRecorderListener mListener;

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
                    if (wantToChange(x, y)) {
                        changeState(STATE_WANT_TO_CANCEL);
                    } else {
                        changeState(STATE_RECORDING);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!mReady) {
                    reset();
                    return super.onTouchEvent(event);
                }
                // 如果按的时间太短，还没准备好或者时间录制太短，就离开了，则显示这个dialog
                if (!isRecording || mTime < 0.5f) {
                    mDialogManager.tooShort();
                    mAudioManager.cancel();
                    mHandler.sendEmptyMessageDelayed(MSG_DIALOG_DISMISS, 1300);
                } else if (mCurrentState == STATE_RECORDING) {//正常录制结束
                    mDialogManager.dismissDialog();
                    mAudioManager.release();

                    if (mListener != null) {
                        mListener.onFinished(mTime, mAudioManager.getCurrentFilePath());
                    }
                } else if (mCurrentState == STATE_WANT_TO_CANCEL) {
                    mAudioManager.cancel();
                    mDialogManager.dismissDialog();
                }
                reset();
                break;
        }
        return super.onTouchEvent(event);
    }

    /*
    恢复标志位以及状态
     */
    private void reset() {
        isRecording = false;
        changeState(STATE_NORMAL);
        mReady = false;
        mTime = 0;
    }

    /*
    发生什么会取消发送
     */
    private boolean wantToChange(int x, int y) {
        if (x < 0 || x > getWidth()) {// 判断是否在左边，右边，上边，下边
            return true;
        }
        if (y < -DISTANCE_Y_CANCEL || y > getHeight() + DISTANCE_Y_CANCEL) {
            return true;
        }
        return false;
    }

    private void changeState(int state) {
        if (mCurrentState != state) {
            mCurrentState = state;
            switch (mCurrentState) {
                case STATE_NORMAL:
                    setBackgroundResource(R.drawable.button_record_normal);
                    setText("按住 说话");
                    break;
                case STATE_RECORDING:
                    setBackgroundResource(R.drawable.button_recording);
                    setText("松开 结束");
                    if (isRecording) {
                        mDialogManager.recording();
                    }
                    break;
                case STATE_WANT_TO_CANCEL:
                    setBackgroundResource(R.drawable.button_recording);
                    setText("松开手指，取消发送");
                    mDialogManager.wantToCancel();
                    break;
            }
        }
    }

    @Override
    public boolean onPreDraw() {
        // TODO Auto-generated method stub
        return false;
    }
}
