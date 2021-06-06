import javax.crypto.spec.PSource;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HTTPRequestHandler implements Runnable {
    /*
    Websocket protocol

     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-------+-+-------------+-------------------------------+
    |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
    |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
    |N|V|V|V|       |S|             |   (if payload len==126/127)   |
    | |1|2|3|       |K|             |                               |
    +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
    |     Extended payload length continued, if payload len == 127  |
    + - - - - - - - - - - - - - - - +-------------------------------+
    |                               |Masking-key, if MASK set to 1  |
    +-------------------------------+-------------------------------+
    | Masking-key (continued)       |          Payload Data         |
    +-------------------------------- - - - - - - - - - - - - - - - +
    :                     Payload Data continued ...                :
    + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
    |                     Payload Data continued ...                |
    +---------------------------------------------------------------+
    */

    final Socket clientSocket;
    private static final byte[] NO_FILES = {};
    private String magicKey;
    private BufferedReader reader;
    private DataInputStream websocketpipe; // Provide features to treat byte

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
        responseHeaders += "Sec-WebSocket-Accept: " + this.magicKey +"\r\n";
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
            this.clientSocket.getOutputStream().write(wsResponse.getBytes(StandardCharsets.UTF_8));
            // Ecoute constante après envoie du handshake et de l'entête Websocket
            System.out.println("Prepare treatment of a websocket trame");
            // Récupération du flux en provenance du websocket
            this.websocketpipe = new DataInputStream(this.clientSocket.getInputStream());
            // Treatment of the websocket
            while(true) {
                this.treatmentTrame();
            }
        } else {
            System.out.println("Prepare http request");
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
                System.out.println("Header start display : \n" + s + "\nHeader end display\n");
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
        }
        return NO_FILES;
    }

    private boolean isWebSocket() throws IOException, NoSuchAlgorithmException {
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
                        if (splitLine[1].contains("Upgrade")) {
                            map.replace("Connection", true);
                        }
                        break;
                    case "Sec-WebSocket-Key":
                        map.replace("Sec-WebSocket-Key", true);
                        this.magicKey = Base64
                                .getEncoder()
                                .encodeToString(Tools.toSHA1(
                                        splitLine[1]
                                                .concat("258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                                .getBytes()
                                        )
                                );
                        break;
                }
            }
        }
        return !map.containsValue(false);
    }

    private void treatmentTrame() throws IOException {
        // Declarate buffer
        // We will treat byte by byte
        try{


        System.out.println("TEST");
        Frame frame = new Frame(this.websocketpipe);
        frame.setPayload(Tools.unMaskPayload(frame));
        frame.displayMessage();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
