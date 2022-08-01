package com.example.socket;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.example.socket.dispatcher.ISocketActionListener;
import com.example.socket.exception.ReadRecoverableException;
import com.example.socket.exception.ReadUnrecoverableException;

/**
 * Author：Alex
 * Date：2019/6/1
 * Note：
 */
public class SocketReader {
    /**
     * 输入流
     */
    private InputStream inputStream;

    private SocketOptions socketOptions;

    /**
     * 读取数据时，没读完的残留数据缓存
     */
    private ByteBuffer remainingBuf;
    /**
     * 读数据线程
     */
    private Thread readerThread;

    private final Socket socket;

    /**
     * 处理接收到数据
     */

    private final ISocketActionListener listener;

    private boolean stopThread;


    public SocketReader(Socket socket, ISocketActionListener listener) {
        this.listener = listener;
        this.socket = socket;
    }

    public void read() throws IOException, ReadRecoverableException, ReadUnrecoverableException {

        // 定义了消息协议
        int headerLength = 8; // 包头长度
        ByteBuffer headBuf = ByteBuffer.allocate(headerLength); // 包头数据的buffer
        headBuf.order(ByteOrder.BIG_ENDIAN);

        //1、读 header=====>>>
        if (remainingBuf != null) { // 有余留
            // flip方法：一般从Buffer读数据前调用，将limit设置为当前position，将position设置为0，在读数据时，limit代表可读数据的有效长度
            remainingBuf.flip();
            // 读余留数据的长度
            int length = Math.min(remainingBuf.remaining(), headerLength);
            // 读入余留数据
            headBuf.put(remainingBuf.array(), 0, length);

            if (length < headerLength) { // 余留数据小于一个header
                // there are no data left
                remainingBuf = null;
                // 从stream中读剩下的header数据
                readHeaderFromSteam(headBuf, headerLength - length);
            } else {
                // 移动开始读数据的指针
                remainingBuf.position(headerLength);
            }
        } else { // 无余留
            // 从stream读取一个完整的 header
            readHeaderFromSteam(headBuf, headBuf.capacity());
        }

        //2、读 body=====>>>
        int bodyLength = headBuf.getInt(0);
        int bodyType = headBuf.getInt(4);
        if (bodyLength > 0) {
            if (bodyLength > socketOptions.getMaxResponseDataMb() * 1024 * 1024) {
                throw new ReadUnrecoverableException("服务器返回的单次数据超过了规定的最大值，可能你的Socket消息协议不对，一般消息格式" +
                        "为：Header+Body，其中Header保存消息长度和类型等，Body保存消息内容，请规范好你的协议");
            }
            // 分配空间
            ByteBuffer bodyBuf = ByteBuffer.allocate(bodyLength);
            bodyBuf.order(ByteOrder.BIG_ENDIAN);

            // 有余留
            if (remainingBuf != null) {
                int bodyStartPosition = remainingBuf.position();

                int length = Math.min(remainingBuf.remaining(), bodyLength);
                // 读length大小的余留数据
                bodyBuf.put(remainingBuf.array(), bodyStartPosition, length);
                // 移动position位置
                remainingBuf.position(bodyStartPosition + length);

                // 读的余留数据刚好等于一个body
                if (length == bodyLength) {
                    if (remainingBuf.remaining() > 0) { // 余留数据未读完
                        ByteBuffer temp = ByteBuffer.allocate(remainingBuf.remaining());
                        temp.order(ByteOrder.BIG_ENDIAN);
                        temp.put(remainingBuf.array(), remainingBuf.position(), remainingBuf.remaining());
                        remainingBuf = temp;
                    } else { // there are no data left
                        remainingBuf = null;
                    }

                    // 分发数据
                    listener.onSocketReceivePacket(socket, bodyBuf.array(),bodyType);
                    return;

                } else { // there are no data left in buffer and some data pieces in channel
                    remainingBuf = null;
                }
            }
            // 无余留，则从stream中读
            readBodyFromStream(bodyBuf);
            listener.onSocketReceivePacket(socket, bodyBuf.array(),bodyType);
        } else if (bodyLength == 0) { // 没有body数据
            if (remainingBuf != null) {
                // the body is empty so header remaining buf need set null
                if (remainingBuf.hasRemaining()) {
                    ByteBuffer temp = ByteBuffer.allocate(remainingBuf.remaining());
                    temp.order(ByteOrder.BIG_ENDIAN);
                    temp.put(remainingBuf.array(), remainingBuf.position(), remainingBuf.remaining());
                    remainingBuf = temp;
                } else {
                    remainingBuf = null;
                }
            }
        } else {
            throw new ReadUnrecoverableException("数据body的长度不能小于0");
        }
    }

    /**
     * 读取数据任务类
     */
    private final Runnable readerTask = new Runnable() {
        @Override
        public void run() {
            try {
                while (!stopThread) {
                    read();
                }
            } catch (ReadUnrecoverableException unrecoverableException) {
                // 读异常
                unrecoverableException.printStackTrace();
                // 停止线程
                stopThread = true;
                release();
                listener.onSocketDisconnect(socket,false);
            } catch (ReadRecoverableException | IOException readRecoverableException) {
                readRecoverableException.printStackTrace();
                // 重连
                //connectionManager.disconnect(true);
                listener.onSocketDisconnect(socket,true);
            }
        }
    };

    public void readHeaderFromSteam(ByteBuffer headBuf, int readLength) throws ReadRecoverableException, IOException {
        for (int i = 0; i < readLength; i++) {
            byte[] bytes = new byte[1];
            // 从输入流中读数据，无数据时会阻塞
            int value = inputStream.read(bytes);
            // -1代表读到了文件的末尾，一般是因为服务器断开了连接
            if (value == -1) {
                throw new ReadRecoverableException("Connect reset by peer:Socket read error");
            }
            headBuf.put(bytes);
        }
    }

    private void readBodyFromStream(ByteBuffer byteBuffer) throws ReadRecoverableException, IOException {
        // while循环直到byteBuffer装满数据
        while (byteBuffer.hasRemaining()) {
            byte[] bufArray = new byte[50]; // 从服务器单次读取的最大值
            int len = inputStream.read(bufArray);
            if (len == -1) { // no more data
                throw new ReadRecoverableException("Connect reset by peer:Socket read error");
            }
            int remaining = byteBuffer.remaining();
            if (len > remaining) { // 从stream读的数据超过byteBuffer的剩余空间
                byteBuffer.put(bufArray, 0, remaining);
                // 将多余的数据保存到remainingBuf中缓存，等下一次读取
                remainingBuf = ByteBuffer.allocate(len - remaining);
                remainingBuf.order(ByteOrder.BIG_ENDIAN);
                remainingBuf.put(bufArray, remaining, len - remaining);
            } else { // 从stream读的数据小于或等于byteBuffer的剩余空间
                byteBuffer.put(bufArray, 0, len);
            }
        }
    }

    public void openReader() {
        try {
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (readerThread == null || !readerThread.isAlive()) {
            readerThread = new Thread(readerTask, "reader thread");
            stopThread = false;
            readerThread.start();
        }
    }

    public void closeReader() {
        try {
            // 关闭线程释放资源
            shutDownThread();
            release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 释放资源
    private void release() {

        if (remainingBuf != null) {
            remainingBuf = null;
        }

        try {
            if (inputStream != null)
                inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            inputStream = null;
        }
    }

    public void setOption(SocketOptions socketOptions) {
        this.socketOptions = socketOptions;
    }

    private void shutDownThread() throws InterruptedException {
        if (readerThread != null && readerThread.isAlive() && !readerThread.isInterrupted()) {
            stopThread = true;
            readerThread.interrupt();
            readerThread.join();
            readerThread = null;
        }
    }
}
