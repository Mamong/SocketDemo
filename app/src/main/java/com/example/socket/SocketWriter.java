package com.example.socket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.LinkedBlockingDeque;

import com.example.socket.dispatcher.ISocketActionListener;


/**
 * Author：Alex
 * Date：2019/6/1
 * Note：
 */
public class SocketWriter {

    /**
     * 输出流
     */
    private OutputStream outputStream;

    private SocketOptions socketOptions;

    /**
     * 写入数据的线程
     */
    private Thread writerThread;
    /**
     * 需要写入的数据
     */
    private final LinkedBlockingDeque<byte[]> packetsToSend = new LinkedBlockingDeque<>();
    /**
     * 是否关闭线程
     */
    private boolean isShutdown;

    private final Socket socket;

    private ISocketActionListener listener;

    private SocketConnection io;

    public SocketWriter(SocketConnection io, ISocketActionListener listener) {
        this.io = io;
        this.socket = io.getSocket();
        this.listener = listener;
    }

    public void openWriter() {
        try {
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        if (writerThread == null) {
//            writerThread = new Thread(writerTask, "writer thread");
//            isShutdown = false;
//            writerThread.start();
//        }
    }

    public void setOption(SocketOptions socketOptions) {
        this.socketOptions = socketOptions;
    }

    /**
     * io写任务
     */
//    private final Runnable writerTask = new Runnable() {
//        @Override
//        public void run() {
//            //只要socket处于连接的状态，就一直活动
//            while (socket.isConnected() && !isShutdown && !socket.isClosed()) {
//                try {
//                    byte[] data = packetsToSend.take();
//                    write(data);
//                } catch (InterruptedException e) {
//                    //取数据异常
//                    e.printStackTrace();
//                    isShutdown = true;
//                }
//            }
//        }
//    };

    public void write(byte[] sendBytes) {
        if (sendBytes != null) {
            try {
                outputStream.write(sendBytes);
                outputStream.flush();
//                int packageSize = 100; //每次发送的数据包的大小
//                int remainingCount = sendBytes.length;
//                ByteBuffer writeBuf = ByteBuffer.allocate(packageSize); //分配一个内存缓存
//                writeBuf.order(ByteOrder.BIG_ENDIAN);
//                int index = 0;
//                //如果要发送的数据大小大于每次发送的数据包的大小， 则要分多次将数据发出去
//                while (remainingCount > 0) {
//                    int realWriteLength = Math.min(packageSize, remainingCount);
//                    writeBuf.clear(); //清空缓存
//                    writeBuf.rewind(); //将position位置移到0
//                    writeBuf.put(sendBytes, index, realWriteLength);
//                    writeBuf.flip(); //将position赋为0，limit赋为数据大小
//                    byte[] writeArr = new byte[realWriteLength];
//                    writeBuf.get(writeArr);
//                    outputStream.write(writeArr);
//                    outputStream.flush(); //强制缓存中残留的数据写入清空
//                    index += realWriteLength;
//                    remainingCount -= realWriteLength;
//                }
            } catch (SocketException e){
                //Socket异常
                e.printStackTrace();
                io.disconnect(true);
                //listener.onSocketDisconnect(socket,true);
            } catch (Exception e) {
                //写数据异常
                e.printStackTrace();
                isShutdown = true;
            }
        }
    }

    //消息入队列
//    public void offer(byte[] bytes) {
//        packetsToSend.offer(bytes);
//    }

    //直接发送chunk
    public void chunk(RandomAccessFile file,int tag) { sendFile(file,tag); }

    private void sendFile(RandomAccessFile fileOutStream,int tag) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0);
        buffer.putInt(tag);

        byte[] bytes = new byte[1024*4];
        try {
            OutputStream os = outputStream;
            int size;
            while ((size = fileOutStream.read(bytes)) > 0) {
                buffer.putInt(0,size);
                os.write(buffer.array());
                os.write(bytes, 0, size);
                os.flush();
                listener.onSocketDidSendChunk(socket,size);
                //每1kb清空一次缓冲区
                //为了避免每读入一个字节都写一次，java的输流有了缓冲区，读入数据时会首先将数据读入缓冲区，等缓冲区满后或执行flush或close时一次性进行写入操作
            }
        }catch(SocketException e){
            //己方关闭后读写,java.net.SocketException: Socket is closed
            //对方关闭后读写，java.net.SocketException: （Connection reset或者Connect reset by peer:Socket write error）
            //继续发送，java.net.SocketException: Broken pipe
            //读写超时,java.net.SocketTimeoutException
            e.printStackTrace();
            //io.disconnect(true);
            //listener.onSocketDisconnect(socket,true);
        }catch (IOException e) {
            //其他异常，应该只要传输失败就行
            e.printStackTrace();
            //isShutdown = true;
        }

        try {
            fileOutStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void closeWriter() {
        try {
            // 关闭线程释放资源
            shutDownThread();
            release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void release() {
        try {
            packetsToSend.clear();

            if (outputStream != null)
                outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            outputStream = null;
        }
    }

    private void shutDownThread() throws InterruptedException{
        if (writerThread != null && writerThread.isAlive() && !writerThread.isInterrupted()) {
            isShutdown = true;
            writerThread.interrupt();
            writerThread.join();
            writerThread = null;
        }
    }
}
