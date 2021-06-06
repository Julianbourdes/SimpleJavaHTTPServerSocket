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

    protected static byte[] bytesToArray(int nbByte, DataInputStream data) throws IOException {
        byte[] res = new byte[nbByte];

        for (int i = 0; i < nbByte-1;i++ ) {
           res[i] = data.readByte();
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

            byte[] masks = frame.getMaskingKey();

            for (int i=0; i < data.length; i++){
                int currentMask = Byte.toUnsignedInt(masks[i % 4]);
                data[i] = (byte) (data[i]|currentMask);
                System.out.println(currentMask);
                System.out.println(String.format("%8s", Integer.toBinaryString(currentMask & 0xFF)).replace(' ', '0'));
            }
        }


        return data;
    }

}
