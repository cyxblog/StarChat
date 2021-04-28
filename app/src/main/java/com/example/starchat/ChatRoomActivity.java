package com.example.starchat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;


import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.filepicker.FilePicker;
import com.app.filepicker.OnFilePickerSelectListener;
import com.app.filepicker.model.EssFile;
import com.example.starchat.adapter.MessageAdapter;

import com.example.starchat.bean.MessageBean;
import com.example.starchat.service.ServerService;
import com.example.starchat.util.FileUtil;
import com.example.starchat.util.GsonUtil;
import com.example.starchat.util.MsgTypeUtil;
import com.example.starchat.util.SocketUtil;
import com.example.starchat.util.TimeUtil;
import com.example.starchat.util.ToastUtil;
import com.lqr.audio.AudioRecordManager;
import com.lqr.audio.IAudioRecordListener;
import com.wildma.pictureselector.PictureBean;
import com.wildma.pictureselector.PictureSelector;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.Socket;

import java.util.ArrayList;
import java.util.List;

import top.zibin.luban.CompressionPredicate;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;


/**
 * 聊天室页面，收发消息模块
 * @author 陈
 */
public class ChatRoomActivity extends AppCompatActivity {

    //用于标识Log的字符串
    public static final String TAG = "ChatRoomActivity";

    //msg.what：数据显示在聊天列表
    public static final int SHOW_ON_CHAT_LIST_CODE = 1;
    //msg.what：数据显示在弹窗
    public static final int SHOW_ON_DIALOG_CODE = 2;

    //SharedPreferences中取数据
    private SharedPreferences mSharedPreferences;
    //聊天室中的设备昵称
    private String mNickname;
    //从SharedPreference文件中获取的设备角色
    private String mRole;

    // 消息输入框
    private EditText mInputText;
    //聊天信息显示列表
    private RecyclerView mMsgListView;
    //消息集合
    private List<MessageBean> mMessageBeans;
    //聊天信息列表适配器
    private MessageAdapter mMessageAdapter;
    //“更多”，点击可获得更多功能
    private ImageView mMoreImg;
    //承载更多功能的布局
    private ConstraintLayout mMoreFuncLayout;
    //发送按钮
    private Button mBnSendMsg;
    //定位
    private ImageView mLocatedImg;
    //图库
    private ImageView mPhotoImg;
    //文件
    private ImageView mFileImg;

    //客户端发送给服务器的消息
    private String mMsgToServer = "";
    //客户端Socket
    private Socket mClientSocket;

    //所选文件的路径集合
    private ArrayList<Uri> mListFilePath = new ArrayList<>();


    //Service连接回调
    public ServiceConnection mServiceConnection;

    //创建Handler执行UI更新，在聊天界面更新消息
    private Handler mHandler = new Handler(msg -> {
        switch (msg.what) {
            case SHOW_ON_CHAT_LIST_CODE:
                showMessageOnChatRoom((String) msg.obj);
                break;
            case SHOW_ON_DIALOG_CODE:
                showDialog();
                break;
            default:
                break;
        }
        return false;
    });

    private LinearLayoutManager mLayoutManager;
    private Intent mIntent;
    private ImageView mImgAudio;
    private Button mBnAudio;
    private File mAudioDir;


    /**
     * onCreate()阶段，主要用于初始化参数
     * @param savedInstanceState 实例状态
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        //加载聊天室界面
        initView();

        //设置监听事件
        initEvent();

        //初始化设备角色，服务器或客户端，由SharedPreferences文件决定
        initServerOrClient();

    }

    /**
     * 初始化聊天室界面
     */
    private void initView() {
        getWindow().setStatusBarColor(getResources().getColor(R.color.Tiffany));

        mMsgListView = findViewById(R.id.message_list_view);
        mImgAudio = findViewById(R.id.audio_img);
        mBnAudio = findViewById(R.id.bn_audio);
        mMoreImg = findViewById(R.id.more_img);
        mMoreFuncLayout = findViewById(R.id.moreFuncLayout);
        mInputText = findViewById(R.id.input_text);
        mBnSendMsg = findViewById(R.id.bn_send_msg);
        mLocatedImg = findViewById(R.id.location_img);
        mPhotoImg = findViewById(R.id.photo_img);
        mFileImg = findViewById(R.id.file_img);

        //初始化SharedPreferences
        mSharedPreferences = getSharedPreferences(getString(R.string.starChatData), MODE_PRIVATE);
        mNickname = mSharedPreferences.getString(getString(R.string.nickname), getString(R.string.anonymity));

        //设置标题栏
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.chatRoom);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        mMessageBeans = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(mMessageBeans, ChatRoomActivity.this);
        mLayoutManager = new LinearLayoutManager(this);
        mMsgListView.setLayoutManager(mLayoutManager);
        mMsgListView.setAdapter(mMessageAdapter);

        mMessageAdapter.notifyDataSetChanged();
    }

    /**
     * 初始化组件事件
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initEvent() {

        //监听语音输入按钮
        mImgAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermission();
                if (!permissionGranted()) {
                    return;
                }
                if (mInputText.getVisibility() == View.VISIBLE) {
                    mInputText.setVisibility(View.GONE);
                    mBnAudio.setVisibility(View.VISIBLE);
                    mImgAudio.setImageResource(R.drawable.keyboard);
                    InputMethodManager inputMethodManager = (InputMethodManager) ChatRoomActivity.this.getSystemService(INPUT_METHOD_SERVICE);
                    if (inputMethodManager != null) {
                        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }

                    AudioRecordManager.getInstance(ChatRoomActivity.this).setMaxVoiceDuration(60);
                    mAudioDir = new File(getExternalCacheDir().getAbsolutePath() + File.separator + "AUDIO");
                    if (!mAudioDir.exists()) {
                        mAudioDir.mkdir();
                    }
                    mAudioDir = new File(mAudioDir.getAbsolutePath() + File.separator + "self");
                    if (!mAudioDir.exists()) {
                        mAudioDir.mkdir();
                    }
                    AudioRecordManager.getInstance(ChatRoomActivity.this).setAudioSavePath(mAudioDir.getAbsolutePath());
                    if (mMoreFuncLayout.getVisibility() == View.VISIBLE) {
                        mMoreFuncLayout.setVisibility(View.GONE);
                    }
                } else {
                    mInputText.setVisibility(View.VISIBLE);
                    mBnAudio.setVisibility(View.GONE);
                    mImgAudio.setImageResource(R.drawable.mic);
                }
            }
        });

        //监听更多按钮的点击
        mMoreImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMoreFuncLayout.setVisibility(View.VISIBLE);
                if (mMessageAdapter.getItemCount() - 1 > 0) {
                    mMsgListView.smoothScrollToPosition(mMessageAdapter.getItemCount() - 1);
                }
            }
        });

        //监听录音按钮，发送或取消语音
        mBnAudio.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        AudioRecordManager.getInstance(ChatRoomActivity.this).startRecord();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (isCancelled(v, event)) {
                            AudioRecordManager.getInstance(ChatRoomActivity.this).willCancelRecord();
                        } else {
                            AudioRecordManager.getInstance(ChatRoomActivity.this).continueRecord();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        AudioRecordManager.getInstance(ChatRoomActivity.this).stopRecord();
                        break;
                }
                return false;
            }

            private boolean isCancelled(View v, MotionEvent event) {
                int[] location = new int[2];
                v.getLocationOnScreen(location);
                if (event.getRawX() < location[0] || event.getRawX() > location[0] + v.getWidth() || event.getRawY() < location[1] - 40) {
                    return true;
                }
                return false;
            }

        });

        //监听语音录制变化
        AudioRecordManager.getInstance(ChatRoomActivity.this).setAudioRecordListener(new IAudioRecordListener() {

            private AnimationDrawable mDrawable;
            private PopupWindow mPopupWindow;
            private ImageView mImgTip;
            private TextView mTextTip;

            @Override
            public void initTipView() {
                View view = LayoutInflater.from(ChatRoomActivity.this).inflate(R.layout.popup_window_audio, null, false);

                mImgTip = view.findViewById(R.id.img_mic);
                mTextTip = view.findViewById(R.id.text_tip);

                mPopupWindow = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mPopupWindow.setAnimationStyle(R.anim.anim_pop);
                mPopupWindow.setBackgroundDrawable(new ColorDrawable(0x000000));
                mPopupWindow.setFocusable(true);
                mPopupWindow.setOutsideTouchable(false);
                mPopupWindow.showAtLocation(ChatRoomActivity.this.getWindow().getDecorView(), Gravity.CENTER, 0, 0);
            }

            @Override
            public void setTimeoutTipView(int counter) {

            }

            @Override
            public void setRecordingTipView() {
                mImgTip.setImageResource(R.drawable.mic_color_change);
                mDrawable = (AnimationDrawable) mImgTip.getDrawable();
                mDrawable.start();
                mTextTip.setText("向外滑动取消发送");
            }

            @Override
            public void setAudioShortTipView() {
                ToastUtil.showToast(ChatRoomActivity.this, "时间太短");
            }

            @Override
            public void setCancelTipView() {
                mDrawable.stop();
                mImgTip.setImageResource(R.drawable.alarm);
                mTextTip.setText("抬手取消发送");
            }

            @Override
            public void destroyTipView() {
                mDrawable.stop();
                mPopupWindow.dismiss();
                mPopupWindow = null;
            }

            @Override
            public void onStartRecord() {

            }

            @Override
            public void onFinish(Uri audioPath, int duration) {
                File file = new File(audioPath.getPath());

                MessageBean messageBean = new MessageBean();
                messageBean.type = MsgTypeUtil.SELF_AUDIO;
                messageBean.time = TimeUtil.getCurrentTime();
                messageBean.nickname = mNickname;
                messageBean.filePath = file.getAbsolutePath();
                messageBean.fileName = file.getName();
                messageBean.fileType = -1;
                messageBean.audioLength = duration + "″";
                messageBean.profilePath = mSharedPreferences.getString("profile_path", "");
                showMessageOnChatRoom(GsonUtil.toJsonStr(messageBean));
                if (mRole.equals("客户端")) {
                    new Thread(() -> {
                        try {
                            sendPicOrFile(file, messageBean);
                            Log.d(TAG, "Audio run: " + GsonUtil.toJsonStr(messageBean));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    mIntent.putExtra("action", "audio");
                    mIntent.putExtra("audio_info", GsonUtil.toJsonStr(messageBean));
                    startService(mIntent);
                }
            }

            @Override
            public void onAudioDBChanged(int db) {

            }
        });

        //监听输入框的点击事件
        mInputText.setOnClickListener(v -> {

            if (mMessageAdapter.getItemCount() - 1 > 0) {
                mMsgListView.smoothScrollToPosition(mMessageAdapter.getItemCount() - 1);
            }
            mMoreFuncLayout.setVisibility(View.GONE);

        });

        //监听输入框的字符串输入变化事件，主要是改变发送按钮和更多功能的显示与隐藏
        mInputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                mMoreFuncLayout.setVisibility(View.GONE);

                if (s.length() == 0) {
                    mBnSendMsg.setVisibility(View.GONE);
                    mMoreImg.setVisibility(View.VISIBLE);
                } else {
                    mMoreImg.setVisibility(View.GONE);
                    mBnSendMsg.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //监听发送按钮的点击事件
        mBnSendMsg.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                //将数据转化成Json格式
                MessageBean messageBean = new MessageBean();
                messageBean.time = TimeUtil.getCurrentTime();
                messageBean.nickname = mNickname;
                messageBean.msg = mInputText.getText().toString();
                messageBean.type = MsgTypeUtil.SELF_MSG;
                messageBean.profilePath = mSharedPreferences.getString("profile_path", "");
                showMessageOnChatRoom(GsonUtil.toJsonStr(messageBean));
                if (mRole.equals(getString(R.string.client))) {
                    mMsgToServer = GsonUtil.toJsonStr(messageBean);
                    new Thread(() -> {
                        try {
                            sendMessage(mMsgToServer, mClientSocket);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    mIntent.putExtra("action", "text");
                    mIntent.putExtra("text", GsonUtil.toJsonStr(messageBean));
                    startService(mIntent);
                }
                mInputText.setText("");
            }
        });

        //监听消息列表滚动时隐藏软键盘
        mMsgListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    InputMethodManager inputMethodManager = (InputMethodManager) ChatRoomActivity.this.getSystemService(INPUT_METHOD_SERVICE);
                    if (inputMethodManager != null) {
                        inputMethodManager.hideSoftInputFromWindow(recyclerView.getWindowToken(), 0);
                    }
                }
            }
        });

        //点击定位，产生设备所处地方经纬度，并发送给其他设备
        mLocatedImg.setOnClickListener(v -> {

            //按情况获取MessageBean
            MessageBean messageBean = new MessageBean();
            messageBean.time = TimeUtil.getCurrentTime();
            messageBean.nickname = mNickname;
            messageBean.msg = getDeviceLocation();
            messageBean.type = MsgTypeUtil.SELF_MSG;
            messageBean.profilePath = mSharedPreferences.getString("profile_path", "");

            if (!messageBean.msg.equals("无法定位")) {
                showMessageOnChatRoom(GsonUtil.toJsonStr(messageBean));
                new Thread(() -> {
                    try {
                        if (mRole.equals("客户端")) {
                            sendMessage(GsonUtil.toJsonStr(messageBean), mClientSocket);
                        } else {
                            mIntent.putExtra("action", "location");
                            mIntent.putExtra("location_info", GsonUtil.toJsonStr(messageBean));
                            startService(mIntent);
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }).start();
            } else {
                ToastUtil.showToast(ChatRoomActivity.this, "无法定位");
            }
        });

        //打开图库，选择图片
        mPhotoImg.setOnClickListener(v -> PictureSelector
                .create(ChatRoomActivity.this, PictureSelector.SELECT_REQUEST_CODE)
                .selectPicture(false));

        //打开文件夹，选择文件
        mFileImg.setOnClickListener(v -> FilePicker.chooseForMimeType().setTheme(R.style.FilePicker_Elec).setMaxCount(10)
                .setFileTypes("docx", "pptx", "xlsx", "pdf", "zip", "mp3", "mp4")
                .selectFiles(new OnFilePickerSelectListener() {
                    @Override
                    public void onFilePickerResult(List<EssFile> essFiles) {
                        for (EssFile essFile : essFiles) {
                            mMoreFuncLayout.setVisibility(View.GONE);
                            Log.d(TAG, "onFilePickerResult: ok");
                            MessageBean messageBean = new MessageBean();
                            messageBean.nickname = mNickname;
                            messageBean.time = TimeUtil.getCurrentTime();
                            messageBean.type = MsgTypeUtil.SELF_FILE;
                            messageBean.fileName = essFile.getName();
                            messageBean.fileType = FileUtil.getFileType(essFile.getName());
                            messageBean.profilePath = mSharedPreferences.getString("profile_path", "");
                            Log.d(TAG, "onFilePickerResult: " + messageBean.fileType);
                            messageBean.fileLength = FileUtil.getFileLength(essFile.getFile());
                            messageBean.filePath = essFile.getAbsolutePath();
                            showMessageOnChatRoom(GsonUtil.toJsonStr(messageBean));

                            File file = essFile.getFile();

                            new Thread(() -> {
                                try {
                                    if (mRole.equals("客户端")) {
                                        sendPicOrFile(file, messageBean);
                                    } else {
                                        mIntent.putExtra("action", "file");
                                        mIntent.putExtra("file_info", GsonUtil.toJsonStr(messageBean));
                                        startService(mIntent);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }).start();
                        }
                    }
                }).start(ChatRoomActivity.this));

    }

    /**
     * 获得设备定位信息
     * @return 设备经纬度组成的字符串
     */
    private String getDeviceLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        List<String> providerList = locationManager.getProviders(true);
        String provider;
        if (providerList.contains(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else {
            provider = null;
        }
        Location location;
        String deviceLocation = "无法定位";
        if (provider != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
            location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                deviceLocation = "纬度：" + location.getLatitude() + "\n" + "经度：" + location.getLongitude();
            }
        }
        return deviceLocation;
    }

    /**
     * 询问权限
     */
    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WAKE_LOCK, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    /**
     * 检查设备是否授予
     * @return true:被授予 false:未被授予
     */
    private boolean permissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 客户端发送图片或文件到服务器
     * @param file 发送的文件
     * @param messageBean 对应要发送的消息标志，来分辨消息类型
     * @throws IOException 抛出IOException
     */
    public void sendPicOrFile(File file, MessageBean messageBean) throws IOException {
        Log.d(TAG, "sendPicOrFile: " + messageBean.type);
        DataOutputStream dos = new DataOutputStream(mClientSocket.getOutputStream());
        FileInputStream fileInputStream = new FileInputStream(file);

        int length;
        byte[] buffer = new byte[1024];

        //写入数据类型
        dos.write(messageBean.type);
        //写入标志长度
        int messageBeanLength = GsonUtil.toJsonStr(messageBean).getBytes().length;
        if (messageBeanLength < 256) {
            dos.write(messageBeanLength);
        } else {
            while (messageBeanLength > 0) {
                if (messageBeanLength >= 255) {
                    dos.write(255);
                } else {
                    dos.write(messageBeanLength);
                }
                messageBeanLength = messageBeanLength - 255;
            }
        }
        Log.d(TAG, "sendPicOrFile: " + messageBeanLength);
        dos.write('\n');
        dos.write(GsonUtil.toJsonStr(messageBean).getBytes());
        dos.flush();

        while ((length = fileInputStream.read(buffer)) != -1) {
            dos.write(buffer, 0, length);
            dos.flush();
        }
        dos.write("\t\r".getBytes());
    }

    /**
     * 判断设备角色
     * 若为服务器，则绑定ServerService，开启服务器；
     * 若为客户端，则使用Socket连接到服务器。
     */
    private void initServerOrClient() {
        //从SharedPreferences取出设备角色，如果是客户端，则连接服务器；反之，启动服务器
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.starChatData), MODE_PRIVATE);
        mRole = sharedPreferences.getString(getString(R.string.role), null);
        final String serverIP = sharedPreferences.getString(getString(R.string.serverIP), null);
        if (mRole != null && serverIP != null) {
            if (mRole.equals(getString(R.string.client))) {

                //客户端，创建Socket连接服务器
                new Thread(() -> {
                    try {
                        mClientSocket = new Socket(serverIP, SocketUtil.PORT);
                        sendPersonalInfo();
                        new Thread(new ClientThread(mClientSocket)).start();
                    } catch (IOException e) {
                        //未连接到服务器，弹窗提示
                        Log.d(TAG, "Client run: " + "未连接到服务器");
                        send2Handler(null, SHOW_ON_DIALOG_CODE, mHandler);
                    }
                }).start();

            } else {
                Log.d(TAG, "initServerOrClient: recover");
                //绑定服务
                mIntent = new Intent(ChatRoomActivity.this, ServerService.class);
                mServiceConnection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        ServerService.DataBinder dataBinder = (ServerService.DataBinder) service;
                        ServerService serverService = dataBinder.geService();
                        serverService.onDataChanged(data -> {
                            Log.d(TAG, "dataChanged: " + data);
                            send2Handler(data, SHOW_ON_CHAT_LIST_CODE, mHandler);
                        });
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {

                    }
                };
                bindService(mIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
            }
        }
    }

    /**
     * TODO
     */
    private void sendPersonalInfo() {

    }

    /**
     * 客户端专属弹窗，若未连接到服务器，则弹出。
     */
    private void showDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(ChatRoomActivity.this)
                .setTitle("提示")
                .setMessage("服务器未启动，请稍后进入！")
                .setPositiveButton("确认", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setCancelable(false)
                .create();
        if (!ChatRoomActivity.this.isFinishing()) {
            alertDialog.show();
        }
    }

    /**
     * 将消息显示在聊天窗口
     * @param strJson 消息对应的Json格式字符串
     */
    private void showMessageOnChatRoom(String strJson) {
        MessageBean messageBean = GsonUtil.getObject(strJson);
        mMessageBeans.add(messageBean);
        mMessageAdapter.notifyDataSetChanged();
        if (mMessageAdapter.getItemCount() > 0) {
            mLayoutManager.scrollToPositionWithOffset(mMessageAdapter.getItemCount() - 1, 0);
        }
    }

    /**
     * 发送文本消息给服务器
     * @param content 消息对应的Json格式字符串
     * @param socket 客户端socket
     * @throws IOException socket传输消息抛出异常
     */
    private void sendMessage(String content, Socket socket) throws IOException {
        DataOutputStream dos;
        if (socket != null) {
            dos = new DataOutputStream(socket.getOutputStream());
        } else {
            return;
        }

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content.getBytes());

        int length;
        byte[] buffer = new byte[1024];
        dos.write(MsgTypeUtil.SELF_MSG);
        while ((length = byteArrayInputStream.read(buffer)) != -1) {
            dos.write(buffer, 0, length);
            dos.flush();
        }

        dos.write("\t\r".getBytes());

    }

    /**
     * 客户端线程类，用于接收服务器发送的数据
     */
    public class ClientThread implements Runnable {

        public Socket socket;

        public ClientThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

            try {
                while (true) {

                    DataInputStream dis = new DataInputStream(socket.getInputStream());

                    int flag;

                    flag = dis.read();

                    if (flag == 123) {//接收服务器的信息并发送个人信息到服务器

                        //接收服务器的信息保存到数据库
                        receivePersonalInfo(dis);

                        //发送个人信息到服务器
                        int length;
                        byte[] buffer = new byte[1024];

                        DataOutputStream dos = new DataOutputStream(mClientSocket.getOutputStream());
                        String path = mSharedPreferences.getString("profile_path", null);
                        dos.write(124);

                        if (mNickname != null) {
                            dos.write(mNickname.getBytes().length);
                            dos.write(mNickname.getBytes());
                        } else {
                            dos.write("无名氏".getBytes().length);
                            dos.write("无名氏".getBytes());
                        }

                        if (path != null) {
                            dos.write(1);
                            FileInputStream fis = new FileInputStream(new File(path));
                            while ((length = fis.read(buffer)) != -1) {
                                dos.write(buffer, 0, length);
                            }
                            dos.write('\t');
                            dos.write('\r');
                        } else {
                            dos.write(0);
                        }
                    } else if (flag == 124) {//新加入的成员信息并保存至数据库
                        receivePersonalInfo(dis);
                    } else if (flag == MsgTypeUtil.SELF_IMG) {//图片
                        receivePicOrFile(dis, MsgTypeUtil.OTHERS_IMG);
                    } else if (flag == MsgTypeUtil.SELF_FILE) {//文件
                        receivePicOrFile(dis, MsgTypeUtil.OTHERS_FILE);
                    } else if (flag == MsgTypeUtil.SELF_AUDIO) {//语音
                        receivePicOrFile(dis, MsgTypeUtil.OTHERS_AUDIO);
                    } else if (flag == MsgTypeUtil.SELF_MSG) {//文本
                        int length;

                        byte[] buffer = new byte[1024];

                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                        while ((length = dis.read(buffer)) != -1) {
                            byteArrayOutputStream.write(buffer, 0, length);

                            //传输结束标志
                            if (length > 1 && buffer[length - 2] == '\t' && buffer[length - 1] == '\r') {
                                break;
                            } else if (length == 1 && buffer[length - 1] == '\r') {
                                break;
                            }
                        }

                        MessageBean messageBean = GsonUtil.getObject(byteArrayOutputStream.toString());
                        if (messageBean != null) {
                            messageBean.time = TimeUtil.getCurrentTime();
                            messageBean.type = MsgTypeUtil.OTHERS_MSG;
                            messageBean.profilePath = getExternalCacheDir().getAbsolutePath() + File.separator + "MEMBER" + File.separator + messageBean.nickname + ".jpeg";
                            send2Handler(GsonUtil.toJsonStr(messageBean), SHOW_ON_CHAT_LIST_CODE, mHandler);
                        }
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
                send2Handler(null, SHOW_ON_DIALOG_CODE, mHandler);
            }

        }
    }

    /**
     * 接收服务器发来的设备信息
     * @param dis 数据输入流
     * @throws IOException 输入流读取数据抛出异常
     * TODO
     */
    public void receivePersonalInfo(DataInputStream dis) throws IOException {
        int length = dis.read();
        byte[] buffer = new byte[1024];
        length = dis.read(buffer, 0, length);
        String nickname = new String(buffer, 0, length);
        int hasFile = dis.read();
        if (hasFile == 1) {
            File dir = new File(getExternalCacheDir().getAbsolutePath() + File.separator + "MEMBER");
            boolean isMkdir = true;
            File file;
            if (!dir.exists()) {
                isMkdir = dir.mkdir();
            }
            if (isMkdir) {
                file = new File(dir.getAbsolutePath() + File.separator + nickname + ".jpeg");
            } else {
                file = new File(getExternalCacheDir().getAbsolutePath() + File.separator + nickname + ".jpeg");
            }
            FileOutputStream fos = new FileOutputStream(file);
            while ((length = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
                //传输结束标志
                if (length > 1 && buffer[length - 2] == '\t' && buffer[length - 1] == '\r') {
                    break;
                } else if (length == 1 && buffer[length - 1] == '\r') {
                    break;
                }
            }
            //保存到数据库
            saveToDB(nickname, file.getAbsolutePath());
        } else {
            saveToDB(nickname, null);
        }

    }

    /**
     * TODO
     * @param nickname
     * @param absolutePath
     */
    private void saveToDB(String nickname, String absolutePath) {

    }

    /**
     * 接收服务器发来的图片或其他文件
     * @param dis 数据输入流
     * @param type 消息类型，图片或文件
     * @throws IOException 读取数据抛出异常
     */
    public void receivePicOrFile(DataInputStream dis, int type) throws IOException {
        int length = 0;
        byte[] buffer = new byte[1024];
        int offset;
        while ((offset = dis.read()) != '\n') {
            length += offset;
        }
        Log.d(TAG, "receivePicOrFile: " + length);

        length = dis.read(buffer, 0, length);

        String mark = new String(buffer, 0, length);

        Log.d(TAG, "receivePicOrFile: " + mark);

        MessageBean fileBean = GsonUtil.getObject(mark);
        String finalDirName;
        if (type == MsgTypeUtil.OTHERS_FILE) {
            finalDirName = "DOCUMENTS";
        } else if (type == MsgTypeUtil.OTHERS_IMG) {
            finalDirName = "IMAGES";
        } else {
            finalDirName = "AUDIO";
        }
        File dir = new File(getExternalCacheDir().getAbsolutePath() + File.separator + finalDirName);
        boolean isMkdir = true;
        if (!dir.exists()) {
            isMkdir = dir.mkdir();
        }
        if (type == MsgTypeUtil.OTHERS_AUDIO) {
            dir = new File(dir.getAbsolutePath() + File.separator + "others");
            if (!dir.exists()) {
                isMkdir = dir.mkdir();
            }
        }
        File file;
        if (isMkdir) {
            file = new File(dir.getAbsolutePath() + File.separator + fileBean.fileName);
        } else {
            file = new File(getExternalCacheDir().getAbsolutePath() + File.separator + fileBean.fileName);
        }
        Log.d(TAG, "receivePicOrFile: " + file.getAbsolutePath());
        FileOutputStream fileOutputStream = new FileOutputStream(file);

        while ((length = dis.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, length);
            fileOutputStream.flush();

            if (length > 1 && buffer[length - 2] == '\t' && buffer[length - 1] == '\r') {
                break;
            } else if (length == 1 && buffer[length - 1] == '\r') {
                break;
            }
        }

        MessageBean messageBean = new MessageBean();
        messageBean.nickname = fileBean.nickname;
        messageBean.type = type;
        messageBean.time = TimeUtil.getCurrentTime();
        messageBean.fileName = fileBean.fileName;
        messageBean.profilePath = getExternalCacheDir().getAbsolutePath() + File.separator + "MEMBER" + File.separator + fileBean.nickname + ".jpeg";
        messageBean.filePath = file.getAbsolutePath();
        if (type == MsgTypeUtil.OTHERS_AUDIO) {
            messageBean.audioLength = fileBean.audioLength;
        }
        messageBean.fileLength = FileUtil.getFileLength(file);
        messageBean.fileType = fileBean.fileType;
        send2Handler(GsonUtil.toJsonStr(messageBean), SHOW_ON_CHAT_LIST_CODE, mHandler);
    }

    /**
     * 获得图片选择器的结果
     * @param requestCode 请求码
     * @param resultCode 结果码
     * @param data 获得的图片数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*结果回调*/
        if (requestCode == PictureSelector.SELECT_REQUEST_CODE) {
            if (data != null) {
                PictureBean pictureBean = data.getParcelableExtra(PictureSelector.PICTURE_RESULT);
                Uri uri = pictureBean.getUri();
                //图片存储位置
                File dir = new File(getExternalCacheDir().getAbsolutePath() + File.separator + "COMPRESSED");
                boolean isMkdir = true;
                String path;
                if (!dir.exists()) {
                    isMkdir = dir.mkdir();
                }
                if (isMkdir) {
                    path = dir.getPath();
                } else {
                    path = getExternalCacheDir().getAbsolutePath();
                }
                //压缩图片
                Luban.with(ChatRoomActivity.this)
                        .load(uri)
                        .ignoreBy(-1)
                        .setTargetDir(path)
                        .filter(new CompressionPredicate() {
                            @Override
                            public boolean apply(String path) {
                                return !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif"));
                            }
                        })
                        .setCompressListener(new OnCompressListener() {
                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onSuccess(File file) {

                                Log.d(TAG, "onSuccess: ok");
                                mMoreFuncLayout.setVisibility(View.GONE);
                                MessageBean messageBean = new MessageBean();
                                messageBean.type = MsgTypeUtil.SELF_IMG;
                                messageBean.nickname = mNickname;
                                messageBean.filePath = file.getAbsolutePath();
                                messageBean.time = TimeUtil.getCurrentTime();
                                messageBean.fileLength = FileUtil.getFileLength(file);
                                messageBean.fileName = file.getName();
                                messageBean.profilePath = mSharedPreferences.getString("profile_path", "");
                                messageBean.fileType = -1;
                                Log.d(TAG, "onSuccess: " + FileUtil.getFileLength(file));
                                showMessageOnChatRoom(GsonUtil.toJsonStr(messageBean));

                                new Thread(() -> {
                                    //发送图片
                                    try {
                                        if (mRole.equals("客户端")) {
                                            if (mClientSocket == null) {
                                                return;
                                            }
                                            sendPicOrFile(file, messageBean);
                                        } else {
                                            String fileStr = GsonUtil.toJsonStr(messageBean);
                                            mIntent.putExtra("action", "picture");
                                            mIntent.putExtra("info", fileStr);
                                            startService(mIntent);
                                        }

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }).start();
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.d(TAG, "onError: " + e.getLocalizedMessage());
                            }
                        }).launch();
            }
        }
    }


    /**
     * 交给Handler处理数据
     * @param content 数据
     * @param what 对应码
     * @param handler mHandler
     */
    public void send2Handler(String content, int what, Handler handler) {
        Message message = new Message();
        message.what = what;
        message.obj = content;
        handler.sendMessage(message);
    }

    /**
     * 点击坐上返回标志，返回MainActivity
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    /**
     * 创建右上角菜单
     * @param menu 菜单列表
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 点击菜单进入SettingActivity
     * @param item 菜单项
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.setting:
                Intent intent = new Intent(ChatRoomActivity.this, SettingActivity.class);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    //返回到ChatRoomActivity时，取出更新后的nickname，恢复服务

    /**
     * 在SettingActivity修改nickname后从SharedPreferences中取出nickname。
     */
    @Override
    protected void onStart() {
        super.onStart();
        mNickname = mSharedPreferences.getString(getString(R.string.nickname), getString(R.string.anonymity));
    }

    /**
     * 在销毁Activity时解绑ServerService
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        if (mRole.equals("服务器")) {
            unbindService(mServiceConnection);
            mServiceConnection = null;
        }
    }
}
