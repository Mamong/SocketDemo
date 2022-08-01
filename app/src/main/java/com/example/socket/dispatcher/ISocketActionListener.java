package com.example.socket.dispatcher;

import java.net.Socket;

/**
 * Author：Alex
 * Date：2019/6/1
 * Note：socket行为监听接口
 */
public interface ISocketActionListener {

    void onSocketAccept(Socket socket);

    void onSocketConnSuccess(Socket socket);

    void onSocketConnFail(Socket socket, boolean isNeedReconnect);

    void onSocketDisconnect(Socket socket, boolean isNeedReconnect);

//    void onSocketReceiveData(Socket socket, byte[] readData);

//    void onSocketDidSendData(Socket socket);

    void onSocketReceivePacket(Socket socket, byte[] readData, int tag);
//    void onSocketReceiveChunk(Socket socket, byte[] readData);

    void  onSocketDidSendChunk(Socket socket, int chunkLength);
    //void onSocketDidSendPartialData(Socket socket, int length);

    //void onSocketDidReceivePartialData(Socket socket, int length);
}
