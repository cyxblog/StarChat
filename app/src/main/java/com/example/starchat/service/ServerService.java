package com.example.starchat.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.starchat.R;
import com.example.starchat.bean.MessageBean;
import com.example.starchat.util.FileUtil;
import com.example.starchat.util.GsonUtil;
import com.example.starchat.util.MsgTypeUtil;
import com.example.starchat.util.SocketUtil;
import com.example.starchat.util.TimeUtil;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * 后台运行服务器，设备角色为服务器的设备绑定该Service
 * @author 陈
 */
public class ServerService extends Service {

    public static final String TAG = "ServerService";
    public ServerSocket mServerSocket;
    public ArrayList<Socket> listClient = new ArrayList<>();

    //发送给客户端的数据
    public String dataToClient;

    public boolean isServiceConnected = true;

    @Override
    public void onCreate() {
        super.onCreate();

        //启动服务器，等待客户端连接
        new Thread(() -> {
            try {
                mServerSocket = new ServerSocket(SocketUtil.PORT);
                Log.d(TAG, "onCreate : " + "等待客户端接入");
                listClient.clear();
                while (true) {
                    Socket socket = mServerSocket.accept();
                    Log.d(TAG, "onCreate: " + socket.getInetAddress() + ":" + socket.getPort() + "接入");
                    if (!listClient.contains(socket)) {
                        listClient.add(socket);
                    }
                    new Thread(new ServerThread(socket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();

        //定时向Activity发送数据
        new Thread(() -> {
            int number = 0;
            while (isServiceConnected) {
                Log.d(TAG, "onCreate run: " + dataToClient + number++);
                if (dataToClient != null && mDataChanged != null) {
                    //Log.d(TAG, "onCreate run: " + dataToClient + number++);
                    mDataChanged.dataChanged(dataToClient);
                    dataToClient = null;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

//    private void sendConnectedInfo(Socket socket) {
//
//        try {
//            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//            dos.write(123);
//
//            //发送服务器的信息给客户端
//            SharedPreferences sharedPreferences = getSharedPreferences("starChatData", MODE_PRIVATE);
//            String nickname = sharedPreferences.getString("nickname", "服务器");
//            dos.write(nickname.getBytes().length);
//            dos.write(nickname.getBytes());
//
//            String path = sharedPreferences.getString("profile_path", null);
//            FileInputStream fis;
//            if (path != null) {
//                dos.write(1);
//                fis = new FileInputStream(new File(path));
//                int length;
//                byte[] buffer = new byte[1024];
//                while ((length = fis.read(buffer)) != -1) {
//                    dos.write(buffer, 0, length);
//                }
//            } else {
//                dos.write(0);
//            }
//            dos.write('\t');
//            dos.write('\r');
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }

    /**
     * 服务器线程类，用于接收客户端发送的数据，并发送给其他客户端
     */
    public class ServerThread implements Runnable {
        private Socket socket;
        public ServerThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                //sendConnectedInfo(socket);
                while (true) {

                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    int flag;
                    flag = dis.read();
                    Log.d(TAG, "Server run: flag:" + flag);

                    if (flag == 124) {//保存新成员信息到数据库，并向所有客户端发送
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
                            //发送给所有客户端
                            for (Socket _socket : listClient) {
                                if (_socket == socket) {
                                    continue;
                                }
                                DataOutputStream dos = new DataOutputStream(_socket.getOutputStream());
                                dos.write(124);
                                dos.write(nickname.getBytes().length);
                                dos.write(nickname.getBytes());
                                FileInputStream fis = new FileInputStream(file);
                                while ((length = fis.read(buffer)) != -1) {
                                    dos.write(buffer, 0, length);
                                }
                                dos.write('\t');
                                dos.write('\r');
                            }
                        } else {
                            //保存到数据库
                            saveToDB(nickname, null);
                        }
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
                            Log.d(TAG, "Server run: length:" + length);
                            byteArrayOutputStream.write(buffer, 0, length);

                            if (length > 1 && buffer[length - 2] == '\t' && buffer[length - 1] == '\r') {
                                break;
                            } else if (length == 1 && buffer[length - 1] == '\r') {
                                break;
                            }
                        }
                        String content = byteArrayOutputStream.toString();
                        //发送给所有客户端
                        for (Socket _socket : listClient) {
                            if (socket == _socket) {
                                continue;
                            }
                            DataOutputStream dos = new DataOutputStream(_socket.getOutputStream());
                            dos.write(MsgTypeUtil.SELF_MSG);
                            dos.write(content.getBytes(), 0, content.getBytes().length);
                            dos.write("\t\r".getBytes());
                        }
                        Log.d(TAG, "Server run: " + content);
                        MessageBean messageBean = GsonUtil.getObject(content);
                        messageBean.type = MsgTypeUtil.OTHERS_MSG;
                        messageBean.profilePath = getExternalCacheDir().getAbsolutePath() + File.separator + "MEMBER" + File.separator + messageBean.nickname + ".jpeg";
                        messageBean.time = TimeUtil.getCurrentTime();
                        dataToClient = GsonUtil.toJsonStr(messageBean);
                    }
                }
            } catch (IOException e) {
                Log.d(TAG, "Server run: " + socket.getInetAddress() + ":" + socket.getPort() + "断开连接");
                listClient.remove(socket);
            }

        }

        /**
         * 从客户端接收图片或文件
         * @param dis 数据输入流
         * @param type 数据类型，图片或文件
         * @throws IOException IO流异常
         */
        public void receivePicOrFile(DataInputStream dis, int type) throws IOException {
            int length = 0;

            byte[] buffer = new byte[1024];

            int offset;
            while ((offset = dis.read()) != '\n') {
                length += offset;
            }
            Log.d(TAG, "Server run: " + length);

            int messageBeanLength = dis.read(buffer, 0, length);

            String mark = new String(buffer, 0, messageBeanLength);

            MessageBean fileBean = GsonUtil.getObject(mark);
            Log.d(TAG, "receivePicOrFile: " + GsonUtil.toJsonStr(fileBean));
            Log.d(TAG, "receivePicOrFile: " + fileBean.fileType);

            String finalDirName;
            if (type == MsgTypeUtil.OTHERS_IMG) {
                finalDirName = "IMAGES";
            } else if (type == MsgTypeUtil.OTHERS_FILE) {
                finalDirName = "DOCUMENTS";
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
            FileOutputStream fos = new FileOutputStream(file);

            while ((length = dis.read(buffer)) != -1) {

                fos.write(buffer, 0, length);
                fos.flush();

                if (length > 1 && buffer[length - 2] == '\t' && buffer[length - 1] == '\r') {
                    break;
                } else if (length == 1 && buffer[length - 1] == '\r') {
                    break;
                }
            }

            //发给所有客户端
            for (Socket _socket : listClient) {

                if (_socket == socket) {
                    continue;
                }

                DataOutputStream dataOutputStream = new DataOutputStream(_socket.getOutputStream());
                FileInputStream fileInputStream = new FileInputStream(file);

                if (type == MsgTypeUtil.OTHERS_IMG) {
                    dataOutputStream.write(MsgTypeUtil.SELF_IMG);
                } else if (type == MsgTypeUtil.OTHERS_FILE) {
                    dataOutputStream.write(MsgTypeUtil.SELF_FILE);
                } else {
                    dataOutputStream.write(MsgTypeUtil.SELF_AUDIO);
                }
                if (messageBeanLength < 256) {
                    dataOutputStream.write(GsonUtil.toJsonStr(fileBean).getBytes().length);
                } else {
                    while (messageBeanLength > 0) {
                        if (messageBeanLength >= 255) {
                            dataOutputStream.write(255);
                        } else {
                            dataOutputStream.write(messageBeanLength);
                        }
                        messageBeanLength = messageBeanLength - 255;
                    }
                }
                dataOutputStream.write('\n');
                dataOutputStream.write(GsonUtil.toJsonStr(fileBean).getBytes());
                dataOutputStream.flush();

                while ((length = fileInputStream.read(buffer)) != -1) {
                    dataOutputStream.write(buffer, 0, length);
                    dataOutputStream.flush();
                }

                dataOutputStream.write("\t\r".getBytes());

            }

            MessageBean messageBean = new MessageBean();
            messageBean.nickname = fileBean.nickname;
            messageBean.profilePath = getExternalCacheDir().getAbsolutePath() + File.separator + "MEMBER" + File.separator + fileBean.nickname + ".jpeg";
            messageBean.filePath = file.getAbsolutePath();
            messageBean.fileLength = FileUtil.getFileLength(file);
            messageBean.fileName = file.getName();
            messageBean.fileType = fileBean.fileType;
            if (type == MsgTypeUtil.OTHERS_AUDIO) {
                messageBean.audioLength = fileBean.audioLength;
            }
            messageBean.type = type;
            messageBean.time = TimeUtil.getCurrentTime();
            dataToClient = GsonUtil.toJsonStr(messageBean);

            fos.close();
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
     * 服务器再次绑定Service时，接收ChatRoomActivity发来的数据
     * @param intent 接收对应的数据
     * @param flags ...
     * @param startId ...
     * @return ...
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getStringExtra("action");
        Log.d(TAG, "onStartCommand: " + action);
        if (action != null) {
            switch (action) {
                case "text":
                    String msg = intent.getStringExtra("text");
                    sendText(msg);
                    break;
                case "picture":
                    String info = intent.getStringExtra("info");
                    if (info != null) {
                        MessageBean messageBean = GsonUtil.getObject(info);
                        sendPicOrFile(messageBean, MsgTypeUtil.SELF_IMG);
                    }
                    break;
                case "file":
                    String fileInfo = intent.getStringExtra("file_info");
                    if (fileInfo != null) {
                        MessageBean messageBean = GsonUtil.getObject(fileInfo);
                        sendPicOrFile(messageBean, MsgTypeUtil.SELF_FILE);
                    }
                    break;
                case "audio":
                    String audioInfo = intent.getStringExtra("audio_info");
                    if (audioInfo != null) {
                        MessageBean messageBean = GsonUtil.getObject(audioInfo);
                        sendPicOrFile(messageBean, MsgTypeUtil.SELF_AUDIO);
                    }
                    break;
                case "location":
                    String locationInfo = intent.getStringExtra("location_info");
                    if(locationInfo !=null){
                        sendText(locationInfo);
                    }
                    break;
                default:
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * 发送图片或文件
     * @param messageBean 消息数据，包括文件路径
     * @param type 数据类型，图片或文件
     */
    private void sendPicOrFile(MessageBean messageBean, int type) {

        File file = new File(messageBean.filePath);
        Log.d(TAG, "sendPicOrFile: " + file.getAbsolutePath());

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (Socket socket : listClient) {
                    try {

                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        FileInputStream fis = new FileInputStream(file);

                        Log.d(TAG, "sendPicOrFile run: " + GsonUtil.toJsonStr(messageBean));

                        dos.write(type);//写进图片或文件或语音标志

                        Log.d(TAG, "sendPicOrFile run: " + type);

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
                        Log.d(TAG, "sendPicOrFile run: " + GsonUtil.toJsonStr(messageBean).getBytes().length);
                        dos.write('\n');
                        dos.write(GsonUtil.toJsonStr(messageBean).getBytes());
                        dos.flush();

                        int length;
                        byte[] buffer = new byte[1024];

                        while ((length = fis.read(buffer)) != -1) {
                            dos.write(buffer, 0, length);
                            dos.flush();
                        }
                        dos.write("\t\r".getBytes());
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 发送文本数据
     * @param msg 包含文本数据，为MessageBean对应的Json格式字符串
     */
    private void sendText(String msg) {
        new Thread(() -> {
            try {
                for (Socket socket : listClient) {
                    DataOutputStream dos;
                    if (socket.getOutputStream() != null) {
                        dos = new DataOutputStream(socket.getOutputStream());
                    } else {
                        return;
                    }

                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(msg.getBytes());

                    int length;
                    byte[] buffer = new byte[1024];
                    dos.write(MsgTypeUtil.SELF_MSG);
                    while ((length = byteArrayInputStream.read(buffer)) != -1) {
                        dos.write(buffer, 0, length);
                        dos.flush();
                    }

                    dos.write("\t\r".getBytes());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Activity绑定ServerService的回调方法
     * @param intent 获得数据
     * @return 返回绑定的数据
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        isServiceConnected = true;
        return new DataBinder();
    }

    /**
     * 解绑回调方法， 关闭socket
     * @param intent 获得数据
     * @return 返回解绑数据
     */
    @Override
    public boolean onUnbind(Intent intent) {
        isServiceConnected = false;
        try {
            for (Socket socket : listClient) {
                socket.close();
            }
            mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.onUnbind(intent);
    }

    /**
     * 获得ServerService实例的类
     */
    public class DataBinder extends Binder {
        public ServerService geService() {
            return ServerService.this;
        }
    }

    /**
     * 数据改变的回调接口
     */
    public interface DataChanged {
        void dataChanged(String data);
    }

    /**
     * 接口实例
     */
    public DataChanged mDataChanged = null;

//    public DataChanged getDataChanged() {
//        return mDataChanged;
//    }

    /**
     * 初始化接口实例
     * @param dataChanged 数据改变的回调接口实例
     */
    public void onDataChanged(DataChanged dataChanged) {
        mDataChanged = dataChanged;
    }

}
