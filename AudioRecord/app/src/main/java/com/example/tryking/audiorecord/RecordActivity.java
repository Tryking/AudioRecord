package com.example.tryking.audiorecord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 实现长按录音的Activity类
 */
public class RecordActivity extends Activity {
    private static final int SUCCESS_RESULT_CODE = 1;
    private static final int FAIL_RESULT_CODE = 2;
    private Recorder recorder;
    private int mMinItemWith;// 设置对话框的最大宽度和最小宽度
    private int mMaxItemWith;
    private Intent intent;
    private TextView recorderTime;
    private AudioRecordButton recordButton;
    private FrameLayout recorderLength;
    private Button cancelButton;
    private Button saveButton;
    private View idRecorderAnim;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Toast.makeText(RecordActivity.this, "create啦", Toast.LENGTH_SHORT).show();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        recordButton = (AudioRecordButton) findViewById(R.id.recordButton);
        recorderTime = (TextView) findViewById(R.id.recorder_time);
        recorderLength = (FrameLayout) findViewById(R.id.recorder_length);
        cancelButton = (Button) findViewById(R.id.bt_cancel);
        saveButton = (Button) findViewById(R.id.bt_save);
        idRecorderAnim = findViewById(R.id.id_recorder_anim);

        //设置点击Activity外dialog不消失
        setFinishOnTouchOutside(false);

        intent = getIntent();

        // 获取系统宽度
        WindowManager wManager = (WindowManager) this
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wManager.getDefaultDisplay().getMetrics(outMetrics);
        mMaxItemWith = (int) (outMetrics.widthPixels * 0.7f);
        mMinItemWith = (int) (outMetrics.widthPixels * 0.15f);

        recordButton.setAudioFinishRecorderListener(new AudioRecordButton.AudioFinishRecorderListener() {

            @Override
            public void onFinished(float seconds, String filePath) {
                recorder = new Recorder(seconds, filePath);
                Toast.makeText(RecordActivity.this, "存储时间为：" + seconds + "\n存储路径为:" + filePath, Toast.LENGTH_SHORT).show();

                RecordActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recorderTime.setText(Math.round(recorder.time) + "\"");
                        ViewGroup.LayoutParams lParams = recorderLength.getLayoutParams();
                        lParams.width = (int) (mMinItemWith + mMaxItemWith / 60f * recorder.time);
                        recorderLength.setLayoutParams(lParams);
                    }
                });
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(FAIL_RESULT_CODE, intent);
                finish();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null == recorder) {
                    Toast.makeText(RecordActivity.this, "对不起，您还没有录音", Toast.LENGTH_SHORT).show();
                } else {

                    intent.putExtra("time", String.valueOf(recorder.time));
                    intent.putExtra("filePath", recorder.filePathString);
                    setResult(SUCCESS_RESULT_CODE, intent);
                    finish();
                }
            }
        });

        recorderLength.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null == recorder) {
                    Toast.makeText(RecordActivity.this, "对不起，您还没有说话", Toast.LENGTH_SHORT).show();
                } else {
                    idRecorderAnim.setBackgroundResource(R.drawable.play);
                    AnimationDrawable drawable = (AnimationDrawable) idRecorderAnim
                            .getBackground();
                    drawable.start();

                    // 播放音频
                    MediaManager.playSound(recorder.filePathString,
                            new MediaPlayer.OnCompletionListener() {

                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    idRecorderAnim.setBackgroundResource(R.mipmap.adj);
                                }
                            });
                }
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        MediaManager.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MediaManager.resume();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        MediaManager.release();
    }

    class Recorder {
        float time;
        String filePathString;

        public Recorder(float time, String filePathString) {
            super();
            this.time = time;
            this.filePathString = filePathString;
        }

        public float getTime() {
            return time;
        }

        public void setTime(float time) {
            this.time = time;
        }

        public String getFilePathString() {
            return filePathString;
        }

        public void setFilePathString(String filePathString) {
            this.filePathString = filePathString;
        }
    }

}
