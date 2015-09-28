package asl.utils;


import asl.db.Message;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/*
Implements the protocol parsing functionality for message passing.
 */
public class Parser {


    public Parser() {

    }

    public static HashMap<String, String> stringToMap(String str) {

        String[] terms = str.split(";");
        HashMap<String, String> query = new HashMap<String, String>();
        for (String term : terms) {
            String[] parts = term.split(":");
            query.put(parts[0], parts[1]);
        }
        return query;
    }

    public static List<Integer> stringToList(String str) {
        str = str.substring(1, str.length() - 1);
        List<String> items = Arrays.asList(str.split("\\s*,\\s*"));
        List<Integer> out_list = new ArrayList<Integer>();

        for (String s : items) {
            try {
                out_list.add(Integer.valueOf(s));
            } catch (NumberFormatException e) {
            }
        }
        return out_list;
    }

    public static Message mapToMessage(HashMap<String, String> msg) {
        return new Message(Integer.valueOf(msg.get("id")), Integer.valueOf(msg.get("sender")), Integer.valueOf(msg.get("receiver")),
                Integer.valueOf(msg.get("queue")), msg.get("content"));

    }


    public static String messageToString(Message msg) {
        String msg_str = "sender:" + msg.getSenderID()
                + ";content:" + msg.getContent() + ";queue:" + msg.getQueueID();
        if (msg.getReceiverID() != null)
            msg_str += ";receiver:" + msg.getReceiverID();
        return msg_str;
    }

    public static String ResultMessageToString(ResultSet rs) throws SQLException {
        String msg_str = null;
        try {
            msg_str = "id:" + rs.getInt(1) + ";sender:" + rs.getInt(2) + ";receiver:" + rs.getInt(3) + ";queue:" + rs.getInt(4)
                    + ";content:" + rs.getString(5);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return msg_str;
    }
}
