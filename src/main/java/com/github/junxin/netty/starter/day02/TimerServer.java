package com.github.junxin.netty.starter.day02;

import com.github.junxin.netty.starter.day01.TimerServerHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TimerServer {
    public static void main(String[] args) throws IOException {

        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
            System.out.println("The time server is start in port: " + port);
            Socket socket = null;
            TimerServerHandlerExecutePool singleExecutor = new TimerServerHandlerExecutePool(
                50, 10000); // 创建任务I/O线程池
            while (true) {
                socket = server.accept();
                singleExecutor.execute(new TimerServerHandler(socket));
            }

        } finally {
            if (server != null) {
                System.out.println("The time server close");
                server.close();
                server = null;
            }
        }

    }

}
