/*Это вариант второй архитектуры сервера, где он нужен, там его и запускайте, но не забудте прописать его хост и порт
 * в Constants*/

package ru.ifmo.java.chat.server;

import ru.ifmo.java.chat.Constants;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server2 {
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private Map<Socket, ExecutorService> singles = new HashMap<>();
    private final ExecutorService poolTasks = Executors.newFixedThreadPool(Constants.POOL_SIZE);

    public static void main(String[] args) throws IOException {
        new Server2().run();
    }

    public void run() throws IOException {
        ServerSocket serverSocket = new ServerSocket(Constants.PORT_2);
        System.out.println("Server2 is ready to work!");
        while (true) {
            Socket socket = serverSocket.accept();
            singles.put(socket, Executors.newSingleThreadExecutor());
            pool.submit(new ServerWorker2(socket, poolTasks, singles.get(socket)));
        }
    }
}

