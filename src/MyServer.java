import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.io.IOException;
import java.io.File;
import java.util.*;


public class MyServer {
    private int myIndex;
    private int totalNumOfServers;
    private int myPort;
    private List<ObjectOutputStream> outputStreams;
    private List<ObjectInputStream> inputStreams;

    public MyServer(int totalNumOfServers, int myIndex) {
        this.myIndex=myIndex;
        this.totalNumOfServers=totalNumOfServers;
        outputStreams = new ArrayList<>();
        inputStreams = new ArrayList<>();
    }

    public void start() {
        String serverInfoFile = "server_list.txt";
        File file = new File(serverInfoFile);
        Scanner fileScanner;
        try {
            fileScanner = new Scanner(file);    //read the file
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
        List<String[]> otherServers = new ArrayList<>();
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

        //Client Thread starts, for connecting to other servers
        ClientThread clientThread = new ClientThread(this.outputStreams, this.inputStreams, otherServers);
        clientThread.start();
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

        while (true) {
            try {
                Socket sock = serverSock.accept();      //listen
                ObjectOutputStream output = new ObjectOutputStream(sock.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(sock.getInputStream());
                String result = (String) input.readObject();
                System.out.println(result);             //print the input from other servers
            }
            catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(System.err);
                return;
            }
        }
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

class ClientThread extends Thread {
    private List<ObjectOutputStream> outputStreams;
    private List<ObjectInputStream> inputStreams;
    private List<String[]> otherServers;

    public ClientThread(List<ObjectOutputStream> outputStreams, List<ObjectInputStream> inputStreams, List<String[]> otherServers) {
        this.outputStreams = outputStreams;
        this.inputStreams = inputStreams;
        this.otherServers = otherServers;
    }

    public void run(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter to connect to other servers...");
        scanner.nextLine();
        connectOtherSevers(this.otherServers);

        try {
            this.outputStreams.get(0).writeObject(new String("111"));       // testing, output to other servers
        }
        catch (Exception e) {
            System.err.println("Failed to output" + "\nError: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void connectOtherSevers(List<String[]> otherServers) {
        for (String[] server: otherServers) {
            String serverName = server[0];
            int port = Integer.parseInt(server[1]);
            try {
                Socket socket = new Socket(server[0], Integer.parseInt(server[1]));
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                this.outputStreams.add(output);
                this.inputStreams.add(input);
                System.out.println("Connected to " + serverName + " on port " + port);
            }
            catch (Exception e) {
                System.err.println("Failed to connect to " + serverName + " on port " + port + "\nError: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }
}