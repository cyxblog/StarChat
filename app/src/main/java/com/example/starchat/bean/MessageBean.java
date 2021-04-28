package com.example.starchat.bean;


/**
 * 消息类
 */
public class MessageBean {

    //数据发出时间
    public String time;
    //用户昵称
    public String nickname;
    //数据类型，包括本设备发的和其他设备发的，在MsgTypeUtil中定义
    public int type;
    //用户头像路径
    public String profilePath;

    //文本
    public String msg;

    //文件路径
    public String filePath;
    //文件图标
    public int fileType;
    //文件名
    public String fileName;
    //文件大小
    public String fileLength;

    //语音时长
    public String audioLength;

}
