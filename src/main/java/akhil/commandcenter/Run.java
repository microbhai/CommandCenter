package akhil.commandcenter;

import java.io.BufferedReader;

import java.io.InputStream;
import java.io.InputStreamReader;

import com.jcraft.jsch.ChannelExec;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class Run {

	private static Map<String, ExecutorService> esm = new HashMap<>();
	private static Map<String, List<Integer>> rendezvous = new HashMap<>();
	private static Map<String, List<Integer>> rendezvousCheck = new HashMap<>();
	private static Map<String, Integer> hostCount = new HashMap<>();
	private static Map<String, Boolean> syncLock = new HashMap<>();

	public static void stop() {
		for (ExecutorService es : esm.values())
			es.shutdownNow();
		esm.clear();
		rendezvous.clear();
		rendezvousCheck.clear();
		hostCount.clear();
		Functions.clear();
		syncLock.clear();
	}

	public static void stop(String key) {

		if (esm.containsKey(key)) {
			ExecutorService es = esm.get(key);
			if (es != null && !es.isTerminated())
				es.shutdownNow();
		}
		esm.remove(key);

	}

	public static void unlockSync(String name) {
		if (syncLock.containsKey(name))
			synchronized (syncLock) {
				syncLock.put(name, false);
			}
		else
			UI.writeToOutput("Sync point not found in the lock list");
	}

	public static Map<String, List<String>> processFile(List<String> file) {
		rendezvous.clear();
		rendezvousCheck.clear();

		Map<String, Runnable> runnableTasks = new LinkedHashMap<>();
		Map<String, List<String>> results = new LinkedHashMap<>();

		List<String> commands = new ArrayList<String>();
		String host = "";
		String user = "";
		for (String s : file) {
			if (!s.isEmpty() && !s.trim().startsWith("#")) {
				if (s.trim().startsWith("Machine")) {
					if (commands.size() > 0) {
						final String hostx = host;
						final String userx = user;
						final String pwd = StorageAndRetrieval.toUse(PasswordStore.getPwd(userx));
						if (pwd == null) {
							UI.writeToOutput("\n" + "ERROR: Password not set for user:" + userx + "\n");
							return null;
						}
						final List<String> commandsx = commands;
						List<String> result = new ArrayList<>();
						if (hostCount.containsKey(hostx))
							hostCount.put(hostx, hostCount.get(hostx) + 1);
						else
							hostCount.put(hostx, 1);
						Runnable task = () -> {
							runCommands(userx, pwd, hostx, 22, 100000, commandsx, result);
						};

						results.put(hostx + "_" + hostCount.get(hostx), result);
						runnableTasks.put(hostx + "_" + hostCount.get(hostx), task);
						commands = new ArrayList<String>();
					}
					if (!s.contains("<@>")) {
						UI.writeToOutput(
								"\n" + "ERROR: Script line with \"Machine\" should have @ <remote user id>:" + "\n");
						break;
					} else {
						List<String> arr = StringOps.fastSplit(StringOps.fastSplit(s, ":").get(1).trim(), "<@>");
						host = arr.get(0).trim();
						String user_pwd = arr.get(1).trim();
						if (user_pwd.contains("</>")) {
							user = StringOps.fastSplit(user_pwd, "</>").get(0).trim();
							String pwd = StringOps.fastSplit(user_pwd, "</>").get(1).trim();
							PasswordStore.setPwd(user, pwd);
						} else
							user = user_pwd;
					}
				} else {

					commands.add(s.trim());
					if (s.trim().startsWith("store")) {
						List<String> store = StringOps.fastSplit(s.trim(), ":");
						Functions.setParam(store.get(1).trim(), store.get(2).trim());
					}
					if (s.trim().startsWith("synchronize")) {
						List<String> synchronizesplit = StringOps.fastSplit(s.trim(), "@");
						String rendezvousPoint = synchronizesplit.get(1).trim();
						if (rendezvous.containsKey(rendezvousPoint))
							rendezvous.get(rendezvousPoint).add(1);
						else {
							List<Integer> li = new ArrayList<>();
							li.add(1);
							rendezvous.put(rendezvousPoint, li);
							rendezvousCheck.put(rendezvousPoint, new ArrayList<Integer>());
							if (synchronizesplit.size() == 3 && synchronizesplit.get(2).trim().equals("lock"))
								synchronized (syncLock) {
									syncLock.put(rendezvousPoint, true);
								}
						}
					}
				}
			}
		}
		if (commands.size() > 0) {
			final String hostx = host;
			final String userx = user;
			final String pwd = StorageAndRetrieval.toUse(PasswordStore.getPwd(userx));
			if (pwd == null) {
				UI.writeToOutput("\n" + "ERROR: Password not set for user:" + userx + "\n");
				return null;
			}

			final List<String> commandsx = commands;
			List<String> result = new ArrayList<>();
			if (hostCount.containsKey(hostx))
				hostCount.put(hostx, hostCount.get(hostx) + 1);
			else
				hostCount.put(hostx, 1);
			results.put(hostx + "_" + hostCount.get(hostx), result);
			Runnable task = () -> {
				runCommands(userx, pwd, hostx, 22, 100000, commandsx, result);
			};

			runnableTasks.put(hostx + "_" + hostCount.get(hostx), task);
			commands = new ArrayList<String>();
		}

		for (Map.Entry<String, Runnable> r : runnableTasks.entrySet())
			try {
				ExecutorService es = Executors.newFixedThreadPool(1);
				es.submit(r.getValue());
				esm.put(r.getKey(), es);
			} catch (Exception e) {
				UI.writeToOutput("\n" + LogStackTrace.get(e) + "\n");
			}

		for (ExecutorService es : esm.values())
			es.shutdown();
		return results;
	}

	public static void runCommands(String username, String password, String host, int port, long defaultTimeoutSeconds,
			List<String> commands, List<String> result) {
		if (host.equals("ParameterStore")) {
			for (String command : commands) {
				synchronized (result) {
					result.add(command);
				}
			}
			synchronized (result) {
				result.add("###Command Processing complete...");
			}
			return;
		}
		if (password != null) {
			Session session = null;
			ChannelExec channel = null;
			JSch jsch = new JSch();
			try {
				session = jsch.getSession(username, host, port);
				session.setPassword(password);
				session.setConfig("StrictHostKeyChecking", "no");
				session.connect();
				if (!commands.isEmpty()) {
					for (String command : commands) {
						synchronized (result) {
							result.add(command);
						}
						if (command.trim().startsWith("wait")) {
							long time = Long.parseLong(StringOps.fastSplit(command, ":").get(1).trim());
							Thread.sleep(time);
						} else if (command.trim().startsWith("synchronize")) {

							List<String> synchronizesplit = StringOps.fastSplit(command.trim(), "@");
							String rendezvousPoint = synchronizesplit.get(1).trim();
							rendezvousCheck.get(rendezvousPoint).add(1);
							while (rendezvousCheck.get(rendezvousPoint).size() < rendezvous.get(rendezvousPoint).size())
								Thread.sleep(1000);
							if (synchronizesplit.size() == 3 && synchronizesplit.get(2).trim().equals("lock")
									&& syncLock.containsKey(rendezvousPoint)) {
								UI.writeToOutput("Waiting on UI lock for synchronize @ " + rendezvousPoint + "\n");
								while (syncLock.get(rendezvousPoint))
									Thread.sleep(1000);
							}
						} else if (command.trim().startsWith("timestamp")) {
							String timestampname = StringOps.fastSplit(command, ":").get(1);
							Functions.setTimestamp(timestampname);
						} else if (command.trim().startsWith("store")) {
							continue;
						} else {
							channel = (ChannelExec) session.openChannel("exec");

							command = Functions.substitute(command);
							channel.setCommand(command);
							InputStream outputstream_from_the_channel = channel.getInputStream();
							BufferedReader br = new BufferedReader(
									new InputStreamReader(outputstream_from_the_channel));
							String line;
							channel.connect();
							while (true) {
								while ((line = br.readLine()) != null) {
									synchronized (result) {
										result.add(line);
									}
								}
								if (channel.isClosed()) {
									break;
								}
							}
							channel.disconnect();
						}
					}
				} else
					synchronized (result) {
						result.add("No valid commands found...");
					}
			} catch (Exception e) {
				UI.writeToOutput("\n" + LogStackTrace.get(e) + "\n");
			} finally {

				if (session != null) {
					session.disconnect();
				}

			}
			synchronized (result) {
				result.add("###Command Processing complete...");
			}
		}
	}
}
