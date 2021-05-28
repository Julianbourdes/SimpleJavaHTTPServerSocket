import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HTTPRequestHandler implements Runnable {

    final Socket clientSocket;
    private static final byte[] NO_FILES = {};
    private String magicKey;
    private BufferedReader reader;

    public HTTPRequestHandler(Socket s) {
        this.clientSocket = s;
    }

    @Override
    public void run() {

        try {
            System.out.println("New thread started to respond to client request");
            byte[] content = this.readHeader();
            this.dispatchResponse(content);
            reader.close();
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public String generateHttpResponse(byte[] content) throws UnsupportedEncodingException {

        // Calcul of the size (in bytes) of the response's useful charge
        String message = new String(content);
        int length = message.length();
        //  Generation of response's headers
        String responseHeaders = "";

        if (content.length == 0)
            responseHeaders += "HTTP/1.1 404 NOT FOUND\r\n";
        else
            responseHeaders += "HTTP/1.1 200 OK\r\n";


        responseHeaders += "Content-Type: text/html; charset=utf-8\r\n";
        responseHeaders += "Content-Length: " + length + "\r\n";
        responseHeaders += "\r\n";

        return responseHeaders + message;
    }

    public String generateWebSoResponse() throws UnsupportedEncodingException {
        //  Generation of response's headers
        String responseHeaders = "";
        responseHeaders += "HTTP/1.1 101 Switching Protocols\r\n";
        responseHeaders += "Upgrade: websocket\r\n";
        responseHeaders += "Connection: Upgrade\r\n";
        responseHeaders += "Sec-Websocket-Accept: " + this.magicKey+"\r\n";
        responseHeaders += "Content-Type: text/html; charset=utf-8\r\n";
        responseHeaders += "\r\n";

        return responseHeaders;
    }

    public void dispatchResponse(byte[] content) throws IOException, NoSuchAlgorithmException {
        System.out.println("Displatch response");

        // Response HTTP
        if (isWebSocket()) {
            System.out.println("Prepare websocket");
            String wsResponse = this.generateWebSoResponse();
            System.out.println(wsResponse);
            this.clientSocket.getOutputStream().write(wsResponse.getBytes(StandardCharsets.UTF_8));

        } else {
            //  Generate
            String httpResponse = this.generateHttpResponse(content);
            //  Send response
            this.clientSocket.getOutputStream().write(httpResponse.getBytes(StandardCharsets.UTF_8));
            //  Close socket (otherwise the client remains waiting for data)
            this.clientSocket.close();
        }
    }

    public byte[] readHeader() throws IOException {

        this.reader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
        String s = "";

        // Read the message from the client
        if((s = reader.readLine()) != null && s.length() != 0) {
            // To treat GET method request, I check if the message begins with GET
            if (s.startsWith("GET")) {
                System.out.println("Header start display : \n" + s + "Header end display\n");
                // The answer should be with the form "GET <path> HTTP/1.1"
                // So we want to get the path, we use split function and get it at the 1 index
                String[] answer = s.split("\\s+");
                // Check if the path is equals to / to avoid an error for opened an unknown file
                if (!answer[1].equals("/")) {
                    // Remove / character
                    File file = new File(answer[1].substring(1));
                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] content = new byte[(int) file.length()];

                    while (fileInputStream.available() > 0) {
                        fileInputStream.read(content);
                    }

                    return content;
                } else {
                    return NO_FILES;
                }
            }
            else {
                // Web socket treatment
                // FF2A --> Utiliser le input stream pour lire octet par octet
            }
        }
        return NO_FILES;
    }

    private boolean isWebSocket() throws IOException, NoSuchAlgorithmException {
        System.out.println("IsWebsocket ?");
        HashMap<String, Boolean> map = new HashMap<>();

        map.put("Upgrade", false);
        map.put("Connection", false);
        map.put("Sec-WebSocket-Key", false);

        String line = "";

        while ((line = this.reader.readLine()) != null && line.length() != 0) {


            String[] splitLine;
            splitLine = line.split("\\s*:\\s*");

            if (map.containsKey(splitLine[0])) {
                switch (splitLine[0]) {
                    case "Upgrade":
                        if (splitLine[1].equals("websocket")) {
                            map.replace("Upgrade", true);
                        }
                        break;
                    case "Connection":
                        if (splitLine[1].equals("Upgrade")) {
                            map.replace("Connection", true);
                        }
                        break;
                    case "Sec-WebSocket-Key":
                        map.replace("Sec-WebSocket-Key", true);
                        this.magicKey = Base64.getEncoder().encodeToString(toSHA1(splitLine[1].concat("258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes()));

                        break;
                }

            }
        }
        return !map.containsValue(false);
    }


    private static byte[] toSHA1(final byte[] data) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        }
        catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        assert md != null;
        return md.digest(data);

    }

}
