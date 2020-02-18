package ru.ifmo.java.chat.server;

import ru.ifmo.java.chat.protocol.Protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SelectorWrite implements Runnable {
    private Selector selector;
    private Map<Channel, Queue<ByteBuffer>> buffs;

    public SelectorWrite() {
        try {
            selector = Selector.open();
            buffs = new HashMap<>();
        } catch (IOException e) {
            System.out.println("Selector problem");
        }
    }
    @Override
    public void run() {
        while (true) {
            try {
                selector.selectNow();
                Set<SelectionKey> channels = selector.selectedKeys();
                Iterator<SelectionKey> iterator = channels.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    SocketChannel client = (SocketChannel) key.attachment();
                    if (key.isWritable()) {
                        processClient(client);
                        if (buffs.get(client).isEmpty()) key.cancel();
                    }
                    iterator.remove();
                }
            } catch (IOException e) {
                System.out.println("While select IO problem");
            }
        }
    }

    private void processClient(SocketChannel client) throws IOException{
        ByteBuffer next = buffs.get(client).element();
        client.write(next);
        if (next.position() == next.limit()) {
            buffs.get(client).poll();
        }
    }

    public void add(SocketChannel client, Integer[] list, long sort, long start) {
        /*добавляем новый запрос на вывод, если клиент уже зарегистрирован,
        то в его очередь, а если нет, то рагистрируем и создаем под него новую очередь*/
        selector.wakeup();
        Protocol.SendSortResponse response = Protocol.SendSortResponse.newBuilder()
                .setCount(list.length).addAllList(Arrays.asList(list))
                .setSort(sort).setAll(System.currentTimeMillis() - start).build();

        ByteBuffer bsize = ByteBuffer.allocate(4).putInt(response.getSerializedSize());
        bsize.flip();
        ByteBuffer barray = ByteBuffer.wrap(response.toByteArray());

        if (client.keyFor(selector) == null) {

            try {
                buffs.put(client, new ConcurrentLinkedQueue<>());
                buffs.get(client).add(bsize);
                buffs.get(client).add(barray);

                selector.wakeup();
                client.register(selector, SelectionKey.OP_WRITE, client);
            } catch (ClosedChannelException e) {
                System.out.println("Register in writer problem");
            }

        } else {
            buffs.get(client).add(bsize);
            buffs.get(client).add(barray);
        }
    }
}
