package com.github.junxin.netty.starter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @Author zhangjx
 * @Date 2018/7/7.18:27
 */
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
            while (true) {
                socket = server.accept();
                new Thread(new TimerServerHandler(socket)).start();
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
