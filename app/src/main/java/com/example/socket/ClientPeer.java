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
        String fileName = downloadFile.optString("fileName","");
        int acceptSize = downloadFile.optInt("acceptSize",0);
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

        int fileSize = downloadFile.optInt("fileSize",0);
        int acceptSize = downloadFile.optInt("acceptSize",0);
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

    @Override
    public void onSocketConnSuccess(Socket socket) {
        Log.d("ClientPeer","---> socket连接成功");
        sendRequest(Request.authRequest());
    }

    @Override
    public void onSocketConnFail(Socket socket, boolean isNeedReconnect) {
        Log.d("ClientPeer","---> socket连接失败");
    }

    @Override
    public void onSocketDisconnect(Socket socket, boolean isNeedReconnect) {
        Log.d("ClientPeer","---> socket连接断开");
    }

    @Override
    public void onSocketReceivePacket(Socket socket, byte[] readData, int tag) {
        if (tag == -1){
            handleReceiveMessage(readData);
        }else {
            handleReceiveChunk(readData);
        }
    }

    /*public void onSocketReceiveData(Socket socket, byte[] readData) {
        try {
            int headerLength = 8; //默认的包头长度是4个字节
            int position = remainingBuf.position();
            int length = readData.length;
            int remaining = readData.length;
            if (position == 0){
                //获得完整的头部
                if (position + remaining <= headerLength) {
                    remainingBuf.put(readData);
                    io.reader.readHeaderFromSteam(remainingBuf, headerLength - position - remaining);
                    remaining = 0;
                }else {
                    remainingBuf.put(readData,0,headerLength - position);
                    remaining -= headerLength - position;
                }
                int bodyLength = remainingBuf.getInt(0);
                int bodyType = remainingBuf.getInt(4);
                if (bodyType == 0){
                    //创建实际所需大小的buffer
                    ByteBuffer temp = ByteBuffer.allocate(headerLength + bodyLength);
                    temp.order(ByteOrder.BIG_ENDIAN);
                    if(remaining == 0){
                        temp.put(remainingBuf.array());
                        remainingBuf = temp;
                    }else{
                        temp.put(readData,0,headerLength);
                        remainingBuf = temp;

                        //把剩余数据丢给后面处理
                        byte[] dataR = Arrays.copyOfRange(readData,headerLength,length);
                        onSocketReceiveData(socket,dataR);
                    }
                }else{
                    chunkTotal = 0;
                    //把剩余数据丢给后面处理
                    byte[] dataR = Arrays.copyOfRange(readData,headerLength,length);
                    onSocketReceiveData(socket,dataR);
                }
                return;
            }
            int bodyLength = remainingBuf.getInt(0);
            int bodyType = remainingBuf.getInt(4);
            int bufferRemaining = remainingBuf.remaining();
            if(bodyType == 0){
                if(bufferRemaining > length){
                    remainingBuf.put(readData);
                }else{
                    remainingBuf.put(readData,0,bufferRemaining);
                    byte[] data = Arrays.copyOfRange(remainingBuf.array(),headerLength,headerLength+bodyLength);
                    handleReceiveMessage(data);

                    //有剩余丢给下次处理
                    initBuffer();
                    if(bufferRemaining < length){
                        byte[] dataR = Arrays.copyOfRange(readData,bufferRemaining,length);
                        onSocketReceiveData(socket,dataR);
                    }
                }
            }else {
                int remainingL = bodyLength - chunkTotal;
                if(remainingL > length){
                    handleReceiveChunk(readData);
                }else {
                    byte[] data = Arrays.copyOfRange(readData,0,remainingL);
                    handleReceiveChunk(data);

                    //有超出bodyLength的剩余，丢给下次处理
                    initBuffer();
                    if (remainingL < length){
                        byte[] dataR = Arrays.copyOfRange(readData,bodyLength - chunkTotal,length);
                        onSocketReceiveData(socket,dataR);
                    }
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }*/
}
