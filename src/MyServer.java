import java.util.Scanner;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class MyServer {
    private int myIndex;
    private int totalNumOfServers;

    public MyServer(int totalNumOfServers, int myIndex) {
        this.myIndex=myIndex;
        this.totalNumOfServers=totalNumOfServers;
    }

    public void start() {
        String serverInfo = "serverlist.txt";

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
