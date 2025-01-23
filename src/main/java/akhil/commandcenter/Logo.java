package akhil.commandcenter;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class Logo extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ImageIcon img = new ImageIcon("../images/icon.jpg");

	public Logo() {
		showLogo();
	}

	private void showLogo() {
		JLabel logo = new JLabel(img, SwingConstants.CENTER);
		JPanel pane = new JPanel();
		pane.add(logo);
		this.add(pane);
		

		this.setIconImage(img.getImage());
		this.setSize(657, 541);
		this.setLocationRelativeTo(null);
		this.setUndecorated(true);
		this.setVisible(true);

	}
}
