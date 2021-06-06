import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

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
        continuation (0),
        text (1), // Can be segmented
        binary (2), // Can be segmented
        closure (8),
        ping (9),
        pong (10); // Because A = 10 in hexadecimal world

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
    private byte[] maskingKey;
    private byte[] payload;

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

    public byte[] getMaskingKey() {
        return maskingKey;
    }

    public void setMaskingKey(byte[] maskingKey) {
        this.maskingKey = maskingKey;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public Frame(DataInputStream dataInputStream) throws IOException {
        // Read first byte
        byte octet = dataInputStream.readByte();
        this.fin = octet & 0b10000000 >> 7;
        this.rsv1 = 1 << octet & 0b01000000 >> 6;
        this.rsv2 = 2 << octet & 0b00100000 >> 5;
        this.rsv3 = 3 << octet & 0b00010000 >> 4;
        this.opcode = octet & 0b00001111;

        // End treatment first byte
        System.out.println("Test - Fin : "+this.fin+", rsv1 : "+this.rsv1+", rsv2 : "+this.rsv2+", rsv3 : "+this.rsv3+", opcode : "+ this.opcode);

        // Get second octet
        octet = dataInputStream.readByte();
        this.mask = ((Byte.toUnsignedInt(octet) & 0b10000000) >> 7);
        this.payloadLength = (Byte.toUnsignedInt(octet) & 0b01111111); // Non signé

        System.out.println("------mask/payloadLength-----");
        System.out.println("Mask : " + mask);
        System.out.println("Payload : " + payloadLength);
        System.out.println("-----------");

        // Is there and extended payload length or extendedPayloadlengthContinue ?
        if(payloadLength == 126){
            //TODO
            //this.extendedPayloadLength = Tools.concatByteToInt(2, dataInputStream);
            //System.out.println("Extended payload length");
        }
        else if (payloadLength == 127) {
            //TODO
            //this.extendedPayloadLengthContinued = Tools.concatByteToInt(8, dataInputStream);
            //System.out.println("Extended payload length continue : "+ extendedPayloadLengthContinued);
        }

        // Is there a mask ?
        if(this.mask == 1) {
            this.maskingKey = Tools.bytesToArray(4, dataInputStream);
        }

        // Payload
        int lngth = Math.max(Math.max(payloadLength,extendedPayloadLength),extendedPayloadLengthContinued);
        byte[] res = new byte[lngth];
        for (int i=0;i<lngth;i++){
            res[i] = dataInputStream.readByte();
        }
        this.payload = res;
    }

    public void displayMessage() {

        String payload = new String(this.payload);
        System.out.println(payload);
    }

}
