package asl.analytics;

import au.com.bytecode.opencsv.CSVWriter;
import org.apache.commons.math3.distribution.TDistribution;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * This class parses the server, client and postgres logs and extract response time and compute throughput for different nodes.
 * It is not a part of system but it is included in case you need to verify results.
 */
public class log_parser {
    public static void main(String[] args) {
        String log_dir = "..\\..\\..\\log\\last_logs\\";
        if (args.length > 0)
            log_dir = args[0];
        String csv_out_dir = log_dir + "output\\";
        log_parser parser = new log_parser();

        parser.middleware_log_parser(log_dir + "m1_log\\server.log", csv_out_dir, "server1");
        parser.client_log_parser(log_dir + "c1_log\\clients.log", csv_out_dir, "client1");
        try {
            parser.middleware_log_parser(log_dir + "m2_log\\server.log", csv_out_dir, "server2");
            parser.client_log_parser(log_dir + "c2_log\\clients.log", csv_out_dir, "client2");

            parser.middleware_log_parser(log_dir + "m3_log\\server.log", csv_out_dir, "server3");
            parser.client_log_parser(log_dir + "c3_log\\clients.log", csv_out_dir, "client3");

            parser.middleware_log_parser(log_dir + "m4_log\\server.log", csv_out_dir, "server4");
            parser.client_log_parser(log_dir + "c4_log\\clients.log", csv_out_dir, "client4");
        } catch (Exception e) {
        }
    }

    public void middleware_log_parser(String log_path, String csv_out_dir, String prefix) {
        BufferedReader br = null;
        String line = "";
        DecimalFormat df = new DecimalFormat("#.0000");
        CSVWriter writer1 = null;
        CSVWriter writer2 = null;
        CSVWriter writer3 = null;
        CSVWriter writer4 = null;
        CSVWriter writer5 = null;
        CSVWriter writer6 = null;
        try {
            br = new BufferedReader(new FileReader(log_path));
            new File(csv_out_dir + prefix + "__DB-response_time.csv").getParentFile().mkdirs();
            writer1 = new CSVWriter(new FileWriter(csv_out_dir + prefix + "__DB-response_time.csv", false));
            writer4 = new CSVWriter(new FileWriter(csv_out_dir + prefix + "__DB_queue_time.csv", false));
            writer2 = new CSVWriter(new FileWriter(csv_out_dir + prefix + "__middleware_overall_time.csv", false));
            writer5 = new CSVWriter(new FileWriter(csv_out_dir + prefix + "__server_queue_time.csv", false));
            writer3 = new CSVWriter(new FileWriter(csv_out_dir + prefix + "__middleware_throughput.csv", false));
            writer6 = new CSVWriter(new FileWriter(csv_out_dir + prefix + "__middleware_socket_read_query.csv", false));

            int last_minute = -1;
            int last_second = -1;
            int minutes = 0;
            String[] record1 = new String[4];
            String[] record2 = new String[4];
            String[] record3 = new String[10];
            String[] record4 = new String[4];
            String[] record5 = new String[4];
            String[] record6 = new String[4];
            ArrayList<Integer> numbers1 = new ArrayList<Integer>();
            ArrayList<Integer> numbers2 = new ArrayList<Integer>();
            ArrayList<Integer> numbers4 = new ArrayList<Integer>();
            ArrayList<Integer> numbers5 = new ArrayList<Integer>();
            ArrayList<Integer> numbers6 = new ArrayList<Integer>();
            ArrayList<Integer> numbers_send = new ArrayList<Integer>();
            ArrayList<Integer> numbers_get = new ArrayList<Integer>();
            ArrayList<Integer> numbers_read = new ArrayList<Integer>();
            ArrayList<Integer> numbers_all = new ArrayList<Integer>();
            double duration1 = 0;
            double duration2 = 0;
            double duration4 = 0;
            double duration5 = 0;
            double duration6 = 0;
            int count1 = 0;
            int count2 = 0;
            int count4 = 0;
            int count5 = 0;
            int count6 = 0;
            double count_read = 0;
            double count_get = 0;
            double count_send = 0;

            int count_read_s = 0;
            int count_get_s = 0;
            int count_send_s = 0;
            boolean warm_up = true;
            while (true) {
                line = br.readLine();
                if (line == null) {
                    break;
                } else if (line.startsWith("INFO")) {
                    String[] parts = line.split("-:-");
                    long millis = Long.parseLong(parts[0].split(":")[1].trim());
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(millis);
                    int minute = c.get(Calendar.MINUTE);
                    int second = c.get(Calendar.SECOND);
                    if (last_minute == -1) {
                        last_minute = minute;
                        last_second = second;
                    }
                    String msg = parts[3];
                    if (minute != last_minute) {
                        last_minute = minute;
                        record1[0] = String.valueOf(minutes);
                        record2[0] = String.valueOf(minutes);
                        record3[0] = String.valueOf(minutes);
                        record4[0] = String.valueOf(minutes);
                        record5[0] = String.valueOf(minutes);
                        record6[0] = String.valueOf(minutes);

                        record1[1] = String.valueOf(df.format(duration1 / count1));
                        double sd1 = sd(numbers1, duration1 / count1);
                        record1[2] = String.valueOf(df.format(sd1));
                        record1[3] = String.valueOf(df.format(conf_interval(sd1, numbers1.size())));

                        record2[1] = String.valueOf(df.format(duration2 / count2));
                        double sd2 = sd(numbers2, duration2 / count2);
                        record2[2] = String.valueOf(df.format(sd2));
                        record2[3] = String.valueOf(df.format(conf_interval(sd2, numbers2.size())));

                        record3[1] = String.valueOf(df.format(count_send / 60));
                        record3[2] = String.valueOf(df.format(count_get / 60));
                        record3[3] = String.valueOf(df.format(count_read / 60));
                        record3[4] = String.valueOf(df.format(sd(numbers_send, count_send / 60)));
                        record3[5] = String.valueOf(df.format(sd(numbers_get, count_get / 60)));
                        record3[6] = String.valueOf(df.format(sd(numbers_read, count_read / 60)));
                        record3[7] = String.valueOf(df.format((count_send + count_get + count_read) / 60));
                        double sd3 = sd(numbers_all, (count_send + count_get + count_read) / 60);
                        record3[8] = String.valueOf(df.format(sd3));
                        record3[9] = String.valueOf(df.format(conf_interval(sd3, numbers_all.size())));

                        record4[1] = String.valueOf(df.format(duration4 / count4));
                        double sd4 = sd(numbers4, duration4 / count4);
                        record4[2] = String.valueOf(df.format(sd4));
                        record4[3] = String.valueOf(df.format(conf_interval(sd4, numbers4.size())));

                        record5[1] = String.valueOf(df.format(duration5 / count5));
                        double sd5 = sd(numbers5, duration5 / count5);
                        record5[2] = String.valueOf(df.format(sd5));
                        record5[3] = String.valueOf(df.format(conf_interval(sd5, numbers5.size())));

                        record6[1] = String.valueOf(df.format(duration6 / count6));
                        double sd6 = sd(numbers6, duration6 / count6);
                        record6[2] = String.valueOf(df.format(sd6));
                        record6[3] = String.valueOf(df.format(conf_interval(sd6, numbers6.size())));

                        duration1 = 0;
                        duration2 = 0;
                        duration4 = 0;
                        duration5 = 0;
                        duration6 = 0;
                        count1 = 0;
                        count2 = 0;
                        count4 = 0;
                        count5 = 0;
                        count6 = 0;
                        count_send = 0;
                        count_get = 0;
                        count_read = 0;
                        numbers1.clear();
                        numbers2.clear();
                        numbers4.clear();
                        numbers5.clear();
                        numbers6.clear();
                        numbers_get.clear();
                        numbers_send.clear();
                        numbers_read.clear();
                        numbers_all.clear();
                        if (warm_up) {
                            warm_up = false;
                            continue;
                        } else {
                            writer1.writeNext(record1);
                            writer2.writeNext(record2);
                            writer3.writeNext(record3);
                            writer4.writeNext(record4);
                            writer5.writeNext(record5);
                            writer6.writeNext(record6);
                            minutes++;
                        }
                    }
                    if (last_second != second) {
                        numbers_send.add(count_send_s);
                        numbers_get.add(count_get_s);
                        numbers_read.add(count_read_s);
                        numbers_all.add(count_send_s + count_get_s + count_read_s);
                        count_send_s = 0;
                        count_read_s = 0;
                        count_get_s = 0;
                        last_second = second;
                    }
                    if (msg.contains("DB response time")) {
                        int value = Integer.parseInt(msg.split(":")[1].trim());
                        duration1 += value;
                        numbers1.add(value);
                        count1++;
                    } else if (msg.contains("client wait time")) {
                        int value = Integer.parseInt(msg.split(":")[1].trim());
                        duration5 += value;
                        numbers5.add(value);
                        count5++;
                    } else if (msg.contains("DB connection wait time")) {
                        int value = Integer.parseInt(msg.split(":")[1].trim());
                        duration4 += value;
                        numbers4.add(value);
                        count4++;
                    } else if (msg.contains("query reading time for command")) {
                        int value = Integer.parseInt(msg.split(":")[1].trim());
                        duration6 += value;
                        numbers6.add(value);
                        count6++;
                    } else if (msg.contains("overall time for command") && (msg.contains("send msg") || msg.contains("get msg") || msg.contains("read msg"))) {
                        int value = Integer.parseInt(msg.split(":")[1].trim());
                        duration2 += value;
                        numbers2.add(value);
                        count2++;
                        if (msg.contains("send msg")) {
                            count_send++;
                            count_send_s++;
                        } else if (msg.contains("get msg")) {
                            count_get++;
                            count_get_s++;
                        } else if (msg.contains("read msg")) {
                            count_read++;
                            count_read_s++;
                        }
                    }

                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (writer1 != null)
                    writer1.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (writer2 != null)
                    writer2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (writer3 != null)
                    writer3.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (writer4 != null)
                    writer4.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (writer5 != null)
                    writer5.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (writer6 != null)
                    writer6.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static double sd(ArrayList<Integer> numbers, double mean) {
        double sd = 0;
        for (int number : numbers) {
            sd += Math.pow(number - mean, 2);
        }
        sd = sd / (numbers.size() - 1);
        sd = Math.sqrt(sd);
        return sd;
    }

    public static double conf_interval(double sd, int s_size) {
        if (s_size < 2)
            return 0;
        TDistribution tDist = new TDistribution(s_size - 1);
        double critVal = tDist.inverseCumulativeProbability(1.0 - (1 - 0.95) / 2);
        return critVal * sd / Math.sqrt(s_size);
    }

    public void client_log_parser(String log_path, String csv_out_dir, String prefix) {
        BufferedReader br = null;
        String line = "";
        DecimalFormat df = new DecimalFormat("#.000");
        CSVWriter writer1 = null;
        CSVWriter writer2 = null;
        try {
            br = new BufferedReader(new FileReader(log_path));
            writer1 = new CSVWriter(new FileWriter(csv_out_dir + prefix + "__server-response_time.csv", false));
            writer2 = new CSVWriter(new FileWriter(csv_out_dir + prefix + "__client_overall_time.csv", false));
            int last_minute = -1;
            int minutes = 0;
            String[] record1 = new String[4];
            String[] record2 = new String[4];
            double duration1 = 0;
            double duration2 = 0;
            int count1 = 0;
            int count2 = 0;
            ArrayList<Integer> numbers1 = new ArrayList<Integer>();
            ArrayList<Integer> numbers2 = new ArrayList<Integer>();
            boolean warm_up = true;
            while (true) {
                line = br.readLine();
                if (line == null) {
                    break;
                } else if (line.startsWith("INFO")) {
                    String[] parts = line.split("-:-");
                    long millis = Long.parseLong(parts[0].split(":")[1].trim());
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(millis);
                    int minute = c.get(Calendar.MINUTE);
                    if (last_minute == -1) {
                        last_minute = minute;
                    }
                    String msg = parts[3];
                    if (minute != last_minute) {
                        last_minute = minute;
                        record1[0] = String.valueOf(minutes);
                        record2[0] = String.valueOf(minutes);
                        record1[1] = String.valueOf(df.format(count1 == 0 ? 0 : duration1 / count1));
                        record2[1] = String.valueOf(df.format(count2 == 0 ? 0 : duration2 / count2));
                        record1[2] = String.valueOf(df.format(count1 == 0 ? 0 : sd(numbers1, duration1 / count1)));
                        record1[3] = String.valueOf(df.format(count1 == 0 ? 0 : conf_interval(sd(numbers1, duration1 / count1), numbers1.size())));
                        record2[2] = String.valueOf(df.format(count2 == 0 ? 0 : sd(numbers2, duration2 / count2)));
                        record2[3] = String.valueOf(df.format(count2 == 0 ? 0 : conf_interval(sd(numbers2, duration2 / count2), numbers2.size())));
                        duration1 = 0;
                        duration2 = 0;
                        count1 = 0;
                        count2 = 0;
                        numbers1.clear();
                        numbers2.clear();
                        if (warm_up) {
                            warm_up = false;
                            continue;
                        } else {
                            writer1.writeNext(record1);
                            writer2.writeNext(record2);
                            minutes++;
                        }
                    }
                    if (msg.contains("server response time")) {
                        String[] parts_t = msg.split(";");
                        int value = Integer.parseInt(parts_t[parts_t.length - 1].split(":")[1].trim());
                        duration1 += value;
                        numbers1.add(value);
                        count1++;
                    } else if (msg.contains("duration")) {
                        int value = Integer.parseInt(msg.split(":")[1].trim());
                        duration2 += value;
                        numbers2.add(value);
                        count2++;
                    }

                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (writer1 != null)
                    writer1.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (writer2 != null)
                    writer2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void postgres_log_parser(String csvFile) {
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = "!";
        try {

            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {

                String[] parts = line.split(cvsSplitBy);
                if (line.contains("duration")) {
                    for (String part : parts) {
                        System.out.print(part + " #### ");
                    }
                    System.out.println("\n*********");
                    Thread.sleep(100);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
