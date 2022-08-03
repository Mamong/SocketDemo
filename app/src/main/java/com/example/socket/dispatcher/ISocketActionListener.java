package com.example.socket.dispatcher;

import com.example.socket.SocketConnection;

import java.net.Socket;

/**
 * Author：Alex
 * Date：2019/6/1
 * Note：socket行为监听接口
 */
public interface ISocketActionListener {

    void onSocketAccept(Socket socket);

    void onSocketConnSuccess(SocketConnection socket);

    void onSocketConnFail(SocketConnection socket, boolean isNeedReconnect);

    void onSocketDisconnect(SocketConnection socket, boolean isNeedReconnect);

//    void onSocketReceiveData(Socket socket, byte[] readData);

//    void onSocketDidSendData(Socket socket);

    void onSocketReceivePacket(SocketConnection socket, byte[] readData, int tag);
//    void onSocketReceiveChunk(Socket socket, byte[] readData);

    void  onSocketDidSendChunk(SocketConnection socket, int chunkLength);
    //void onSocketDidSendPartialData(Socket socket, int length);

    //void onSocketDidReceivePartialData(Socket socket, int length);
}
