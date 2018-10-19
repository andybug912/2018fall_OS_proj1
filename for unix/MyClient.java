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
        File file = new File(inputFile);
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
System.out.println(newNumber );
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

    public static void main(String[] args) {
		/*try {
            int sleep = 3000;
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        if (!args[2].equals("y")) {
            MyClient client = new MyClient(Integer.parseInt(args[0]), Integer.parseInt(args[1]), false, args[3], Integer.parseInt(args[4]), Integer.parseInt(args[5]));
            client.start();
        }
        else {
            MyClient client = new MyClient(Integer.parseInt(args[0]), Integer.parseInt(args[1]), true);
            client.start();
        }
    }
}
