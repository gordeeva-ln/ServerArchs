package ru.ifmo.java.chat.server;

import ru.ifmo.java.chat.Constants;
import ru.ifmo.java.chat.protocol.Protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;

public class ServerWorker2 implements Runnable {
    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;
    private long timeForSort, timeAll;
    private final ExecutorService pool;
    private final ExecutorService outThread;
    private CyclicBarrier bar;
    private Integer need;
    private int reqCount;

    public ServerWorker2(Socket socket, ExecutorService pool, ExecutorService outThread, Server2 server) throws IOException {
        this.socket = socket;
        input = socket.getInputStream();
        output = socket.getOutputStream();
        this.pool = pool;
        this.outThread = outThread;
        this.bar = server.BARRIER;
        need = server.need;
        reqCount = 0;
    }

    @Override
    public void run() {
        try {bar.await();
        } catch (Exception e) { System.out.println("probs");}
        try {
            while (true) {
                long start = System.currentTimeMillis();
                Protocol.SortRequest request = receiveRequest();
                if (request.hasSendSortRequest()) {
                    reqCount += need;
                    processSendSort(request.getSendSortRequest(), start);

                } else if (request.hasDead()) {
                    break;
                }
            }
        } catch (Exception e) {
            need = 0;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Protocol.SortRequest receiveRequest() throws IOException {
        return Protocol.SortRequest.parseDelimitedFrom(input);
    }

    private void processSendSort(Protocol.SendSortRequest request, long start) {
        Integer[] list = request.getListList().stream().toArray(Integer[]::new);
        pool.submit(new Task(list, request, start));
    }

    class Task implements Runnable {
        private Integer[] list;
        Protocol.SendSortRequest request;
        long start;
        private Task(Integer[] list, Protocol.SendSortRequest request, long start) {
            this.list = list;
            this.request = request;
            this.start = start;
        }
        @Override
        public void run() {
            long startSort = System.currentTimeMillis();
            Integer[] res = Constants.mySort(list);
            long finish = System.currentTimeMillis();
            outThread.submit(new TaskOut(res, request, finish - startSort, start));
        }
    }

    class TaskOut implements Runnable {
        private Integer[] res;
        long sortTime, start;
        Protocol.SendSortRequest request;
        public TaskOut(Integer[] list, Protocol.SendSortRequest request, long sortTime, long start) {
            this.res = list;
            this.request = request;
            this.sortTime = sortTime;
            this.start = start;
        }
        @Override
        public void run() {
            try {
                Protocol.SendSortResponse.newBuilder().setCount(request.getCount())
                        .addAllList(Arrays.asList(res)).setSort(need * sortTime)
                        .setAll(need * (System.currentTimeMillis() - start)).setReqs(reqCount)
                        .build().writeDelimitedTo(output);
            } catch (IOException e) {
                System.out.println("Write socket problem " + e.getMessage());
            }

        }
    }
}
