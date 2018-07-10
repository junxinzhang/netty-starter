package com.github.junxin.netty.starter.day04;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @Author zhangjx
 * @Date 2018/7/10.15:08
 */
public class AcceptCompletionHandler implements
    java.nio.channels.CompletionHandler<java.nio.channels.AsynchronousSocketChannel, AsyncTimerServerHandler> {
    /**
     * Invoked when an operation has completed.
     *
     * @param result     The result of the I/O operation.
     * @param attachment
     */
    @Override
    public void completed(AsynchronousSocketChannel result, AsyncTimerServerHandler attachment) {

        attachment.asynchronousServerSocketChannel.accept(attachment, this);

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        result.read(buffer, buffer, new ReadCompletionHandler(result));

    }

    /**
     * Invoked when an operation fails.
     *
     * @param exc        The exception to indicate why the I/O operation failed
     * @param attachment
     */
    @Override
    public void failed(Throwable exc, AsyncTimerServerHandler attachment) {
        exc.printStackTrace();
        attachment.latch.countDown();
    }
}
