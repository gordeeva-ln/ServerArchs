package ru.ifmo.java.chat.server;

import ru.ifmo.java.chat.protocol.Protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;

public class SelectorWrite implements Runnable {
    public Selector selector;
    private Map<Channel, Queue<ByteBuffer>> buffs;
    private ConcurrentLinkedQueue<SocketChannel> forReg;
    public CyclicBarrier bar;

    public SelectorWrite() {
        try {
            selector = Selector.open();
            buffs = new ConcurrentHashMap<>();
            forReg = new ConcurrentLinkedQueue<>();
        } catch (IOException e) {
            System.out.println("Selector problem");
        }
    }

    public void add(SocketChannel client, Integer[] list, long sort, long start, long reqs, int need) {
        /*добавляем новый запрос на вывод, если клиент уже зарегистрирован,
        то в его очередь, а если нет, то рагистрируем и создаем под него новую очередь*/
        Protocol.SendSortResponse response = Protocol.SendSortResponse.newBuilder()
                .setCount(list.length).addAllList(Arrays.asList(list)).setReqs(reqs)
                .setSort(need * sort).setAll(need * (System.currentTimeMillis() - start)).build();

        //System.out.println("In add to write " + start + " " + System.currentTimeMillis());

        ByteBuffer bsize = ByteBuffer.allocate(4).putInt(response.getSerializedSize());
        bsize.flip();
        ByteBuffer barray = ByteBuffer.wrap(response.toByteArray());

        if (client.keyFor(selector) == null) {
            buffs.put(client, new ConcurrentLinkedQueue<>());
            buffs.get(client).add(bsize);
            buffs.get(client).add(barray);
            forReg.add(client);
            selector.wakeup();

        } else {
            buffs.get(client).add(bsize);
            buffs.get(client).add(barray);
            selector.wakeup();
        }
    }

    @Override
    public void run() {

        while (true) {
            try {
                while (!forReg.isEmpty()) {
                    SocketChannel client = forReg.poll();
                    client.register(selector, SelectionKey.OP_WRITE, client);
                }
            } catch (ClosedChannelException e) {
                System.out.println("Register in writer problem");
            }
            try {
                //System.out.println("try select write");
                selector.select();
                //System.out.println("select ok");
                Set<SelectionKey> channels = selector.selectedKeys();
                Iterator<SelectionKey> iterator = channels.iterator();
                //System.out.println("Channels for write" + channels.size());
                while (iterator.hasNext()) {
                    //System.out.println("write next");
                    SelectionKey key = iterator.next();
                    SocketChannel client = (SocketChannel) key.attachment();
                    if (key.isWritable()) {
                        if (buffs.get(client).isEmpty()) key.cancel();
                        else processClient(client);
                    }
                    iterator.remove();
                }
            } catch (IOException e) {
                System.out.println("While select IO problem");
            }
        }
    }

    private void processClient(SocketChannel client) throws IOException{
        //System.out.println("Process next write " + client);
        ByteBuffer next = buffs.get(client).element();
        //System.out.println(next);
        client.write(next);
        if (next.position() == next.limit()) {
            buffs.get(client).remove();
            //System.out.println("Write full");
        }
    }
}
