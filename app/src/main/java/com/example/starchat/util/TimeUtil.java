package com.example.starchat.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间工具类
 * 获得当前设备时间
 */
public class TimeUtil {

    //获得当前系统时间
    public static String getCurrentTime(){
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }
}
