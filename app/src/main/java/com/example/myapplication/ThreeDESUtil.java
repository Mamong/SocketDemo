package com.example.myapplication;

import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * 3DES加密工具类
 */
public class ThreeDESUtil {

//    // 加密
//    public static String encrypt(String str, String key) {
//        try {
//            return getByteStr(encrypt(str.getBytes("utf-8"), key));
//        } catch (Exception e) {
//        }
//        return null;
//    }

//    private static byte[] encrypt(byte[] bytP, String key) throws Exception {
//        byte[] res = null;
//        if (key.length() == 48) {
//            byte[] bytK1 = getKeyByStr(key.substring(0, 16));
//            byte[] bytK2 = getKeyByStr(key.substring(16, 32));
//            byte[] bytK3 = getKeyByStr(key.substring(32, 48));
//            res = encrypt(encrypt(encrypt(bytP, bytK1), bytK2), bytK3);
//        } else {
//            System.out.println("密码错误");
//        }
//        return res;
//    }

//    // 【用密钥加密】
//    private static byte[] encrypt(byte[] bytP, byte[] key) throws Exception {
//        // System.out.println(key);
//        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
//        DESKeySpec desKeySpec = new DESKeySpec(key);
//        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
//        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
//        IvParameterSpec iv = new IvParameterSpec(key);
//        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
//        return cipher.doFinal(bytP);
//    }

    public static String decrypt(String bytP, String key) throws Exception {
        byte[] plantext = getKeyByStr(bytP);
        return AsciiToString(decryptBy3DES(plantext ,key));
    }


    private static byte[] decryptBy3DES(byte[] bytP, String key) throws Exception {
        byte[] res = null;
        if (key.length() == 48) {
            byte[] bytK1 = getKeyByStr(key.substring(0, 16));
            byte[] bytK2 = getKeyByStr(key.substring(16, 32));
            byte[] bytK3 = getKeyByStr(key.substring(32, 48));

            byte[] res1 = decryptByDES(bytP, bytK3);
            byte[] res2 = decryptByDES(res1, bytK2);
            res = decryptByDES(res2, bytK1);

            //res = decryptByDES(decryptByDES(decryptByDES(bytP, bytK3), bytK2), bytK1);
        } else {
            Log.d("decryptDES", "密码错误");
        }
        return res;
    }

    /**
     * 用DES方法解密输入的字节 bytKey需为8字节长，是解密的密码
     */
    private static byte[] decryptByDES(byte[] bytE, byte[] bytKey) throws Exception {
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        DESKeySpec desKS = new DESKeySpec(bytKey);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
        SecretKey sk = skf.generateSecret(desKS);
        IvParameterSpec iv = new IvParameterSpec(bytKey);
        cipher.init(Cipher.DECRYPT_MODE, sk, iv);
        return cipher.doFinal(bytE);
    }

//    private static String getByteStr(byte[] byt) {
//        StringBuilder strRet = new StringBuilder();
//        for (int i = 0; i < byt.length; i++) {
//            strRet.append(getHexValue((byt[i] & 240) / 16));
//            strRet.append(getHexValue(byt[i] & 15));
//        }
//        return strRet.toString();
//    }

    /**
     * 输入密码的字符形式，返回字节数组形式。 如输入字符串：AD67EA2F3BE6E5AD
     * 返回字节数组：{173,103,234,47,59,230,229,173}
     */
    public static byte[] getKeyByStr(String str) {
        byte[] bRet = new byte[str.length() / 2];
        for (int i = 0; i < str.length() / 2; i++) {
            Integer itg = 16 * getChrInt(str.charAt(2 * i)) + getChrInt(str.charAt(2 * i + 1));
            bRet[i] = itg.byteValue();
        }
        return bRet;
    }

//    private static String getHexValue(int s) {
//        String sRet = null;
//        switch (s) {
//            case 0:
//                sRet = "0";
//                break;
//            case 1:
//                sRet = "1";
//                break;
//            case 2:
//                sRet = "2";
//                break;
//            case 3:
//                sRet = "3";
//                break;
//            case 4:
//                sRet = "4";
//                break;
//            case 5:
//                sRet = "5";
//                break;
//            case 6:
//                sRet = "6";
//                break;
//            case 7:
//                sRet = "7";
//                break;
//            case 8:
//                sRet = "8";
//                break;
//            case 9:
//                sRet = "9";
//                break;
//            case 10:
//                sRet = "A";
//                break;
//            case 11:
//                sRet = "B";
//                break;
//            case 12:
//                sRet = "C";
//                break;
//            case 13:
//                sRet = "D";
//                break;
//            case 14:
//                sRet = "E";
//                break;
//            case 15:
//                sRet = "F";
//        }
//        return sRet;
//    }

    /**
     * 计算一个16进制字符的10进制值 输入：0-F
     */
    private static int getChrInt(char chr) {
        int iRet = 0;
        if (chr == "0".charAt(0))
            iRet = 0;
        if (chr == "1".charAt(0))
            iRet = 1;
        if (chr == "2".charAt(0))
            iRet = 2;
        if (chr == "3".charAt(0))
            iRet = 3;
        if (chr == "4".charAt(0))
            iRet = 4;
        if (chr == "5".charAt(0))
            iRet = 5;
        if (chr == "6".charAt(0))
            iRet = 6;
        if (chr == "7".charAt(0))
            iRet = 7;
        if (chr == "8".charAt(0))
            iRet = 8;
        if (chr == "9".charAt(0))
            iRet = 9;
        if (chr == "A".charAt(0))
            iRet = 10;
        if (chr == "B".charAt(0))
            iRet = 11;
        if (chr == "C".charAt(0))
            iRet = 12;
        if (chr == "D".charAt(0))
            iRet = 13;
        if (chr == "E".charAt(0))
            iRet = 14;
        if (chr == "F".charAt(0))
            iRet = 15;
        return iRet;
    }


    public static String AsciiToString(byte[] result2) {
        StringBuilder sbu = new StringBuilder();
        for (byte b : result2) {
            if (0 == b) {
                break;
            }
            sbu.append((char) Integer.parseInt(String.valueOf(b)));
        }
        return sbu.toString();
    }
}