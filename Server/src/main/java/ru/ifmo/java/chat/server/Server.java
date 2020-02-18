/*Это вариант первой архитектуры сервера, где он нужен, там его и запускайте, но не забудте прописать его хост и порт
* в Constants*/

package ru.ifmo.java.chat.server;

import ru.ifmo.java.chat.Constants;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) throws IOException {
        new Server().run();
    }

    private void run() throws IOException {
        ServerSocket serverSocket = new ServerSocket(Constants.PORT_1);
        System.out.println("Server is ready to work!");
        while (true) {
            Socket socket = serverSocket.accept();
            pool.submit(new ServerWorker(socket));
        }
    }
}

