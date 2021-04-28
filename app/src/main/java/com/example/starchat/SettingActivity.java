package com.example.starchat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.wildma.pictureselector.PictureBean;
import com.wildma.pictureselector.PictureSelector;

import java.io.File;

/**
 * 设置页面
 * 包括用户头像、用户昵称和聊天室用户列表
 */
public class SettingActivity extends AppCompatActivity {

    private TextView mTextNickname;

    private View dialogView;
    private Button mBnNo;
    private Button mBnYes;
    private EditText mEditNickname;
    private AlertDialog mMakeNicknameDialog;
    private SharedPreferences mSharedPreferences;
    private ImageView mViewProfile;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        getWindow().setStatusBarColor(getResources().getColor(R.color.Tiffany));

        //初始化界面
        initView();

        //设置组件监听事件
        initEvent();
    }

    /**
     * 初始化界面
     */
    private void initView() {
        //设置标题栏
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        mTextNickname = findViewById(R.id.text_nickname);
        mViewProfile = findViewById(R.id.profile);
        mSharedPreferences = getSharedPreferences(getString(R.string.starChatData), MODE_PRIVATE);
        mTextNickname.setText(mSharedPreferences.getString(getString(R.string.nickname), getString(R.string.anonymity)));
        String path;
        if ((path = mSharedPreferences.getString("profile_path", null)) == null) {
            Glide.with(SettingActivity.this)
                    .load(R.drawable.profile)
                    .override(500)
                    .fitCenter()
                    .placeholder(R.drawable.profile)
                    .into(mViewProfile);
        } else {
            Glide.with(SettingActivity.this)
                    .load(path)
                    .override(500)
                    .fitCenter()
                    .placeholder(R.drawable.profile)
                    .into(mViewProfile);
        }

        //初始化取名弹窗
        dialogView = View.inflate(SettingActivity.this, R.layout.view_nickname_dialog, null);
        mEditNickname = dialogView.findViewById(R.id.edit_nickname);
        mBnNo = dialogView.findViewById(R.id.bn_no);
        mBnYes = dialogView.findViewById(R.id.bn_yes);
        mMakeNicknameDialog = new AlertDialog.Builder(SettingActivity.this)
                .setTitle("好汉请留名！")
                .setView(dialogView)
                .create();
    }


    /**
     * 初始化组件事件
     */
    private void initEvent() {

        //打开改名窗口
        mTextNickname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //打开弹窗
                mMakeNicknameDialog.show();
            }
        });

        //头像选择
        mViewProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PictureSelector
                        .create(SettingActivity.this, PictureSelector.SELECT_REQUEST_CODE)
                        .selectPicture(false);
            }
        });

        //点击取消按钮隐藏弹窗
        mBnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMakeNicknameDialog.dismiss();
            }
        });

        //点击确定按钮取名
        mBnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //取名存在sharedPreferences中
                if (mEditNickname.getText().toString().trim().equals("")) {
                    return;
                }
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putString(getString(R.string.nickname), mEditNickname.getText().toString().trim());
                editor.apply();
                mTextNickname.setText(mEditNickname.getText().toString().trim());
                mMakeNicknameDialog.dismiss();
            }
        });
    }

    /**
     * 获得头像选择结构
     * @param requestCode 请求码
     * @param resultCode 结果码
     * @param data 头像
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PictureSelector.SELECT_REQUEST_CODE) {
            if (data != null) {
                PictureBean pictureBean = data.getParcelableExtra(PictureSelector.PICTURE_RESULT);
                String path = pictureBean.getPath();
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putString("profile_path", path);
                editor.apply();
                Glide.with(SettingActivity.this)
                        .load(new File(path))
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mViewProfile);
            }
        }
    }

    /**
     * 返回聊天室
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
