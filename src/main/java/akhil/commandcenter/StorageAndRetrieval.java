package akhil.commandcenter;

import java.util.Base64;
import java.util.Random;

public class StorageAndRetrieval {
	public StorageAndRetrieval() {
	}

	private static Random rand;

	public static String toKeep(String value) {
		if (value == null)
			return null;
		else {
			String toBeEncoded = value;
			String key = getKey();

			int count = toBeEncoded.length();

			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < count; i++) {
				sb.append(key.charAt(i));
				sb.append(toBeEncoded.charAt(i));
			}
			sb.append(key.charAt(count));

			toBeEncoded = sb.toString();

			return Base64.getEncoder().encodeToString(toBeEncoded.getBytes());
		}
	}

	public static String toUse(String encodedString) {
		if (encodedString == null)
			return null;
		{
			byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
			String decodedString = new String(decodedBytes);

			StringBuilder sb1 = new StringBuilder();
			for (int i = 0; i < decodedString.length(); i++) {
				if (i % 2 != 0) {
					sb1.append(decodedString.charAt(i));
				}
			}
			return sb1.toString();
		}
	}

	private static String getKey() {
		String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJLMNOPQRSTUVWXYZ1234567890";
		StringBuilder result = new StringBuilder();
		int x = 200;
		while (x > 0) {
			rand = new Random();
			result.append(characters.charAt(rand.nextInt(characters.length())));
			x--;
		}
		return result.toString();
	}

}
