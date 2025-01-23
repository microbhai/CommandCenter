package akhil.commandcenter;

import java.awt.EventQueue;

public class CommandCenter {

	public static void main(String[] args) {
		Logo x = new Logo();
		x.setVisible(true);

		try {
			Thread.sleep(2500);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		x.setVisible(false);
			EventQueue.invokeLater(() -> {
				UI ex = new UI();
				ex.setVisible(true);
			});
	}
	
}
