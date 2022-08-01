package com.example.myapplication;

import android.util.Log;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 加密工具类
 * author：wiki on 2018/12/12
 * email：zhengweiqunemail@qq.com
 */
public class SHAUtil {

    public static final String TAG = SHAUtil.class.getSimpleName();

    /**
     * 传入文本内容，返回 SHA-256 串
     *
     * @param strText
     * @return
     */
    public static String getTextSHA256(final String strText) {
        return SHA(strText, "SHA-256");
    }

    /**
     * 传入文本内容，返回 SHA-512 串
     *
     * @param strText
     * @return
     */
    public static String getTextSHA512(final String strText) {
        return SHA(strText, "SHA-512");
    }

    /**
     * 字符串 SHA 加密
     *
     * @param strText
     * @return
     */
    private static String SHA(final String strText, final String strType) {
        // 返回值
        String strResult = null;

        // 是否是有效字符串
        if (strText != null && strText.length() > 0) {
            try {
                // SHA 加密开始
                // 创建加密对象 并傳入加密類型
                MessageDigest messageDigest = MessageDigest.getInstance(strType);
                // 传入要加密的字符串
                messageDigest.update(strText.getBytes());
                // 得到 byte 類型结果
                byte byteBuffer[] = messageDigest.digest();

                // 將 byte 轉換爲 string
                StringBuffer strHexString = new StringBuffer();
                // 遍歷 byte buffer
                for (int i = 0; i < byteBuffer.length; i++) {
                    String hex = Integer.toHexString(0xff & byteBuffer[i]);
                    if (hex.length() == 1) {
                        strHexString.append('0');
                    }
                    strHexString.append(hex);
                }
                // 得到返回結果
                strResult = strHexString.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        return strResult;
    }


    /**
     * 校验
     * @param filename 文件名
     * @param strType   校验类型--SHA-1,SHA-256,SHA-384,SHA-512,MD5,MD2
     * @return  byte[]
     * @throws Exception
     */
    private static byte[] createChecksum(String filename, String strType) throws Exception {
        InputStream fis = new FileInputStream(filename);          //将流类型字符串转换为String类型字符串

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance(strType);
        int numRead;

        do {
            numRead = fis.read(buffer);    //从文件读到buffer，最多装满buffer
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);  //用读到的字节进行strType的计算，第二个参数是偏移量
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }


    public static String getMD5Checksum(String filename) throws Exception {
        byte[] b = createChecksum(filename, "MD5");
        return byte2String(b);
    }

    public static String getSHA256Checksum(String filename) throws Exception {
        byte[] b = createChecksum(filename, "SHA-256");
        return byte2String(b);
    }

    public static String getSHA512Checksum(String filename) throws Exception {
        byte[] b = createChecksum(filename, "SHA-512");
        return byte2String(b);
    }

    private static String byte2String(byte[] bytes) {
        StringBuilder result = new StringBuilder();

        for (byte b : bytes) {
            //加0x100是因为有的b[i]的十六进制只有1位
            result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        Log.i(TAG, "checkSum: " + result.toString());
        return result.toString();
    }

}

