package com.github.junxin.netty.starter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The type Timer server handler.
 *
 * @Author zhangjx
 * @Date 2018 /7/7.18:34
 */
public class TimerServerHandler implements Runnable {

    private Socket socket;

    /**
     * Instantiates a new Timer server handler.
     *
     * @param socket the socket
     */
    public TimerServerHandler(Socket socket) {
        this.socket = socket;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

        BufferedReader in = null;
        PrintWriter out = null;
        try {
            in = new BufferedReader(
                new InputStreamReader(this.socket.getInputStream()));
            out = new PrintWriter(this.socket.getOutputStream(), true);

            String currentTime = null;
            String body = null;
            while (true) {
                body = in.readLine();
                if (body == null)
                    break;
                System.err.println("The time server receive order: " + body);
                currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ?
                    new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒").format(new Date()) :
                    "BAD ORDER";
                out.println(currentTime);
            }

        } catch (IOException e) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (out != null) {
                out.close();
                out = null;
            }
            if (this.socket != null) {
                try {
                    this.socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                this.socket = null;
            }
        }

    }
}
