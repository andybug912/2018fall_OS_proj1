import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class MyClient {
    private String serverInfoFile = "server_list.txt";
    private int serverIndex;
    private int requestID;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    public MyClient(int serverIndex, int initialRequestID) {
        this.serverIndex = serverIndex;
        this.requestID = initialRequestID;
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
        while (true) {
            try {
                System.out.println("Enter to continue...");
                scanner.nextLine();
                Message message = new Message("REQUEST", new Request(this.requestID++, this.serverIndex, 0, true));
                output.writeObject(message);
                Message response = (Message) input.readObject();
                System.out.println(response.getMessage());
            }
            catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }

    public static void main(String[] argv) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Input your daddy server:");
        int daddy = Integer.parseInt(scanner.nextLine());

        System.out.println("Input an initial request ID:");
        int initialRequestID = Integer.parseInt(scanner.nextLine());
        MyClient client = new MyClient(daddy, initialRequestID);
        client.start();
    }
}
