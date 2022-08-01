package com.example.socket;

import com.example.myapplication.AESEnc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;

public class Request {
    public static String REQ_AUTH = "REQ_AUTH";
    public static String RSP_AUTH = "RSP_AUTH";
    public static String REQ_FILE_INFO_LIST = "REQ_FILE_INFO_LIST";
    public static String RSP_FILE_INFO_LIST = "RSP_FILE_INFO_LIST";
    public static String REQ_FILE = "REQ_FILE";
    public static String REQ_FILE_LIST_END = "REQ_FILE_LIST_END";

    public static AESEnc aesEncrypt = new AESEnc();

    public static JSONObject authRequest(){
        JSONObject obj = new JSONObject();
        try {
            obj.put("cmd",REQ_AUTH);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static JSONObject authResponse(int result){
        JSONObject obj = new JSONObject();
        try {
            obj.put("cmd",RSP_AUTH);
            obj.put("result",result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static JSONObject fileListRequest(){
        JSONObject obj = new JSONObject();
        try {
            obj.put("cmd",REQ_FILE_INFO_LIST);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static JSONObject fileListResponse(List<HashMap> files){
        JSONObject obj = new JSONObject();
        try {
            obj.put("cmd",RSP_FILE_INFO_LIST);
            JSONArray filesArray = new JSONArray();
            for (HashMap file : files){
                JSONObject fileObj = new JSONObject();
                fileObj.put("fileName",file.get("fileName"));
                fileObj.put("fileType","zip");
                fileObj.put("fileSize",file.get("fileSize"));
                fileObj.put("id",file.get("id"));
                fileObj.put("checkSum",file.get("checkSum"));
                filesArray.put(fileObj);
            }
            obj.put("files",filesArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static JSONObject fileRequest(JSONObject file){
        JSONObject obj = new JSONObject();
        try {
            obj.put("cmd",REQ_FILE);
            obj.put("file",file);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static JSONObject fileListEndRequest(){
        JSONObject obj = new JSONObject();
        try {
            obj.put("cmd",REQ_FILE_LIST_END);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static byte[] packRequest(JSONObject request){
        byte[] bytes = request.toString().getBytes();
        //加密
        try {
            bytes = aesEncrypt.encrypt(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ByteBuffer buffer = ByteBuffer.allocate(bytes.length+8);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(bytes.length);
        buffer.putInt(-1);
        buffer.put(bytes);
        return buffer.array();
    }

    public static JSONObject unpackRequest(byte[] bytes){
        //解密
        try {
            bytes = aesEncrypt.decrypt(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String string = new String(bytes);
        try {
            return new JSONObject(string);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
