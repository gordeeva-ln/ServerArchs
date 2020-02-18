package ru.ifmo.java.chat.client;

import ru.ifmo.java.chat.Constants;
import ru.ifmo.java.chat.protocol.Protocol;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Client {
    private Socket socket = null;
    private OutputStream output;
    private InputStream input;
    private Short port = 0;
    private String host = "";
    private final Random random = new Random();

    public double[] run(int x, int delta, int n, int ar) throws IOException, InterruptedException {
        /* Клиент первм делом начинает свою работу с сервром и созадет подключение с теми настройками,
         которые прописаны в Constants*/
        long start = System.currentTimeMillis();
        switch (ar) {
            case 1:
                port = Constants.PORT_1;
                host = Constants.HOST_1;
                break;
            case 2:
                port = Constants.PORT_2;
                host = Constants.HOST_2;
                break;
            case 3:
                port = Constants.PORT_3;
                host = Constants.HOST_3;
                break;
        }

        socket = new Socket(host, port);
        output = socket.getOutputStream();
        input = socket.getInputStream();
        long timeSort = 0, timeAll = 0;
        try {
            /*отсылает х запросов с промежутком в delta*/
            for (int i = 0; i < x; i++) {
                List result = processSort(n, ar);
                timeSort += (long) result.get(n);
                timeAll += (long) result.get(n + 1);
                TimeUnit.MILLISECONDS.sleep(delta);
            }
        } finally {
            socket.close();
        }
        long finish = System.currentTimeMillis();
        long timeConsumedMillis = finish - start;
        double[] stat = new double[3];
        stat[0] = (double) timeSort / x;
        stat[1] = (double) timeAll / x;
        stat[2] = (double) timeConsumedMillis / x;

        return stat;
    }

    private List<Object> resutFromResponse(Protocol.SendSortResponse response) {
        List<Object> result = new ArrayList<>();
        for (int i : response.getListList()) {
            result.add(i);
        }

        result.add(response.getSort());
        result.add(response.getAll());
        return result;
    }
    private List<Object> processSort(int n, int ar) throws IOException{
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(random.nextInt());
        }
        Protocol.SortRequest request = Protocol.SortRequest.newBuilder()
                .setSendSortRequest(Protocol.SendSortRequest.newBuilder()
                        .setCount(n).addAllList(list)).build();

        // работа с неблокирующим сервером реализуется иначе, чем для блокирующих
        // поэтому, чтобы не сломать реализации для предыдущих я сделала другую реализацию
        if (ar == 3) sendRequest3(request);
        else sendRequest(request);

        if (ar == 3) return receiveSendSortResponse3();
        return receiveSendSortResponse();
    }

    private List<Object> receiveSendSortResponse() throws IOException{
        Protocol.SendSortResponse response;
        response = Protocol.SendSortResponse.parseDelimitedFrom(socket.getInputStream());
        return resutFromResponse(response);
    }

    private List<Object> receiveSendSortResponse3() throws IOException{
        DataInputStream data = new DataInputStream(input);
        int size3 = data.readInt();
        byte[] array = new byte[size3];
        data.readFully(array);
        Protocol.SendSortResponse response = Protocol.SendSortResponse.parseFrom(array);
        return resutFromResponse(response);
    }

    private void sendRequest(Protocol.SortRequest request) throws IOException {
        request.writeDelimitedTo(socket.getOutputStream());
    }
    private void sendRequest3(Protocol.SortRequest request) throws IOException {
        // сначала пишем размер сообщения, а потом уже его отправляем
        byte[] sizeBytes =  ByteBuffer.allocate(4).putInt(request.getSerializedSize()).array();
        output.write(sizeBytes);
        request.writeTo(output);
        output.flush();
    }
}
