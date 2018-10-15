import java.io.Serializable;

public class Message implements Serializable {
    private int messageID;
    private String message;
    private Request request;
    private int timeStamp;
    private int numOfServerReplies;

    public Message(int messageID, String msg, Request request) {
        this.messageID = messageID;
        this.message = msg;
        this.request = request;
    }

    public Message(int messageID, String msg, Request request, int timeStamp){
        this.messageID = messageID;
        this.message = msg;
        this.timeStamp = timeStamp;
        this.request = request;
    }

    public Message(String msg) {
        this.message = msg;
    }

    public int getMessageID() {
        return messageID;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Request getRequest() {
        return request;
    }

    public int getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(int timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getNumOfServerReplies() {
        return numOfServerReplies;
    }

    public void incrementNumOfServerReplies() {
        this.numOfServerReplies++;
    }
}
