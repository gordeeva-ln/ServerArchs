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

public class Client implements Runnable{
    private Socket socket = null;
    private OutputStream output;
    private InputStream input;
    private Short port = 0;
    private String host = "";
    private final Random random = new Random();
    public double[] stat;
    private int x, delta, m, n, ar;

    public Client(int x, int delta, int m, int n, int ar) throws IOException{
        /* при инициализации клиента он получает постоянное соединение, но пока не отсылает запросы*/
        this.x = x;
        this.delta = delta;
        this.m = m;
        this.n = n;
        this.ar = ar;
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

        /*так как сервер ничего не знает о клиентах и приложениях, то при инициализации кадого клиента отправлеям
         какое количество клиентов должен ждать сервер, чтобы начать их обработку*/
        Protocol.SortRequest request = Protocol.SortRequest.newBuilder()
                .setSendCountRequest(Protocol.SendCountRequest.newBuilder()
                        .setClientCount(m).build()).build();
        if (ar == 3) sendRequest3(request);
        else sendRequest(request);
    }
    @Override
    public void run() {
        /* Это основной код клиента, у него уже есть сокет, через который он общается с сервером и умирает*/
        long start = System.currentTimeMillis();
        long timeSort = 0, timeAll = 0, xx = x;
        try {
            /*отсылает х запросов с промежутком в delta*/
            for (int i = 0; i < x; i++) {
                //System.out.println("prosess sort " + i);
                List result = processSort(n, ar);
                //System.out.println("prosess sort " + i + this);
                timeSort += (long) result.get(n);
                timeAll += (long) result.get(n + 1);
                xx = (long)result.get(n + 2); // каждый раз обновляется, зато последний вариант - истиный
                //System.out.println("All" + timeAll + " " + timeSort + " " + xx);

                TimeUnit.MILLISECONDS.sleep(delta);
            }

            //System.out.println("All" + this);


        } catch (IOException e) {
            System.out.println("Prosess error in client " + e.getMessage());
        }catch (InterruptedException ex) {
            System.out.println("While sleep exception " + ex.getMessage());
        } finally {
            try {
                socket.close();
                input.close();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        long finish = System.currentTimeMillis();
        long timeConsumedMillis = finish - start;
        stat = new double[3];
        stat[0] = (double) timeSort / xx;
        stat[1] = (double) timeAll / xx;
        stat[2] = (double) timeConsumedMillis / xx;
        //System.out.println("Rs" + timeAll + " " + xx);
        //System.out.println("Stat ready " + stat);
        //return stat;
    }

    private List<Object> resutFromResponse(Protocol.SendSortResponse response) {
        List<Object> result = new ArrayList<>();
        for (int i : response.getListList()) {
            result.add(i);
        }

        result.add(response.getSort());
        result.add(response.getAll());
        result.add(response.getReqs());
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

        //System.out.println("Send req ok");
        if (ar == 3) return receiveSendSortResponse3();
        return receiveSendSortResponse();
    }

    private List<Object> receiveSendSortResponse() throws IOException{
        Protocol.SendSortResponse response;
        response = Protocol.SendSortResponse.parseDelimitedFrom(socket.getInputStream());
        return resutFromResponse(response);
    }

    private List<Object> receiveSendSortResponse3() throws IOException{
        //System.out.println("try receive");
        DataInputStream data = new DataInputStream(input);
        int size3 = data.readInt();
        //System.out.println("int receive " + size3);
        byte[] array = new byte[size3];
        data.readFully(array);
        //System.out.println("receive ok");
        Protocol.SendSortResponse response = Protocol.SendSortResponse.parseFrom(array);
        return resutFromResponse(response);
    }

    private void sendRequest(Protocol.SortRequest request) throws IOException {
        request.writeDelimitedTo(socket.getOutputStream());
    }
    private void sendRequest3(Protocol.SortRequest request) throws IOException {
        // сначала пишем размер сообщения, а потом уже его отправляем
        DataOutputStream data = new DataOutputStream(output);
        byte[] sizeBytes =  ByteBuffer.allocate(4).putInt(request.getSerializedSize()).array();
        data.write(sizeBytes);
        request.writeTo(output);
        data.flush();
    }
}
