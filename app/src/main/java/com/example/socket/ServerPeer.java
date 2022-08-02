package com.example.socket;

import android.app.Activity;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.myapplication.SHAUtil;
import com.example.socket.dispatcher.SocketActionListener;

public class ServerPeer extends SocketActionListener {
    ServerSocket serverSocket;
    private final Map<String, SocketConnection> mConnectionManagerMap = new HashMap<>();
    private ExecutorService mExecutorService;
    private ExecutorService launchService;

    private Socket authClient;

    private final Activity context;
    private List<HashMap> files;

    public ServerPeer(Activity context) {
        this.context = context;
        mExecutorService = Executors.newCachedThreadPool();
        launchService = Executors.newFixedThreadPool(1);
    }


    public void listenToPort(int port) {
        try {
            serverSocket = new ServerSocket(port);
            Log.d("ServerPeer", "绑定:" + port);
        } catch (Exception e) {
            //java.net.BindException:Address already in use: JVM_Bind
            e.printStackTrace();
        }
        if (serverSocket != null){
            launchService.execute(listenTask);
        }
    }


    // 连接任务
    private Runnable listenTask = new Runnable() {
        @Override
        public void run() {
            try {
                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    ServerPeer.this.onSocketAccept(socket);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeAllConnect();
            }
        }
    };

    public void closeAllConnect() {
        try {
            // 关闭连接线程
            if (mExecutorService != null && !mExecutorService.isShutdown()) {
                mExecutorService.shutdown();
                mExecutorService = null;
            }

            // 关闭listen线程
            if (launchService != null && !launchService.isShutdown()) {
                launchService.shutdown();
                launchService = null;
            }

            for (SocketConnection io : mConnectionManagerMap.values()) {
                io.disconnect(false);
            }
            mConnectionManagerMap.clear();

            closeServerConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void closeServerConnection() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    private void sendRequest(Socket socket, JSONObject request) {
        byte[] bytes = Request.packRequest(request);
        ioForSocket(socket).sendBytes(bytes);
    }

    private void handleReceiveMessage(Socket socket, byte[] bytes) {
        JSONObject result = Request.unpackRequest(bytes);
        String cmd = result.optString("cmd","");
        Log.d("ServerPeer", "handleReceiveMessage:" + cmd);

        if (cmd.equals(Request.REQ_AUTH)) {
            //验证逻辑
            int code = 1;
            String userid = result.optString("userid","");

            if (code == 1) {
                authClient = socket;
            }
            sendRequest(socket, Request.authResponse(code));
        }

        //后续操作必须来自验证socket
        if (socket != authClient) {
            return;
        }

        if (cmd.equals(Request.REQ_FILE_INFO_LIST)) {
            //备份并发送文件列表
            sendFilesInfo();
        } else if (cmd.equals(Request.REQ_FILE)) {
            HashMap uploadFile = null;
            JSONObject obj = result.optJSONObject("file");
            int id = obj.optInt("id",-1);
            int acceptSize = obj.optInt("acceptSize",0);

            for (HashMap file : files) {
                int fid = (int) file.get("id");
                if (id == fid) {
                    uploadFile = file;
                    uploadFile.put("acceptSize",acceptSize);
                    break;
                }
            }
            if (uploadFile == null) {
                //文件不存在
                return;
            }
            sendFile(uploadFile);
        } else if (cmd.equals(Request.REQ_FILE_LIST_END)) {
            //传输完成，开始导入
        }
    }

    private void sendFilesInfo() {
        files = listFileOfAssets();
        sendRequest(authClient, Request.fileListResponse(files));
    }

    private List<HashMap> listFileOfAssets() {
        List<HashMap> list = new ArrayList();
        File file = context.getFilesDir();
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            String checkSum = null;
            try {
                checkSum = SHAUtil.getMD5Checksum(f.getPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
            HashMap map = new HashMap();
            map.put("id", i);
            map.put("fileName", f.getName());
            map.put("fileSize", f.length());
            map.put("filePath", f.getPath());
            if (checkSum != null) {
                map.put("checkSum", checkSum);
            }
            list.add(map);
        }
        return list;
    }

    private void sendFile(HashMap uploadFile) {
        try {
            String filePath = (String) uploadFile.get("filePath");
            File file = new File(filePath);
            RandomAccessFile fileOutStream = new RandomAccessFile(file, "r");
            int acceptSize = 0;
            if (uploadFile.containsKey("acceptSize")) {
                acceptSize = (int) uploadFile.get("acceptSize");
            }
            fileOutStream.seek(acceptSize);
            ioForSocket(authClient).sendFile(fileOutStream,1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SocketConnection ioForSocket(Socket socket) {
        for(SocketConnection io:mConnectionManagerMap.values()){
            if (socket == io.getSocket()){
                return io;
            }
        }
        return null;
    }

    @Override
    public void onSocketAccept(Socket socket) {
        Log.d("ServerPeer", "Accept Socket");

        InetSocketAddress address =  (InetSocketAddress)socket.getRemoteSocketAddress();
        String ip = address.getHostString();
        int port = address.getPort();
        SocketAddress socketAddress = new SocketAddress(ip,port);

        SocketConnection io = new SocketConnection(socketAddress,this);
        io.accept(socket);

        String key = socketAddress.toString();
        mConnectionManagerMap.put(key, io);
        mExecutorService.execute(io);
    }

    @Override
    public void onSocketDisconnect(Socket socket, boolean isNeedReconnect) {
        Log.d("ClientPeer","---> socket连接断开");
        SocketConnection io = ioForSocket(socket);
        if (io.getSocket() == authClient) {
            authClient = null;
        }
        mConnectionManagerMap.remove(io.getSocketAddress().toString());
    }

    @Override
    public void onSocketDidSendChunk(Socket socket, int chunkLength) {
        //统计传输速率
    }

    @Override
    public void onSocketReceivePacket(Socket socket, byte[] readData, int tag) {
        //处理消息
        handleReceiveMessage(socket,readData);
    }

}
