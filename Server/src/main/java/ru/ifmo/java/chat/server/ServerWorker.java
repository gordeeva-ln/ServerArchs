package ru.ifmo.java.chat.server;

import ru.ifmo.java.chat.Constants;
import ru.ifmo.java.chat.protocol.Protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

public class ServerWorker implements Runnable {
    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;
    private long timeForSort, timeAll;

    public ServerWorker(Socket socket) throws IOException {
        this.socket = socket;
        input = socket.getInputStream();
        output = socket.getOutputStream();
    }

    @Override
    public void run() {
        try {
            while (true) {
                long start = System.currentTimeMillis();
                Protocol.SortRequest request = receiveRequest();
                if (request.hasSendSortRequest()) {
                    processSendSort(request.getSendSortRequest(), start);
                } else if (request.hasDead()) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
                .addAllList(Arrays.asList(sorted)).setSort(finish - startSort)
                .setAll(System.currentTimeMillis() - start)
                .build().writeDelimitedTo(output);
    }
}
