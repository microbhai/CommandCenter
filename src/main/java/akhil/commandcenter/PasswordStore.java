package akhil.commandcenter;

import java.util.HashMap;
import java.util.Map;

public class PasswordStore {
	
	private static Map<String, String> hm = new HashMap<>();
	
	public static void setPwd(String user, String pwd)
	{
		if (hm.containsKey(user))
			hm.remove(user);
		hm.put(user, pwd);
	}
	
	public static String getPwd(String user)
	{
		if (hm.containsKey(user))
		return hm.get(user);
		else
			return null;
	}

}
