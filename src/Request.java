import java.io.Serializable;

public class Request implements Serializable {
    private int serverID;
    private int clientID;
    private boolean isRead;
    private int newNumber;

    public Request(int serverID, boolean isRead, int newNumber) {
        this.serverID = serverID;
        this.isRead = isRead;
        this.newNumber = newNumber;
    }

    public Request(int serverID, boolean isRead) {
        this.serverID = serverID;
        this.isRead = isRead;
    }

    public int getClientID() {
        return clientID;
    }

    public void setClientID(int clientID) {
        this.clientID = clientID;
    }

    public int getServerID() {
        return serverID;
    }

    public boolean isRead() {
        return isRead;
    }

    public int getNewNumber() {
        return newNumber;
    }

}
