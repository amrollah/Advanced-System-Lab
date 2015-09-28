package asl.server;

import asl.db.Prepared_Query;
import asl.db.Query;
import asl.utils.Parser;
import org.postgresql.ds.PGPoolingDataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.FileHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static asl.utils.Constants.*;

class client_holder {
    Socket socket;
    Integer client_id;
    long arrival_time;

    client_holder(Socket socket, Integer client_id) {
        this.socket = socket;
        this.client_id = client_id;
        this.arrival_time = System.currentTimeMillis();
    }
}

public class Server implements Runnable {
    private final static Logger lgr = Logger.getLogger(Server.class.getName());
    private static volatile boolean server_running = true;
    private static Thread ServerThread;
    private static FileHandler handler = null;
    protected LinkedBlockingQueue<client_holder> clients;
    private int client_id = -1;
    private Prepared_Query read_msg_prepared_st1 = null;
    private Prepared_Query read_msg_prepared_st2 = null;
    private Prepared_Query get_msg_prepared_st1 = null;
    private Prepared_Query get_msg_prepared_st2 = null;
    private Prepared_Query queue_exist = null;
    private int log_id;

    private Prepared_Query sendMsg_pst = null;
    private PGPoolingDataSource dbpool;

    public Server(PGPoolingDataSource db_pool, LinkedBlockingQueue<client_holder> clients) {
        this.dbpool = db_pool;
        this.clients = clients;
        this.log_id = -1;
        try {
            if (handler == null) {
                handler = new FileHandler(log_dir + "server.log", false);
                handler.setFormatter(new java.util.logging.Formatter() {
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
        try {
            this.read_msg_prepared_st1 = new Prepared_Query("SELECT * FROM Read_Message_By_Queue(?,?)");
            this.read_msg_prepared_st2 = new Prepared_Query("SELECT * FROM Read_Message_By_Sender(?,?)");

            this.get_msg_prepared_st1 = new Prepared_Query("SELECT * FROM Get_Message_By_Queue(?,?)");
            this.get_msg_prepared_st2 = new Prepared_Query("SELECT * FROM Get_Message_By_Sender(?,?)");

            this.sendMsg_pst = new Prepared_Query("SELECT Insert_Message(?,?,?,?)");
            this.queue_exist = new Prepared_Query("SELECT Queue_Exist(?)");


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            lgr.log(ERROR, "JDBC Driver not found!");
            return;
        }
        String db_host = "localhost";
        String db_username = db_user;
        String db_pass = db_password;
        int db_port = 5432;
        int workers = coreThreadPoolSize;
        int con_poll_size = db_con_pool_size;
        int ConcurrentClients = 0;
        final Logger mainLogger = Logger.getLogger("ServerLogger");
        int server_port = 3456;

        if (args.length > 0)
            server_port = Integer.parseInt(args[0]);
        if (args.length > 1)
            db_host = args[1];
        if (args.length > 2)
            db_port = Integer.parseInt(args[2]);
        if (args.length > 3)
            db_username = args[3];
        if (args.length > 4)
            db_pass = args[4];
        if (args.length > 5) {
            workers = Integer.parseInt(args[5]);
            lgr.log(INFO, "worker count: " + workers);
        }


        ServerThread = Thread.currentThread();
        final ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(server_port);
        } catch (IOException e) {
            e.printStackTrace();
            lgr.log(ERROR, "Could not open port " + server_port + "\n"
                    + e.getMessage());
            System.exit(-1);
            return;
        }
        final PGPoolingDataSource db_pool = new PGPoolingDataSource();
        final LinkedBlockingQueue<client_holder> clients = new LinkedBlockingQueue<client_holder>(ClientsQueue_limit);
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        // creating the ThreadPoolExecutor
        final ThreadPoolExecutor executorPool = new ThreadPoolExecutor(workers,
                maxThreadPoolSize, keepAlive_thread_pool, TimeUnit.MINUTES,
                new ArrayBlockingQueue<Runnable>(workers, true),
                threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
        Socket clientSocket;

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                Server.ServerThread.interrupt();
                server_running = false;
                mainLogger.log(INFO, "Shutdown messaging server.");
                try {
                    db_pool.close();
                    for (client_holder client : clients) {
                        try {
                            client.socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    executorPool.shutdown();
                    serverSocket.close();
                } catch (IOException e) {
                    mainLogger.log(ERROR, e.getMessage(), e);
                }
            }
        });

        // Create DB connection pool
        db_pool.setDataSourceName("db_pool");
        db_pool.setServerName(db_host);
        db_pool.setPortNumber(db_port);
        db_pool.setDatabaseName(db_name);
        db_pool.setUser(db_username);
        db_pool.setPassword(db_pass);
        db_pool.setMaxConnections(con_poll_size);
        db_pool.setTcpKeepAlive(true);

        for (int i = 0; i < workers; i++) {
            /*
            Creates a worker for handling clients.
             */
            executorPool.execute(new Server(db_pool, clients));
        }
        while (server_running) {
            try {
                if (clients.size() >= ClientsQueue_limit)
                    continue;
                clientSocket = serverSocket.accept();
                ConcurrentClients++;
                mainLogger.log(INFO, "Accepted connection from client: "
                        + clientSocket.getInetAddress());
                clients.put(new client_holder(clientSocket, -1 * ConcurrentClients));
            } catch (IOException e) {
                mainLogger.log(ERROR, e.getMessage(), e);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mainLogger.log(INFO, "Accepted " + ConcurrentClients
                + " clients.");

        try {
            db_pool.close();
            for (client_holder client : clients) {
                try {
                    client.socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            executorPool.shutdown();
            serverSocket.close();
        } catch (IOException e) {
            mainLogger.log(ERROR, e.getMessage(), e);
        }

    }

    public String get_client_list(LinkedBlockingQueue<client_holder> clients) {
        String client_ids = "";
        for (client_holder client : clients) {
            client_ids += client.client_id + ", ";
        }
        return client_ids;
    }

    @Override
    public void run() {
        try {
            while (true) {
                client_holder client = null;
                if (this.clients.size() == 0) {
                    continue;
                }
//                else {
//                    lgr.log(INFO, Thread.currentThread().getName() + " -- clients: " + get_client_list(clients));
//                }
                long before_exe_time = System.currentTimeMillis();
                try {
                    client = this.clients.take();
                    lgr.log(INFO, "client wait time: " + (before_exe_time - client.arrival_time));
                    this.client_id = client.client_id;
                } catch (NullPointerException e) {
                    lgr.warning("clients queue is null.");
                }
                if (client == null) {
                    continue;
                }
                lgr.log(INFO, "popping client " + this.client_id + " from queue.");
                PrintWriter out;
                BufferedReader in;
                try {
                    out = new PrintWriter(client.socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(
                            client.socket.getInputStream()));
                    String cmd, response;
                    long last_command_time = System.currentTimeMillis();
                    while ((cmd = in.readLine()) == null) {
                        if (System.currentTimeMillis() - last_command_time > client_idle_timeout) {
                            break;
                        }
                    }
                    long before_time = System.currentTimeMillis();
                    response = CommandHandler(cmd, in);
                    long after_time = System.currentTimeMillis();
                    lgr.log(INFO, "log_id: " + this.log_id + "; Middleware (" + cmd + ") first socket read time: " + (before_time - last_command_time));
//                    lgr.log(INFO, "log_id: " + this.log_id + "; Middleware (" + cmd + ") command socket read timestamp: " + before_time);
                    lgr.log(INFO, "Middleware response preparation time for command  (" + cmd + ") was: " + (after_time - before_time));
//                    lgr.log(INFO, "response: " + response);
                    if (response != null) {
                        long before_socket_time = System.currentTimeMillis();
                        if (response.contains("!")) {
                            String[] mtc = response.split("!");
                            out.println(mtc[0] + "\n" + mtc[1]);
//                            out.println(mtc[1]);
                            out.flush();
                        } else {
                            out.println(response);
                            out.flush();
                        }
                        long after_socket_time = System.currentTimeMillis();
                        lgr.log(INFO, "Middleware client socket write time for command  (" + cmd + ") was:  " + (after_socket_time - before_socket_time));
                    }
                    if (cmd != null && cmd.equals("un_register")) {
                        try {
                            client.socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        client.client_id = this.client_id;
                        client.arrival_time = System.currentTimeMillis();
                        this.clients.put(client);
                        lgr.log(INFO, "putting back client " + this.client_id + " to queue.");
                        after_time = System.currentTimeMillis();
                    }
                    if (cmd != null && response != null) {//  && response.contains(OK)
                        lgr.log(INFO, "Middleware overall time for command  (" + cmd + ") was: " + (after_time - before_time));
                    }
                } catch (IOException e) {
                    lgr.log(WARNING, "IO Error in reading socket.");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String CommandHandler(String cmd, BufferedReader in) throws IOException {
        /*
        Basic handler for all commands for general error check and dispatching. Later on, it sends commands to more specific handlers.
         */
        if (cmd == null)
            return null;
        String response;
        if (cmd.equals("un_register")) {
            response = OK;
        } else if (cmd.equals("list users")) {
            List<Integer> users = ListClients();
            response = (users == null) ? "Error" : OK + "!users:"
                    + users;
        } else if (cmd.equals("list all queues")) {
            List<Integer> queues = listQueues();
            response = (queues == null) ? OK + "[]" : OK + "!queues:"
                    + queues;
        } else if (cmd.equals("create queue")) {
            int id = createQueue();
            response = id >= 0 ? OK + "!" + "id:" + id : "Error";
        } else {
            long before_time = System.currentTimeMillis();
            String query_str = in.readLine();
            long after_time = System.currentTimeMillis();
            lgr.log(INFO, "log_id= " + this.log_id++ + "# Middleware query reading time for command  (" + cmd + ") was: " + (after_time - before_time));
            HashMap<String, String> query = Parser.stringToMap(query_str);
            if (cmd.equals("register")) {
                int id = register(Integer.parseInt(query.get("id")));
                response = id >= 0 ? OK + "!" + "id:" + id
                        : "Registration failed.";
            } else if (cmd.equals("delete queue")) {
                boolean success = deleteQueueHandler(query);
                response = success ? OK : "Error";
            } else if (cmd.equals("send msg")) {
                int res = sendMsgHandler(query);
                response = res >= 0 ? OK : (res == -2 ? "Non existing queue" : "Error");
//                lgr.log(INFO, "Middleware sendMessage query socket read timestamp: " + before_time);
            } else if (cmd.equals("get msg") || cmd.equals("read msg")) {
                String str = messageQueryHandler(cmd, query);
                response = (str != null && !str.equals("Empty queue") && !str.equals("Non existing queue")) ? OK + "!" + str
                        : (str == null ? "Error" : str);
            } else if (cmd.equals("list queues")) {
                response = OK + "!" + listQueueHandler(query);
            } else {
                response = "Bad Request";
            }
        }
        return response;
    }


    private boolean deleteQueueHandler(HashMap<String, String> query) {
        /*
        Handler for delete queue command.
         */
        if (query.containsKey("id")) {
            lgr.log(INFO, "Deleting queue:" + query.get("id"));
            return deleteQueue(Integer.valueOf(query.get("id")));
        }
        return false;
    }

    private int sendMsgHandler(HashMap<String, String> query) {
        /*
        Handler for send message command.
         */
        Integer receiver = -1, sender = -1, queue = 1;
        String content = query.get("content");
        if (query.get("receiver") != null)
            receiver = Integer.parseInt(query.get("receiver"));
        if (query.get("sender") != null)
            sender = Integer.parseInt(query.get("sender"));
        if (query.get("queue") != null)
            queue = Integer.parseInt(query.get("queue"));
        return insertMessage(sender, receiver, queue, content);
    }

    private String messageQueryHandler(String cmd, HashMap<String, String> query) {
        /*
        Handler for get or read message commands.
         */
        if (query.containsKey("queue")) {
            int queue = Integer.parseInt(query.get("queue"));
            if (cmd.equals("read msg"))
                return readMessage(queue);
            else if (cmd.equals("get msg"))
                return getMessage(queue);
        } else if (query.containsKey("sender")) {
            int sender = Integer.parseInt(query.get("sender"));
            if (cmd.equals("get msg"))
                return GetSenderMessage(sender);
            else if (cmd.equals("read msg"))
                return readSenderMessage(sender);
        }
        return null;
    }

    private String listQueueHandler(HashMap<String, String> query) {
        /*
        Handler for list queue command. (there are two commands: list all queues, and list queues with pending messages for client)
         */
        int receiver = -1;
        if (query.containsKey("receiver")) {
            try {
                receiver = Integer.valueOf(query.get("receiver"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        List<Integer> q_ids = null;
        if (receiver != -1)
            q_ids = PendingQueues(receiver);
        return "queues:" + q_ids;
    }

    /*
     * DB interface
     */
    private int register(int client_id) {
        this.client_id = client_id;
        try {
            Query qu = new Query(this.dbpool.getConnection());
            ResultSet rs = qu.no_trans_execute("SELECT Create_Client(" + client_id + ");");
            if (rs != null && rs.next()) {
                client_id = rs.getInt(1);
            }
            if (rs != null)
                rs.close();
            try {
                qu.getCon().close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
//            lgr.log(ERROR, e.getMessage(), e);
        }
        lgr.log(INFO, "Client created with id : " + this.client_id);
        return client_id;
    }

    private int createQueue() {
        /*
        create a queue in database and return its id.
         */
        int queue_id = -1;
        try {
            Query qu = new Query(this.dbpool.getConnection());
            ResultSet rs = qu.no_trans_execute("SELECT Create_Queue();");
            if (rs != null && rs.next()) {
                queue_id = rs.getInt(1);
            }
            if (rs != null)
                rs.close();
            try {
                qu.getCon().close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            lgr.log(ERROR, e.getMessage(), e);
            return -1;
        }
        return queue_id;
    }

    private boolean deleteQueue(int id) {
        /*
        delete a queue identified by id.
         */
        boolean success = false;
        try {
            Query qu = new Query(this.dbpool.getConnection());
            ResultSet rs = qu.no_trans_execute("SELECT Delete_Queue(" + id + ");");
            if (rs != null && rs.next()) {
                success = true;
                lgr.log(INFO, "Deleted queue: " + id);
            }
            if (rs != null)
                rs.close();
            try {
                qu.getCon().close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            lgr.log(ERROR, e.getMessage(), e);
            return false;
        }
        return success;
    }

    private int insertMessage(int sender_id, int receiver_id, int queue_id,
                              String content) {
        /*
        Insert a message with these parameters in database.
         */
        int msg_id = -1;
        try {
            long before_time = System.currentTimeMillis();
            Connection con = this.dbpool.getConnection();
            long after_wait_time = System.currentTimeMillis();
            sendMsg_pst.setCon(con);
            sendMsg_pst.get_pst().setInt(1, sender_id);
            if (receiver_id == -1) {
                sendMsg_pst.get_pst().setNull(2, Types.NULL);
            } else {
                sendMsg_pst.get_pst().setInt(2, receiver_id);
            }
            sendMsg_pst.get_pst().setInt(3, queue_id);
            sendMsg_pst.get_pst().setString(4, content);
            ResultSet rs = sendMsg_pst.no_trans_execute();
            long after_time = System.currentTimeMillis();

            if (rs != null && rs.next()) {
                msg_id = rs.getInt(1);
                lgr.log(INFO, "insertMessage DB connection wait time: " + (after_wait_time - before_time));
                lgr.log(INFO, "insertMessage DB response time: " + (after_time - after_wait_time));
            } else {
                queue_exist.setCon(this.dbpool.getConnection());
                queue_exist.get_pst().setInt(1, queue_id);
                ResultSet check_msg = queue_exist.no_trans_execute();
                try {
                    if (check_msg != null && check_msg.next() && !check_msg.getBoolean(1))
                        msg_id = -2;
                    if (check_msg != null)
                        check_msg.close();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            lgr.log(ERROR, e.getMessage(), e);
            return -1;
        }
        if (msg_id <= 0)
            lgr.log(ERROR, "inserting message failed with error code: " + msg_id + " sender_id: " + sender_id + "receiver_id: " + receiver_id + "queue_id: " + queue_id);
        return msg_id;
    }

    private String getMessage(int queue) {
        /*
        Returns the top recent message of the queue which is has the client_id as its receiver of is a broadcast message.
         Then delete that message from DB.
         */
        String msg = null;
        try {
            ResultSet rs;
            long before_time = System.currentTimeMillis();
            Connection con = this.dbpool.getConnection();
            long after_wait_time = System.currentTimeMillis();
            get_msg_prepared_st1.setCon(con);
            get_msg_prepared_st1.get_pst().setInt(1, this.client_id);
            get_msg_prepared_st1.get_pst().setInt(2, queue);
            rs = get_msg_prepared_st1.no_trans_execute();
            if (rs != null && rs.next()) {
                long after_time = System.currentTimeMillis();
                msg = Parser.ResultMessageToString(rs);
//                lgr.log(INFO, "message for client " + this.client_id + " --  msg: " + msg);
                lgr.log(INFO, "getMessage DB connection wait time: " + (after_wait_time - before_time));
                lgr.log(INFO, "getMessage DB response time: " + (after_time - after_wait_time));
            } else {
                queue_exist.setCon(this.dbpool.getConnection());
                queue_exist.get_pst().setInt(1, queue);
                ResultSet check_msg = queue_exist.no_trans_execute();
                try {
                    if (check_msg != null && check_msg.next() && check_msg.getBoolean(1))
                        msg = "Empty queue";
                    else
                        msg = "Non existing queue";
                    if (check_msg != null)
                        check_msg.close();
                } catch (SQLException e1) {
                    lgr.log(ERROR, e1.getMessage(), e1);
                }
            }
            if (rs != null)
                rs.close();
        } catch (SQLException e) {
            lgr.log(ERROR, e.getMessage(), e);
            return null;
        }
        return msg;
    }


    private String GetSenderMessage(int sender) {
        /*
        Returns one message (if any exist) with this sender_id and accessible by current user of worker(client_id)
         */
        String msg = null;
        try {

            ResultSet rs;
            long before_time = System.currentTimeMillis();
            get_msg_prepared_st2.setCon(this.dbpool.getConnection());
            long after_wait_time = System.currentTimeMillis();
            get_msg_prepared_st2.get_pst().setInt(1, this.client_id);
            get_msg_prepared_st2.get_pst().setInt(2, sender);
            rs = get_msg_prepared_st2.no_trans_execute();

            if (rs != null && rs.next()) {
                long after_time = System.currentTimeMillis();
                msg = Parser.ResultMessageToString(rs);
                lgr.log(INFO, "getMessage DB connection wait time: " + (after_wait_time - before_time));
                lgr.log(INFO, "getMessage DB response time: " + (after_time - after_wait_time));
            }
            if (rs != null)
                rs.close();
        } catch (SQLException e) {
            lgr.log(ERROR, e.getMessage(), e);
            return null;
        }
        return msg;
    }

    private String readMessage(int queue) {
        /*
        Returns the top recent message of queue for the current client_id which worker is handling.
         */
        String msg = null;
        try {
            ResultSet rs;
            long before_time = System.currentTimeMillis();
            read_msg_prepared_st1.setCon(this.dbpool.getConnection());
            long after_wait_time = System.currentTimeMillis();
            read_msg_prepared_st1.get_pst().setInt(1, queue);
            read_msg_prepared_st1.get_pst().setInt(2, this.client_id);
            rs = read_msg_prepared_st1.no_trans_execute();
            long after_time = System.currentTimeMillis();
            if (rs != null && rs.next()) {
                msg = Parser.ResultMessageToString(rs);
                lgr.log(INFO, "readMessage DB connection wait time: " + (after_wait_time - before_time));
                lgr.log(INFO, "readMessage DB response time: " + (after_time - after_wait_time));
            } else {
                queue_exist.setCon(this.dbpool.getConnection());
                queue_exist.get_pst().setInt(1, queue);
                ResultSet check_msg = queue_exist.no_trans_execute();
                try {
                    if (check_msg != null && check_msg.next() && check_msg.getBoolean(1))
                        msg = "Empty queue";
                    else
                        msg = "Non existing queue";
                    if (check_msg != null)
                        check_msg.close();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
            if (rs != null)
                rs.close();
        } catch (SQLException e) {
            lgr.log(ERROR, e.getMessage(), e);
            return null;
        }
        return msg;
    }

    private String readSenderMessage(int sender) {
        /*
        Returns the top recent message of queue for the current client_id which worker is handling.
         */
        String msg = null;
        try {
            ResultSet rs;
            long before_time = System.currentTimeMillis();
            read_msg_prepared_st2.setCon(this.dbpool.getConnection());
            long after_wait_time = System.currentTimeMillis();
            read_msg_prepared_st2.get_pst().setInt(1, sender);
            read_msg_prepared_st2.get_pst().setInt(2, this.client_id);
            rs = read_msg_prepared_st2.no_trans_execute();
            long after_time = System.currentTimeMillis();
            if (rs != null && rs.next()) {
                msg = Parser.ResultMessageToString(rs);
                lgr.log(INFO, "readMessage DB connection wait time: " + (after_wait_time - before_time));
                lgr.log(INFO, "readMessage DB response time: " + (after_time - after_wait_time));
            } else {
                msg = "No message";
            }
            if (rs != null)
                rs.close();
        } catch (SQLException e) {
            lgr.log(ERROR, e.getMessage(), e);
            return null;
        }
        return msg;
    }


    private List<Integer> PendingQueues(int receiver_id) {
        /*
        Returns list of all queues which has at least a message for this receiver_id or a broadcast message.
         */
        try {
            long before_time = System.currentTimeMillis();
            Query qu = new Query(this.dbpool.getConnection());
            long after_wait_time = System.currentTimeMillis();
            ResultSet rs = qu.no_trans_execute("SELECT List_Pending_Queues(" + receiver_id + ");");
            long after_time = System.currentTimeMillis();
            lgr.log(INFO, "pending_queues DB connection wait time: " + (after_wait_time - before_time));
            lgr.log(INFO, "pending_queues DB response time: " + (after_time - after_wait_time));
            List<Integer> q_list = new ArrayList<Integer>();
            while (rs != null && rs.next()) {
                q_list.add(rs.getInt(1));
            }
            if (rs != null)
                rs.close();
            try {
                qu.getCon().close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return q_list;
        } catch (SQLException e) {
            lgr.log(ERROR, e.getMessage(), e);
            return null;
        }
    }

    private List<Integer> listQueues() {
        /*
        returns list of all queues in database.
         */
        try {

            Query qu = new Query(this.dbpool.getConnection());
            ResultSet rs = qu.no_trans_execute("SELECT List_Queues(); ");

            List<Integer> queue_ids = new ArrayList<Integer>();
            while (rs != null && rs.next()) {
                queue_ids.add(rs.getInt(1));
            }
            if (rs != null)
                rs.close();
            try {
                qu.getCon().close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return queue_ids;
        } catch (SQLException e) {
            lgr.log(ERROR, e.getMessage(), e);
            return null;
        }
    }

    private List<Integer> ListClients() {
        /*
        Returns list of all clients in database.
         */
        try {

            Query qu = new Query(this.dbpool.getConnection());
            ResultSet rs = qu.no_trans_execute("SELECT List_Clients();");

            List<Integer> user_ids = new ArrayList<Integer>();
            while (rs != null && rs.next()) {
                user_ids.add(rs.getInt(1));
            }
            if (rs != null)
                rs.close();
            try {
                qu.getCon().close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return user_ids;
        } catch (SQLException e) {
            lgr.log(ERROR, e.getMessage(), e);
            return null;
        }
    }
}
