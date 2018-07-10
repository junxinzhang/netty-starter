package com.github.junxin.netty.starter.day04;

import com.github.junxin.netty.starter.day03.TimerClientHandler;

/**
 * @Author zhangjx
 * @Date 2018/7/9.20:29
 */
public class TimerClient {

    public static void main(String[] args) {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        new Thread(new AsyncTimerClientHandler("127.0.0.1", port), "AIO-AsyncTimerClientHandler-001").start();

    }
}
