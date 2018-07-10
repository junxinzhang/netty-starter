package com.github.junxin.netty.starter.day03;

/**
 * @Author zhangjx
 * @Date 2018/7/9.17:08
 */
public class TimerServer {

    public static void main(String[] args) {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        MultiplexerTimerServer timerServer = new MultiplexerTimerServer(port);

        new Thread(timerServer, "NIO-MultiplexerTimerServer-001").start();
    }
}
