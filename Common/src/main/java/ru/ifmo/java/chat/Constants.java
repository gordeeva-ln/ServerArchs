package ru.ifmo.java.chat;

public class Constants {
    public static final int PORT_1 = 8080;
    public static final String HOST_1 = "localhost";
    public static final int PORT_2 = 8081;
    public static final String HOST_2 = "localhost";
    public static final int PORT_3 = 8081;
    public static final String HOST_3 = "localhost";

    public static final int POOL_SIZE = 10;

    private Constants() {}

    public static Integer[] mySort(Integer[] list) {
        int n = list.length;
        for (int i = 0; i < n; i++) {
            int m = i;
            for (int j = i + 1; j < n; j++) {
                if(list[j] < list[m]) {
                    m = j;
                }

            }
            int tmp = list[m];
            list[m] = list[i];
            list[i] = tmp;

        }
        return list;
    }
}
