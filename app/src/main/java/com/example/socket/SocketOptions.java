package com.example.socket;

import java.nio.ByteOrder;

public class SocketOptions {

    /**
     * 写入Socket管道的字节序
     */
    private ByteOrder writeOrder;
    /**
     * 从Socket读取字节时的字节序
     */
    private ByteOrder readOrder;

    /**
     * 写数据时单个数据包的最大值
     */
    private int maxWriteBytes;
    /**
     * 读数据时单次读取最大缓存值，数值越大效率越高，但是系统消耗也越大
     */
    private int maxReadBytes;
    /**
     * 心跳频率/毫秒
     */
    private long heartbeatFreq;
    /**
     * 心跳最大的丢失次数，大于这个数据，将断开socket连接
     */
    private int maxHeartbeatLoseTimes;
    /**
     * 连接超时时间(毫秒)
     */
    private int connectTimeout;
    /**
     * 服务器返回数据的最大值（单位Mb），防止客户端内存溢出
     */
    private int maxResponseDataMb;

    /**
     * 请求超时时间，单位毫秒
     */
    private long requestTimeout;
    /**
     * 是否开启请求超时检测
     */
    private boolean isOpenRequestTimeout;

    public static class Builder {
        SocketOptions socketOptions;

        // 首先获得一个默认的配置
        public Builder() {
            this(getDefaultOptions());
        }

        public Builder(SocketOptions defaultOptions) {
            socketOptions = defaultOptions;
        }

        /**
         * 设置是否开启请求超时的检测
         *
         */
        public Builder setOpenRequestTimeout(boolean openRequestTimeout) {
            socketOptions.isOpenRequestTimeout = openRequestTimeout;
            return this;
        }

        /**
         * 设置请求超时时间
         *
         * @param requestTimeout 毫秒
         * @return
         */
        public Builder setRequestTimeout(long requestTimeout) {
            socketOptions.requestTimeout = requestTimeout;
            return this;
        }

        /**
         * 设置写数据的字节顺序
         *
         * @param writeOrder
         * @return
         */
        public Builder setWriteOrder(ByteOrder writeOrder) {
            socketOptions.writeOrder = writeOrder;
            return this;
        }

        /**
         * 设置读数据的字节顺序
         *
         * @param readOrder
         * @return
         */
        public Builder setReadOrder(ByteOrder readOrder) {
            socketOptions.readOrder = readOrder;
            return this;
        }


        /**
         * 设置写数据时单个数据包的最大值
         *
         * @param maxWriteBytes
         * @return
         */
        public Builder setMaxWriteBytes(int maxWriteBytes) {
            socketOptions.maxWriteBytes = maxWriteBytes;
            return this;
        }

        /**
         * 设置读数据时单次读取的最大缓存值
         *
         * @param maxReadBytes
         * @return
         */
        public Builder setMaxReadBytes(int maxReadBytes) {
            socketOptions.maxReadBytes = maxReadBytes;
            return this;
        }

        /**
         * 设置心跳发送频率，单位毫秒
         *
         * @param heartbeatFreq
         * @return
         */
        public Builder setHeartbeatFreq(long heartbeatFreq) {
            socketOptions.heartbeatFreq = heartbeatFreq;
            return this;
        }

        /**
         * 设置心跳丢失的最大允许数，如果超过这个最大数就断开socket连接
         *
         * @param maxHeartbeatLoseTimes
         * @return
         */
        public Builder setMaxHeartbeatLoseTimes(int maxHeartbeatLoseTimes) {
            socketOptions.maxHeartbeatLoseTimes = maxHeartbeatLoseTimes;
            return this;
        }

        /**
         * 设置连接超时时间
         *
         * @param connectTimeout
         * @return
         */
        public Builder setConnectTimeout(int connectTimeout) {
            socketOptions.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * 设置服务器返回数据的允许的最大值，单位兆
         *
         * @param maxResponseDataMb
         * @return
         */
        public Builder setMaxResponseDataMb(int maxResponseDataMb) {
            socketOptions.maxResponseDataMb = maxResponseDataMb;
            return this;
        }

        public SocketOptions build() {
            return socketOptions;
        }
    }


    /**
     * 获取默认的配置
     *
     * @return
     */
    public static SocketOptions getDefaultOptions() {
        SocketOptions options = new SocketOptions();
        options.heartbeatFreq = 5 * 1000;
        options.maxResponseDataMb = 5;
        options.connectTimeout = 10 * 1000; // 连接超时默认5秒
        options.maxWriteBytes = 100;
        options.maxReadBytes = 50;
        options.readOrder = ByteOrder.BIG_ENDIAN;
        options.writeOrder = ByteOrder.BIG_ENDIAN;
        options.maxHeartbeatLoseTimes = 5;
        //options.reconnectionManager = new DefaultReConnection();
        options.requestTimeout = 10 * 1000; // 默认十秒
        options.isOpenRequestTimeout = true; // 默认开启
        //options.charsetName = "UTF-8";
        return options;
    }

    public ByteOrder getWriteOrder() {
        return writeOrder;
    }

    public ByteOrder getReadOrder() {
        return readOrder;
    }

    public int getMaxWriteBytes() {
        return maxWriteBytes;
    }

    public int getMaxReadBytes() {
        return maxReadBytes;
    }

    public long getHeartbeatFreq() {
        return heartbeatFreq;
    }

    public int getMaxHeartbeatLoseTimes() {
        return maxHeartbeatLoseTimes;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getMaxResponseDataMb() {
        return maxResponseDataMb;
    }

    public long getRequestTimeout() {
        return requestTimeout;
    }

    public boolean isOpenRequestTimeout() {
        return isOpenRequestTimeout;
    }

    public void setWriteOrder(ByteOrder writeOrder) {
        this.writeOrder = writeOrder;
    }

    public void setReadOrder(ByteOrder readOrder) {
        this.readOrder = readOrder;
    }

    public void setMaxWriteBytes(int maxWriteBytes) {
        this.maxWriteBytes = maxWriteBytes;
    }

    public void setMaxReadBytes(int maxReadBytes) {
        this.maxReadBytes = maxReadBytes;
    }

    public void setHeartbeatFreq(long heartbeatFreq) {
        this.heartbeatFreq = heartbeatFreq;
    }

    public void setMaxHeartbeatLoseTimes(int maxHeartbeatLoseTimes) {
        this.maxHeartbeatLoseTimes = maxHeartbeatLoseTimes;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setMaxResponseDataMb(int maxResponseDataMb) {
        this.maxResponseDataMb = maxResponseDataMb;
    }



    public void setRequestTimeout(long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public void setOpenRequestTimeout(boolean openRequestTimeout) {
        isOpenRequestTimeout = openRequestTimeout;
    }

}
