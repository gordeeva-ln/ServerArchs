/*Это вариант третьей архитектуры сервера, где он нужен, там его и запускайте, но не забудте прописать его хост и порт
 * в Constants*/

package ru.ifmo.java.chat.server;

import ru.ifmo.java.chat.Constants;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server3 {

    public static void main(String[] args) throws IOException {
        new Server3().run();
    }

    public void run() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(Constants.PORT_3));
        ExecutorService selectors = Executors.newFixedThreadPool(2);
        System.out.println("Server3 is ready to work!");

        // второй поток под селектор на вывод
        SelectorWrite swWrite = new SelectorWrite();
        selectors.submit(swWrite);

        // создаем поток под селектор на ввод
        ServerWorker3 swRead = new ServerWorker3(swWrite);
        selectors.submit(swRead);

        while (true) {
            SocketChannel socketChannel = serverSocketChannel.accept();
            if (socketChannel != null) {
                socketChannel.configureBlocking(false);
                swRead.add(socketChannel);
                // на вывод не регистрируем, только когда захотим что-то выводить
            }
        }
    }
}

