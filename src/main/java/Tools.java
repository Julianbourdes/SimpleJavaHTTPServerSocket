import java.io.DataInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Tools {

    /**
     * Encrypt a provided data byte with Sha-1 algorithm
     * @param data byte table to encrypt
     * @return the encrypted data byte provided with sha-1 algorithm
     */
    protected static byte[] toSHA1(final byte[] data) {
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

    /**
     * Convert extracted nbByte byte from an input stream and convert them
     * @param nbByte the number of bytes wanted
     * @param data the input stream provided
     * @return nbByte bytes from input stream in a tab
     * @throws IOException
     */
    protected static int[] bytesToArray(int nbByte, DataInputStream data) throws IOException {
        int[] res = new int[nbByte];

        for (int i = 0; i < nbByte;i++ ) {
           res[i] = data.readByte() & 0xff;
        }

        return res;
    }

    /**
     * Unmask the payload of the provided frame
     * @param frame the frame
     * @return the unmask payload of the provided frame treated
     */
    protected static byte[] unMaskPayload(Frame frame){
        byte[] data = frame.getPayload();

        // If there is a mask
        if(frame.getMask() == 1){
            int[] masks = frame.getMaskingKey();

            // Foreach payload byte treated, we will used the correct byte mask key associate to unmask it
            for (int i=0; i < data.length; i++){
                // Compared to the 4 bytes composing the maskingkey, we will choose the good one thanks to the iteration
                int currentMask = (byte)masks[i % 4];

                // Use an XOR operation between and the good part of the mask to decode the payload
                data[i] = (byte) (data[i]^currentMask);

                // Display the payload in the console only when the opcode is in text mode
                if (frame.getOpcode() == Frame.Opcode.text.getCode() || frame.getOpcode() == Frame.Opcode.binary.getCode())
                    System.out.println("Payload ["+i+"] :"+data[i] + " : "+(char)data[i] );
            }
        }


        return data;
    }

    /**
     * Concat nbByte from an input stream into an int
     * @param nbByte the number of byte to concat
     * @param data the input stream provided
     * @return the concatenation of nbByte as an int
     * @throws IOException
     */
    public static int concatByteToInt(int nbByte, DataInputStream data) throws IOException {
        int res = data.readByte() & 0xff;
        int tmp;

        for (int i = 1; i < nbByte;i++ ) {
            tmp = data.readByte() & 0xff;
            res <<= 8;
            res |= tmp;
        }

        return res;
    }
}
