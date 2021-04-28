//package com.example.starchat;
//
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.graphics.BitmapFactory;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Message;
//import android.text.Editable;
//import android.text.TextUtils;
//import android.text.TextWatcher;
//
//import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.View;
//
//
//import android.view.inputmethod.InputMethodManager;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ImageView;
//
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.annotation.RequiresApi;
//import androidx.appcompat.app.ActionBar;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.constraintlayout.widget.ConstraintLayout;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.app.filepicker.FilePicker;
//import com.app.filepicker.OnFilePickerSelectListener;
//import com.app.filepicker.model.EssFile;
//import com.example.starchat.adapter.MessageAdapter;
//import com.example.starchat.bean.FileBean;
//import com.example.starchat.bean.MessageBean;
//import com.example.starchat.util.GsonUtil;
//import com.example.starchat.util.MsgTypeUtil;
//import com.example.starchat.util.SocketUtil;
//import com.example.starchat.util.TimeUtil;
//import com.example.starchat.util.ToastUtil;
//import com.google.gson.Gson;
//import com.wildma.pictureselector.PictureBean;
//import com.wildma.pictureselector.PictureSelector;
//
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.DataInputStream;
//import java.io.DataOutputStream;
//import java.io.File;
//
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//
//import java.net.ServerSocket;
//import java.net.Socket;
//
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Locale;
//
//import top.zibin.luban.CompressionPredicate;
//import top.zibin.luban.Luban;
//import top.zibin.luban.OnCompressListener;
//
//public class ChatRoomActivity2 extends AppCompatActivity {
//
//    //用于标识Log的字符串
//    public static final String TAG = "ChatRoomActivity";
//
//    //msg.what：数据显示在聊天列表
//    public static final int SHOW_ON_CHAT_LIST_CODE = 1;
//    //msg.what：数据显示在弹窗
//    public static final int SHOW_ON_DIALOG_CODE = 2;
//
//    //SharedPreferences中取数据
//    private SharedPreferences mSharedPreferences;
//    //聊天室中的设备昵称
//    private String mNickname;
//    //从SharedPreference文件中获取的设备角色
//    private String mRole;
//
//    // 消息输入框
//    private EditText mInputText;
//    //聊天信息显示列表
//    private RecyclerView mMsgListView;
//    //消息集合
//    private List<MessageBean> mMessageBeans;
//    //聊天信息列表适配器
//    private MessageAdapter mMessageAdapter;
//    //“更多”，点击可获得更多功能
//    private ImageView mMoreImg;
//    //承载更多功能的布局
//    private ConstraintLayout mMoreFuncLayout;
//    //发送按钮
//    private Button mBnSendMsg;
//    //定位
//    private ImageView mLocatedImg;
//    //图库
//    private ImageView mPhotoImg;
//    //文件
//    private ImageView mFileImg;
//
//    //客户端发送给服务器的消息
//    private String mMsgToServer = "";
//    //服务器发送给所有客户端的消息
//    private String mMsgToAllClient;
//    //客户端Socket
//    private Socket mClientSocket;
//    //服务器Socket
//    private ServerSocket mServerSocket;
//    //接入服务器的所有客户端Socket
//    private List<Socket> listClient = new ArrayList<>();
//
//    //所选文件的路径集合
//    private ArrayList<Uri> mListFilePath = new ArrayList<>();
//
//    //创建Handler执行UI更新，在聊天界面更新消息
//    private Handler mHandler = new Handler(new Handler.Callback() {
//        @Override
//        public boolean handleMessage(@NonNull Message msg) {
//            switch (msg.what) {
//                case SHOW_ON_CHAT_LIST_CODE:
//                    showMessageOnChatRoom((String) msg.obj);
//                    break;
//                case SHOW_ON_DIALOG_CODE:
//                    showDialog();
//                    break;
//                case 5:
//                    mMessageAdapter.notifyDataSetChanged();
//                    if (mMessageAdapter.getItemCount() - 1 > 0) {
//                        mMsgListView.smoothScrollToPosition(mMessageAdapter.getItemCount() - 1);
//                    }
//                    break;
//                default:
//                    break;
//            }
//            return false;
//        }
//    });
//
//    //设备定位信息
//    public String deviceLocation = "";
//    private LinearLayoutManager mLayoutManager;
//
//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_chat_room);
//        getWindow().setStatusBarColor(getResources().getColor(R.color.Tiffany));
//
//        //加载聊天室界面
//        initView();
//
//        //设置监听事件
//        initEvent();
//
//        //初始化设备角色，服务器或客户端，由SharedPreferences文件决定
//        initServerOrClient();
//
//    }
//
//    //初始化界面
//    public void initView() {
//        mMsgListView = findViewById(R.id.message_list_view);
//        mMoreImg = findViewById(R.id.more_img);
//        mMoreFuncLayout = findViewById(R.id.moreFuncLayout);
//        mInputText = findViewById(R.id.input_text);
//        mBnSendMsg = findViewById(R.id.bn_send_msg);
//        mLocatedImg = findViewById(R.id.location_img);
//        mPhotoImg = findViewById(R.id.photo_img);
//        mFileImg = findViewById(R.id.file_img);
//
//        //初始化SharedPreferences
//        mSharedPreferences = getSharedPreferences(getString(R.string.starChatData), MODE_PRIVATE);
//        mNickname = mSharedPreferences.getString(getString(R.string.nickname), getString(R.string.anonymity));
//
//        //设置标题栏
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setTitle(R.string.chatRoom);
//            actionBar.setDisplayHomeAsUpEnabled(true);
//            actionBar.setHomeButtonEnabled(true);
//        }
//
//        mMessageBeans = new ArrayList<>();
//        mMessageAdapter = new MessageAdapter(mMessageBeans, ChatRoomActivity2.this);
//        mLayoutManager = new LinearLayoutManager(this);
//        mMsgListView.setLayoutManager(mLayoutManager);
//        mMsgListView.setAdapter(mMessageAdapter);
//
//        mMessageAdapter.notifyDataSetChanged();
//    }
//
//    //设置组件的监听事件
//    public void initEvent() {
//
//        //监听更多按钮的点击
//        mMoreImg.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mMoreFuncLayout.setVisibility(View.VISIBLE);
//            }
//        });
//
//        //监听输入框的点击事件
//        mInputText.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                if (mMessageAdapter.getItemCount() - 1 > 0) {
//                    mMsgListView.smoothScrollToPosition(mMessageAdapter.getItemCount() - 1);
//                }
//                mMoreFuncLayout.setVisibility(View.GONE);
//
//            }
//        });
//
//        //监听输入框的字符串输入变化事件，主要是改变发送按钮和更多功能的显示与隐藏
//        mInputText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//                mMoreFuncLayout.setVisibility(View.GONE);
//
//                if (s.length() == 0) {
//                    mBnSendMsg.setVisibility(View.GONE);
//                    mMoreImg.setVisibility(View.VISIBLE);
//                } else {
//                    mMoreImg.setVisibility(View.GONE);
//                    mBnSendMsg.setVisibility(View.VISIBLE);
//                }
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//
//            }
//        });
//
//        //监听发送按钮的点击事件
//        mBnSendMsg.setOnClickListener(new View.OnClickListener() {
//            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
//            @Override
//            public void onClick(View v) {
//                //将数据转化成Json格式
//                MessageBean messageBean = getMessageBean(TimeUtil.getCurrentTime(), mNickname, mInputText.getText().toString(), MsgTypeUtil.SELF_MSG);
//                if (mRole.equals(getString(R.string.client))) {
//                    mMsgToServer = GsonUtil.toJsonStr(messageBean);
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                sendMessage(mMsgToServer, mClientSocket);
//                                send2Handler(mMsgToServer, SHOW_ON_CHAT_LIST_CODE, mHandler);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }).start();
//                } else {
//                    mMsgToAllClient = GsonUtil.toJsonStr(messageBean);
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                sendMsg2AllClient(mMsgToAllClient);
//                                send2Handler(mMsgToAllClient, SHOW_ON_CHAT_LIST_CODE, mHandler);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }).start();
//                }
//                mInputText.setText("");
//            }
//        });
//
//        //监听消息列表滚动时隐藏软键盘
//        mMsgListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
//                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
//                    InputMethodManager inputMethodManager = (InputMethodManager) ChatRoomActivity2.this.getSystemService(INPUT_METHOD_SERVICE);
//                    if (inputMethodManager != null) {
//                        inputMethodManager.hideSoftInputFromWindow(recyclerView.getWindowToken(), 0);
//                    }
//                }
//            }
//        });
//
//        //点击定位，产生设备所处地方经纬度，并发送给其他设备
//        mLocatedImg.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ToastUtil.showToast(ChatRoomActivity2.this, deviceLocation);
//
//                //按情况获取MessageBean
//                MessageBean messageBean = getMessageBean(TimeUtil.getCurrentTime(), mNickname, deviceLocation, MsgTypeUtil.SELF_MSG);
//
//                if (mRole.equals(getString(R.string.client))) {
//                    mMsgToServer = GsonUtil.toJsonStr(messageBean);
//                    new Thread(() -> {
//                        try {
//                            sendMessage(mMsgToServer, mClientSocket);
//                            send2Handler(mMsgToServer, SHOW_ON_CHAT_LIST_CODE, mHandler);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }).start();
//                } else {
//                    mMsgToAllClient = GsonUtil.toJsonStr(messageBean);
//                    new Thread(() -> {
//                        try {
//                            sendMsg2AllClient(mMsgToAllClient);
//                            send2Handler(mMsgToAllClient, SHOW_ON_CHAT_LIST_CODE, mHandler);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }).start();
//                }
//            }
//        });
//
//        //打开图库，选择图片
//        mPhotoImg.setOnClickListener(v -> PictureSelector
//                .create(ChatRoomActivity2.this, PictureSelector.SELECT_REQUEST_CODE)
//                .selectPicture(false));
//
//        //打开文件夹，选择文件
//        mFileImg.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                FilePicker.chooseForMimeType().setTheme(R.style.FilePicker_Elec).setMaxCount(10)
//                        .setFileTypes("docx", "pptx", "xlsx", "pdf", "zip", "mp3", "mp4")
//                        .selectFiles(new OnFilePickerSelectListener() {
//                            @Override
//                            public void onFilePickerResult(List<EssFile> essFiles) {
//                                for (EssFile essFile : essFiles) {
//                                    Log.d(TAG, "onFilePickerResult: ok");
//                                    MessageBean messageBean = new MessageBean();
//                                    messageBean.nickname = mNickname;
//                                    messageBean.time = TimeUtil.getCurrentTime();
//                                    messageBean.type = MsgTypeUtil.SELF_FILE;
//                                    messageBean.fileName = essFile.getName();
//                                    messageBean.fileType = getFileType(essFile.getName());
//                                    messageBean.fileLength = getFileLength(essFile.getFile());
//                                    messageBean.filePath = essFile.getAbsolutePath();
//                                    showMessageOnChatRoom(GsonUtil.toJsonStr(messageBean));
//
//                                    //客户端发给服务器
//                                    File file = essFile.getFile();
//                                    FileBean fileBean = new FileBean();
//                                    fileBean.fileName = essFile.getName();
//                                    fileBean.fileType = getFileType(essFile.getName());
//                                    fileBean.senderName = mNickname;
//                                    fileBean.fileLength = file.length();
//
//                                    new Thread(new Runnable() {
//                                        @Override
//                                        public void run() {
//
//                                            try {
//
//                                                if (mRole.equals("客户端")) {
//
//                                                    DataOutputStream dataOutputStream = new DataOutputStream(mClientSocket.getOutputStream());
//                                                    FileInputStream fileInputStream = new FileInputStream(file);
//
//                                                    int length;
//                                                    byte[] buffer = new byte[1024];
//
//                                                    //传文件标志
//                                                    dataOutputStream.write(MsgTypeUtil.SELF_FILE);
////                                                    dataOutputStream.write(GsonUtil.fileBeanToJsonStr(fileBean).getBytes().length);
////                                                    dataOutputStream.write(GsonUtil.fileBeanToJsonStr(fileBean).getBytes());
//                                                    dataOutputStream.flush();
//
//                                                    while ((length = fileInputStream.read(buffer)) != -1) {
//                                                        dataOutputStream.write(buffer, 0, length);
//                                                        dataOutputStream.flush();
//                                                    }
//
//                                                    dataOutputStream.write("\t\r".getBytes());
//
//                                                } else {
//                                                    for (Socket socket : listClient) {
//
//                                                        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
//                                                        FileInputStream fileInputStream = new FileInputStream(file);
//
//                                                        int length;
//                                                        byte[] buffer = new byte[1024];
//
//                                                        //传文件标志
//                                                        dataOutputStream.write(MsgTypeUtil.SELF_FILE);
//                                                       // dataOutputStream.write(GsonUtil.fileBeanToJsonStr(fileBean).getBytes().length);
//                                                       // dataOutputStream.write(GsonUtil.fileBeanToJsonStr(fileBean).getBytes());
//                                                       // dataOutputStream.flush();
//
//                                                        while ((length = fileInputStream.read(buffer)) != -1) {
//                                                            dataOutputStream.write(buffer, 0, length);
//                                                            dataOutputStream.flush();
//                                                        }
//
//                                                        dataOutputStream.write("\t\r".getBytes());
//                                                    }
//                                                }
//
//
//                                            } catch (IOException e) {
//                                                e.printStackTrace();
//                                            }
//
//                                        }
//                                    }).start();
//                                }
//                            }
//                        }).start(ChatRoomActivity2.this);
//            }
//        });
//    }
//
//    //获得文件大小，带单位
//    private String getFileLength(File file) {
//
//        long length = file.length();
//        String fileLength;
//        if (length < 1024) {
//            fileLength = length + "B";
//        } else if (length < 1024 * 1024) {
//            fileLength = String.format(Locale.CHINA, "%.2f", (double) length / 1024) + "KB";
//        } else if (length < 1024 * 1024 * 1024) {
//            fileLength = String.format(Locale.CHINA, "%.2f", (double) length / 1024 / 1024) + "MB";
//        } else {
//            fileLength = String.format(Locale.CHINA, "%.2f", (double) length / 1024 / 1024 / 1024) + "GB";
//        }
//
//        return fileLength;
//
//    }
//
//    //获得文件后缀，返回对应图标
//    private int getFileType(String fileName) {
//        String fileSuffix = fileName.split("\\.")[fileName.split("\\.").length - 1];
//        switch (fileSuffix) {
//            case "docx":
//            case "doc":
//                return R.mipmap.docx;
//            case "pptx":
//            case "ppt":
//                return R.mipmap.pptx;
//            case "xlsx":
//            case "xls":
//                return R.mipmap.xlsx;
//            case "pdf":
//                return R.mipmap.pdf;
//            case "zip":
//                return R.mipmap.zip;
//            case "mp4":
//                return R.mipmap.video_file;
//            case "mp3":
//                return R.mipmap.audio_file;
//            default:
//                return R.mipmap.file;
//        }
//    }
//
//
//    //将所要传输的内容转化成messageBean格式
//    private MessageBean getMessageBean(String time, String nickname, String msg, int type) {
//        MessageBean messageBean = new MessageBean();
//        messageBean.time = time;
//        messageBean.type = type;
//        messageBean.nickname = nickname;
//        messageBean.msg = msg;
//        return messageBean;
//    }
//
//
//    //建立服务器或者客户端
//    private void initServerOrClient() {
//        //从SharedPreferences取出设备角色，如果是客户端，则连接服务器；反之，启动服务器
//        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.starChatData), MODE_PRIVATE);
//        mRole = sharedPreferences.getString(getString(R.string.role), null);
//        final String serverIP = sharedPreferences.getString(getString(R.string.serverIP), null);
//        if (mRole != null && serverIP != null) {
//            if (mRole.equals(getString(R.string.client))) {
//
//                //客户端，创建Socket连接服务器
//                new Thread(() -> {
//                    try {
//                        mClientSocket = new Socket(serverIP, SocketUtil.PORT);
//                        new Thread(new ClientThread(mClientSocket)).start();
//                    } catch (IOException e) {
//                        //未连接到服务器，弹窗提示
//                        Log.d(TAG, "Client run: " + "未连接到服务器");
//                        send2Handler(null, SHOW_ON_DIALOG_CODE, mHandler);
//                        //e.printStackTrace();
//                    }
//                }).start();
//
//            } else {
//
//                //服务器，启动之，等待客户端连接
//                new Thread(() -> {
//
//                    try {
//                        mServerSocket = new ServerSocket(SocketUtil.PORT);
//                        Log.d(TAG, "Server run: " + "等待客户端接入");
//                        while (true) {
//                            Socket socket = mServerSocket.accept();
//                            Log.d(TAG, "Server run: " + socket.getInetAddress() + ":" + socket.getPort() + "接入");
//                            listClient.add(socket);
//                            new Thread(new ServerThread(socket)).start();
//                        }
//                    } catch (IOException e) {
//                        try {
//                            mServerSocket.close();
//                        } catch (IOException ex) {
//                            ex.printStackTrace();
//                        }
//                        e.printStackTrace();
//                    }
//
//                }).start();
//            }
//        }
//    }
//
//    //弹窗，提示未连接服务器
//    private void showDialog() {
//        AlertDialog alertDialog = new AlertDialog.Builder(ChatRoomActivity2.this)
//                .setTitle("提示")
//                .setMessage("未连接到服务器，请重新进入！")
//                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                        finish();
//                    }
//                })
//                .setCancelable(false)
//                .create();
//        if (!ChatRoomActivity2.this.isFinishing()) {
//            alertDialog.show();
//        }
//    }
//
//    //将信息显示在聊天列表
//    public void showMessageOnChatRoom(String strJson) {
//
//        MessageBean messageBean = GsonUtil.getObject(strJson);
//
//        Log.d(TAG, "showMessageOnChatRoom: " + messageBean.type);
//
//        mMessageBeans.add(messageBean);
//
//        mMessageAdapter.notifyDataSetChanged();
//
//        Log.d(TAG, "showMessageOnChatRoom: " + mMessageAdapter.getItemCount());
//        if (mMessageAdapter.getItemCount() > 0) {
//            //mMsgListView.smoothScrollToPosition(mMessageAdapter.getItemCount() - 1);
//            mLayoutManager.scrollToPositionWithOffset(mMessageAdapter.getItemCount() - 1, 0);
//        }
//
//    }
//
//    //发消息给一个设备
//    private void sendMessage(String content, Socket socket) throws IOException {
//        DataOutputStream dos;
//        if (socket.getOutputStream() != null) {
//            dos = new DataOutputStream(socket.getOutputStream());
//        } else {
//            return;
//        }
//
//        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content.getBytes());
//
//        int length;
//        byte[] buffer = new byte[1024];
//        dos.write(MsgTypeUtil.SELF_MSG);
//        while ((length = byteArrayInputStream.read(buffer)) != -1) {
//            dos.write(buffer, 0, length);
//            dos.flush();
//        }
//
//        dos.write("\t\r".getBytes());
//
//    }
//
//    //服务器向所有客户端发送消息
//    private void sendMsg2AllClient(String content) throws IOException {
//        for (Socket socket : listClient) {
//            sendMessage(content, socket);
//        }
//
//    }
//
//    //客户端线程，处理从服务器读取数据
//    public class ClientThread implements Runnable {
//
//        public Socket socket;
//
//        public ClientThread(Socket socket) {
//            this.socket = socket;
//        }
//
//        @Override
//        public void run() {
//
//            try {
//                while (true) {
//
//                    DataInputStream dis = new DataInputStream(socket.getInputStream());
//
//
//                    int flag;
//
//                    flag = dis.read();
//
//                    if (flag == MsgTypeUtil.SELF_IMG) {//图片文件
//
//                        int length;
//
//                        byte[] buffer = new byte[1024];
//
//                        int fileMarkLength = dis.read();
//                        Log.d(TAG, "Client run: fileMarkLength " + fileMarkLength);
//
//                        length = dis.read(buffer, 0, fileMarkLength);
//
//                        //头标志
//                        String mark = new String(buffer, 0, length);
//                        Log.d(TAG, "Client run: " + mark);
//
//                        FileBean fileBean = GsonUtil.getFileBean(mark);
//
//
//                        File dir = new File(getExternalCacheDir().getAbsolutePath() + File.separator + "IMAGES");
//                        boolean isMkdir = true;
//                        File file;
//                        if (!dir.exists()) {
//                            isMkdir = dir.mkdir();
//                        }
//                        if (isMkdir) {
//                            file = new File(dir.getAbsolutePath() + File.separator + fileBean.fileName);
//                        } else {
//                            file = new File(getExternalCacheDir().getAbsolutePath() + File.separator + fileBean.fileName);
//                        }
//                        Log.d(TAG, "Client run: " + getExternalCacheDir().getAbsolutePath() + File.separator + fileBean.fileName);
//                        FileOutputStream fos = new FileOutputStream(file);
//
//                        while ((length = dis.read(buffer)) != -1) {
//
//                            fos.write(buffer, 0, length);
//                            fos.flush();
//
//                            if (length > 1 && buffer[length - 2] == '\t' && buffer[length - 1] == '\r') {
//                                break;
//                            } else if (length == 1 && buffer[length - 1] == '\r') {
//                                break;
//                            }
//                        }
//
//                        MessageBean messageBean = new MessageBean();
//                        messageBean.nickname = fileBean.senderName;
//                        messageBean.fileLength = getFileLength(file);
//                        messageBean.filePath = file.getAbsolutePath();
//                        messageBean.fileName = file.getName();
//                        messageBean.type = MsgTypeUtil.OTHERS_IMG;
//                        messageBean.time = TimeUtil.getCurrentTime();
//                        send2Handler(GsonUtil.toJsonStr(messageBean), SHOW_ON_CHAT_LIST_CODE, mHandler);
//
//                        fos.close();
//
//                    } else if (flag == MsgTypeUtil.SELF_MSG) {//文本
//
//                        int length;
//
//                        byte[] buffer = new byte[1024];
//
//                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//
//                        while ((length = dis.read(buffer)) != -1) {
//                            byteArrayOutputStream.write(buffer, 0, length);
//
//                            //传输结束标志
//                            if (length > 1 && buffer[length - 2] == '\t' && buffer[length - 1] == '\r') {
//                                break;
//                            } else if (length == 1 && buffer[length - 1] == '\r') {
//                                break;
//                            }
//                        }
//
//                        MessageBean messageBean = GsonUtil.getObject(byteArrayOutputStream.toString());
//                        if (messageBean != null) {
//                            messageBean.time = TimeUtil.getCurrentTime();
//                            messageBean.type = MsgTypeUtil.OTHERS_MSG;
//
//                            send2Handler(GsonUtil.toJsonStr(messageBean), SHOW_ON_CHAT_LIST_CODE, mHandler);
//                        }
//                    } else {
//
//                        int length;
//                        byte[] buffer = new byte[1024];
//
//                        length = dis.read();
//
//                        length = dis.read(buffer, 0, length);
//
//                        if(length == -1){
//                            return;
//                        }
//                        String mark = new String(buffer, 0, length);
//
//                        Log.d(TAG, "Client run: " + mark);
//                        Log.d(TAG, "Client run: ok");
//
//                        FileBean fileBean = GsonUtil.getFileBean(mark);
//
//                        File dir = new File(getExternalCacheDir().getAbsolutePath() + File.separator + "DOCUMENTS");
//                        boolean isMkdir = true;
//                        File file;
//                        if (!dir.exists()) {
//                            isMkdir = dir.mkdir();
//                        }
//                        if (isMkdir) {
//                            file = new File(dir.getAbsolutePath() + File.separator + fileBean.fileName);
//                        } else {
//                            file = new File(getExternalCacheDir().getAbsolutePath() + File.separator + fileBean.fileName);
//                        }
//                        FileOutputStream fileOutputStream = new FileOutputStream(file);
//
//                        while ((length = dis.read(buffer)) != -1) {
//                            fileOutputStream.write(buffer, 0, length);
//                            fileOutputStream.flush();
//
//                            if (length > 1 && buffer[length - 2] == '\t' && buffer[length - 1] == '\r') {
//                                break;
//                            } else if (length == 1 && buffer[length - 1] == '\r') {
//                                break;
//                            }
//                        }
//
//                        MessageBean messageBean = new MessageBean();
//                        messageBean.nickname = fileBean.senderName;
//                        messageBean.type = MsgTypeUtil.OTHERS_FILE;
//                        messageBean.time = TimeUtil.getCurrentTime();
//                        messageBean.fileName = fileBean.fileName;
//                        messageBean.filePath = file.getAbsolutePath();
//                        messageBean.fileLength = getFileLength(file);
//                        messageBean.fileType = fileBean.fileType;
//                        send2Handler(GsonUtil.toJsonStr(messageBean), SHOW_ON_CHAT_LIST_CODE, mHandler);
//                    }
//
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                send2Handler(null, SHOW_ON_DIALOG_CODE, mHandler);
//            }
//
//        }
//    }
//
//    //服务器线程，处理从客户端读取数据
//    public class ServerThread implements Runnable {
//
//        private Socket socket;
//
//        public ServerThread(Socket socket) {
//            this.socket = socket;
//        }
//
//        @Override
//        public void run() {
//
//            try {
//                while (true) {
//                    DataInputStream dis = new DataInputStream(socket.getInputStream());
//
//                    int flag;
//
//                    flag = dis.read();
//
//                    if (flag == MsgTypeUtil.SELF_IMG) {//图片文件
//
//                        int length;
//
//                        byte[] buffer = new byte[1024];
//
//                        int fileMarkLength = dis.read();
//
//                        length = dis.read(buffer, 0, fileMarkLength);
//
//                        //头标志
//                        String mark = new String(buffer, 0, length);
//
//                        FileBean fileBean = GsonUtil.getFileBean(mark);
//
//
//                        File dir = new File(getExternalCacheDir().getAbsolutePath() + File.separator + "IMAGES");
//                        boolean isMkdir = true;
//                        File file;
//                        if (!dir.exists()) {
//                            isMkdir = dir.mkdir();
//                        }
//                        if (isMkdir) {
//                            file = new File(dir.getAbsolutePath() + File.separator + fileBean.fileName);
//                        } else {
//                            file = new File(getExternalCacheDir().getAbsolutePath() + File.separator + fileBean.fileName);
//                        }
//                        FileOutputStream fos = new FileOutputStream(file);
//
//                        while ((length = dis.read(buffer)) != -1) {
//
//                            fos.write(buffer, 0, length);
//                            fos.flush();
//
//                            if (length > 1 && buffer[length - 2] == '\t' && buffer[length - 1] == '\r') {
//                                break;
//                            } else if (length == 1 && buffer[length - 1] == '\r') {
//                                break;
//                            }
//                        }
//
//                        //发给所有客户端
//                        for (Socket _socket : listClient) {
//
//                            if (_socket == socket) {
//                                continue;
//                            }
//
//                            DataOutputStream dataOutputStream = new DataOutputStream(_socket.getOutputStream());
//                            FileInputStream fileInputStream = new FileInputStream(file);
//                            dataOutputStream.write(MsgTypeUtil.SELF_IMG);
//                            dataOutputStream.write(GsonUtil.fileBeanToJsonStr(fileBean).getBytes().length);
//                            dataOutputStream.write(GsonUtil.fileBeanToJsonStr(fileBean).getBytes());
//                            dataOutputStream.flush();
//
//                            while ((length = fileInputStream.read(buffer)) != -1) {
//                                dataOutputStream.write(buffer, 0, length);
//                                dataOutputStream.flush();
//                            }
//
//                            dataOutputStream.write("\t\r".getBytes());
//
//                        }
//
//                        MessageBean messageBean = new MessageBean();
//                        messageBean.nickname = fileBean.senderName;
//                        messageBean.filePath = file.getAbsolutePath();
//                        messageBean.fileLength = getFileLength(file);
//                        messageBean.fileName = file.getName();
//                        messageBean.type = MsgTypeUtil.OTHERS_IMG;
//                        messageBean.time = TimeUtil.getCurrentTime();
//                        send2Handler(GsonUtil.toJsonStr(messageBean), SHOW_ON_CHAT_LIST_CODE, mHandler);
//
//                        fos.close();
//
//                    } else if (flag == MsgTypeUtil.SELF_MSG) {//文本
//
//                        int length;
//                        byte[] buffer = new byte[1024];
//
//                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                        while ((length = dis.read(buffer)) != -1) {
//                            Log.d(TAG, "run: length:" + length);
//                            byteArrayOutputStream.write(buffer, 0, length);
//
//                            if (length > 1 && buffer[length - 2] == '\t' && buffer[length - 1] == '\r') {
//                                break;
//                            } else if (length == 1 && buffer[length - 1] == '\r') {
//                                break;
//                            }
//                        }
//
//                        String content = byteArrayOutputStream.toString();
//
//
//                        //发送给所有客户端
//                        for (Socket _socket : listClient) {
//
//                            if (socket == _socket) {
//                                continue;
//                            }
//
//                            DataOutputStream dos = new DataOutputStream(_socket.getOutputStream());
//                            dos.write(MsgTypeUtil.SELF_MSG);
//                            dos.write(content.getBytes(), 0, content.getBytes().length);
//                            dos.write("\t\r".getBytes());
//
//                        }
//
//                        Log.d(TAG, "run: " + content);
//                        MessageBean messageBean = GsonUtil.getObject(content);
//                        messageBean.type = MsgTypeUtil.OTHERS_MSG;
//                        messageBean.time = TimeUtil.getCurrentTime();
//                        send2Handler(GsonUtil.toJsonStr(messageBean), SHOW_ON_CHAT_LIST_CODE, mHandler);
//                    } else {
//
//                        int length;
//                        byte[] buffer = new byte[1024];
//
//                        length = dis.read();
//
//                        length = dis.read(buffer, 0, length);
//
//                        String mark = new String(buffer, 0, length);
//
//                        FileBean fileBean = GsonUtil.getFileBean(mark);
//
//                        File dir = new File(getExternalCacheDir().getAbsolutePath() + File.separator + "DOCUMENTS");
//                        boolean isMkdir = true;
//                        File file;
//                        if (!dir.exists()) {
//                            isMkdir = dir.mkdir();
//                        }
//                        if (isMkdir) {
//                            file = new File(dir.getAbsolutePath() + File.separator + fileBean.fileName);
//                        } else {
//                            file = new File(getExternalCacheDir().getAbsolutePath() + File.separator + fileBean.fileName);
//                        }
//
//                        FileOutputStream fileOutputStream = new FileOutputStream(file);
//
//                        while ((length = dis.read(buffer)) != -1) {
//                            fileOutputStream.write(buffer, 0, length);
//                            fileOutputStream.flush();
//
//                            if (length > 1 && buffer[length - 2] == '\t' && buffer[length - 1] == '\r') {
//                                break;
//                            } else if (length == 1 && buffer[length - 1] == '\r') {
//                                break;
//                            }
//                        }
//
//                        //发给所有客户端
//                        for (Socket _socket : listClient) {
//
//                            if (_socket == socket) {
//                                continue;
//                            }
//
//                            DataOutputStream dataOutputStream = new DataOutputStream(_socket.getOutputStream());
//                            FileInputStream fileInputStream = new FileInputStream(file);
//                            dataOutputStream.write(MsgTypeUtil.SELF_FILE);
//                            dataOutputStream.write(GsonUtil.fileBeanToJsonStr(fileBean).getBytes().length);
//                            dataOutputStream.write(GsonUtil.fileBeanToJsonStr(fileBean).getBytes());
//                            dataOutputStream.flush();
//
//                            while ((length = fileInputStream.read(buffer)) != -1) {
//                                dataOutputStream.write(buffer, 0, length);
//                                dataOutputStream.flush();
//                            }
//
//                            dataOutputStream.write("\t\r".getBytes());
//
//                        }
//
//                        MessageBean messageBean = new MessageBean();
//                        messageBean.nickname = fileBean.senderName;
//                        messageBean.type = MsgTypeUtil.OTHERS_FILE;
//                        messageBean.time = TimeUtil.getCurrentTime();
//                        messageBean.fileName = fileBean.fileName;
//                        messageBean.fileLength = getFileLength(file);
//                        messageBean.filePath = file.getAbsolutePath();
//                        messageBean.fileType = fileBean.fileType;
//                        send2Handler(GsonUtil.toJsonStr(messageBean), SHOW_ON_CHAT_LIST_CODE, mHandler);
//
//                    }
//                }
//            } catch (
//                    IOException e) {
//                listClient.remove(socket);
//                e.printStackTrace();
//            }
//        }
//
//    }
//
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        /*结果回调*/
//        if (requestCode == PictureSelector.SELECT_REQUEST_CODE) {
//            if (data != null) {
//
//                PictureBean pictureBean = data.getParcelableExtra(PictureSelector.PICTURE_RESULT);
//
//                Uri uri = pictureBean.getUri();
//
//                //图片存储位置
//                File dir = new File(getExternalCacheDir().getAbsolutePath() + File.separator + "COMPRESSED");
//                boolean isMkdir = true;
//                String path;
//                if (!dir.exists()) {
//                    isMkdir = dir.mkdir();
//                }
//                if (isMkdir) {
//                    path = dir.getPath();
//                } else {
//                    path = getExternalCacheDir().getAbsolutePath();
//                }
//                //压缩图片
//                Luban.with(ChatRoomActivity2.this)
//                        .load(uri)
//                        .ignoreBy(-1)
//                        .setTargetDir(path)
//                        .filter(new CompressionPredicate() {
//                            @Override
//                            public boolean apply(String path) {
//                                return !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif"));
//                            }
//                        })
//                        .setCompressListener(new OnCompressListener() {
//                            @Override
//                            public void onStart() {
//
//                            }
//
//                            @Override
//                            public void onSuccess(File file) {
//
//                                Log.d(TAG, "onSuccess: ok");
//
//                                MessageBean messageBean = new MessageBean();
//                                messageBean.type = MsgTypeUtil.SELF_IMG;
//                                messageBean.nickname = mNickname;
//                                messageBean.filePath = file.getAbsolutePath();
//                                messageBean.time = TimeUtil.getCurrentTime();
//                                messageBean.fileLength = getFileLength(file);
//                                messageBean.fileName = file.getName();
//                                Log.d(TAG, "onSuccess: " + getFileLength(file));
//                                showMessageOnChatRoom(GsonUtil.toJsonStr(messageBean));
//
//                                new Thread(() -> {
//                                    //发送图片
//                                    try {
//                                        if (mRole.equals("客户端")) {
//                                            if (mClientSocket == null) {
//                                                return;
//                                            }
//                                            DataOutputStream dos = new DataOutputStream(mClientSocket.getOutputStream());
//
//                                            FileInputStream fis = new FileInputStream(file);
//
//                                            FileBean fileBean = new FileBean();
//                                            fileBean.fileName = file.getName();
//                                            fileBean.fileLength = file.length();
//                                            fileBean.senderName = mNickname;
//                                            fileBean.fileType = -1;
//
//                                            int length;
//                                            byte[] buffer = new byte[1024];
//
//                                            //写进图片文件标志
//                                            dos.write(MsgTypeUtil.SELF_IMG);//标志，代表图片文件
//                                            dos.write(GsonUtil.fileBeanToJsonStr(fileBean).getBytes().length);//标志长度
//                                            Log.d(TAG, "onSuccess: mark length" + GsonUtil.fileBeanToJsonStr(fileBean));
//                                            dos.write(GsonUtil.fileBeanToJsonStr(fileBean).getBytes());
//                                            dos.flush();
//
//                                            //写进文件内容
//                                            while ((length = fis.read(buffer)) != -1) {
//                                                dos.write(buffer, 0, length);
//                                                dos.flush();
//                                            }
//                                            dos.write("\t\r".getBytes());
//
//                                            fis.close();
//
//                                        } else {
//                                            FileBean fileBean = new FileBean();
//                                            fileBean.fileName = file.getName();
//                                            fileBean.fileLength = file.length();
//                                            fileBean.senderName = mNickname;
//                                            fileBean.fileType = -1;
//                                            for (Socket socket : listClient) {
//
//                                                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//                                                FileInputStream fis = new FileInputStream(file);
//
//                                                //写进图片文件标志
//                                                dos.write(MsgTypeUtil.SELF_IMG);//标志，代表图片文件
//                                                dos.write(GsonUtil.fileBeanToJsonStr(fileBean).getBytes().length);//标志长度
//                                                Log.d(TAG, "onSuccess: mark length" + GsonUtil.fileBeanToJsonStr(fileBean));
//                                                dos.write(GsonUtil.fileBeanToJsonStr(fileBean).getBytes());
//                                                dos.flush();
//
//                                                int length;
//                                                byte[] buffer = new byte[1024];
//
//                                                while ((length = fis.read(buffer)) != -1) {
//                                                    dos.write(buffer, 0, length);
//                                                    dos.flush();
//                                                }
//                                                dos.write("\t\r".getBytes());
//                                                fis.close();
//                                            }
//                                        }
//
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                }).start();
//                            }
//
//                            @Override
//                            public void onError(Throwable e) {
//                                Log.d(TAG, "onError: " + e.getLocalizedMessage());
//                            }
//                        }).launch();
//
//            }
//        }
//    }
//
//
//    //发送给Handler处理消息
//    public void send2Handler(String content, int what, Handler handler) {
//        Message message = new Message();
//        message.what = what;
//        message.obj = content;
//        handler.sendMessage(message);
//    }
//
//    //返回前一Activity
//    @Override
//    public boolean onSupportNavigateUp() {
//        finish();
//        return super.onSupportNavigateUp();
//    }
//
//    //创建菜单，可看到进入聊天室的成员和设置信息
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.settings, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    //点击菜单进入设置页面的回调方法
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//
//        switch (item.getItemId()) {
//            case R.id.setting:
//                Intent intent = new Intent(ChatRoomActivity2.this, SettingActivity.class);
//                startActivity(intent);
//                break;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
//
//    //从SettingActivity返回时，取出更新后的nickname
//    @Override
//    protected void onStart() {
//        super.onStart();
//        mNickname = mSharedPreferences.getString(getString(R.string.nickname), getString(R.string.anonymity));
//    }
//
//    //返回MainActivity，服务器socket关闭
//    @Override
//    protected void onStop() {
//        super.onStop();
//        if (mRole.equals("服务器")) {
//            try {
//                mServerSocket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//}
