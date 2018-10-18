import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.*;


public class MyServer {
    private int myIndex;
    private int totalNumOfServers;
    private int myPort;
    private int timeStamp;
    volatile static private Queue<Message> LamportQueue;
    volatile static private Queue<Message> messageBuffer;
    volatile static public Map<Integer, String[]> otherServers;
    volatile static public Map<Integer, ObjectOutputStream> serverOutputs;
    volatile static public Map<Integer, ObjectOutputStream> clientOutputs;
    volatile static public int clientNum;
    final private String serverInfoFile = "server_list.txt";
    final public String sharedFile = "shared.txt";

    public MyServer(int totalNumOfServers, int myIndex) {
        this.myIndex=myIndex;
        this.totalNumOfServers=totalNumOfServers;
        this.otherServers = new HashMap<>();
        this.serverOutputs = new HashMap<>();
        this.clientOutputs = new HashMap<>();
        this.clientNum = 0;
        this.timeStamp=0;
        LamportQueue=new PriorityQueue<>(Comparator.comparingInt(Message::getTimeStamp));
        messageBuffer = new LinkedList<>();
        if (myIndex == 0) {
            File file = new File(this.sharedFile);
            if (file.exists()) {
                file.delete();
            }
            try {
                file.createNewFile();
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
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
                otherServers.put(i, serverInfo);
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

//        LamportThread lamportThread = new LamportThread(this);
//        lamportThread.start();

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

        for (int j = 0; j < this.server.getTotalNumOfServers(); j++) {
            if (j == this.server.getMyIndex()) continue;
            try {
                Socket socket = new Socket(this.server.otherServers.get(j)[0], Integer.parseInt(this.server.otherServers.get(j)[1]));
                this.server.serverOutputs.put(j, new ObjectOutputStream(socket.getOutputStream()));
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
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
            if (this.server.getLamportQueue().size() != 0) {
                Message message = this.server.getLamportQueue().peek();
                if (message != null) {
                    if (message.getRequest().isRead() && !message.isReplySent() &&
                            message.getRequest().getServerID() == this.server.getMyIndex() &&
                            message.getNumOfServerReplies() == this.server.getTotalNumOfServers() - 1
                            ) {
                        try {
                            File file = new File(this.server.sharedFile);
                            Scanner fileScanner = new Scanner(file);
                            String lastLine = "";
                            while (fileScanner.hasNext()) {
                                lastLine = fileScanner.nextLine();
                            }
                            this.server.clientOutputs.get(message.getRequest().getClientID()).writeObject(new Message("OK " + lastLine));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        message.setReplySent(true);
                    }
                    else if (!message.getRequest().isRead() && !message.isReplySent() &&
                            message.getRequest().getServerID() == this.server.getMyIndex() &&
                            message.getNumOfServerReplies() == this.server.getTotalNumOfServers() - 1
                            ) {
                        String lastLine = "";
                        Scanner fileScanner;
                        File file = new File(this.server.sharedFile);
                        FileOutputStream fos;
                        BufferedWriter bw;
                        try {
                            fos = new FileOutputStream(file, true);
                            bw = new BufferedWriter(new OutputStreamWriter(fos));
                            fileScanner = new Scanner(file);    //read the file
                        }
                        catch (Exception e) {
                            System.out.println(e.getMessage());
                            return;
                        }
                        while (fileScanner.hasNext()) {
                            lastLine = fileScanner.nextLine();
                        }
                        int newSum = message.getRequest().getNewNumber();
                        if (!lastLine.equals("")) {
                            int oldSum = Integer.parseInt(lastLine.split(":")[1]);
                            newSum += oldSum;
                        }
                        try {
                            Thread.sleep(500);
                            bw.write(String.valueOf("\n" + this.server.getTimeStamp()) + ":");
                            Thread.sleep(500);
                            bw.write(String.valueOf(newSum));fileScanner.close();
                            bw.close();
                            fos.close();
                        }
                        catch (Exception e) {
                            System.out.println(e.getMessage());
                            return;
                        }
                        try {
                            this.server.clientOutputs.get(message.getRequest().getClientID()).writeObject(new Message("OK"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        message.setReplySent(true);
                    }
                }
            }
            if(this.server.getMessageBuffer().size() == 0) {
                continue;
            }
            Message message = this.server.getMessageBuffer().poll();
            if (message == null) continue;
            if (message.getMessage().equals("REQUEST")){
                System.out.println(message.getMessageID() + "receive request");
                this.server.incrementTimeStamp();
                message.setTimeStamp(this.server.getTimeStamp());
                this.server.getLamportQueue().offer(message);
                try {
                    for (int i = 0; i < this.server.getTotalNumOfServers(); i++) {  // spreads word to all other servers about this new request
                        if (i == this.server.getMyIndex()) continue;
                        ObjectOutputStream out = this.server.serverOutputs.get(i);
                        this.server.incrementTimeStamp();
                        Message tempMsg = new Message(message.getMessageID(), "ENQUEUE", message.getRequest(), this.server.getTimeStamp());
                        Thread.sleep(250);
                        out.writeObject(tempMsg);
                    }
                }
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
            else if(message.getMessage().equals("ENQUEUE")){
                System.out.println(message.getMessageID() + "receive enqueue");
                this.server.updateTimeStamp(message.getTimeStamp());
                int incomingServerIndex = message.getRequest().getServerID();
                this.server.getLamportQueue().offer(message);
                try {
                    ObjectOutputStream out = this.server.serverOutputs.get(incomingServerIndex);
                    this.server.incrementTimeStamp();
                    Message tempMsg = new Message(message.getMessageID(), "ENQUEUEREPLY", message.getRequest(), this.server.getTimeStamp());
                    Thread.sleep(250);
                    out.writeObject(tempMsg);
                    System.out.println("send enqueue reply");
                }
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
            else if (message.getMessage().equals("ENQUEUEREPLY")){
                System.out.println(message.getMessageID() + "receive enqueue reply");
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
                System.out.println(message.getMessageID() + "receive client release");
                Message serverRelease = new Message(message.getMessageID(), "SRELEASE", message.getRequest());
                for (Message msg: this.server.getLamportQueue()) {
                    if (msg.getMessageID() == message.getMessageID()) {
                        this.server.getLamportQueue().remove(msg);
                        System.out.println("found msg in client release");
                        break;
                    }
                }
                try {
                    for (int i = 0; i < this.server.getTotalNumOfServers(); i++) {  // spreads word to all other servers about this new request
                        if (i == this.server.getMyIndex()) continue;
                        ObjectOutputStream out = this.server.serverOutputs.get(i);
                        this.server.incrementTimeStamp();
                        serverRelease.setTimeStamp(this.server.getTimeStamp());
                        Thread.sleep(250);
                        out.writeObject(serverRelease);
                    }
                }
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
            else if(message.getMessage().equals("SRELEASE")) {
                System.out.println(message.getMessageID() + "receive server release");
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

//class LamportThread extends Thread {
//    private MyServer server;
//
//    LamportThread(MyServer server){
//        this.server = server;
//    }
//
//    public void run() {
//        while (true) {
//            if (this.server.getLamportQueue().size() == 0) continue;
//
//        }
//    }
//}

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
            int clientID = this.server.clientNum++;
            this.server.clientOutputs.put(clientID, output);
            while (true) {
                Message msg = (Message)input.readObject();
                if (msg == null) continue;
                if (msg.getRequest() != null) {
                    msg.getRequest().setClientID(clientID);
                }
                if (msg.getMessage().equals("REQUEST") ||
                         msg.getMessage().equals("CRELEASE") ||
                         msg.getMessage().equals("SRELEASE") ||
                         msg.getMessage().equals("ENQUEUE") ||
                         msg.getMessage().equals("ENQUEUEREPLY")){
                    Thread.sleep(100);
                    this.server.getMessageBuffer().offer(msg);
                }
                else if (msg.getMessage().equals("DISCONNECT")) {
                    socket.close();
                    return;
                }
            }
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}