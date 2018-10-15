import org.omg.CORBA.portable.OutputStream;

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
    volatile static public List<String[]> otherServers;
    volatile static public List<ObjectOutputStream> outputs;
    volatile static public List<ObjectInputStream> inputs;
    final private String serverInfoFile = "server_list.txt";

    public MyServer(int totalNumOfServers, int myIndex) {
        this.myIndex=myIndex;
        this.totalNumOfServers=totalNumOfServers;
        this.otherServers = new ArrayList<>();
        this.outputs = new ArrayList<>();
        this.inputs = new ArrayList<>();
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
                otherServers.add(serverInfo);
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

        ConnectOtherServers connectOtherServers = new ConnectOtherServers(this);
        connectOtherServers.start();

        // **** listening to incoming messages ****
        while (true) {
            try {
                Socket socket = serverSock.accept();      //listen
                ServerThread serverThread = new ServerThread(socket, this);
                serverThread.start();
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

class ConnectOtherServers extends Thread {
    private MyServer server;

    public ConnectOtherServers(MyServer server) {
        this.server = server;
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter to connect to other servers...");
        scanner.nextLine();
        for (int j = 0; j < this.server.getTotalNumOfServers() - 1; j++) {
            try {
                Socket socket = new Socket(this.server.otherServers.get(j)[0], Integer.parseInt(this.server.otherServers.get(j)[1]));
                this.server.outputs.add(new ObjectOutputStream(socket.getOutputStream()));
                this.server.inputs.add(new ObjectInputStream(socket.getInputStream()));
                System.out.println("connect to a server " + this.server.otherServers.get(j)[0] + " on port " + Integer.parseInt(this.server.otherServers.get(j)[1]));
            }
            catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
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
                        ObjectOutputStream out = this.server.outputs.get(i);
                        this.server.incrementTimeStamp();
                        Message tempMsg = new Message(message.getMessageID(), "ENQUEUE", message.getRequest(), this.server.getTimeStamp());
                        out.writeObject(tempMsg);
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
                try {
                    ObjectOutputStream out = this.server.outputs.get(incomingServerIndex);
                    this.server.incrementTimeStamp();
                    Message tempMsg = new Message(message.getMessageID(), "ENQUEUEREPLY", message.getRequest(), this.server.getTimeStamp());
                    out.writeObject(tempMsg);
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
                        System.out.println("found msg in enqueue reply");
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
                        System.out.println("found msg in client release");
                        break;
                    }
                }
                try {
                    for (int i = 0; i < this.server.getTotalNumOfServers() - 1; i++) {  // spreads word to all other servers about this new request
                        ObjectOutputStream out = this.server.outputs.get(i);
                        this.server.incrementTimeStamp();
                        serverRelease.setTimeStamp(this.server.getTimeStamp());
                        out.writeObject(serverRelease);
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
                        System.out.println("found msg in server release");
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
                Message msg = (Message)input.readObject();
                if (msg.getMessage().equals("REQUEST")) {   // receives a request from client
                    this.server.getMessageBuffer().offer(msg);
                    if (!msg.getRequest().isRead()) {   // a write request
                        while(!(this.server.getLamportQueue().size() > 0 &&
                                this.server.getLamportQueue().peek().getMessageID() == msg.getMessageID()&&
                                this.server.getTotalNumOfServers() - 1 == this.server.getLamportQueue().peek().getNumOfServerReplies())){
                            // do nothing, wait for the write request to be the head of LamportQueue
                            // and all replies from other servers have been received
                        }
                    }
                    else {  // a read request
                        boolean isApproved = false;
                        while (!isApproved) {
                            for (Message temp: this.server.getLamportQueue()) {
                                if (!temp.getRequest().isRead()) {  // if there is write request ahead of it, cannot be approved to read
                                    break;
                                }
                                if (temp.getMessageID() == msg.getMessageID()) {
                                    if (this.server.getTotalNumOfServers() - 1 == temp.getNumOfServerReplies()) {
                                        isApproved = true;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    output.writeObject(new Message("OK"));
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