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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerWorker3 implements Runnable {
    public Selector selector;
    private Map<Channel, ByteBuffer> buffSize;
    private Map<Channel, ByteBuffer> buffReq;
    private final ExecutorService pool = Executors.newFixedThreadPool(Constants.POOL_SIZE);
    private SelectorWrite swWrite;

    public ServerWorker3(SelectorWrite swWrite) {
        try {
            selector = Selector.open();
            buffSize = new HashMap<>();
            buffReq = new HashMap<>();
            this.swWrite = swWrite;
        } catch (IOException e) {
            System.out.println("Selector problem");
        }

    }

    public void add(SocketChannel client) throws ClosedChannelException {
        /*добавление нового клиента в селектор на чтение*/
        selector.wakeup();
        buffSize.put(client, ByteBuffer.allocate(4));
        selector.wakeup();
        client.register(selector, SelectionKey.OP_READ, client);
    }

    @Override
    public void run() {
        /*читающий селектор в отдельном потоке бегает по клиентам, которые что-то готовы отдать*/
        while (true) {
            try {
                selector.selectNow();
                Set<SelectionKey> channels = selector.selectedKeys();
                Iterator<SelectionKey> iterator = channels.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    SocketChannel client = (SocketChannel) key.attachment();
                    if (key.isReadable()) {
                        ByteBuffer size = buffSize.get(client);
                        if (size.position() != size.capacity()) {
                            // если в буфере соответсвующем размеру размер еще не лежит дочитываем его туда
                            client.read(size);
                            continue;
                        } else if (!buffReq.containsKey(client)) {
                            // как только размер считан в буфер, достаем число и создаем буфер под сообщение
                            // после того как очередное сообщение получено, удалем соответствующий буфер
                            size.flip();
                            int n = size.getInt();
                            buffReq.put(client, ByteBuffer.allocate(n));
                        }
                        client.read(buffReq.get(client));

                        if (buffReq.get(client).capacity() == buffReq.get(client).position()) {
                            // если сообщение готово, его надо распарсить и отправить в пул на исполнение
                            processSendSort(receiveRequest(client), client);
                        }
                    }
                    iterator.remove();
                }
            } catch (IOException e) {
                System.out.println("While select IO problem");
            }
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
            swWrite.add(client, res, finish - startSort, start);
        }
    }
}
