/*Это основной класс приложения, в котором происходит основная логика - запуск клиентов, получение от них результатов
* и отправелние их обратно GUI
* Сразу заметим, что запуск сервера здесь мы не осуществяем - так как возможно мы хотим подключиться к любому другому
* серверу. И адрес этого сервера будет прописан с классе Constants (по умолчанию - это lockalhost, если нужен какой-то
 * другой можно поменять его именно там, то же самое с портом)*/
package ru.ifmo.java.chat;
import ru.ifmo.java.chat.client.Client;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class App {
    public Map<Integer, Double[]> run(int x, int min, int max, int step, int n, int m, int delta, int ar, char param) throws IOException, InterruptedException {
        // заглушка для приложения (основной кусок в котором будет происходить вся логика отдельно от GUI)
        Map<Integer, Double[]> res = new HashMap<>();
        int count = (max - min) / step + 1;
        res.put(1, new Double[count]);
        res.put(2, new Double[count]);
        res.put(3, new Double[count]);

        double[] next = new double[3];

        switch (param) {
            case 'N':
                for (int ni = min; ni <= max; ni += step) {
                    next = session(x, delta, ni, m, ar);
                    res.get(1)[(ni - min) / step] = next[0];
                    res.get(2)[(ni - min) / step] = next[1];
                    res.get(3)[(ni - min) / step] = next[2];
                }
                break;
            case 'M' :
                for (int mi = min; mi <= max; mi += step) {
                    next = session(x, delta, n, mi, ar);
                    res.get(1)[(mi - min) / step] = next[0];
                    res.get(2)[(mi - min) / step] = next[1];
                    res.get(3)[(mi - min) / step] = next[2];
                }
                break;
            case 'D' :
                for (int di = min; di <= max; di += step) {
                    next = session(x, di, n, m, ar);
                    res.get(1)[(di - min) / step] = next[0];
                    res.get(2)[(di - min) / step] = next[1];
                    res.get(3)[(di - min) / step] = next[2];
                }
                break;
        }

        return res;
    }

    private double[] session(int x, int delta, int n, int m, int ar) throws IOException, InterruptedException{
        double[] means = new double[3];
        for (int i = 0; i < m; i++) {
            double[] next = new Client().run(x, delta, n, ar);
            for (int j = 0; j < 3; j++) means[j] += next[j];
        }
        for (int j = 0; j < 3; j++) means[j] /= m;
        return means;
    }
}
