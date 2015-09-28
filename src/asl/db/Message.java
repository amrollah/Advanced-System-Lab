package asl.db;

import static asl.utils.Constants.message_size;

public class Message {
    private Integer id;
    private Integer messageID;
    private Integer senderID;
    private Integer receiverID;
    private Integer queueID;
    private long timestamp;
    private String content;

    public Message(Integer senderID, Integer receiverID, Integer queueID) {
        this.senderID = senderID;
        this.receiverID = receiverID;
        this.queueID = queueID;
        String content = " message! "; // this is 10 chars.
        for (int i = 1; i < message_size; i++) {
            content += " message! ";
        }
        this.content = content;
    }

    public Message(Integer id, Integer senderID, Integer receiverID, Integer queueID, String content) {
        this.id = id;
        this.senderID = senderID;
        this.receiverID = receiverID;
        this.queueID = queueID;
        this.content = content;
    }

    public long getMessageID() {
        return this.messageID;
    }

    public void setMessageID(Integer messageID) {
        this.messageID = messageID;
    }

    public Integer getSenderID() {
        return this.senderID;
    }

    public void setSenderID(Integer senderID) {
        this.senderID = senderID;
    }

    public Integer getReceiverID() {
        return this.receiverID;
    }

    public void setReceiverID(Integer receiverID) {
        this.receiverID = receiverID;
    }

    public Integer getQueueID() {
        return this.queueID;
    }

    public void setQueueID(Integer queueID) {
        this.queueID = queueID;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long ts) {
        this.timestamp = ts;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
