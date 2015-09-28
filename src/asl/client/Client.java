package asl.client;

import asl.db.Message;
import asl.utils.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static asl.utils.Constants.*;

public class Client implements Runnable {

    public static final Logger lgr = Logger.getLogger(Client.class.getName());
    public static FileHandler handler = null;
    protected Integer ID = -1;
    private Socket serverSocket;
    private PrintWriter out;
    private BufferedReader in;
    private long run_duration;
    private int client_id;
    private int log_id;
    private List<Integer> receivers = null;

    public Client(InetAddress server_host, int server_port,
                  long timeRunning, int client_id, List<Integer> receivers) {
        try {
            this.client_id = client_id;
            this.receivers = receivers;
            this.log_id = 0;
            this.serverSocket = new Socket(server_host, server_port);
            this.out = new PrintWriter(this.serverSocket.getOutputStream(),
                    true);
            this.in = new BufferedReader(new InputStreamReader(
                    this.serverSocket.getInputStream()));
            this.run_duration = System.currentTimeMillis() + timeRunning;
            try {
                if (handler == null) {
                    handler = new FileHandler(log_dir + "clients"
                            + ".log", true);
                    handler.setFormatter(new Formatter() {
                        public String format(LogRecord record) {
                            return record.getLevel() + "  :  "
                                    + record.getMillis() + " -:- "
                                    + record.getSourceClassName() + " -:- "
                                    + record.getSourceMethodName() + " -:- "
                                    + record.getMessage() + "\n";
                        }
                    });
                    lgr.addHandler(handler);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        long timeRunning = 20 * 60 * 1000;
        int numClients = client_counts;
        int offset = 0;
        String client_type = "client";
        ArrayList<String> server_hosts = new ArrayList<String>();
        String server_hosts_str = "localhost";
        int server_port = 3456;
        if (args.length > 0)
            numClients = Integer.parseInt(args[0]);
        if (args.length > 1)
            offset = Integer.parseInt(args[1]);
        if (args.length > 2) {
            timeRunning = Long.parseLong(args[2]) * 60 * 1000;
        }
        if (args.length > 3) {
            server_hosts_str = args[3];
        }
        if (args.length > 4) {
            client_type = args[4];
        }
        Collections.addAll(server_hosts, server_hosts_str.split(","));
        try {
            List<Integer> receivers = new ArrayList<Integer>();
            for (int i = 0; i < numClients; i++) {
                int client_id = offset + i + 1;
                receivers.add(client_id);
            }
//            receivers.add(-1); // For now we don't send broadcast messages
            for (int i = 0; i < numClients; i++) {
                int client_id = offset + i + 1;
                Client client = new Client(InetAddress.getByName(server_hosts.get(i % server_hosts.size())),
                        server_port, timeRunning, client_id, receivers);
                Thread clientThread = new Thread(client, client_type + " " + client_id);
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        Random ran = new Random();
        try {
            if (this.ID == -1)
                this.ID = register(this.client_id);
            lgr.log(INFO, Thread.currentThread().getName() + " started.");
            long last_lists_update_time = 0;
            List<Integer> queues = null;
            Thread.sleep(3); // waiting for all clients to register.
            while (System.currentTimeMillis() < run_duration) {
                try {
                    if (!Thread.currentThread().getName().contains("consumer_client")) {
//                        if (ran.nextFloat() < 0.0000f) { // according to this probability, we never create a new queue or get queue list
//                            createQueue();
//                            if (System.currentTimeMillis() - last_lists_update_time > list_update_interval) {

//                            receivers = List_Clients();
//                            if (receivers != null && !receivers.isEmpty()) {
//                                receivers.remove(this.ID);
//                                receivers.add(-1);
//                            }
//                                queues = List_Queues();
//                                last_lists_update_time = System.currentTimeMillis();
//                            }
//                        }
                        if (ran.nextFloat() <= 1.0f) {// according to this probability, we always send messages
                            long before_send_time = System.currentTimeMillis();
                            int receiverID, queueID;
//                            if (queues != null && !queues.isEmpty()) {
//                                queueID = queues
//                                        .get(ran.nextInt(queues.size()));
//                                if (ran.nextFloat() < 0.0000f) { // according to this probability, we never remove a queue
//                                    if (deleteQueue(queueID)) {
//                                        queues.remove(queueID);
//                                        continue;
//                                    }
//                                }
//                            } else {
//                                createQueue();
//                                continue;
//                            }
                            if (receivers != null && !receivers.isEmpty()) {
                                receiverID = receivers.get(receivers.size() - receivers.indexOf(this.client_id) - 1);
//                                lgr.log(INFO, this.client_id + " recieverID is: " + receiverID);
                            } else {
                                continue;
                            }
                            Boolean res;
                            boolean fake_sending = false;
                            if (fake_sending) {
                                res = fake_sendMessage(receiverID, receiverID);
                            } else {
                                res = sendMessage(receiverID, receiverID);  // I decided to send messages for each client to only its dedicated queue which we assume for it.
                            }
                            if (res) {
                                long after_send_time = System.currentTimeMillis();
                                lgr.log(INFO, Thread.currentThread().getName()
                                        + ", sending message duration: " + (after_send_time - before_send_time));
                            }
                        }
                    }
                    if (Thread.currentThread().getName().contains("populate_client"))
                        continue;

                    Thread.sleep(15);
//                    long before_get_pending_queues_time = System.currentTimeMillis();
//                    List<Integer> pending_queues = getPendingQueues();
//                    long after_get_pending_queues_time = System.currentTimeMillis();
//                    lgr.log(INFO, Thread.currentThread().getName()
//                            + ", pending_queues duration: " + (after_get_pending_queues_time - before_get_pending_queues_time));

                    Message msg = null;
                    int queueID;
                    long before_receive_time = System.currentTimeMillis();
                    int operation = 1; //mean getMsg

//                    if (pending_queues != null && !pending_queues.isEmpty()) {
//                        queueID = pending_queues
//                                .get(ran.nextInt(pending_queues.size()));
//                        if (ran.nextFloat() >= 0.0f) // according to this probability, we always get (and remove)  messages
//                            msg = getMessage(queueID);
//                        else {
//                            msg = readMessage(queueID);
//                            operation = 0; //means readMsg
//                        }
//                    }
                    msg = getMessage(this.client_id);
                    if (msg != null) {
                        long after_receive_time = System.currentTimeMillis();
//                        lgr.log(INFO, Thread.currentThread().getName() + " received a message");
                        lgr.log(INFO, Thread.currentThread().getName()
                                + ", " + (operation == 1 ? "get" : "read") + " message duration: " + (after_receive_time - before_receive_time));
                    }
                    Thread.sleep(15);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            un_register();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                this.serverSocket.close();
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
    }

    public Integer register(int id) throws IOException {
        /*
        Register this client in server and return client_id in case of success, null otherwise.
         */
        out.println("register");
        out.println("id:" + id);
        out.flush();
        String status = in.readLine();
        if (!OK.equals(status)) {
            return null;
        } else {
            HashMap<String, String> query = Parser
                    .stringToMap(in.readLine());
            return Integer.valueOf(query.get("id"));
        }
    }

    public void un_register() throws IOException {
        /*
        un-register client from server. and close the socket. Attention: it will not remove the client from database.
         */
        try {
            do {
                out.println("un_register");
                out.flush();
            } while (!(in.readLine()).equals(OK));

            this.out.close();
            this.in.close();
            this.serverSocket.close();
        } catch (Exception e) {
        }
    }

    public Integer createQueue() throws IOException {
        /*
        Create a queue in the database and returns queue_id
         */

        out.println("create queue");
        out.flush();
        String status = in.readLine();

        if (!OK.equals(status)) {
            lgr.log(WARNING, "Creating queue failed.");
            return null;

        } else {
            HashMap<String, String> query = Parser
                    .stringToMap(in.readLine());
            return Integer.valueOf(query.get("id"));

        }
    }

    public boolean deleteQueue(int queueID) throws IOException {

        out.println("delete queue");
        out.println("id:" + queueID);
        out.flush();
        String state = in.readLine();
        if (!OK.equals(state)) {
            lgr.log(WARNING, "Deleting queue failed.");
            return false;
        }
        return true;
    }

    public boolean fake_sendMessage(Integer receiverID, Integer queueID) throws IOException {
        /*
        Only used for testing how many messages an isolated client can send per second.
         */
        if (receiverID == -1) {
            receiverID = null;
        }
        Message msg = new Message(this.ID, receiverID, queueID);
        String msg_str = Parser.messageToString(msg);
        long before_time = System.currentTimeMillis();
        out.println("send msg");
        out.println(msg_str);
        out.flush();
        long after_time = System.currentTimeMillis();
        long resp_time = after_time - before_time;
        lgr.log(INFO, "sendMessage server response time: " + resp_time);
        return true;
    }

    public boolean sendMessage(Integer receiverID, Integer queueID) throws IOException {
        /*
        send a message to receiver_id in queue_id.
         */
        if (receiverID == -1) {
            receiverID = null;
        }
        Message msg = new Message(this.ID, receiverID, queueID);
        String msg_str = Parser.messageToString(msg);
        long before_time = System.currentTimeMillis();
        out.println("send msg\n" + msg_str);
//        out.println(msg_str);
        out.flush();
        long after_write_time = System.currentTimeMillis();
        String state = in.readLine();
        long after_time = System.currentTimeMillis();
        if (!OK.equals(state)) {
            lgr.log(WARNING, "sendMessage failed because: " + state);
            return false;
        } else {
            long resp_time = after_time - before_time;
            lgr.log(INFO, "sendMessage succeed.");
            lgr.log(INFO, "sendMessage client socket write time: " + (after_write_time - before_time));
            lgr.log(INFO, "sendMessage server response time: " + resp_time);
//            lgr.log(INFO, "log_id= " + this.log_id++ + "# sendMessage client socket write timestamp: " + (before_time));
            return true;
        }
    }


    public Message getMessage(Integer queueID) throws IOException {
        /*
        returns the top most message which this client has access to, from queue_id and delete it from that queue.
         */

        String queryString = "queue:" + queueID;
        queryString += ";receiver:" + this.ID;
        long before_time = System.currentTimeMillis();
        out.println("get msg\n" + queryString);
//        out.println(queryString);
        out.flush();
        long after_write_time = System.currentTimeMillis();
        String state = in.readLine();
        if (!OK.equals(state)) {
            lgr.log(ERROR, "getMessage failed because: " + state);
        } else {
            String res_str = in.readLine();
            long after_time = System.currentTimeMillis();
            HashMap<String, String> query = Parser
                    .stringToMap(res_str);
            lgr.log(INFO, "id: " + query.get("id") + "; getMessage client socket write time: " + (after_write_time - before_time));
            lgr.log(INFO, "id: " + query.get("id") + "; getMessage server response time: " + (after_time - before_time));
//            lgr.log(INFO, "log_id= " + this.log_id++ + "# getMessage client socket write timestamp: " + (before_time));
            return Parser.mapToMessage(query);
        }
        return null;
    }

    public Message readMessage(Integer queueID) throws IOException {
        /*
        returns the top most message which this client has access to, from queue_id.
         */
        String queryString = "queue:" + queueID;
        queryString += ";receiver:" + this.ID;
        long before_time = System.currentTimeMillis();
        out.println("read msg");
        out.println(queryString);
        out.flush();
        long after_write_time = System.currentTimeMillis();
        String state = in.readLine();
        long after_time = System.currentTimeMillis();
        if (!OK.equals(state)) {
            lgr.log(WARNING, "reading message failed because: " + state);
        } else {
            lgr.log(INFO, "reading message succeed.");
            HashMap<String, String> query = Parser
                    .stringToMap(in.readLine());
            lgr.log(INFO, "id: " + query.get("id") + "; readMessage server response time: " + (after_time - before_time));
            lgr.log(INFO, "id: " + query.get("id") + "; readMessage client socket write time: " + (after_write_time - before_time));
            return Parser.mapToMessage(query);

        }
        return null;
    }

    public List<Integer> List_Clients() throws IOException {
        /*
        returns list of clients currently in database. Then client can send a message to one of them.
         */
        out.println("list users");
        out.flush();
        String status = in.readLine();
        if (OK.equals(status)) {
            HashMap<String, String> query = Parser
                    .stringToMap(in.readLine());
            return Parser.stringToList(query.get("users"));
        }
        return null;
    }

    public List<Integer> getPendingQueues() throws IOException {
        /*
        returns list of queues which this client have pending message in. Then client can issue a get/read command for one of them.
         */
        long before_time = System.currentTimeMillis();
        out.println("list queues");
        out.println("receiver:" + this.ID);
        out.flush();
        String status = in.readLine();
        if (!OK.equals(status)) {
            return null;
        } else {
            long after_time = System.currentTimeMillis();
            HashMap<String, String> query = Parser
                    .stringToMap(in.readLine());
            lgr.log(INFO, "pending_queues server response time: " + (after_time - before_time));
            return Parser.stringToList(query.get("queues"));
        }
    }

    public List<Integer> List_Queues() throws IOException {
        /*
        returns list of queues currently in database. Then client can send a message to one of them.
         */

        out.println("list all queues");
        out.flush();
        String status = in.readLine();
        if (!OK.equals(status)) {
            return null;

        } else {
            HashMap<String, String> query = Parser
                    .stringToMap(in.readLine());
            return Parser.stringToList(query.get("queues"));
        }
    }
}
