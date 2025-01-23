package akhil.commandcenter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Functions {

	private static Date startTime;

	private static Map<String, Date> timestamp = new HashMap<>();
	private static Map<String, String> keyVal = new HashMap<>();

	public static void setStartDate(Date dt) {
		startTime = dt;
	}

	public static void setTimestamp(String timestampname) {
		timestamp.put(timestampname, new Date());
	}

	public static void setParam(String key, String val) {
		keyVal.put(key, val);
	}

	public static void clear() {
		timestamp.clear();
		keyVal.clear();
	}

	public static String substitute(String command) {
		if (command.contains("${startdatetime") && command.contains("}")) {
			List<String> params = getInBetweenFast(command, "${startdatetime", "}", false);
			for (String param : params) {
				String dateFormat = param.replace("${startdatetime", "").replace("}", "").trim();
				dateFormat = dateFormat.substring(1, dateFormat.length());
				DateFormat dtf = new SimpleDateFormat(dateFormat);
				String strDate = dtf.format(startTime);
				command = command.replace(param, strDate);
			}
		}
		if (command.contains("${currentdatetime") && command.contains("}")) {
			List<String> params = getInBetweenFast(command, "${currentdatetime", "}", false);
			for (String param : params) {
				String dateFormat = param.replace("${currentdatetime", "").replace("}", "").trim();
				dateFormat = dateFormat.substring(1, dateFormat.length());
				DateFormat dtf = new SimpleDateFormat(dateFormat);
				String strDate = dtf.format(new Date());
				command = command.replace(param, strDate);
			}
		}
		if (command.contains("${timestamp") && command.contains("}")) {
			List<String> params = getInBetweenFast(command, "${timestamp", "}", false);
			for (String param : params) {
				String nameDateFormat = param.replace("${timestamp", "").replace("}", "").trim();
				List<String> nameAndDateFormat = StringOps
						.fastSplit(nameDateFormat.substring(1, nameDateFormat.length()), ":");

				DateFormat dtf = new SimpleDateFormat(nameAndDateFormat.get(1));
				String strDate = dtf.format(timestamp.get(nameAndDateFormat.get(0)));
				command = command.replace(param, strDate);
			}
		}
		for (Map.Entry<String, String> entry : keyVal.entrySet())
		{
			String paramDef = "${"+entry.getKey()+"}";
			if (command.contains(paramDef)) {
				command = command.replace(paramDef, entry.getValue());
			}
		}
		return command;
	}

	public static List<String> getInBetweenFast(String str, String startPattern, String endPattern,
			boolean excludeStartEndPattern) {
		List<String> toReturn = new ArrayList<>();
		int index = 0;
		int is;
		int ie;

		while (index <= str.length() && str.indexOf(startPattern, index) >= 0 && str.indexOf(endPattern, index) >= 0) {

			if (excludeStartEndPattern) {
				is = str.indexOf(startPattern, index) + startPattern.length();
				ie = str.indexOf(endPattern, is);
				index = ie + +endPattern.length();
			} else {
				is = str.indexOf(startPattern, index);
				ie = str.indexOf(endPattern, is + startPattern.length()) + endPattern.length();
				index = ie;
			}
			if (ie < 0 || is < 0)
				break;

			toReturn.add(str.substring(is, ie));

		}
		return toReturn;
	}
}
