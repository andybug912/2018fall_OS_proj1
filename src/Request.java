import java.io.Serializable;

public class Request implements Serializable {
    private int serverID;
    private int clientID;
    private boolean isRead;
    private int newNumber;

    public Request(int serverID, int clientID, boolean isRead, int newNumber) {
        this.serverID = serverID;
        this.clientID = clientID;
        this.isRead = isRead;
        this.newNumber = newNumber;
    }

    public Request(int serverID, int clientID, boolean isRead) {
        this.serverID = serverID;
        this.clientID = clientID;
        this.isRead = isRead;
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

}
