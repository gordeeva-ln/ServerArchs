/*Это вариант третьей архитектуры сервера, где он нужен, там его и запускайте, но не забудте прописать его хост и порт
 * в Constants*/

package ru.ifmo.java.chat.server;

import ru.ifmo.java.chat.Constants;
import ru.ifmo.java.chat.protocol.Protocol;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.*;


public class Server3 {
    private static Map<Channel, ByteBuffer> buffSize;
    private static Map<Channel, ByteBuffer> buffReq;
    private static ExecutorService selectors = Executors.newFixedThreadPool(2);
    private static SelectorWrite swWrite;
    private static ServerWorker3 swRead;

    private static ServerSocketChannel serverSocketChannel;
    private boolean first = true;
    private CyclicBarrier BARRIER;
    Future write, read;
    int m = 0;
    boolean ready = false;

    public static void main(String[] args) throws IOException {
        swWrite = new SelectorWrite();
        swRead = new ServerWorker3(swWrite);
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(Constants.PORT_3));
        buffSize = new ConcurrentHashMap<>();
        buffReq = new ConcurrentHashMap<>();

        //selectors.submit(swWrite);
        //selectors.submit(swRead);
        System.out.println("Server3 is ready to work!");
        new Server3().run();
    }

    public void run() {
        /*последовательно подключаем всех клиентов к селектору на чтение*/
        try {
            while (true) {
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel != null) {
                    socketChannel.configureBlocking(false);
                    buffSize.put(socketChannel, ByteBuffer.allocate(4));
                    while (!ready) processChannel(socketChannel);
                    //System.out.println(m);
                    if (first) {
                        BARRIER = new CyclicBarrier(m, null);
                        swRead.bar = BARRIER;
                        swWrite.bar = BARRIER;
                        read = selectors.submit(swRead);
                        write = selectors.submit(swWrite);
                        first = false;
                    }
                    new Fake(socketChannel).start();
                    ready = false;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocketChannel.close();
                swRead.selector.close();
                swWrite.selector.close();
                selectors.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }
    class Fake extends Thread{
        SocketChannel socketChannel;
        public Fake(SocketChannel socketChannel) {
            this.socketChannel = socketChannel;
        }
        @Override
        public void run() {
            try {BARRIER.await();
            } catch (Exception e) { System.out.println("probs");}
            //System.out.println("Awqke!!!");
            swRead.add(socketChannel, System.currentTimeMillis());
        }
    }

    private Protocol.SendCountRequest receiveRequest(SocketChannel client) throws IOException {
        Protocol.SendCountRequest req = Protocol.SortRequest.parseFrom(buffReq.get(client).array()).getSendCountRequest();
        buffSize.get(client).clear();
        buffReq.remove(client);
        return req;
    }

    private void processChannel(SocketChannel client) throws IOException {
        ByteBuffer size = buffSize.get(client);
        if (size.position() != size.capacity()) {
            client.read(size);
            return;
        } else if (!buffReq.containsKey(client)) {
            size.flip();
            int n = size.getInt();
            buffReq.put(client, ByteBuffer.allocate(n));
        }
        client.read(buffReq.get(client));

        if (buffReq.get(client).capacity() == buffReq.get(client).position()) {
            processM(receiveRequest(client));
            ready = true;
        }
    }

    private void processM(Protocol.SendCountRequest request) {
        m = request.getClientCount();
    }
}

