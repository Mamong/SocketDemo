package com.example.socket.dispatcher;

import com.example.socket.SocketConnection;

import java.net.Socket;

/**
 * Author：Alex
 * Date：2019/6/4
 * Note：socket行为监听的抽象类，继承此类可以选择性地重写方法
 */
public abstract class SocketActionListener implements ISocketActionListener {

    public void onSocketAccept(Socket socket) {
    }

    public void onSocketConnSuccess(SocketConnection socket) {
    }

    public void onSocketConnFail(SocketConnection socket, boolean isNeedReconnect) {
    }

    public void onSocketDisconnect(SocketConnection socket, boolean isNeedReconnect) {
    }

    public void onSocketReceivePacket(SocketConnection socket, byte[] readData, int tag) {
    }

    public void onSocketDidSendChunk(SocketConnection socket, int chunkLength) {
    }
}
