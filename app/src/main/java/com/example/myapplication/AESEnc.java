package com.example.myapplication;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class AESEnc {

    private String mode = "AES/ECB/PKCS5Padding";
    private final String KEY_ALGORITHM = "AES";
    private SecretKey aesKey;
    private byte[] key;

    /**
     *
     * 构造方法
     *
     */
    public AESEnc() {

        key = generateAesKey();
        aesKey = new SecretKeySpec(key, KEY_ALGORITHM); //得到aesKey
    }

    /**
     *
     * 构造方法
     *
     * @param k
     */
    public AESEnc(byte[] k) {

        aesKey = new SecretKeySpec(k, KEY_ALGORITHM);
    }

    /**
     *
     * 生成key值,应该是一个随机值
     *
     * @return 返回密钥
     */
    private byte[] generateAesKey(){
        //192.168.2.79;17245;1892
        //32297ee49262ca5c83c5f57c551b9ec354520159a5f405e0f539af8fe0f042f8

        byte[] keyBytes = "A1B@C#F4G5H6~*71".getBytes();
        return keyBytes;
        /*
        KeyGenerator kgen = null;
        try {
            kgen = KeyGenerator.getInstance(KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        kgen.init(256);

        SecretKey secretKey = kgen.generateKey();

        return secretKey.getEncoded();
         */
    }

    /**
     *
     * 加密
     *
     * 该方法才是在加解密当中最重要的，Clipher类
     *
     * c.init(Cipher.ENCRYPT_MODE, aesKey, iv);  //用指定解密的公钥aesKey和iv
     * c.doFinal(src); //指定待加密的数据
     *
     * @param src  待加密数据
     * @return
     * @throws Exception
     */
    public byte[] encrypt(byte[] src) throws Exception {

        try {
            Cipher c = Cipher.getInstance(mode);
            c.init(Cipher.ENCRYPT_MODE, aesKey);

            return c.doFinal(src);
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
     *
     * 解密
     *
     * @param src
     * @return
     * @throws Exception
     */
    public byte[] decrypt(byte[] src) throws Exception {

        try {
            Cipher c = Cipher.getInstance(mode);
            c.init(Cipher.DECRYPT_MODE, aesKey);

            return c.doFinal(src);
        } catch (Exception ex) {
            throw ex;
        }
    }



    public byte[] getKey() {
        return key;
    }

}
