import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Random;

public class Frame {
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

    enum Opcode {
        continuation(0),
        text(1), // Can be segmented
        binary(2), // Can be segmented
        closure(8),
        ping(9),
        pong(10); // Because A = 10 in hexadecimal world

        private int code;

        Opcode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * If 0 : wait other frame
     * If 1 : end
     */
    private int fin;

    // Thanks to this documentation, we know that we can ignored this bytes. They will be initialized to 0
    // https://developer.mozilla.org/fr/docs/Web/API/WebSockets_API/Writing_WebSocket_servers
    private int rsv1;
    private int rsv2;
    private int rsv3;
    private int opcode;
    private int mask; // If 1 -> there is a mask, 0 -> there is no mask
    private int payloadLength;
    private int extendedPayloadLength = 0;
    private int extendedPayloadLengthContinued = 0;
    private int[] maskingKey;
    private byte[] payload;

    // Getter and setter

    public int getFin() {
        return fin;
    }

    public void setFin(int fin) {
        this.fin = fin;
    }

    public int getRsv1() {
        return rsv1;
    }

    public void setRsv1(int rsv1) {
        this.rsv1 = rsv1;
    }

    public int getRsv2() {
        return rsv2;
    }

    public void setRsv2(int rsv2) {
        this.rsv2 = rsv2;
    }

    public int getRsv3() {
        return rsv3;
    }

    public void setRsv3(int rsv3) {
        this.rsv3 = rsv3;
    }

    public int getOpcode() {
        return opcode;
    }

    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public void setPayloadLength(int payloadLength) {
        this.payloadLength = payloadLength;
    }

    public int getExtendedPayloadLength() {
        return extendedPayloadLength;
    }

    public void setExtendedPayloadLength(int extendedPayloadLength) {
        this.extendedPayloadLength = extendedPayloadLength;
    }

    public int getExtendedPayloadLengthContinued() {
        return extendedPayloadLengthContinued;
    }

    public void setExtendedPayloadLengthContinued(int extendedPayloadLengthContinued) {
        this.extendedPayloadLengthContinued = extendedPayloadLengthContinued;
    }

    public int[] getMaskingKey() {
        return maskingKey;
    }

    public void setMaskingKey(int[] maskingKey) {
        this.maskingKey = maskingKey;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    /**
     * Contructor from a inputStream
     * @param dataInputStream
     * @throws IOException
     */
    public Frame(DataInputStream dataInputStream) throws IOException {
        // Read first byte
        byte octet = dataInputStream.readByte();
        this.fin = ((Byte.toUnsignedInt(octet) & 0b10000000) >> 7);
        this.rsv1 = 1 << octet & 0b01000000 >> 6;
        this.rsv2 = 2 << octet & 0b00100000 >> 5;
        this.rsv3 = 3 << octet & 0b00010000 >> 4;
        this.opcode = octet & 0b00001111;

        // End treatment first byte
        System.out.println("Fin : " + this.fin);
        System.out.println("RSV1 : " + this.rsv1 + ", RSV2 : " + this.rsv2 + ", RSV3 : " + this.rsv3);
        System.out.println("Opcode : " + this.opcode);

        // Get second octet
        octet = dataInputStream.readByte();
        this.mask = ((Byte.toUnsignedInt(octet) & 0b10000000) >> 7);
        this.payloadLength = (Byte.toUnsignedInt(octet) & 0b01111111); // Non signé

        System.out.println("Mask : " + mask);
        System.out.println("PayloadLength : " + payloadLength);

        // Is there and extended payload length or extendedPayloadlengthContinue ?
        if (payloadLength == 126) {
            this.extendedPayloadLength = Tools.concatByteToInt(2, dataInputStream);
            System.out.println("extendedPayloadLength : " + extendedPayloadLength);
        } else if (payloadLength == 127) {
            this.extendedPayloadLengthContinued = Tools.concatByteToInt(8, dataInputStream);
            System.out.println("extendedPayloadLengthContinued : "+ extendedPayloadLengthContinued);
        }

        // Is there a mask ?
        if (this.mask == 1) {
            this.maskingKey = Tools.bytesToArray(4, dataInputStream);
            System.out.println("MaskingKey 1:" + maskingKey[0]);
            System.out.println("MaskingKey 2:" + maskingKey[1]);
            System.out.println("MaskingKey 3:" + maskingKey[2]);
            System.out.println("MaskingKey 4:" + maskingKey[3]);
        }

        // Payload
        int lngth = Math.max(Math.max(payloadLength, extendedPayloadLength), extendedPayloadLengthContinued);
        byte[] res = new byte[lngth];
        for (int i = 0; i < lngth; i++) {
            res[i] = dataInputStream.readByte();
        }
        this.payload = res;
    }

    /**
     * Manual constructor to send a message
     * @param fin
     * @param rsv1
     * @param rsv2
     * @param rsv3
     * @param opcode
     * @param mask
     * @param payload
     */
    public Frame(int fin, int rsv1, int rsv2, int rsv3, int opcode, int mask, byte[] payload) {
        this.fin = fin;
        this.rsv1 = rsv1;
        this.rsv2 = rsv2;
        this.rsv3 = rsv3;
        this.opcode = opcode;
        this.mask = mask;
        this.payload = payload;
        this.extendedPayloadLength = 0;
        this.extendedPayloadLengthContinued = 0;
        setLengthAndMask();
    }

    /**
     * Display the message
     */
    public void displayMessage() {
        String payload = new String(this.payload);
        System.out.println(payload);
    }

    /**
     * Set parameters to create a ping frame
     * @return return the ping Frame
     */
    public static Frame createPingFrame() {
        Date date = new Date();
        long currentTime = date.getTime();
        return new Frame(1, 0, 0, 0, Opcode.ping.getCode(), 0, (Long.toString(currentTime)).getBytes());
    }

    /**
     * Send a frame via Websocket protocol
     * @param socket the client socket
     * @throws IOException
     */
    public void send(Socket socket) throws IOException {
        System.out.println("----------------------------------");
        System.out.println("Request sending ....");

        setLengthAndMask();

        int requestLngth = 2;

        // Setting the length according to supplementary possible elements
        if (extendedPayloadLengthContinued > 0){
            requestLngth += 8 + extendedPayloadLengthContinued;
        }else if (extendedPayloadLength > 0){
            requestLngth += 2 + extendedPayloadLength;
        } else {
            requestLngth += payloadLength;
        }
        if(mask == 1){
            requestLngth += 4;
        }

        byte[] request = new byte[requestLngth];
        int pos = 0;

        // Building correct format for the frame
        // Assembling int informations from the different at different position to create a byte
        request[pos] = (byte)((((((int)0 << 1 | fin)<<1|rsv1)<<1|rsv2)<<1|rsv3)<<4|opcode);
        pos++;

        System.out.println("Fin : " + this.fin);
        System.out.println("RSV1 : " + this.rsv1 + ", RSV2 : " + this.rsv2 + ", RSV3 : " + this.rsv3);
        System.out.println("Opcode : " + this.opcode);

        request[pos] = (byte)( (((int)0 << 1 | mask)<<7|payloadLength) );
        pos++;

        System.out.println("Mask : " + mask);
        System.out.println("PayloadLength : " + payloadLength);

        if(extendedPayloadLength>0) {
            request[pos] = (byte)( ((int)0 << 16 | extendedPayloadLength) );
            pos++;
            System.out.println("Extended payload length : "+ extendedPayloadLength);
        }

        if(extendedPayloadLengthContinued>0) {
            request[pos] = (byte)( ((int)0 << 64 | extendedPayloadLengthContinued) );
            pos++;
            System.out.println("Extended payload length continued : "+ extendedPayloadLengthContinued);
        }

        if(mask == 1) {
            request[pos] = (byte)( ((int)0 << 8 | maskingKey[0]) );
            pos++;
            request[pos] = (byte)( ((int)0 << 8 | maskingKey[1]) );
            pos++;
            request[pos] = (byte)( ((int)0 << 8 | maskingKey[2]) );
            pos++;
            request[pos] = (byte)( ((int)0 << 8 | maskingKey[3]) );
            pos++;
            System.out.println("MaskingKey 1:" + maskingKey[0]);
            System.out.println("MaskingKey 2:" + maskingKey[1]);
            System.out.println("MaskingKey 3:" + maskingKey[2]);
            System.out.println("MaskingKey 4:" + maskingKey[3]);
        }

        int payloadLngth = Math.max(Math.max(payloadLength, extendedPayloadLength), extendedPayloadLengthContinued);
        for (int i = 0; i < payloadLngth; i++) {
            if(mask == 0) request[pos] = payload[i];
            else request[pos] = (byte)(payload[i] ^ maskingKey[i%4]);
            pos++;
            if (opcode == Opcode.text.getCode() || opcode == Opcode.binary.getCode() || opcode == Opcode.continuation.getCode())
                System.out.println("Payload ["+i+"] :"+payload[i]+ " : "+(char)payload[i]);
        }

        socket.getOutputStream().write(request);
        System.out.println("Request send !");
        System.out.println("----------------------------------");
    }

    /**
     * Set the length and the mask for the current Frame
     */
    private void setLengthAndMask(){
        int lngth = payload.length;
        if (lngth < 126) this.payloadLength = lngth;
        else if (lngth <= Math.pow(2, 16)) {
            this.payloadLength = 126;
            this.extendedPayloadLength = lngth;
        } else {
            this.payloadLength = 127;
            this.extendedPayloadLengthContinued = lngth;
        }
        if (mask == 1) {
            Random r = new Random();
            this.maskingKey = new int[]{r.nextInt((255) + 1), r.nextInt((255) + 1), r.nextInt((255) + 1), r.nextInt((255) + 1)};
        }
    }

    /**
     * Create a segmented frame composed of 2 frames
     * @param socket
     * @throws IOException
     * @throws InterruptedException
     */
    public static void createSegmentedRequest(Socket socket) throws IOException, InterruptedException {
       Frame frame1 =  new Frame(
               0,
               0,
               0,
               0,
               Opcode.text.getCode(),
               0,
               new String("Part1").getBytes()
       );

       Frame frame2 =  new Frame(
               1,
               0,
               0,
               0,
               Opcode.continuation.getCode(),
               0,
               new String("Part2").getBytes()
       );

       frame1.send(socket);
       Thread.sleep(20000);
       frame2.send(socket);
    }
}
