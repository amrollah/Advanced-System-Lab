package asl.client;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class my_test {
    private static ArrayList<Integer> numbers = new ArrayList<Integer>();

    public static void main(String[] args) {
        for (int i=0;i<100;i++) {
            numbers.add(10);
        }
        System.out.println(sd(numbers, 10));
    }
    public static double sd(ArrayList<Integer> numbers, int mean) {
        double sd = 0;
        for (int number : numbers) {
            sd += Math.pow(number - mean, 2);
        }
        sd = sd / numbers.size();
        sd = Math.sqrt(sd);
        return (int) sd;
    }
}
