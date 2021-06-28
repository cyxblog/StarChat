package com.example.starchat.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.example.starchat.R;

import java.io.File;
import java.util.Locale;

/**
 * 文件工具类
 */
public class FileUtil {
    //获得文件大小，带单位
    public static String getFileLength(File file) {
        long length = file.length();
        String fileLength;
        if (length < 1024) {
            fileLength = length + "B";
        } else if (length < 1024 * 1024) {
            fileLength = String.format(Locale.CHINA, "%.2f", (double) length / 1024) + "KB";
        } else if (length < 1024 * 1024 * 1024) {
            fileLength = String.format(Locale.CHINA, "%.2f", (double) length / 1024 / 1024) + "MB";
        } else {
            fileLength = String.format(Locale.CHINA, "%.2f", (double) length / 1024 / 1024 / 1024) + "GB";
        }
        return fileLength;
    }

    //获得文件后缀，返回对应图标
    public static int getFileIcon(String fileName) {
        String fileSuffix = fileName.split("\\.")[fileName.split("\\.").length - 1];
        switch (fileSuffix) {
            case "docx":
            case "doc":
                return R.mipmap.docx;
            case "pptx":
            case "ppt":
                return R.mipmap.pptx;
            case "xlsx":
            case "xls":
                return R.mipmap.xlsx;
            case "pdf":
                return R.mipmap.pdf;
            case "zip":
                return R.mipmap.zip;
            case "mp4":
                return R.mipmap.video_file;
            case "mp3":
                return R.mipmap.audio_file;
            default:
                return R.mipmap.file;
        }
    }

    //获取文件类型
    public static String getMIMEType(File f) {
        String type = "";
        String fName = f.getName();
        /* 取得扩展名 */
        String end = fName.substring(fName.lastIndexOf(".") + 1, fName.length()).toLowerCase();
        /* 依扩展名的类型决定MimeType */
        if (end.equals("pdf")) {
            type = "application/pdf";
        } else if (end.equals("m4a") || end.equals("mp3") || end.equals("mid") ||
                end.equals("xmf") || end.equals("ogg") || end.equals("wav")) {
            type = "audio/*";
        } else if (end.equals("3gp") || end.equals("mp4")) {
            type = "video/*";
        } else if (end.equals("jpg") || end.equals("gif") || end.equals("png") ||
                end.equals("jpeg") || end.equals("bmp")) {
            type = "image/*";
        } else if (end.equals("apk")) {
            type = "application/vnd.android.package-archive";
        } else if (end.equals("pptx") || end.equals("ppt")) {
            type = "application/vnd.ms-powerpoint";
        } else if (end.equals("docx") || end.equals("doc")) {
            type = "application/vnd.ms-word";
        } else if (end.equals("xlsx") || end.equals("xls")) {
            type = "application/vnd.ms-excel";
        }else if(end.equals("txt")){
            type = "text/plain";
        }else if(end.equals("html") || end.equals("htm")){
            type = "text/html";
        } else {
            //如果无法直接打开，就跳出软件列表给用户选择
            type = "*/*";
        }
        return type;
    }

    /**
     * 查看文件
     * @param path 文件路径
     */
    public static void openFileByPath(String path, Context context) {
        File file = new File(path);
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = FileProvider.getUriForFile(context, "com.example.starchat.fileprovider", file);
        String type = FileUtil.getMIMEType(file);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(uri, type);
        context.startActivity(intent);
    }
}
