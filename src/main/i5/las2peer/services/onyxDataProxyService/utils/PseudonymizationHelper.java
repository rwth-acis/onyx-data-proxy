package i5.las2peer.services.onyxDataProxyService.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PseudonymizationHelper {
	
	public static String pseudonomize(String input) {
		try {
			// getInstance() method is called with algorithm SHA-384
			MessageDigest md = MessageDigest.getInstance("SHA-384");

			// digest() method is called
			// to calculate message digest of the input string
			// returned as array of byte
			byte[] messageDigest = md.digest(input.getBytes());

			// convert byte[] to string
			StringBuilder sb = new StringBuilder();
            for (int i = 0; i < messageDigest.length; i++) {
                sb.append(Integer.toString((messageDigest[i] & 0xff) + 0x100, 16).substring(1));
            }
            
            return sb.toString();
		}

		// For specifying wrong message digest algorithms
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
