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
    private int timeStamp;
    volatile static private Queue<Message> LamportQueue;
    volatile static private Queue<Message> messageBuffer;
    private List<String[]> otherServers;
    final private String serverInfoFile = "server_list.txt";

    public MyServer(int totalNumOfServers, int myIndex) {
        this.myIndex=myIndex;
        this.totalNumOfServers=totalNumOfServers;
        this.otherServers = new ArrayList<>();
        this.timeStamp=0;
        LamportQueue=new PriorityQueue<>(Comparator.comparingInt(Message::getTimeStamp));
        messageBuffer = new LinkedList<>();
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

        MessageThread messageThread = new MessageThread(this);
        messageThread.start();

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

    void updateTimeStamp(int newTimeStamp) {
        this.timeStamp = Math.max(this.timeStamp, newTimeStamp) + 1;
    }

    void incrementTimeStamp() {
        this.timeStamp++;
    }

    public int getMyIndex() {
        return myIndex;
    }

    List<String[]> getOtherServers() {
        return otherServers;
    }

    int getTotalNumOfServers() {
        return totalNumOfServers;
    }

    int getTimeStamp() {
        return timeStamp;
    }

    Queue<Message> getLamportQueue() {
        return LamportQueue;
    }

    Queue<Message> getMessageBuffer() {
        return messageBuffer;
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

class MessageThread extends Thread{
    private MyServer server;
    MessageThread(MyServer server){
        this.server = server;
    }

    public void run() {
        while(true){
            if(this.server.getMessageBuffer().size() == 0) {
                continue;
            }
            Message message = this.server.getMessageBuffer().poll();
            if (message.getMessage().equals("REQUEST")){
                System.out.println("receive request");
                this.server.incrementTimeStamp();
                message.setTimeStamp(this.server.getTimeStamp());
                this.server.getLamportQueue().offer(message);
                try {
                    for (int i = 0; i < this.server.getTotalNumOfServers() - 1; i++) {  // spreads word to all other servers about this new request
                        String[] otherServer = this.server.getOtherServers().get(i);
                        Socket sock = new Socket(otherServer[0], Integer.parseInt(otherServer[1]));

                        ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
                        ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
                        this.server.incrementTimeStamp();
                        Message tempMsg = new Message(message.getMessageID(), "ENQUEUE", message.getRequest(), this.server.getTimeStamp());
                        out.writeObject(tempMsg);

                        Message disconnect = new Message("DISCONNECT");     // disconnect every time to avoid socket bugs
                        out.writeObject(disconnect);
                    }
                }
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
            else if(message.getMessage().equals("ENQUEUE")){
                System.out.println("receive enqueue");
                this.server.updateTimeStamp(message.getTimeStamp());
                int incomingServerIndex = message.getRequest().getServerID();
                this.server.getLamportQueue().offer(message);
                String[] otherServer = this.server.getOtherServers().get(incomingServerIndex);
                try {
                    Socket sock = new Socket(otherServer[0], Integer.parseInt(otherServer[1]));

                    ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
                    this.server.incrementTimeStamp();
                    Message tempMsg = new Message(message.getMessageID(), "ENQUEUEREPLY", message.getRequest(), this.server.getTimeStamp());
                    out.writeObject(tempMsg);

                    Message disconnect = new Message("DISCONNECT");     // disconnect every time to avoid socket bugs
                    out.writeObject(disconnect);
                }
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
            else if (message.getMessage().equals("ENQUEUEREPLY")){
                System.out.println("receive enqueue reply");
                this.server.updateTimeStamp(message.getTimeStamp());
                for (Message msg: this.server.getLamportQueue()) {
                    if (msg.getMessageID() == message.getMessageID()) {
                        msg.incrementNumOfServerReplies();
                        break;
                    }
                }
            }
            else if(message.getMessage().equals("CRELEASE")) {
                System.out.println("receive client release");
                Message serverRelease = new Message(message.getMessageID(), "SRELEASE", message.getRequest());
                for (Message msg: this.server.getLamportQueue()) {
                    if (msg.getMessageID() == message.getMessageID()) {
                        this.server.getLamportQueue().remove(msg);
                        break;
                    }
                }
                try {
                    for (int i = 0; i < this.server.getTotalNumOfServers() - 1; i++) {  // spreads word to all other servers about this new request
                        String[] otherServer = this.server.getOtherServers().get(i);
                        Socket sock = new Socket(otherServer[0], Integer.parseInt(otherServer[1]));

                        ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
                        ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
                        this.server.incrementTimeStamp();
                        serverRelease.setTimeStamp(this.server.getTimeStamp());
                        out.writeObject(serverRelease);

                        Message disconnect = new Message("DISCONNECT");     // disconnect every time to avoid socket bugs
                        out.writeObject(disconnect);
                    }
                }
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
            else if(message.getMessage().equals("SRELEASE")) {
                System.out.println("receive server release");
                this.server.updateTimeStamp(message.getTimeStamp());
                for (Message msg: this.server.getLamportQueue()) {
                    if (msg.getMessageID() == message.getMessageID()) {
                        this.server.getLamportQueue().remove(msg);
                        break;
                    }
                }
            }
        }
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
//                output.reset();
                Message msg = (Message)input.readObject();
                if (msg.getMessage().equals("REQUEST")) {   // receives a request from client
                    this.server.getMessageBuffer().offer(msg);
                    while(!(this.server.getLamportQueue().size() > 0 &&
                            this.server.getLamportQueue().peek().getMessageID() == msg.getMessageID()&&
                            this.server.getTotalNumOfServers() - 1 == this.server.getLamportQueue().peek().getNumOfServerReplies())){
                        // do nothing
                    }
                    output.writeObject(new Message("OK"));
                }
                else if (msg.getMessage().equals("DISCONNECT")) {   // receive a disconnect message
                    this.socket.close();
                    break;
                }
                else if (msg.getMessage().equals("CRELEASE") ||
                         msg.getMessage().equals("SRELEASE") ||
                         msg.getMessage().equals("ENQUEUE") ||
                         msg.getMessage().equals("ENQUEUEREPLY")){
                    this.server.getMessageBuffer().offer(msg);
                }
            }
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

}