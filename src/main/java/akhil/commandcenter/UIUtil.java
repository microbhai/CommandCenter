package akhil.commandcenter;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

public class UIUtil {
	static boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
	static UndoManager undoMgr = new UndoManager();
	static int undoCount = 0;
	static List<Integer> undoRedoCount = new ArrayList<>();
	static int urcindex = 0;
	static boolean collect = false;

	public static void resetURC() {
		urcindex = 0;
		undoCount = 0;
		undoRedoCount.clear();
		undoMgr.discardAllEdits();
	}

	public static void setUndoRedoCount(int x) {
		undoRedoCount.add(x);
	}

	public static void setUndoCapability(JTextComponent tc) {
		undoMgr.setLimit(100000);
		// Add listener for undoable events
		tc.getDocument().addUndoableEditListener(e -> {
			undoMgr.addEdit(e.getEdit());
			undoCount++;
			if (!collect)
				setUndoRedoCount(1);
			urcindex = 0;
		}

		);

		// Add undo/redo actions
		tc.getActionMap().put("UNDO_ACTION", new AbstractAction("UNDO_ACTION") {

			private static final long serialVersionUID = 8622426637654807938L;

			public void actionPerformed(ActionEvent pEvt) {

				if (urcindex == 0) {
					urcindex = undoRedoCount.size() - 1;
				}

				try {
					int howmany = undoRedoCount.get(urcindex);

					for (int i = 0; i < howmany; i++) {
						try {
							if (undoMgr.canUndo()) {
								undoMgr.undo();
								undoCount--;
							}
						} catch (CannotUndoException e) {
							//DMSLogger.log(200, "ERROR: Can't undo exception...\n" + LogStackTrace.get(e));
						}
					}
					urcindex--;
				} catch (Exception e) {
					urcindex=0;
				}
				
			}
		});

		tc.getActionMap().put("REDO_ACTION", new AbstractAction("REDO_ACTION") {

			private static final long serialVersionUID = 2452205273890933579L;

			public void actionPerformed(ActionEvent pEvt) {

				if (urcindex == 0) {
					urcindex = undoRedoCount.size() - 1;
				}
				try {
					int howmany = undoRedoCount.get(urcindex);

					for (int i = 0; i < howmany; i++) {
						try {
							if (undoMgr.canRedo()) {
								undoMgr.redo();
								undoCount++;
							}
						} catch (CannotRedoException e) {
							//DMSLogger.log(200, "ERROR: Can't undo exception...\n" + LogStackTrace.get(e));
						}
					}
					urcindex++;
				} catch (Exception e) {
					urcindex = undoRedoCount.size() - 1;
				}
			}

		});

		// Create keyboard accelerators for undo/redo actions (Ctrl+Z/Ctrl+Y)
		tc.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "UNDO_ACTION");
		tc.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "REDO_ACTION");
	}

	public static void setFontBackground(JTextPane tp, Color c) {
		MutableAttributeSet attrs = tp.getInputAttributes();
		String str = tp.getText();
		str = str.replaceAll("\n", "");

		StyleConstants.setBackground(attrs, c);
		tp.getStyledDocument().setCharacterAttributes(0, str.length(), attrs, true);
	}

	public static int setFontBackground(JTextPane tp, String s, Color c) {
		MutableAttributeSet attrs = tp.getInputAttributes(); // to be checked....
		String str = tp.getText();
		if (isWindows)
			str = str.replace("\n", "");

		StyleConstants.setBackground(attrs, c);
		int count = 0;
		int currIndex = 0;
		int pos = 0;
		do {
			pos = str.indexOf(s, currIndex);
			if (pos != -1) {
				tp.getStyledDocument().setCharacterAttributes(pos, s.length(), attrs, true);
				count++;
				currIndex = pos + 1;
			}
		} while (pos != -1);

		return count;
	}

	public static void setFontColor(JTextPane tp, Color c) {
		MutableAttributeSet attrs = tp.getInputAttributes();
		StyleConstants.setForeground(attrs, c);
		tp.getStyledDocument().setCharacterAttributes(tp.getCaretPosition(), 0, attrs, true);
	}

	public static void setFontColor(JTextPane tp, Map<String, Color> foregroundmap, Map<String, Color> backgroudmap) {
		MutableAttributeSet attrs = tp.getInputAttributes();
		String str = tp.getText();
		if (isWindows)
			str = str.replace("\n", "");

		if (backgroudmap != null) {
			for (Map.Entry<String, Color> s : backgroudmap.entrySet()) {
				StyleConstants.setBackground(attrs, s.getValue());
				int currIndex = 0;
				int pos = 0;
				do {
					pos = str.indexOf(s.getKey(), currIndex);
					if (pos != -1) {
						tp.getStyledDocument().setCharacterAttributes(pos, s.getKey().length(), attrs, true);
						currIndex = pos + 1;
					}
				} while (pos != -1);
			}
		}
		if (foregroundmap != null) {
			for (Map.Entry<String, Color> s : foregroundmap.entrySet()) {
				StyleConstants.setForeground(attrs, s.getValue());
				int currIndex = 0;
				int pos = 0;
				do {
					pos = str.indexOf(s.getKey(), currIndex);
					if (pos != -1) {
						tp.getStyledDocument().setCharacterAttributes(pos, s.getKey().length(), attrs, true);
						currIndex = pos + 1;
					}
				} while (pos != -1);
			}
		}
	}

	public static void writeToPane(JTextPane tp, String msg, Color fontcolor, Color background, boolean scrollToEnd,
			boolean append, boolean appendAtStartorEnd) {
		if (tp != null && msg != null && fontcolor != null) {
			StyleContext sc = StyleContext.getDefaultStyleContext();
			AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, fontcolor);
			aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
			aset = sc.addAttribute(aset, StyleConstants.Background, background);
			aset = sc.addAttribute(aset, StyleConstants.Foreground, fontcolor);
			aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

			try {
				if (tp.getSelectedText() != null) {
					String[] sa = msg.split("\n\n");
					int end = tp.getSelectionEnd();
					int start = tp.getSelectionStart();
					if (sa.length > 1) {
						tp.getDocument().insertString(end, "\n" + sa[1], aset);
						tp.getDocument().insertString(start, sa[0] + "\n", aset);
						tp.setCaretPosition(start);
					} else {
						tp.getDocument().remove(start, end - start);
						tp.getDocument().insertString(start, msg, aset);
					}
				} else {
					if (append) {
						if (appendAtStartorEnd) {
							tp.setCaretPosition(0);
							tp.getDocument().insertString(tp.getCaretPosition(), msg, aset);
						} else {
							tp.setCaretPosition(tp.getDocument().getLength());
							tp.getDocument().insertString(tp.getCaretPosition(), msg, aset);
						}
					} else
					{
						int len = tp.getDocument().getLength();
						tp.getDocument().remove(0, len-1);
						tp.setCaretPosition(0);
						tp.getDocument().insertString(tp.getCaretPosition(), msg, aset);
					}
				}
			} catch (BadLocationException e) {
				//DMSLogger.log(200, "ERROR: Bad location exception...\n" + LogStackTrace.get(e));
			}
			// scrolls the text area to the end of data
			if (scrollToEnd)
				tp.setCaretPosition(tp.getDocument().getLength());
		} //else
			//DMSLogger.log(200, "ERROR: Tried to write to script pane but found null values\n");
	}

	public void setCaret(JTextPane tp, String s) {
		String str = tp.getText();
		if (isWindows)
			str = str.replace("\n", "");
		tp.setCaretPosition(str.indexOf(s));
	}

	public static void setCaret(JTextPane tp, boolean s) {
		try {
			String str = tp.getText();
			if (isWindows)
				str = str.replace("\n", "");
			if (s)
				tp.setCaretPosition(0);
			else
				tp.setCaretPosition(str.length());
		} catch (Exception e) {
			//DMSLogger.log(200, "ERROR: Illegal position on text pane\n" + LogStackTrace.get(e));
		}
	}

}
