package com.campus.util;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * MD5加密工具类
 */
public class MD5Util {
    /**
     * MD5加密
     */
    public static String md5(String str) {
        return DigestUtils.md5Hex(str);
    }
}



