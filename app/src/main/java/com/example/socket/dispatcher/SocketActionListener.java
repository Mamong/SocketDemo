package com.example.socket.dispatcher;

import java.net.Socket;

/**
 * Author：Alex
 * Date：2019/6/4
 * Note：socket行为监听的抽象类，继承此类可以选择性地重写方法
 */
public abstract class SocketActionListener implements ISocketActionListener {

    public void onSocketAccept(Socket socket) {
    }

    public void onSocketConnSuccess(Socket socket) {
    }

    public void onSocketConnFail(Socket socket, boolean isNeedReconnect) {
    }

    public void onSocketDisconnect(Socket socket, boolean isNeedReconnect) {
    }

    public void onSocketReceivePacket(Socket socket, byte[] readData, int tag) {
    }

//    public void onSocketReceiveChunk(Socket socket, byte[] readData) {
//    }

    public void onSocketDidSendChunk(Socket socket, int chunkLength) {
    }
}
