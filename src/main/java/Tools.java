import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
}
