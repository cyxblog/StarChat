package com.example.starchat.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
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
import com.app.filepicker.model.EssFile;
import com.example.starchat.R;
import com.example.starchat.adapter.MessageAdapter;

import com.example.starchat.bean.MessageBean;
import com.example.starchat.service.ServerService;
import com.example.starchat.util.DataHandleUtil;
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

import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;


/**
 * ????????????????????????????????????
 *
 * @author ???
 */
public class ChatRoomActivity extends AppCompatActivity {

    //????????????Log????????????
    public static final String TAG = "ChatRoomActivity";

    //msg.what??????????????????????????????
    public static final int SHOW_ON_CHAT_LIST_CODE = 1;
    //msg.what????????????????????????
    public static final int SHOW_ON_DIALOG_CODE = 2;

    //SharedPreferences????????????
    private SharedPreferences mSharedPreferences;
    //???????????????????????????
    private String mNickname;
    //???SharedPreference??????????????????????????????
    private String mRole;

    // ???????????????
    private EditText mInputText;
    //????????????????????????
    private RecyclerView mMsgListView;
    //????????????
    private List<MessageBean> mMessageBeans;
    //???????????????????????????
    private MessageAdapter mMessageAdapter;
    //??????????????????????????????????????????
    private ImageView mMoreImg;
    //???????????????????????????
    private ConstraintLayout mMoreFuncLayout;
    //????????????
    private Button mBnSendMsg;
    //??????
    private ImageView mLocatedImg;
    //??????
    private ImageView mPhotoImg;
    //??????
    private ImageView mFileImg;

    //????????????????????????????????????
    private String mMsgToServer = "";
    //?????????Socket
    private Socket mClientSocket;

    //???????????????????????????
    private ArrayList<Uri> mListFilePath = new ArrayList<>();


    //Service????????????
    public ServiceConnection mServiceConnection;

    //??????Handler??????UI????????????????????????????????????
    private final Handler mHandler = new Handler(msg -> {
        switch (msg.what) {
            case SHOW_ON_CHAT_LIST_CODE:
                showMessages((String) msg.obj);
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
    private String originalProfilePath;


    /**
     * onCreate()????????????????????????????????????
     *
     * @param savedInstanceState ????????????
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        //?????????????????????
        initView();

        //??????????????????
        initEvent();

        //???????????????????????????????????????????????????SharedPreferences????????????
        initDeviceRole();

    }

    /**
     * ????????????????????????
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

        //?????????SharedPreferences
        mSharedPreferences = getSharedPreferences(getString(R.string.starChatData), MODE_PRIVATE);
        mNickname = mSharedPreferences.getString(getString(R.string.nickname), getString(R.string.anonymity));

        originalProfilePath = mSharedPreferences.getString("profile_path", "");

        //???????????????
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
     * ?????????????????????
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initEvent() {

        //????????????????????????
        mImgAudio.setOnClickListener(v -> {
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
        });

        //???????????????????????????
        mMoreImg.setOnClickListener(v -> {
            mMoreFuncLayout.setVisibility(View.VISIBLE);
            if (mMessageAdapter.getItemCount() - 1 > 0) {
                mMsgListView.smoothScrollToPosition(mMessageAdapter.getItemCount() - 1);
            }
        });

        //??????????????????????????????????????????
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

        //????????????????????????
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
                mTextTip.setText("????????????????????????");
            }

            @Override
            public void setAudioShortTipView() {
                ToastUtil.showToast(ChatRoomActivity.this, "????????????");
            }

            @Override
            public void setCancelTipView() {
                mDrawable.stop();
                mImgTip.setImageResource(R.drawable.alarm);
                mTextTip.setText("??????????????????");
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
                messageBean.audioLength = duration + "???";
                messageBean.profilePath = mSharedPreferences.getString("profile_path", "");
                showMessages(GsonUtil.toJsonStr(messageBean));
                if (mRole.equals("?????????")) {
                    new Thread(() -> {
                        try {
                            sendFile(file, messageBean);
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

        //??????????????????????????????
        mInputText.setOnClickListener(v -> {

            if (mMessageAdapter.getItemCount() - 1 > 0) {
                mMsgListView.smoothScrollToPosition(mMessageAdapter.getItemCount() - 1);
            }
            mMoreFuncLayout.setVisibility(View.GONE);

        });

        //????????????????????????????????????????????????????????????????????????????????????????????????????????????
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

        //?????????????????????????????????
        mBnSendMsg.setOnClickListener(v -> {
            //??????????????????Json??????
            MessageBean messageBean = new MessageBean();
            messageBean.time = TimeUtil.getCurrentTime();
            messageBean.nickname = mNickname;
            messageBean.msg = mInputText.getText().toString();
            messageBean.type = MsgTypeUtil.SELF_MSG;
            messageBean.profilePath = mSharedPreferences.getString("profile_path", "");
            showMessages(GsonUtil.toJsonStr(messageBean));
            if (mRole.equals(getString(R.string.client))) {
                mMsgToServer = GsonUtil.toJsonStr(messageBean);
                new Thread(() -> {
                    try {
                        sendTextMessages(mMsgToServer, mClientSocket);
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
        });

        //??????????????????????????????????????????
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

        //???????????????????????????????????????????????????????????????????????????
        mLocatedImg.setOnClickListener(v -> {

            //???????????????MessageBean
            MessageBean messageBean = new MessageBean();
            messageBean.time = TimeUtil.getCurrentTime();
            messageBean.nickname = mNickname;
            messageBean.msg = getDeviceLocation();
            messageBean.type = MsgTypeUtil.SELF_MSG;
            messageBean.profilePath = mSharedPreferences.getString("profile_path", "");

            if (!messageBean.msg.equals("????????????")) {
                showMessages(GsonUtil.toJsonStr(messageBean));
                new Thread(() -> {
                    try {
                        if (mRole.equals("?????????")) {
                            sendTextMessages(GsonUtil.toJsonStr(messageBean), mClientSocket);
                        } else {
                            mIntent.putExtra("action", "location");
                            mIntent.putExtra("location_info", GsonUtil.toJsonStr(messageBean));
                            startService(mIntent);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                ToastUtil.showToast(ChatRoomActivity.this, "????????????");
            }
        });

        //???????????????????????????
        mPhotoImg.setOnClickListener(v -> PictureSelector
                .create(ChatRoomActivity.this, PictureSelector.SELECT_REQUEST_CODE)
                .selectPicture(false));

        //??????????????????????????????
        mFileImg.setOnClickListener(v -> FilePicker.chooseForMimeType().setTheme(R.style.FilePicker_Elec).setMaxCount(10)
                .setFileTypes("docx", "pptx", "xlsx", "pdf", "zip", "mp3", "mp4")
                .selectFiles(essFiles -> {
                    for (EssFile essFile : essFiles) {
                        mMoreFuncLayout.setVisibility(View.GONE);
                        Log.d(TAG, "onFilePickerResult: ok");
                        MessageBean messageBean = new MessageBean();
                        messageBean.nickname = mNickname;
                        messageBean.time = TimeUtil.getCurrentTime();
                        messageBean.type = MsgTypeUtil.SELF_FILE;
                        messageBean.fileName = essFile.getName();
                        messageBean.fileType = FileUtil.getFileIcon(essFile.getName());
                        messageBean.profilePath = mSharedPreferences.getString("profile_path", "");
                        Log.d(TAG, "onFilePickerResult: " + messageBean.fileType);
                        messageBean.fileLength = FileUtil.getFileLength(essFile.getFile());
                        messageBean.filePath = essFile.getAbsolutePath();
                        showMessages(GsonUtil.toJsonStr(messageBean));

                        File file = essFile.getFile();

                        new Thread(() -> {
                            try {
                                if (mRole.equals("?????????")) {
                                    sendFile(file, messageBean);
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
                }).start(ChatRoomActivity.this));

    }

    /**
     * ????????????????????????
     *
     * @return ?????????????????????????????????
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
        String deviceLocation = "????????????";
        if (provider != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
            location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                deviceLocation = "?????????" + location.getLatitude() + "\n" + "?????????" + location.getLongitude();
            }
        }
        return deviceLocation;
    }

    /**
     * ????????????
     */
    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WAKE_LOCK, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    /**
     * ????????????????????????
     *
     * @return true:????????? false:????????????
     */
    private boolean permissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * ??????????????????????????????
     *
     * @param content ???????????????Json???????????????
     * @param socket  ?????????socket
     * @throws IOException socket????????????????????????
     */
    private void sendTextMessages(String content, Socket socket) throws IOException {
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
            DataHandleUtil.encodeData(buffer);
            dos.write(buffer, 0, length);
            dos.flush();
        }

        dos.write(DataHandleUtil.encodeData("\t\r".getBytes()));

    }

    /**
     * ??????????????????????????????????????????
     *
     * @param file        ???????????????
     * @param messageBean ??????????????????????????????????????????????????????
     * @throws IOException ??????IOException
     */
    public void sendFile(File file, MessageBean messageBean) throws IOException {
        Log.d(TAG, "sendPicOrFile: " + messageBean.type);
        DataOutputStream dos = new DataOutputStream(mClientSocket.getOutputStream());

        int length;
        byte[] buffer = new byte[1024];

        //??????????????????
        dos.write(messageBean.type);
        //??????????????????
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

        FileInputStream fileInputStream;
        fileInputStream = new FileInputStream(file);

        while ((length = fileInputStream.read(buffer)) != -1) {
            DataHandleUtil.encodeData(buffer);
            dos.write(buffer, 0, length);
            dos.flush();
        }
        dos.write(DataHandleUtil.encodeData("\t\r".getBytes()));
    }

    /**
     * ??????????????????
     * ???????????????????????????ServerService?????????????????????
     * ???????????????????????????Socket?????????????????????
     */
    private void initDeviceRole() {
        //???SharedPreferences???????????????????????????????????????????????????????????????????????????????????????
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.starChatData), MODE_PRIVATE);
        mRole = sharedPreferences.getString(getString(R.string.role), null);
        final String serverIP = sharedPreferences.getString(getString(R.string.serverIP), null);
        if (mRole != null && serverIP != null) {
            if (mRole.equals(getString(R.string.client))) {

                //??????????????????Socket???????????????
                new Thread(() -> {
                    try {
                        mClientSocket = new Socket(serverIP, SocketUtil.PORT);
                        new Thread(new ClientThread(mClientSocket)).start();
                    } catch (IOException e) {
                        //????????????????????????????????????
                        Log.d(TAG, "Client run: " + "?????????????????????");
                        send2Handler(null, SHOW_ON_DIALOG_CODE, mHandler);
                    }
                }).start();

            } else {
                Log.d(TAG, "initServerOrClient: recover");
                //????????????
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
     * ???????????????????????????????????????????????????????????????
     */
    private void showDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(ChatRoomActivity.this)
                .setTitle("??????")
                .setMessage("???????????????????????????????????????")
                .setPositiveButton("??????", (dialog, which) -> {
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
     * ??????????????????????????????
     *
     * @param strJson ???????????????Json???????????????
     */
    private void showMessages(String strJson) {
        MessageBean messageBean = GsonUtil.getObject(strJson);
        mMessageBeans.add(messageBean);
        mMessageAdapter.notifyDataSetChanged();
        if (mMessageAdapter.getItemCount() > 0) {
            mLayoutManager.scrollToPositionWithOffset(mMessageAdapter.getItemCount() - 1, 0);
        }
    }

    /**
     * ?????????????????????????????????????????????????????????
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

                    if (flag == MsgTypeUtil.SELF_IMG) {//??????
                        receiveFile(dis, MsgTypeUtil.OTHERS_IMG);
                    } else if (flag == MsgTypeUtil.SELF_FILE) {//??????
                        receiveFile(dis, MsgTypeUtil.OTHERS_FILE);
                    } else if (flag == MsgTypeUtil.SELF_AUDIO) {//??????
                        receiveFile(dis, MsgTypeUtil.OTHERS_AUDIO);
                    } else if (flag == MsgTypeUtil.SELF_PROFILE) {
                        receiveFile(dis, MsgTypeUtil.OTHERS_PROFILE);
                    } else if (flag == MsgTypeUtil.SELF_MSG) {//??????
                        int length;

                        byte[] buffer = new byte[1024];

                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                        while ((length = dis.read(buffer)) != -1) {
                            DataHandleUtil.decodeData(buffer);
                            byteArrayOutputStream.write(buffer, 0, length);

                            //??????????????????
                            if (length > 1 && buffer[length - 2] == '\t' && buffer[length - 1] == '\r') {
                                break;
                            } else if (length == 1 && buffer[length - 1] == '\r') {
                                break;
                            }
                        }

                        MessageBean messageBean = GsonUtil.getObject(byteArrayOutputStream.toString());
                        if (messageBean != null) {
                            //??????????????????
                            File profileDir = new File(getExternalCacheDir().getAbsolutePath() + File.separator +
                                    "MEMBERS" + File.separator + socket.getInetAddress());
                            String profilePath = null;
                            if (profileDir.exists()) {
                                File[] file1 = profileDir.listFiles();
                                if (file1.length > 0) {
                                    profilePath = profileDir.getAbsolutePath() + File.separator + file1[0].getName();
                                }
                            }
                            if (profilePath != null) {
                                Log.d(TAG, "receivePicOrFile: " + profilePath);
                                messageBean.profilePath = profilePath;
                            } else {
                                messageBean.profilePath = "";
                            }
                            messageBean.time = TimeUtil.getCurrentTime();
                            messageBean.type = MsgTypeUtil.OTHERS_MSG;
                            send2Handler(GsonUtil.toJsonStr(messageBean), SHOW_ON_CHAT_LIST_CODE, mHandler);
                        }
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
                send2Handler(null, SHOW_ON_DIALOG_CODE, mHandler);
            }

        }

        /**
         * ?????????????????????????????????????????????
         *
         * @param dis  ???????????????
         * @param type ??????????????????????????????
         * @throws IOException ????????????????????????
         */
        public void receiveFile(DataInputStream dis, int type) throws IOException {
            int length = 0;
            byte[] buffer = new byte[1024];
            int offset;
            while ((offset = dis.read()) != '\n') {
                length += offset;
            }

            length = dis.read(buffer, 0, length);

            String mark = new String(buffer, 0, length);

            MessageBean fileBean = GsonUtil.getObject(mark);
            String finalDirName;
            if (type == MsgTypeUtil.OTHERS_FILE) {
                finalDirName = "DOCUMENTS";
            } else if (type == MsgTypeUtil.OTHERS_IMG) {
                finalDirName = "IMAGES";
            } else if (type == MsgTypeUtil.OTHERS_PROFILE) {
                finalDirName = "MEMBERS";
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
            } else if (type == MsgTypeUtil.OTHERS_PROFILE) {
                dir = new File(dir.getAbsolutePath() + File.separator + socket.getInetAddress());
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

            //??????MEMBERS????????????????????????????????????
            if (type == MsgTypeUtil.OTHERS_PROFILE) {
                File[] files = dir.listFiles();
                if (files.length > 0){
                    for (File file1 : files) {
                        file1.delete();
                    }
                }
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);

            while ((length = dis.read(buffer)) != -1) {
                DataHandleUtil.decodeData(buffer);
                fileOutputStream.write(buffer, 0, length);
                fileOutputStream.flush();

                if (length > 1 && buffer[length - 2] == '\t' && buffer[length - 1] == '\r') {
                    break;
                } else if (length == 1 && buffer[length - 1] == '\r') {
                    break;
                }
            }

            if (type != MsgTypeUtil.OTHERS_PROFILE) {
                MessageBean messageBean = new MessageBean();
                File profileDir = new File(getExternalCacheDir().getAbsolutePath() + File.separator +
                        "MEMBERS" + File.separator + socket.getInetAddress());
                String profilePath = null;
                if (profileDir.exists()) {
                    File[] file1 = profileDir.listFiles();
                    if (file1.length > 0) {
                        profilePath = profileDir.getAbsolutePath() + File.separator + file1[0].getName();
                    }
                }
                if (profilePath != null) {
                    Log.d(TAG, "receivePicOrFile: " + profilePath);
                    messageBean.profilePath = profilePath;
                } else {
                    messageBean.profilePath = "";
                }
                messageBean.nickname = fileBean.nickname;
                messageBean.type = type;
                messageBean.time = TimeUtil.getCurrentTime();
                messageBean.fileName = fileBean.fileName;
                messageBean.filePath = file.getAbsolutePath();
                if (type == MsgTypeUtil.OTHERS_AUDIO) {
                    messageBean.audioLength = fileBean.audioLength;
                }
                messageBean.fileLength = FileUtil.getFileLength(file);
                messageBean.fileType = fileBean.fileType;
                send2Handler(GsonUtil.toJsonStr(messageBean), SHOW_ON_CHAT_LIST_CODE, mHandler);
            }
        }
    }

    /**
     * TODO
     *
     * @param nickname
     * @param absolutePath
     */
    private void saveToDB(String nickname, String absolutePath) {

    }

    /**
     * ??????????????????????????????
     *
     * @param requestCode ?????????
     * @param resultCode  ?????????
     * @param data        ?????????????????????
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*????????????*/
        if (requestCode == PictureSelector.SELECT_REQUEST_CODE) {
            if (data != null) {
                PictureBean pictureBean = data.getParcelableExtra(PictureSelector.PICTURE_RESULT);
                Uri uri = pictureBean.getUri();
                //??????????????????
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
                //????????????
                Luban.with(ChatRoomActivity.this)
                        .load(uri)
                        .ignoreBy(-1)
                        .setTargetDir(path)
                        .filter(path1 -> !(TextUtils.isEmpty(path1) || path1.toLowerCase().endsWith(".gif")))
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
                                showMessages(GsonUtil.toJsonStr(messageBean));

                                new Thread(() -> {
                                    //????????????
                                    try {
                                        if (mRole.equals("?????????")) {
                                            if (mClientSocket == null) {
                                                return;
                                            }
                                            sendFile(file, messageBean);
                                        } else {
                                            String fileStr = GsonUtil.toJsonStr(messageBean);
                                            mIntent.putExtra("action", "picture");
                                            mIntent.putExtra("picture_info", fileStr);
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
     * ??????Handler????????????
     *
     * @param content ??????
     * @param what    ?????????
     * @param handler mHandler
     */
    public void send2Handler(String content, int what, Handler handler) {
        Message message = new Message();
        message.what = what;
        message.obj = content;
        handler.sendMessage(message);
    }

    /**
     * ?????????????????????????????????MainActivity
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    /**
     * ?????????????????????
     *
     * @param menu ????????????
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * ??????????????????SettingActivity
     *
     * @param item ?????????
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

    @Override
    protected void onResume() {
        super.onResume();
        mNickname = mSharedPreferences.getString("nickname", "?????????");
        String profilePath = mSharedPreferences.getString("profile_path", "");
        if(!profilePath.equals(originalProfilePath)){
            Log.d(TAG, "onStart: " + profilePath);
            if (!profilePath.equals("")) {
                File file = new File(profilePath);
                MessageBean messageBean = new MessageBean();
                messageBean.nickname = mNickname;
                messageBean.type = MsgTypeUtil.SELF_PROFILE;
                messageBean.fileName = file.getName();
                messageBean.filePath = profilePath;

                if (mClientSocket != null && mRole.equals("?????????")) {
                    new Thread(() -> {
                        try {
                            sendFile(file, messageBean);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                if (mRole.equals("?????????")) {
                    mIntent.putExtra("action", "profile");
                    mIntent.putExtra("profile_info", GsonUtil.toJsonStr(messageBean));
                    startService(mIntent);
                }

            }
        }
    }

    /**
     * ?????????Activity?????????ServerService
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        if (mRole.equals("?????????")) {
            unbindService(mServiceConnection);
            stopService(mIntent);
        }
    }
}
