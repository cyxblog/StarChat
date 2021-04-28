package com.example.starchat.util;

import com.example.starchat.bean.MessageBean;
import com.google.gson.Gson;

/**
 * Json格式和MessageBean类的转化工具类
 */
public class GsonUtil {

    //MessageBean数据转化为Json格式数据
    public static String toJsonStr(MessageBean messageBean) {
        Gson gson = new Gson();
        return gson.toJson(messageBean);
    }

    //从Json得到MessageBean对象
    public static MessageBean getObject(String strJson){
        Gson gson = new Gson();
        return gson.fromJson(strJson, MessageBean.class);
    }


}
