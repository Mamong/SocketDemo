package com.example.socket;

public class SocketAddress {
    /**
     * IPV4地址
     */
    private String ip;
    /**
     * 连接服务器端口号
     */
    private int port;

    public SocketAddress(String ip, int port){
        this.ip =ip;
        this.port =port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String toString(){
        return ip+":"+port;
    }
}
