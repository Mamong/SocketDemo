package com.example.socket;

import android.app.Activity;
import android.util.Log;

import com.example.myapplication.SHAUtil;
import com.example.socket.dispatcher.SocketActionListener;
import com.example.socket.dispatcher.SocketStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;


public class ClientPeer extends SocketActionListener {

    private SocketConnection io;

    private int fileIndex = 0;
    private JSONObject downloadFile;
    private JSONArray fileItems;
    private RandomAccessFile fileOutStream;

    private Activity context;
    private final String TAG = "ClientPeer";

    public ClientPeer(Activity context){
        this.context = context;
    }

    public synchronized void connectTo(String ip, int port) {
        SocketAddress address = new SocketAddress(ip, port);
        io = new SocketConnection(address,this);
        io.connect();
    }

    private void sendBytes(byte[] bytes) {
        if (io == null || io.getConnectionStatus() != SocketStatus.SOCKET_CONNECTED) {
            return;
        }
        io.sendBytes(bytes);
    }


    private void sendRequest(JSONObject request){
        byte[] bytes = Request.packRequest(request);
        sendBytes(bytes);
    }

    private void handleReceiveMessage(byte[] bytes){
        JSONObject result = Request.unpackRequest(bytes);
        String cmd = result.optString("cmd","");
        if(cmd.equals(Request.RSP_AUTH)){
            //处理验证逻辑
            int code = result.optInt("result",0);
            if (code == 0){
                //不是同一个账户
                return;
            }
            //请求文件列表或继续传输文件
            if(fileItems == null || fileItems.length() == 0){
                Log.d(TAG,"auth");
                sendRequest(Request.fileListRequest());
            }else{
                requestFile();
            }
        }else if(cmd.equals(Request.RSP_FILE_INFO_LIST)){
            Log.d(TAG,"response RSP_FILE_INFO_LIST");

            fileItems = result.optJSONArray("files");
            //检查磁盘剩余空间是否足够

            //开始请求文件
            fileIndex = 0;
            requestFile();
        }
    }

    private void handleReceiveChunk(byte[] bytes){
        prepareWriteFile();
        writeFileChunk(bytes);
    }

    private void requestFile(){
        if (fileIndex >= fileItems.length()){
            return;
        }
        Log.d(TAG,"requestFile");
        downloadFile = fileItems.optJSONObject(fileIndex);
        sendRequest(Request.fileRequest(downloadFile));
    }

    private void prepareWriteFile(){
        finishWriteFile();

        String savePath = downloadFile.optString("savePath");
        String fileName = downloadFile.optString("fileName");
        int acceptSize = downloadFile.optInt("acceptSize");
        try {
            File file = null;
            if (savePath.length() == 0){
                file = File.createTempFile(fileName,null);
                downloadFile.put("savePath",file.getPath());
            }else {
                file = new File(savePath);
            }
            fileOutStream = new RandomAccessFile(file, "rw");
            fileOutStream.seek(acceptSize);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            finishWriteFile();
        }
    }

    private void writeFileChunk(byte[] chunk){
        if (fileOutStream == null) return;

        int fileSize = downloadFile.optInt("fileSize");
        int acceptSize = downloadFile.optInt("acceptSize");
        try {
            fileOutStream.write(chunk);
            acceptSize += chunk.length;
        } catch (IOException e) {
            e.printStackTrace();
            finishWriteFile();
        }

        try {
            downloadFile.put("acceptSize",acceptSize);
        }catch (JSONException e){
            e.printStackTrace();
        }

        //Log.d("handleReceiveChunk","receive:"+chunk.length+",acceptSize:"+acceptSize+",fileSize:"+fileSize);

        //检查是否写入完全
        if (acceptSize == fileSize){
            String checkSum = downloadFile.optString("checkSum");
            String savePath = downloadFile.optString("savePath");
            String localMD5 = "";
            try {
                localMD5 = SHAUtil.getMD5Checksum(savePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(checkSum.length() == 0 || localMD5.equals(checkSum)){
                finishWriteFile();
                downloadFile = null;
                fileIndex++;
                Log.d("handleReceiveChunk","传输完成:"+fileIndex);
                if (fileIndex < fileItems.length()){
                    requestFile();
                }else{
                    sendRequest(Request.fileListEndRequest());
                }
            }
        }
    }

    private void finishWriteFile(){
        if (fileOutStream != null){
            try {
                fileOutStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileOutStream = null;
        }
    }

    //统计接收速率
    public void onSocketDidReceiveChunk(SocketConnection socket, int chunkLength) {
    }

    @Override
    public void onSocketConnSuccess(SocketConnection socket) {
        Log.d("ClientPeer","---> socket连接成功");
        sendRequest(Request.authRequest());
    }

    @Override
    public void onSocketConnFail(SocketConnection socket, boolean isNeedReconnect) {
        Log.d("ClientPeer","---> socket连接失败");
    }

    @Override
    public void onSocketDisconnect(SocketConnection socket, boolean isNeedReconnect) {
        Log.d("ClientPeer","---> socket连接断开");
    }

    @Override
    public void onSocketReceivePacket(SocketConnection socket, byte[] readData, int tag) {
        if (tag == -1){
            handleReceiveMessage(readData);
        }else {
            onSocketDidReceiveChunk(socket,readData.length);
            handleReceiveChunk(readData);
        }
    }
}
