/*Это вариант первой архитектуры сервера, где он нужен, там его и запускайте, но не забудте прописать его хост и порт
* в Constants*/

package ru.ifmo.java.chat.server;

import ru.ifmo.java.chat.Constants;
import ru.ifmo.java.chat.protocol.Protocol;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private boolean first = true;
    public CyclicBarrier BARRIER;
    public Integer needToStoreStat = 1;

    public static void main(String[] args) throws IOException, InterruptedException, BrokenBarrierException  {
        new Server().run();
    }

    private void run() throws IOException,  InterruptedException, BrokenBarrierException {
        ServerSocket serverSocket = new ServerSocket(Constants.PORT_1);
        System.out.println("Server is ready to work!");

        while (true) {
            Socket socket = serverSocket.accept();
            if (socket != null) {
                Protocol.SortRequest requestM = receiveRequest(socket.getInputStream());
                int m = 0;
                if (requestM.hasSendCountRequest()) m = requestM.getSendCountRequest().getClientCount();
                if (first) {
                    BARRIER = new CyclicBarrier(m, null);
                    first = false;
                }
                pool.submit(new ServerWorker(socket, this));
            }
        }
    }

    private Protocol.SortRequest receiveRequest(InputStream input) throws IOException {
        return Protocol.SortRequest.parseDelimitedFrom(input);
    }
}

