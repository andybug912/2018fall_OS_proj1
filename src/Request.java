import java.io.Serializable;

public class Request implements Serializable {
    private int requestID;
    private int serverID;
    private int clientID;
    private boolean isRead;
    private int newNumber;
    private int numOfServerReplies;

    public Request(int requestID, int serverID, int clientID, boolean isRead, int newNumber) {
        this.requestID = requestID;
        this.serverID = serverID;
        this.clientID = clientID;
        this.isRead = isRead;
        this.newNumber = newNumber;
    }

    public Request(int requestID, int serverID, int clientID, boolean isRead) {
        this.requestID = requestID;
        this.serverID = serverID;
        this.clientID = clientID;
        this.isRead = isRead;
    }

    public int getRequestID() {
        return requestID;
    }

    public int getServerID() {
        return serverID;
    }

    public int getClientID() {
        return clientID;
    }

    public boolean isRead() {
        return isRead;
    }

    public int getNewNumber() {
        return newNumber;
    }

    public int getNumOfServerReplies() {
        return numOfServerReplies;
    }

    public void setNumOfServerReplies(int numOfServerReplies) {
        this.numOfServerReplies = numOfServerReplies;
    }
}
