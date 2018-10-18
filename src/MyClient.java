import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

public class MyClient {
    private String serverInfoFile = "server_list.txt";
    private int serverIndex;
    private int messageID;
    private boolean isRead;
    private Queue<Integer> nums;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    public MyClient(int serverIndex, int initialRequestID, boolean isRead) {
        this.serverIndex = serverIndex;
        this.messageID = initialRequestID;
        this.isRead = isRead;
    }

    public MyClient(int serverIndex, int initialRequestID, boolean isRead, String inputFile, int startNum, int endNum) {
        this.serverIndex = serverIndex;
        this.messageID = initialRequestID;
        this.isRead = isRead;
        this.nums = new LinkedList<>();
        File file = new File("distributed_me_requests.txt");
        Scanner fileScanner;
        try {
            fileScanner = new Scanner(file);    //read the file
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
        int i = 1;
        while (fileScanner.hasNext()) {
            int num = Integer.parseInt(fileScanner.next());
            if (i <= endNum && i >= startNum) {
                this.nums.offer(num);
            }
            if (i > endNum) break;
            i++;
        }
        fileScanner.close();
    }

    public void start() {
        File file = new File(this.serverInfoFile);
        Scanner fileScanner;
        try {
            fileScanner = new Scanner(file);    //read the file
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
        for (int i = 0; fileScanner.hasNext(); i++) {
            if (i == this.serverIndex) {
                String[] serverInfo = fileScanner.nextLine().split(" ");
                try {
                    this.socket = new Socket(serverInfo[0], Integer.parseInt(serverInfo[1]));
                    this.output = new ObjectOutputStream(socket.getOutputStream());
                    this.input = new ObjectInputStream(socket.getInputStream());
                    break;
                }
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace(System.err);
                    return;
                }
            }
            fileScanner.nextLine();
        }
        fileScanner.close();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter to connect to start sending requests ...");
        scanner.nextLine();
        scanner.close();

        int readCount = 0;
        while (true) {
            try {
                Request request;
                if (this.isRead) {
                    if (readCount++ >= 100) {
                        break;
                    }
                    request = new Request(this.serverIndex, true);
                }
                else {
                    if (this.nums.isEmpty()) {
                        break;
                    }
                    int newNumber = this.nums.poll();
                    request = new Request(this.serverIndex, false, newNumber);
                }
                Message message = new Message(this.messageID, "REQUEST", request);
                output.writeObject(message);
                Message response = (Message) input.readObject();
                if (response.getMessage().contains("OK")) {
                    if (message.getRequest().isRead()) {
                        System.out.println(readCount + ": [" + response.getMessage().substring(3) + "]");
                    }
                    else {
                        // write request, do nothing
                    }
                    message = new Message(this.messageID++, "CRELEASE", request);
                    output.writeObject(message);
                }
            }
            catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
        try {
            output.writeObject(new Message("DISCONNECT"));
        }
        catch (Exception e) {

        }
    }

    public static void main(String[] argv) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Input your daddy server:");
        int daddy = Integer.parseInt(scanner.nextLine());
        System.out.println("Input an initial message ID:");
        int initialMessageID = Integer.parseInt(scanner.nextLine());
        System.out.println("Is this a read client? (y/n)");
        boolean isRead = scanner.nextLine().equals("y");
        if (!isRead) {
            System.out.println("start num index");
            int start = Integer.parseInt(scanner.nextLine());
            System.out.println("end num index");
            int end = Integer.parseInt(scanner.nextLine());
            MyClient client = new MyClient(daddy, initialMessageID, isRead, "", start, end);
            client.start();
        }
        else {
            MyClient client = new MyClient(daddy, initialMessageID, isRead);
            client.start();
        }
    }
}
