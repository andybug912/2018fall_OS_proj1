import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.io.File;
import java.util.*;


public class MyServer {
    private int myIndex;
    private int totalNumOfServers;
    private int myPort;
    private List<String[]> otherServers;
    final private String serverInfoFile = "server_list.txt";

    public MyServer(int totalNumOfServers, int myIndex) {
        this.myIndex=myIndex;
        this.totalNumOfServers=totalNumOfServers;
        this.otherServers = new ArrayList<>();
    }

    public void start() {
        // **** read server name & port from file ****
        File file = new File(this.serverInfoFile);
        Scanner fileScanner;
        try {
            fileScanner = new Scanner(file);    //read the file
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
        int i = 0;
        while (fileScanner.hasNext() && i < this.totalNumOfServers) {
            String[] serverInfo = fileScanner.nextLine().split(" ");
            if (i == this.myIndex) {
                this.myPort = Integer.parseInt(serverInfo[1]);      //find myPort according to myIndex
            }
            else {
                this.otherServers.add(serverInfo);
            }
            i++;
        }
        fileScanner.close();

        // **** setup server socket ****
        final ServerSocket serverSock;
        try
        {
            serverSock = new ServerSocket(myPort);
        }
        catch(Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return;
        }

        // **** listening to incoming messages ****
        while (true) {
            try {
                Socket socket = serverSock.accept();      //listen
                ServerThread serverThread = new ServerThread(socket, this);
                serverThread.run();
            }
            catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(System.err);
                return;
            }
        }
    }

    public int getMyIndex() {
        return myIndex;
    }

    public List<String[]> getOtherServers() {
        return otherServers;
    }

    public int getTotalNumOfServers() {
        return totalNumOfServers;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Input total numbers of servers:");
        String totalNumOfServers = scanner.nextLine();
        System.out.println("Input myIndex:");
        String myIndex = scanner.nextLine();

        MyServer server = new MyServer(Integer.parseInt(totalNumOfServers),Integer.parseInt(myIndex));
        server.start();
    }
}

class ServerThread extends Thread {
    private Socket socket;
    private MyServer server;

    public ServerThread(Socket socket, MyServer server) {
        this.socket = socket;
        this.server = server;
    }

    public void run(){
        try {
            final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            while (true) {
                output.reset();
                Message msg = (Message)input.readObject();
                if (msg.getMessage().equals("REQUEST")) {   // receives a request from client
                    Request request = msg.getRequest();
                    msg.setTimeStamp(1000 + this.server.getMyIndex());
                    msg.setMessage("SERVER");
                    for (int i = 0; i < this.server.getTotalNumOfServers() - 1; i++) {  // spreads word to all other servers about this new request
                        String[] otherServer = this.server.getOtherServers().get(i);
                        Socket sock = new Socket(otherServer[0], Integer.parseInt(otherServer[1]));

                        ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
                        ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
                        out.writeObject(msg);
                        Message response = (Message) in.readObject();
                        System.out.println("Got response from server " + response.getTimeStamp());

                        Message disconnect = new Message("DISCONNECT");     // disconnect every time to avoid socket bugs
                        out.writeObject(disconnect);
                    }
                    msg.setMessage("OK");
                    output.writeObject(msg);
                }
                else if (msg.getMessage().equals("SERVER")) {   // receive message from another server
                    Request request = msg.getRequest();
                    System.out.println("Got from client from server " + msg.getTimeStamp());
                    Message response = new Message(null, null, 1000 + this.server.getMyIndex());
                    output.writeObject(response);
                }
                else if (msg.getMessage().equals("DISCONNECT")) {   // receive a disconnect message
                    this.socket.close();
                    break;
                }
            }
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

}