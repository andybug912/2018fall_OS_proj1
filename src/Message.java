import java.io.Serializable;

public class Message implements Serializable {
    private String message;
    private Request request;
    private int timeStamp;

    public Message(String msg) {
        this.message = msg;
    }

    public Message(String msg, Request request) {
        this.message = msg;
        this.request = request;
    }

    public Message(String msg, Request request, int timeStamp) {
        this.message = msg;
        this.request = request;
        this.timeStamp = timeStamp;
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
}
