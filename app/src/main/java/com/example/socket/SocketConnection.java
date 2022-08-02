package com.example.socket;


import android.util.Log;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.socket.dispatcher.ISocketActionListener;
import com.example.socket.dispatcher.SocketStatus;

/**
 * Created by boby on 2017/2/16.
 */

public class SocketConnection implements Runnable {
    /**
     * 连接状态，初始值为断开连接
     */
    protected final AtomicInteger connectionStatus = new AtomicInteger(SocketStatus.SOCKET_DISCONNECTED);
    /**
     * 连接线程
     */
    private ExecutorService connExecutor;
    /**
     * socket地址信息
     */
    private final SocketAddress socketAddress;

    private Socket socket;
    private SocketReader reader;
    private SocketWriter writer;
    private final ISocketActionListener listener;

    private SocketOptions socketOptions;


    public SocketConnection(SocketAddress socketAddress,ISocketActionListener listener) {
        this.socketAddress = socketAddress;
        this.listener = listener;
        socketOptions = new SocketOptions.Builder().build();
    }

    public synchronized void connect() {

        connectionStatus.set(SocketStatus.SOCKET_CONNECTING);
        // 开启连接线程
        if (connExecutor == null || connExecutor.isShutdown()) {
            // 核心线程数为0，非核心线程数可以有Integer.MAX_VALUE个，存活时间为60秒，适合于在不断进行连接的情况下，避免重复创建和销毁线程
            connExecutor = Executors.newCachedThreadPool();
        }
        // 执行连接任务
        connExecutor.execute(connTask);
    }

    public synchronized void accept(Socket socket){
        this.socket = socket;
        onConnectionOpened(false);
    }

    // 连接任务
    private final Runnable connTask = new Runnable() {
        @Override
        public void run() {
            try {
                openConnection();
            } catch (RuntimeException | IOException e) {
                //java.net.SocketException: Connection refused: connect
                // 连接异常
                e.printStackTrace();
                connectionStatus.set(SocketStatus.SOCKET_DISCONNECTED); // 设置为未连接
                listener.onSocketConnFail(socket,false);
            }
        }
    };

    public int getConnectionStatus() {
        return connectionStatus.get();
    }

    public Socket getSocket(){
        return socket;
    }

    public SocketAddress getSocketAddress(){
        return socketAddress;
    }

    protected void openConnection() throws RuntimeException, IOException {
        // 进行socket连接
        InetSocketAddress address = new InetSocketAddress(this.socketAddress.getIp(),this.socketAddress.getPort());
        socket = new Socket();
        socket.connect(address, socketOptions.getConnectTimeout());
        //2小时间隔发送一个ack验证连接是否断掉，此处没有意义
        //socket.setKeepAlive(true);
        // 关闭Nagle算法,无论TCP数据报大小,立即发送
        socket.setTcpNoDelay(true);
        // 连接已经打开
        if (socket.isConnected() && !socket.isClosed()) {
            onConnectionOpened(true);
        }
    }

    /**
     * 连接成功
     */
    protected void onConnectionOpened(Boolean useBuiltinThread) {
        connectionStatus.set(SocketStatus.SOCKET_CONNECTED);

        initIO();

        setOptions(socketOptions);

        // 开启连接线程
        if(useBuiltinThread){
            if (connExecutor == null || connExecutor.isShutdown()) {
                // 核心线程数为0，非核心线程数可以有Integer.MAX_VALUE个，存活时间为60秒，适合于在不断进行连接的情况下，避免重复创建和销毁线程
                connExecutor = Executors.newCachedThreadPool();
            }
            connExecutor.execute(this);
        }
    }

    public synchronized void disconnect(boolean isNeedReconnect) {
        // 判断当前socket的连接状态
        if (connectionStatus.get() == SocketStatus.SOCKET_DISCONNECTED) {
            return;
        }
        // 正在重连中
        if (isNeedReconnect && connectionStatus.get() == SocketStatus.SOCKET_RECONNECTING) {
            return;
        }
        // 正在断开连接
        connectionStatus.set(SocketStatus.SOCKET_DISCONNECTING);

        // 开启断开连接线程
        String info = socket.getRemoteSocketAddress().toString();
        Thread disconnectThread = new DisconnectThread(isNeedReconnect, "disconn thread：" + info);
        disconnectThread.setDaemon(true);
        disconnectThread.start();
    }

    /**
     * 断开连接线程
     */
    private class DisconnectThread extends Thread {
        boolean isNeedReconnect; // 当前连接的断开是否需要自动重连

        public DisconnectThread(boolean isNeedReconnect, String name) {
            super(name);
            this.isNeedReconnect = isNeedReconnect;
        }

        @Override
        public void run() {
            try {
                // 关闭io线程
                closeIO();

                // 关闭连接线程
                if (connExecutor != null && !connExecutor.isShutdown()) {
                    connExecutor.shutdown();
                    connExecutor = null;
                }
                // 关闭连接
                closeConnection();
                Log.d("ClientPeer","---> 关闭socket连接");
                connectionStatus.set(SocketStatus.SOCKET_DISCONNECTED);
                listener.onSocketDisconnect(socket,isNeedReconnect);
            } catch (IOException e) {
                // 断开连接发生异常
                e.printStackTrace();
            }
        }
    }


    protected void closeConnection() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }

    @Override
    public void run() {
        startIO();
    }

    //初始化io
    private void initIO() {
        reader = new SocketReader(this,listener);
        writer = new SocketWriter(this,listener);
    }

    public void sendBytes(byte[] bytes) {
        if (writer != null) {
            writer.write(bytes);
        }
    }

    public void sendFile(RandomAccessFile file,int tag) {
        if (writer != null) {
            writer.chunk(file,tag);
        }
    }

    public void startIO() {
        if (writer != null)
            writer.openWriter();
        if (reader != null)
            reader.openReader();
        listener.onSocketConnSuccess(socket);
    }

    public void closeIO() {
        if (writer != null)
            writer.closeWriter();
        if (reader != null)
            reader.closeReader();
    }

    private SocketConnection setOptions(SocketOptions socketOptions) {
        if(socketOptions == null) return this;

        this.socketOptions = socketOptions;
        if (reader!=null) {
            reader.setOption(socketOptions);
        }
        if (writer != null) {
            writer.setOption(socketOptions);
        }
        return this;
    }

}
