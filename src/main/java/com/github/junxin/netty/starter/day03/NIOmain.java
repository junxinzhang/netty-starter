package com.github.junxin.netty.starter.day03;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @Author zhangjx
 * @Date 2018/7/9.15:50
 */
public class NIOmain {

    public static void main(String[] args) throws IOException {

        // 打开ServetSocketChannel:用于监听客户端的连接，它是所有客户端连接的父管道
        ServerSocketChannel channel = ServerSocketChannel.open();

        // 绑定监听端口，设置连接为非阻塞模式
        channel.socket().bind(new InetSocketAddress("localhost", 8080));
        channel.configureBlocking(false);

        // 创建Reactor线程，创建多路复用器并启动线程
        Selector selector = Selector.open();
        new Thread(new ReactorTask()).start();

        // 将ServerSocketChannel注册到Reactor线程的多路复用器Selector上，监听accept事件
        Object ioHandler = null;
        channel.register(selector, SelectionKey.OP_ACCEPT, ioHandler);

        // 多路复用器在线程run方法的无限循环体内轮询准备就绪的Key
        int num = selector.select();
        Set<SelectionKey> selectionKeys = selector.selectedKeys();
        Iterator<SelectionKey> it = selectionKeys.iterator();
        while (it.hasNext()) {
            SelectionKey key = it.next();
            // TODO: ...
        }

        // 多路复用器监听到有新的客户端接入，处理新的接入请求，完成TCP三次握手，建立物理链路
        SocketChannel socketChannel = channel.accept();

        // 设置客户端链路为非阻塞模式
        socketChannel.configureBlocking(false);
        socketChannel.socket().setReuseAddress(true);

        // 将新接入的客户端连接注册到reactor线程的多路复用器上，监听读操作，读取客户端发送的网络消息
        socketChannel.register(selector, SelectionKey.OP_READ, ioHandler);

        // 异步读取客户端请求消息到缓冲区
        ByteBuffer receiveBuffer = null;
        socketChannel.read(receiveBuffer);

        // 对ByteBuffer进行编码，如果有半包消息指针reset，继续读取后续的报文，将解码成功的消息封装成Task，投递到业务线程池中，进行业务逻辑编排
        Object message = null;
        List<Object> messageList = new ArrayList<Object>();
        while (receiveBuffer.hasRemaining()) {
            receiveBuffer.mark();
            message = decode(receiveBuffer);
            if (message == null) {
                receiveBuffer.reset();
                break;
            }
            messageList.add(message);
        }
        if (!receiveBuffer.hasRemaining()) {
            receiveBuffer.clear();
        } else {
            receiveBuffer.compact();
        }
        if (messageList != null && !messageList.isEmpty()) {
            for (Object messageE : messageList) {
                handleTask(messageE);
            }
        }

        // 将POJO对象encode成ByteBuffer，调用SocketChannel的异步write接口，将消息异步发送给客户端
        ByteBuffer buffer = null;
        socketChannel.write(buffer);
    }

    private static void handleTask(Object messageE) {

    }

    private static Object decode(ByteBuffer receiveBuffer) {

        return null;
    }

}
