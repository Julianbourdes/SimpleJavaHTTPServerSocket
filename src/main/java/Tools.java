import java.io.DataInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Tools {
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

    protected static int[] bytesToArray(int nbByte, DataInputStream data) throws IOException {
        int[] res = new int[nbByte];

        for (int i = 0; i < nbByte;i++ ) {
           res[i] = data.readByte() & 0xff;
        }

        return res;
    }

    // Function to extract k bits from p position
    // and returns the extracted value as integer
    protected static int bitExtracted(int number, int k, int p)
    {
        return (((1 << k) - 1) & (number >> (p - 1)));
    }

    protected static byte[] unMaskPayload(Frame frame){

        byte[] data = frame.getPayload();

        if(frame.getMask() == 1){

            int[] masks = frame.getMaskingKey();

            for (int i=0; i < data.length; i++){
                int currentMask = (byte)masks[i % 4];
                data[i] = (byte) (data[i]^currentMask);
                if (frame.getOpcode() == Frame.Opcode.text.getCode() || frame.getOpcode() == Frame.Opcode.binary.getCode()) System.out.println("Payload ["+i+"] :"+data[i] + " : "+(char)data[i] );
            }
        }


        return data;
    }

}
