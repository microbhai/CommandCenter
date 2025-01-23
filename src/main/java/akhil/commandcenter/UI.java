package akhil.commandcenter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;

public class UI {

	private JFrame mainui = new JFrame();

	public JFrame getUI() {
		return mainui;
	}

	public String getFileContent() {
		return fileContent.getText();
	}

	private static final String COMMENTSTART = "<CC_COMMENT>";
	private static final String COMMENTEND = "</CC_COMMENT>";
	private String lastUsedDir = "";
	private String currentFile = "";
	private JButton setpwd = new JButton("Set User/Password");
	JMenuBar menubar = new JMenuBar();
	JMenu filemenu = new JMenu("File");
	JMenuItem save = new JMenuItem("Save");
	JMenuItem open = new JMenuItem("Open File");
	JMenuItem saveAs = new JMenuItem("Save File As");
	JMenu help = new JMenu("Help");
	JMenuItem paraminfo = new JMenuItem("Parameter - Information");
	JMenuItem machineinfo = new JMenuItem("Machine command sample");
	JMenuItem syncinfo = new JMenuItem("Synchronize - Information");
	JMenuItem  processkillinfo = new JMenuItem("Remote Process Termination");
	JMenuItem  parameterstoreinfo = new JMenuItem("Parameter Store ");

	private JLabel userlabel = new JLabel("Remote User ID");
	private JLabel pwdlabel = new JLabel("Remote Password");
	private JLabel unlockSyncPointLabel = new JLabel("Sync Point to Unlock");
	private JTextField unlockSyncPoint = new JTextField(50);
	private JTextField user = new JTextField(50);
	private JTextField outputNumberOfLines = new JTextField(20);
	private JLabel outputNumberOfLinesLabel = new JLabel("Number of Lines in Output");
	private JPasswordField pwd = new JPasswordField(50);

	private JButton execute = new JButton("Run Script");
	private JButton unlock = new JButton("Unlock Sync");
	private JButton comment = new JButton("Comment");
	private JButton stopSelected = new JButton("Stop Selected");
	private JButton shutdown = new JButton("Stop Current Run");
	private JButton clearLog = new JButton("Reload/Color Format/Clear Log");
	private JTextPane fileContent = new JTextPane();
	private static JTextPane output = new JTextPane();
	private String[] status = new String[1];
	private static Map<String, Integer> hostCount = new HashMap<>();

	private Map<String, Color> keywordsAndColors = new HashMap<>();
	private Map<String, List<String>> logs = new HashMap<>();
	private Map<String, String> scriptTabContent = new HashMap<>();

	private Map<String, ScheduledExecutorService> esm = new HashMap<>();
	private Map<String, Integer> count = new HashMap<>();
	private Map<String, JScrollPane> toPaneCreator = new LinkedHashMap<>();

	private JScrollPane filescroll = new JScrollPane(fileContent);
	private JScrollPane outputscroll = new JScrollPane(output);

	JPanel controlPane = new JPanel();

	final StringBuilder saveFileLocationText = new StringBuilder();
	JTabbedPane outputPane = new JTabbedPane();
	JTabbedPane scriptPane = new JTabbedPane();
	JSplitPane splitPaneMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, controlPane, outputPane);
	JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scriptPane, outputscroll);

	public void setVisible(boolean tf) {
		mainui.setVisible(tf);
	}

	public void setCaretPosition(int pos) {
		fileContent.setCaretPosition(pos);
	}

	public int getCaretPosition() {
		return fileContent.getCaretPosition();
	}

	ImageIcon img = new ImageIcon("../images/icon.jpg");

	public UI() {
		new Thread(() -> {
			initUI();
			while (true) {
				try {
					Thread.sleep(2000);

				} catch (Exception e) {

				}
			}
		}).start();
	}

	public static void writeToOutput(String toPrint) {
		if (toPrint.contains("Exception") || toPrint.contains("ERROR"))
			UIUtil.writeToPane(output, toPrint, Color.RED, Color.WHITE, true, true, false);
		else if (toPrint.contains("Script run complete..."))
			UIUtil.writeToPane(output, "\n\n"+toPrint, new Color(0, 153, 0), Color.WHITE, true, true, false);
		else
			UIUtil.writeToPane(output, toPrint, Color.BLACK, Color.WHITE, true, true, false);
	}

	private void fixTextColor(JTextPane fileContent) {
		int x = UIUtil.undoCount;
		UIUtil.collect = true;
		int caretposition = fileContent.getCaretPosition();
		String origContent = (!System.getProperty("os.name").toLowerCase().contains("mac"))
				? fileContent.getText().replaceAll("\r", "")
				: fileContent.getText();
		fileContent.setText("");
		UIUtil.writeToPane(fileContent, origContent, Color.BLACK, Color.WHITE, false, false, false);
		UIUtil.setFontColor(fileContent, keywordsAndColors, null);
		UIUtil.setFontColor(fileContent, Color.BLACK);
		fileContent.setCaretPosition(caretposition);
		UIUtil.setUndoRedoCount(UIUtil.undoCount - x);
		UIUtil.collect = false;
	}

	private String removeCommentsFromScript(String dmsScript) {
		return dmsScript.replaceAll("(?s)" + COMMENTSTART + "(.+?)" + COMMENTEND, "").replaceAll("\r", "");
	}

	private void execute(String script, boolean append) {
		if (!append)
			stop();
		status[0] = "running";

		Date date = new Date();
		Functions.setStartDate(date);
		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		String strDate = dateFormat.format(date);

		Component[] componentList = outputPane.getComponents();
		for (Component c : componentList)
			outputPane.remove(c);
		outputPane.revalidate();

		List<String> list1 = StringOps.fastSplit(script, "\n");

		Map<String, List<String>> results = Run.processFile(list1);

		if (results != null) {
			Map<String, ScheduledExecutorService> esmx = new HashMap<>();
			for (Map.Entry<String, List<String>> entry : results.entrySet()) {
				ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
				esmx.put(entry.getKey(), es);
				logs.put(entry.getKey(), new ArrayList<String>());
				JTextPane x = new JTextPane();
				x.setEditable(false);
				JScrollPane sx = new JScrollPane(x);
				toPaneCreator.put(entry.getKey(), sx);
				List<String> ls = entry.getValue();
				List<String> log = logs.get(entry.getKey());
				Runnable task = () -> {
					try {
						synchronized (ls) {
							if (!ls.isEmpty()) {
								log.addAll(ls);
								String msg = "\n" + ls.stream().collect(Collectors.joining("\n"));
								FileOperation.writeFile("../logs", entry.getKey() + "_" + strDate + ".log", msg, true);
								ls.clear();
								if (msg.endsWith("###Command Processing complete..."))
									count.put(entry.getKey(), 1);
							}
						}
						if (!log.isEmpty()) {
							int outputsize = 10000;
							try {
								outputsize = Integer.parseInt(outputNumberOfLines.getText());
							} catch (NumberFormatException ex) {
							}
							if (log.size() > outputsize) {
								for (int i = 0; i < log.size() - outputsize; i++)
									log.remove(0);
							}
							String msg = "\n" + log.stream().collect(Collectors.joining("\n"));
							UIUtil.writeToPane(x, msg, Color.BLUE, Color.WHITE, true, false, false);
						}

					} catch (Exception ex) {
						ex.printStackTrace();
					}
				};
				es.scheduleAtFixedRate(task, 5, 5, TimeUnit.SECONDS);
			}
			esm.putAll(esmx);
			createOutputPane(outputPane, toPaneCreator);
			if (!append)
				new Thread(() -> {

					while (status[0].equals("running") && count.size() < logs.size()) {
						try {
							Thread.sleep(1000);
						} catch (Exception ex) {
						}
					}
					for (ScheduledExecutorService es : esm.values())
						es.shutdown();
					UI.writeToOutput("Script run complete...\n");
					stop();
				}).start();
		}

	}

	private void stop() {
		Run.stop();
		status[0] = "not running";
		count.clear();
		logs.clear();
		toPaneCreator.clear();
		for (ScheduledExecutorService es : esm.values())
			if (es != null && !es.isTerminated())
				es.shutdownNow();
		esm.clear();
	}

	private void initUI() {
		status[0] = "not running";
		keywordsAndColors.put("Machine", Color.BLUE);
		keywordsAndColors.put("<@>", Color.RED);
		keywordsAndColors.put("</>", Color.RED);
		keywordsAndColors.put("#", Color.RED);
		keywordsAndColors.put("&", Color.RED);
		keywordsAndColors.put("wait", new Color(51, 153, 180));
		keywordsAndColors.put("timestamp", new Color(51, 153, 180));
		keywordsAndColors.put("store", new Color(51, 153, 180));
		keywordsAndColors.put(COMMENTSTART, Color.MAGENTA);
		keywordsAndColors.put(COMMENTEND, Color.MAGENTA);
		keywordsAndColors.put("synchronize", new Color(51, 153, 180));
		outputNumberOfLines.setText("10000");
		String title = "Command Center";

		mainui.setIconImage(img.getImage());
		fileContent.setEditable(true);

		scriptPane.add("Main Script", filescroll);
		createControlPane(controlPane, execute, comment, stopSelected, splitPane, clearLog, shutdown, unlockSyncPoint, unlockSyncPointLabel, unlock, user,
				userlabel, pwd, pwdlabel, setpwd, outputNumberOfLines, outputNumberOfLinesLabel);
		splitPane.setResizeWeight(0.7);
		splitPaneMain.setResizeWeight(0.3);
		createLayout(splitPaneMain);
		filemenu.add(open);
		filemenu.add(save);
		filemenu.add(saveAs);
		menubar.add(filemenu);
		help.add(paraminfo);
		help.add(machineinfo);
		help.add(syncinfo);
		help.add(processkillinfo);
		help.add(parameterstoreinfo);
		menubar.add(help);

		output.setPreferredSize(new Dimension(40, 40));// 50,50

		UIUtil.setUndoCapability(fileContent);

		fileContent.setPreferredSize(new Dimension(40, 40));// 50,50
		outputscroll.setPreferredSize(new Dimension(45, 45));// 60,60
		filescroll.setPreferredSize(new Dimension(45, 45));

		if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
			save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		} else {
			int commandKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
			save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, commandKey));

		}
		paraminfo.addActionListener(e -> {
			String toPrint = "\n\n\nSupported parameters are:\n${startdatetime:yyyyMMddHHmmss}\n${currentdatetime:yyyyMMddHHmmss}\n${timestamp:<some name>:yyyyMMddHHmmss}, recorded earlier via \"timestamp:<some name>\"\n${param name}, saved/stored earlier via \"store:<param name>:<param value>\"\n\nStart date is determined at the press of RUN SCRIPT button.\nThe date format can be any of the supported SimpleDateFormat in Java.\n\n\n";
			UIUtil.writeToPane(output, toPrint, new Color(0, 153, 0), Color.WHITE, true, true, false);
		});
		machineinfo.addActionListener(e -> {
			String toPrint = "\n\n\nMachine : [Replace with machine name] <@> [Replace with user id] </> [Replace with user password, encrypted (optional)]\n\n\n";
			UIUtil.writeToPane(output, toPrint, new Color(0, 153, 0), Color.WHITE, true, true, false);
		});
		syncinfo.addActionListener(e -> {
			String toPrint = "\n\n\nTo synchronize on various points, use command \nsynchronize @ point1\nOR\nsynchronize @ point1 @ lock\n\nIf lock option is used, the processes will be synchronized and will wait for the unlock button press from the UI.\nYou need to specify the name of the synchronize point when using unlock button\n\n\n";
			UIUtil.writeToPane(output, toPrint, new Color(0, 153, 0), Color.WHITE, true, true, false);
		});
		processkillinfo.addActionListener(e -> {
			String toPrint = "\n\n\nTo kill remote process on a Windows machine use comand like \"wmic process where \"name like '%java%'\" delete \"\n.This will kill any java.exe process on the remote machines.\n\n\n";
			UIUtil.writeToPane(output, toPrint, new Color(0, 153, 0), Color.WHITE, true, true, false);
		});
		parameterstoreinfo.addActionListener(e -> {
			String toPrint = "\n\n\nTo store and use parameter values we can create a special \"Machine\" segment called ParameterStore. This will have all the store type parameters as a list and those can be used throughout the script. Sample ParameterStore configuration is provided below:\n\nMachine : ParameterStore <@> ParameterStore </>\nstore:dirname1:C:\\\\Share\nstore:dirname2:C:\\\\Share\nstore:testname:JmeterTest\n\n\n";
			UIUtil.writeToPane(output, toPrint, new Color(0, 153, 0), Color.WHITE, true, true, false);
		});
		
		
		clearLog.addActionListener(e -> {

			int count = scriptPane.getTabCount();
			for (int i = count - 1; i > 0; i--) {
				scriptPane.remove(i);
			}
			scriptTabContent.clear();
			hostCount.clear();
			scriptPane.revalidate();

			String script = removeCommentsFromScript(fileContent.getText());
			List<Integer> startPos = new ArrayList<>();
			List<Integer> endPos = new ArrayList<>();
			int index = script.indexOf("Machine");
			startPos.add(index);
			while (index >= 0) {

				index = script.indexOf("Machine", index + 1);

				if (index >= 0) {
					startPos.add(index);
					endPos.add(index);
				}
			}
			endPos.add(script.length());

			for (int i = 0; i < startPos.size(); i++) {
				String text = script.substring(startPos.get(i), endPos.get(i));

				String firstLine = text.substring(0, text.indexOf("\n")).trim();

				String titlex = StringOps.fastSplit(StringOps.fastSplit(firstLine, ":").get(1).trim(), "<@>").get(0)
						.trim();
				if (hostCount.containsKey(titlex))
					hostCount.put(titlex, hostCount.get(titlex) + 1);
				else
					hostCount.put(titlex, 1);
				JTextPane t = new JTextPane();
				JScrollPane s = new JScrollPane(t);
				scriptPane.add(titlex + "_" + hostCount.get(titlex), s);
				scriptPane.revalidate();
				UIUtil.writeToPane(t, text, Color.BLACK, Color.WHITE, false, false, false);

				scriptTabContent.put(titlex + "_" + hostCount.get(titlex), text);
				fixTextColor(t);
				t.setEditable(false);
			}

			output.setText("");
			fixTextColor(fileContent);

		});
		comment.addActionListener(e -> {

			if (fileContent.getSelectedText() != null)
				UIUtil.writeToPane(fileContent, COMMENTSTART + "\n\n" + COMMENTEND, Color.BLACK, Color.WHITE, false,
						false, false);

			fixTextColor(fileContent);
		});
		stopSelected.addActionListener(e -> {
			if (JOptionPane.showConfirmDialog(null,
					"Are you sure? This will stop the selected process output tabs below.", "Choose Option",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				String key = outputPane.getTitleAt(outputPane.getSelectedIndex());
				count.put(key, 1);
				if (esm.containsKey(key)) {
					ScheduledExecutorService es = esm.get(key);
					if (es != null && !es.isTerminated())
						es.shutdownNow();
				}
				Run.stop(key);
			}

		});
		shutdown.addActionListener(e -> {
			if (status[0].equals("running")) {
				if (JOptionPane.showConfirmDialog(null,
						"Are you sure? This will stop all the processes running currently.", "Choose Option",
						JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
					stop();

			} else {
				writeToOutput("No processes are running currently\n");
			}
		});
		unlock.addActionListener(e -> {
			Run.unlockSync(unlockSyncPoint.getText().trim());
			unlockSyncPoint.setText("");
		});

		open.addActionListener(e -> {
			JFileChooser fileChooser = new JFileChooser();
			if (lastUsedDir.isEmpty())
				fileChooser.setCurrentDirectory(
						new File(System.getProperty("user.home") + System.getProperty("file.separator") + "Desktop"));
			else
				fileChooser.setCurrentDirectory(new File(lastUsedDir));
			if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				File selectedFile = fileChooser.getSelectedFile();
				String content = FileOperation.getContentAsString(selectedFile.getPath(), "utf8");
				currentFile = selectedFile.getPath();
				fileContent.setText(content);
				mainui.setTitle("Command Center - " + currentFile);
			}
			fixTextColor(fileContent);
		});

		setpwd.addActionListener(e -> {
			String encryptedPwd = StorageAndRetrieval.toKeep(new String(pwd.getPassword()));
			PasswordStore.setPwd(user.getText().trim(), encryptedPwd);
			UI.writeToOutput(
					"\n" + "INFO: Password set for user: " + user.getText().trim() + " : " + encryptedPwd + "\n");
			user.setText("");
			pwd.setText("");
		});

		save.addActionListener(e -> {

			if (currentFile.isEmpty()) {
				JFileChooser fileChooser = new JFileChooser();
				if (lastUsedDir.isEmpty())
					fileChooser.setCurrentDirectory(new File(
							System.getProperty("user.home") + System.getProperty("file.separator") + "Desktop"));
				else
					fileChooser.setCurrentDirectory(new File(lastUsedDir));
				if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
					File selectedFile = fileChooser.getSelectedFile();
					currentFile = selectedFile.getPath();
					lastUsedDir = selectedFile.getParent();
				} else
					return;

			}
			String toSave = fileContent.getText().replaceAll("\r", "");

			UI.writeToOutput("Saved\n");
			FileOperation.writeFile("", currentFile.toString(), toSave);

		});

		saveAs.addActionListener(e -> {

			JFileChooser fileChooser = new JFileChooser();
			if (lastUsedDir.isEmpty())
				fileChooser.setCurrentDirectory(
						new File(System.getProperty("user.home") + System.getProperty("file.separator") + "Desktop"));
			else
				fileChooser.setCurrentDirectory(new File(lastUsedDir));
			if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
				File selectedFile = fileChooser.getSelectedFile();
				currentFile = selectedFile.getPath();
				lastUsedDir = selectedFile.getParent();
			} else
				return;

			String toSave = fileContent.getText().replaceAll("\r", "");

			UI.writeToOutput("Saved\n");
			FileOperation.writeFile("", currentFile.toString(), toSave);

		});

		execute.addActionListener(e -> {

			String key = scriptPane.getTitleAt(scriptPane.getSelectedIndex());
			if (key.equals("Main Script")) {
				String script = removeCommentsFromScript(fileContent.getText());

				if (JOptionPane.showConfirmDialog(null,
						"Are you sure? This will start all the processes in the script.", "Choose Option",
						JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

					if (status[0].equals("running")) {
						if (JOptionPane.showConfirmDialog(null,
								"Processes are currently running? Would you like to stop them and start the script? If you choose No, new processes will be added to the current run.",
								"Choose Option", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
							stop();
							try {
								Thread.sleep(3000);
							} catch (Exception ex) {
							}
							execute(script, false);
						} else {
							if (status[0].equals("running"))
								execute(script, true);
							else
								execute(script, false);
						}
					} else
						execute(script, false);
				}
			} else {
				if (JOptionPane.showConfirmDialog(null,
						"Are you sure? This will start the selected process in the scripting tabs.", "Choose Option",
						JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					String script = removeCommentsFromScript(scriptTabContent.get(key));
					if (status[0].equals("running"))
						execute(script, true);
					else
						execute(script, false);
				}

			}
		});

		mainui.setTitle(title);
		mainui.setSize(400, 500);
		mainui.setJMenuBar(menubar);
		mainui.pack();
		mainui.setExtendedState(JFrame.MAXIMIZED_BOTH);
		mainui.setLocationRelativeTo(null);
		mainui.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		mainui.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {

				int jop = JOptionPane.showConfirmDialog(null, "Do you want to save you work?", "Choose Option",
						JOptionPane.YES_NO_CANCEL_OPTION);
				if (jop == JOptionPane.YES_OPTION) {

					if (currentFile.isEmpty()) {
						JFileChooser fileChooser = new JFileChooser();
						if (lastUsedDir.isEmpty())
							fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")
									+ System.getProperty("file.separator") + "Desktop"));
						else
							fileChooser.setCurrentDirectory(new File(lastUsedDir));
						if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
							File selectedFile = fileChooser.getSelectedFile();
							currentFile = selectedFile.getPath();
							lastUsedDir = selectedFile.getParent();
						} else
							return;

					}
					String toSave = fileContent.getText().replaceAll("\r", "");

					UI.writeToOutput("Saved\n");
					FileOperation.writeFile("", currentFile.toString(), toSave);
					System.exit(0);

				}
				if (jop == JOptionPane.NO_OPTION) {
					System.exit(0);

				}
			}
		});

	}

	private void createOutputPane(JTabbedPane panel, Map<String, JScrollPane> tp) {
		for (Map.Entry<String, JScrollPane> entry : tp.entrySet()) {
			panel.add(entry.getKey(), entry.getValue());
		}
	}

	/*
	 * createControlPane(controlPane, execute, comment, stopSelected, splitPane, clearLog, shutdown, unlock, unlockSyncPoint,unlockSyncPointLabel, user,
				userlabel, pwd, pwdlabel, setpwd, outputNumberOfLines, outputNumberOfLinesLabel);
	 */
	private void createControlPane(JPanel panel, JButton execute, JButton comment, JButton stopSelected, JSplitPane sp, JButton clearLog,
			JButton shutdown, JTextField unlockSyncPoint, JLabel unlockSyncPointLabel, JButton unlock, JTextField user, JLabel userlabel, JPasswordField pwd, JLabel pwdlabel,
			JButton setpwd, JTextField nol, JLabel noll) {

		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
						.addComponent(userlabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(user, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(pwdlabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(pwd, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(setpwd, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(noll, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(nol, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
				.addGroup(layout.createSequentialGroup()
						.addComponent(clearLog, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(execute, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(comment, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(unlockSyncPointLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(unlockSyncPoint, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(unlock, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(stopSelected, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(shutdown, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
				.addGroup(layout.createSequentialGroup().addComponent(sp, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(userlabel)
						.addComponent(user).addComponent(pwdlabel).addComponent(pwd).addComponent(setpwd)
						.addComponent(noll).addComponent(nol))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(clearLog).addComponent(comment)
						.addComponent(unlockSyncPointLabel).addComponent(unlockSyncPoint).addComponent(unlock).addComponent(execute).addComponent(stopSelected).addComponent(shutdown))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(sp))

		);

	}

	private void createLayout(JComponent... arg) {
		JPanel pane = new JPanel();
		// JScrollPane scrollPane = new JScrollPane(pane);
		mainui.add(pane);
		GroupLayout layout = new GroupLayout(pane);
		pane.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(
				layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup()
						.addComponent(arg[0], GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))

		);

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(arg[0])));

	}
}
