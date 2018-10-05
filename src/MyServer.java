import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
            fileScanner = new Scanner(file);
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
                this.myPort = Integer.parseInt(serverInfo[1]);
            }
            else {
                otherServers.add(serverInfo);
            }
            i++;
        }
        fileScanner.close();
        // TODO (Zhiben Zhu): add serverSocket
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter to connect to other servers...");
        scanner.nextLine();
        connectOtherSevers(otherServers);


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

    public static void main(String[] args) throws IOException{
        Scanner scanner = new Scanner(System.in);
        System.out.println("Input total numbers of servers:");
        String tnfs = scanner.nextLine();

        Scanner scanner2 = new Scanner(System.in);
        System.out.println("Input myIndex:");
        String mi = scanner2.nextLine();

        MyServer server = new MyServer(Integer.parseInt(tnfs),Integer.parseInt(mi));
        server.start();
    }


}
