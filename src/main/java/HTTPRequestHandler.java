import java.io.*;
import java.net.Socket;
import java.util.*;

public class HTTPRequestHandler implements Runnable {

    final Socket clientSocket;

    public HTTPRequestHandler(Socket s) {
        this.clientSocket = s;
    }

    @Override
    public void run() {

        try {
            System.out.println("New thread started to respond to client request");
            byte[] content = this.readHeader();
            this.dispatchResponse(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String generateHttpResponse(byte[] content) throws UnsupportedEncodingException {
        //  Calcul de la taille (en octet) de la charge utile de la reponse
        String message = new String(content);
        int length = message.length();
        //  Génération des entetes de la reponse
        String responseHeaders = "";
        responseHeaders += "HTTP/1.1 200 OK\r\n";
        responseHeaders += "Content-Type: text/html; charset=utf-8\r\n";
        responseHeaders += "Content-Length: " + length +"\r\n";
        responseHeaders += "\r\n";

        return responseHeaders + message;
    }


    public void dispatchResponse(byte[] content) throws IOException {
        System.out.println("Displatch response");
        //  Generate response

        String httpResponse = this.generateHttpResponse(content);
        //  Send response
        this.clientSocket.getOutputStream().write(httpResponse.getBytes("UTF-8"));
        //  Close socket (sinon le client reste en attente de données)
        this.clientSocket.close();
    }

    public byte[] readHeader() throws IOException {
        // Declarate buffer
        byte[] b = new byte[512];
        int n;

        InputStream inputStream = this.clientSocket.getInputStream();
        OutputStream outputStream = this.clientSocket.getOutputStream();
        // Read the message from the client
        while((n = inputStream.read(b)) > 0) {
            // Convert Characters buffer to String
            String s = new String(b);

            // To treat GET method request, I check if the message contains GET
            if(s.startsWith("GET")) {

                // The answer should be with the form "GET <path> HTTP/1.1"
                // So we want to get the path, we use split function and get it at the 1 index
                String[] answer = s.split("\\s+");
                // Remove / character
                File file = new File(answer[1].substring(1));
                FileInputStream fileInputStream = new FileInputStream(file);
//                fileInputStream.read();
                byte[] content = new byte[(int) file.length()];

                while(fileInputStream.available() > 0) {
                    fileInputStream.read(content);
                }

                //String message = new String(content);
                return content;
            }

            // Initialialise buffer
            b = new byte[512];
        }
        return null;
    }

}
