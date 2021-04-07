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
            this.dispatchResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String generateHttpResponse(String message) throws UnsupportedEncodingException {
        //  Calcul de la taille (en octet) de la charge utile de la reponse
        int length = message.getBytes("UTF-8").length;
        //  Génération des entetes de la reponse
        String responseHeaders = "";
        responseHeaders += "HTTP/1.1 200 OK\r\n";
        responseHeaders += "Content-Type: text/html; charset=utf-8\r\n";
        responseHeaders += "Content-Length: " + length +"\r\n";
        responseHeaders += "\r\n";

        return responseHeaders + message;
    }


    public void dispatchResponse() throws IOException {
        System.out.println("Displatch response");
        //  Generate response
        String welcomeMsg = "Traitement de la requete coté serveur à " + new Date().toString();
        String httpResponse = this.generateHttpResponse(welcomeMsg);
        //  Send response
        this.clientSocket.getOutputStream().write(httpResponse.getBytes("UTF-8"));
        //  Close socket (sinon le client reste en attente de données)
        this.clientSocket.close();
    }

}
