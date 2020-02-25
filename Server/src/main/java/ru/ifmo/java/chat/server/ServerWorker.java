package ru.ifmo.java.chat.server;

import ru.ifmo.java.chat.Constants;
import ru.ifmo.java.chat.protocol.Protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ServerWorker implements Runnable {
    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;
    private long timeForSort, timeAll;
    private CyclicBarrier bar;
    private Integer need;
    int reqCount;

    public ServerWorker(Socket socket, Server server) throws IOException, InterruptedException, BrokenBarrierException {
        this.socket = socket;
        input = socket.getInputStream();
        output = socket.getOutputStream();
        this.bar = server.BARRIER;
        reqCount = 0;
        this.need = server.needToStoreStat;
    }

    @Override
    public void run() {
        try {bar.await();
        } catch (Exception e) { System.out.println("probs");}

        try {
            while (!socket.isClosed()) {
                long start = System.currentTimeMillis();
                Protocol.SortRequest request = receiveRequest();
                //System.out.println("REc ok");
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

    private void processSendSort(Protocol.SendSortRequest request, long start) throws IOException{
        Integer[] list = request.getListList().stream().toArray(Integer[]::new);
        long startSort = System.currentTimeMillis();
        Integer[] sorted = Constants.mySort(list);
        long finish = System.currentTimeMillis();

        Protocol.SendSortResponse.newBuilder().setCount(request.getCount())
                .addAllList(Arrays.asList(sorted)).setSort(need * (finish - startSort))
                .setAll(need *(System.currentTimeMillis() - start)).setReqs(reqCount)
                .build().writeDelimitedTo(output);
    }
}
