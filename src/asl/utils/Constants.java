package asl.utils;

import java.util.logging.Level;

/**
 * Stores the constants used in the project.
 */
public final class Constants {
    public final static String db_name = "asl";
    public final static String log_dir = "log/";
    public final static String db_user = "postgres";
    public final static String db_password = "1111"; // "asldatabase";
    public final static int coreThreadPoolSize = 4;
    public final static int db_con_pool_size = 10; //4
    public final static int maxThreadPoolSize = 50;
    public final static int keepAlive_thread_pool = 1;
    public final static int ClientsQueue_limit = 1000;
    public final static int list_update_interval = 3*60*1000; // only update user_list and queue_list every 3 minutes
    public final static int client_counts = 40;
    public final static int monitor_delay = 1; //seconds
    public final static int message_size = 200; //it means 20*10 (make it 200 for 2000 char message)
    public final static int client_idle_timeout = 6000;
    public final static Level WARNING = Level.WARNING;
    public final static Level ERROR = Level.SEVERE;
    public final static Level INFO = Level.INFO;
    public final static String OK = "OK";

    private Constants() {
    }
}
