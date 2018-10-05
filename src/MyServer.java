import java.util.Scanner;
import java.io.IOException;
import java.io.File;
import java.util.*;


public class MyServer {
    private int myIndex;
    private int totalNumOfServers;
    private int myPort;

    public MyServer(int totalNumOfServers, int myIndex) {
        this.myIndex=myIndex;
        this.totalNumOfServers=totalNumOfServers;
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
        List<List<String[]>> otherServers = new ArrayList<>();
        int i = 0;
        while (fileScanner.hasNext() && i < this.totalNumOfServers) {
            String[] serverInfo = fileScanner.nextLine().split(" ");
            if (i == this.myIndex) {
                this.myPort = Integer.parseInt(serverInfo[1]);
            }
            else {
                List<String[]> tempList = new ArrayList<>();
                tempList.add(serverInfo);
                otherServers.add(tempList);
            }
            i++;
        }
        // TODO (Zhiben Zhu): add serverSocket
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
