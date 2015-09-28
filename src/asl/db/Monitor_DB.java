package asl.db;

import asl.utils.Parser;
import org.postgresql.ds.PGPoolingDataSource;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static asl.utils.Constants.*;

public class Monitor_DB {
    private final static Logger lgr = Logger.getLogger("DB-Monitor");
    FileHandler handler = null;
    private int delay;
    private PGPoolingDataSource dbpool;
    private Prepared_Query sendMsg_pst;
    private Prepared_Query read_msg_prepared_st1;
    private Prepared_Query DelMsg_pst;

    public Monitor_DB(PGPoolingDataSource dbpool, int delay) {
        this.dbpool = dbpool;
        this.delay = delay;
        try {
            handler = new FileHandler(log_dir + "monitorDB.log", false);
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
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        String db_host = "127.0.0.1";
        String db_username = db_user;
        String db_passwd = db_password;
        boolean db_test = false;
        int db_port = 5432;
        if (args.length > 0)
            db_host = args[0];
        if (args.length > 1)
            db_port = Integer.parseInt(args[1]);
        if (args.length > 2)
            db_username = args[2];
        if (args.length > 3)
            db_passwd = args[3];
        if (args.length > 4)
            db_test = args[4].equals("true");
        try {
            // Create DB connection pool
            final PGPoolingDataSource db_pool = new PGPoolingDataSource();
            db_pool.setDataSourceName("db_pool");
            db_pool.setServerName(db_host);
            db_pool.setPortNumber(db_port);
            db_pool.setDatabaseName(db_name);
            db_pool.setUser(db_username);
            db_pool.setPassword(db_passwd);
            db_pool.setMaxConnections(5);
            db_pool.setTcpKeepAlive(true);
            Monitor_DB monitor = new Monitor_DB(db_pool, monitor_delay);

            // Query to DB for the list of queues and number of messages per queue.
            // Show the results as a table
            try {
                if (db_test) {
                    monitor.sendMsg_pst = new Prepared_Query(
                            "SELECT Insert_Message(?,?,?,?)");
                    monitor.read_msg_prepared_st1 = new Prepared_Query(
                            "SELECT * FROM Get_Message_By_Queue(?,?)");
                    monitor.DelMsg_pst = new Prepared_Query("SELECT Delete_Message(?);");
                    String content = " message! "; // this is 10 chars.
                    for (int i = 1; i < message_size; i++) {
                        content += " message! ";
                    }

                    while (true) {
//                        monitor.insertMessage(1, 1, 1, content);
                        monitor.createQueue();
//                        monitor.getMessage(1, 1);
                    }
                } else {
                    while (true) {
                        try {
//                                System.out.println("users: " + monitor.listClients().toString());
//                                System.out.println("queues: " + monitor.listQueues().toString());
                            lgr.log(INFO, "message count: " + monitor.countMessages());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            Thread.sleep(monitor.delay * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private List<Integer> listQueues() {
        try {

            Query qu = new Query(this.dbpool.getConnection());
            ResultSet rs = qu.no_trans_execute("SELECT List_Queues()");

            List<Integer> queue_ids = new ArrayList<Integer>();
            while (rs.next()) {
                queue_ids.add(rs.getInt(1));
            }
            rs.close();
            return queue_ids;
        } catch (SQLException e) {
            lgr.log(ERROR, e.getMessage(), e);
            return null;
        }
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
        } catch (SQLException e) {
            lgr.log(ERROR, e.getMessage(), e);
            return -1;
        }
        return queue_id;
    }

    private List<Integer> listClients() {
        try {
            Query qu = new Query(this.dbpool.getConnection());
            ResultSet rs = qu.no_trans_execute("SELECT List_Clients()");

            List<Integer> user_ids = new ArrayList<Integer>();
            while (rs.next()) {
                user_ids.add(rs.getInt(1));
            }
            rs.close();
            return user_ids;
        } catch (SQLException e) {
            lgr.log(ERROR, e.getMessage(), e);
            return null;
        }
    }

    private int countMessages() {
        int msg_count = -1;
        try {
            Query qu = new Query(this.dbpool.getConnection());
            ResultSet rs = qu.no_trans_execute("SELECT Count_Messages()");
            while (rs != null && rs.next()) {
                msg_count = rs.getInt(1);
            }
            if (rs != null)
                rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return msg_count;
    }


    private int insertMessage(int sender_id, int receiver_id, int queue_id,
                              String content) {
        /*
        Insert a message with these parameters in database.
         */
        int msg_id = -1;
        try {
            long before_time = System.currentTimeMillis();
            sendMsg_pst.setCon(this.dbpool.getConnection());
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
                lgr.log(INFO, "insertMessage DB response time: " + (after_time - before_time));
            } else {
                Query qu = new Query(this.dbpool.getConnection());
                ResultSet check_msg = qu.execute("SELECT Queue_Exist("
                        + queue_id + ");");
                if (check_msg == null || !check_msg.next()) {
                    msg_id = -2;
                }
                if (check_msg != null)
                    check_msg.close();
            }
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            lgr.log(ERROR, e.getMessage(), e);
            return -1;
        }
        if (msg_id <= 0)
            lgr.log(ERROR, "inserting message failed with error code: " + msg_id);
        return msg_id;
    }

    private String getMessage(int queue, int client_id) {
        /*
        Returns the top recent message of the queue which is has the client_id as its receiver of is a broadcast message.
         Then delete that message from DB.
         */
        String msg = null;
        try {
            ResultSet rs;
            long before_time = System.currentTimeMillis();
            read_msg_prepared_st1.setCon(this.dbpool.getConnection());
            read_msg_prepared_st1.get_pst().setInt(1, client_id);
            read_msg_prepared_st1.get_pst().setInt(2, queue);
            rs = read_msg_prepared_st1.no_trans_execute();
            if (rs != null && rs.next()) {
                int msg_id = rs.getInt(1);
//                lgr.log(INFO, "message number (" + msg_id + ") retrieved.");
                DelMsg_pst.setCon(this.dbpool.getConnection());
                DelMsg_pst.get_pst().setInt(1, msg_id);
                ResultSet del_msg = DelMsg_pst.no_trans_execute();
                long after_time = System.currentTimeMillis();
                if (del_msg != null && del_msg.next()) {
//                    lgr.log(INFO, "message number (" + msg_id + ") deleted.");
                    lgr.log(INFO, "getMessage DB response time: " + (after_time - before_time));
                    msg = Parser.ResultMessageToString(rs);
                    del_msg.close();
                }
            } else {
                Query qu = new Query(this.dbpool.getConnection());
                ResultSet check_msg = qu.execute("SELECT Queue_Exist("
                        + queue + ");");
                if (check_msg != null && check_msg.next()) {
                    msg = "Empty queue";
                } else {
                    msg = "Non existing queue";
                }
                if (check_msg != null)
                    check_msg.close();
            }
            if (rs != null)
                rs.close();
        } catch (SQLException e) {
            lgr.log(ERROR, e.getMessage(), e);
            return null;
        }
        return msg;
    }
}

