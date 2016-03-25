package com.example.tryking.audiorecord;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 长按录音：Dialog管理类
 * Created by Tryking on 2016/3/24.
 */
public class DialogManager {

    private Context mContext;
    private Dialog mDialog;
    private ImageView mIcon;
    private ImageView mVoice;
    private TextView mLable;

    public DialogManager(Context context) {
        mContext = context;
    }

    /**
     * 展示正在录音的Dialog
     */
    public void showRecordingDialog() {
        mDialog = new Dialog(mContext, R.style.AudioDialogTheme);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.dialog_audio, null);
        mDialog.setContentView(view);

        mIcon = (ImageView) mDialog.findViewById(R.id.dialog_icon);
        mVoice = (ImageView) mDialog.findViewById(R.id.dialog_voice);
        mLable = (TextView) mDialog.findViewById(R.id.dialog_text);

        mDialog.show();
    }

    /**
     * 设置正在录音的界面
     */
    public void recording() {
        if (mDialog != null && mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.VISIBLE);
            mLable.setVisibility(View.VISIBLE);

            mIcon.setImageResource(R.mipmap.recorder);
            mLable.setText("手指上滑，取消发送");
        }
    }

    /**
     * 设置想要取消的界面
     */
    public void wantToCancel() {
        // TODO Auto-generated method stub
        if (mDialog != null && mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.GONE);
            mLable.setVisibility(View.VISIBLE);

            mIcon.setImageResource(R.mipmap.cancel);
            mLable.setText("松开手指，取消发送");
        }
    }

    /**
     * 设置录音时间太短的界面
     */
    public void tooShort() {
        // TODO Auto-generated method stub
        if (mDialog != null && mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.GONE);
            mLable.setVisibility(View.VISIBLE);

            mIcon.setImageResource(R.mipmap.voice_too_short);
            mLable.setText("录音时间过短");
        }
    }

    /**
     * 设置隐藏Dialog
     */
    public void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    /**
     * 根据声音的等级更改声音图标
     *
     * @param level 声音等级
     */
    public void updateVoiceLevel(int level) {
        if (mDialog != null && mDialog.isShowing()) {
            // name - The name of the desired resource.
            // defType - Optional default resource type to find, if "type/" is not included in the name. Can be null to require an explicit type.
            // defPackage - Optional default package to find, if "package:" is not included in the name. Can be null to require an explicit package.
            int voiceResourceId = mContext.getResources().getIdentifier("v" + level, "mipmap", mContext.getPackageName());
            mVoice.setImageResource(voiceResourceId);
        }
    }

}
