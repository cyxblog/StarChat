package com.example.starchat.util;

/**
 * 数据加解密
 */
public class DataHandleUtil {

    //加密数据
    public static byte[] encodeData(byte[] buffer) {
        int index;
        for (index = 0; index < buffer.length; index++) {
            buffer[index] ^= 1;
        }
        return buffer;
    }

    //解密数据
    public static void decodeData(byte[] buffer) {
        int index;
        for (index = 0; index < buffer.length; index++) {
            buffer[index] ^= 1;
        }
    }
}
