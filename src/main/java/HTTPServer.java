import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.logging.SocketHandler;

public class HTTPServer {
    private static int port = 8089;

    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(port);
        System.out.println("HTTP Server started at http://localhost:"+port);
        while (true) {
            Socket socket = null;
            try {
                socket = server.accept();
                System.out.println("New request from " + socket.getRemoteSocketAddress().toString());
                // New thread for this client
                Thread t = new Thread(new HTTPRequestHandler(socket));
                t.start();
            } catch (Exception e) {
                socket.close();
                e.printStackTrace();
            }
        }
    }
}
