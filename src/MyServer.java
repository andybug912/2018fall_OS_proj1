import java.util.Scanner;

public class MyServer {
    public MyServer(int port) {

    }

    public void start() {
        
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Input server port:");
        String port = scanner.nextLine();
        MyServer server = new MyServer(Integer.parseInt(port));
        server.start();
    }
}
