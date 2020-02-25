/*тут я жертвую логикой названий
 * на самом деле это совсем не worker, это обыкновенный селектор (пока для чтения)
 * и у него будет несоколько зарегистрированых каналов, которые мы будем как-то обрабатывать
 *
 * Есть проблема - из стримов можно было делать parseDelimitedFrom, из канала так делать нельзя
 * поэтому придется перед самим сообщением передавать его размер и делать обычный read*/

package ru.ifmo.java.chat.server;

import ru.ifmo.java.chat.Constants;
import ru.ifmo.java.chat.protocol.Protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

public class ServerWorker3 implements Runnable {
    public Selector selector;
    private Map<Channel, ByteBuffer> buffSize;
    private Map<Channel, ByteBuffer> buffReq;
    private Map<Channel, Long> starts;
    private Queue<SocketChannel> queue;
    private final ExecutorService pool = Executors.newFixedThreadPool(Constants.POOL_SIZE);
    private SelectorWrite swWrite;
    public CyclicBarrier bar;
    public Integer need = 1;
    private Map<Channel, Integer> reqCounts;
    // так как нет отдельного потока для работы с клиентом, информацию о количестве выполненых запросах
    // приходится хранить в мапе

    public ServerWorker3(SelectorWrite swWrite) {
        try {
            selector = Selector.open();
            buffSize = new ConcurrentHashMap<>();
            buffReq = new ConcurrentHashMap<>();
            starts = new ConcurrentHashMap<>();
            queue = new ConcurrentLinkedQueue<>();
            reqCounts = new ConcurrentHashMap<>();
            this.swWrite = swWrite;
        } catch (IOException e) {
            System.out.println("Selector problem");
        }
    }

    public void add(SocketChannel client, long start) {
        buffSize.put(client, ByteBuffer.allocate(4));
        reqCounts.put(client, 0);
        starts.put(client, start);
        queue.add(client);
        selector.wakeup();
    }

    @Override
    public void run() {
        /*читающий селектор в отдельном потоке бегает по клиентам, которые что-то готовы отдать*/
        try {
            while (true) {
                try {
                    while (!queue.isEmpty()) {
                        SocketChannel client = queue.poll();
                        client.register(selector, SelectionKey.OP_READ, client);
                    }
                } catch (ClosedChannelException e ) {
                    System.out.println("Register in writer problem");
                }
                if (selector.select() == 0) continue;
                Set<SelectionKey> channels = selector.selectedKeys();
                Iterator<SelectionKey> iterator = channels.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    SocketChannel client = (SocketChannel) key.attachment();
                    if (key.isReadable()) {
                        processChannel(client);
                    }
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            need = 0;
        }
    }

    private void processChannel(SocketChannel client) throws IOException{
        ByteBuffer size = buffSize.get(client);
        if (size.position() != size.limit()) {
            client.read(size);
            return;
        } else if (!buffReq.containsKey(client)) {
            size.flip();
            int n = size.getInt();
            buffReq.put(client, ByteBuffer.allocate(n));
        }
        client.read(buffReq.get(client));

        if (buffReq.get(client).limit() == buffReq.get(client).position()) {
            //System.out.println("Message ok");
            // если сообщение готово, его надо распарсить и отправить в пул на исполнение
            processSendSort(receiveRequest(client), client);
        } else {
            //System.out.println("Messagw not ok");
        }
    }
    private Protocol.SendSortRequest receiveRequest(SocketChannel client) throws IOException {
        Protocol.SendSortRequest req =  Protocol.SortRequest.parseFrom(buffReq.get(client).array()).getSendSortRequest();
        buffSize.get(client).clear();
        buffReq.remove(client);
        return req;
    }

    private void processSendSort(Protocol.SendSortRequest request, SocketChannel client) {
        Integer[] list = request.getListList().stream().toArray(Integer[]::new);
        pool.submit(new Task(client, list));
        //System.out.println("submitted");
    }

    class Task implements Runnable {
        /*задание, которое будет крутиться в пуле пуле фиксированного размера и в конце своей работы отсылать
         ответ селектору на вывод*/
        private Integer[] list;
        Protocol.SendSortRequest request;
        long start;
        SocketChannel client;
        public Task(SocketChannel client, Integer[] list) {
            this.list = list;
            this.client = client;
        }
        @Override
        public void run() {
            long startSort = System.currentTimeMillis();
            Integer[] res = Constants.mySort(list);
            long finish = System.currentTimeMillis();

            reqCounts.put(client, reqCounts.get(client) + need);
            // тут можно считать, что запрос выполнился и его осталось только отправить в канал
            //System.out.println("Ready to write");
            swWrite.add(client, res, finish - startSort, starts.get(client), reqCounts.get(client), need);
            swWrite.selector.wakeup();

            //System.out.println("Write add");
        }
    }


}
