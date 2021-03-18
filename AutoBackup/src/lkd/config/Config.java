package lkd.config;

import javax.swing.JOptionPane;

public class Config {

	public static final String title = "Auto Backup by Namsic";

	public static String path = "";

	public static void handleError(Exception e) {
		StringBuilder builder;

		String message = e.getMessage();
		if (message != null) {
			builder = new StringBuilder(message);
		} else {
			builder = new StringBuilder("Error!");
		}

		StackTraceElement[] elements = e.getStackTrace();
		for (StackTraceElement element : elements) {
			builder.append("\n\t");
			builder.append(element.toString());
		}

		JOptionPane.showMessageDialog(null, builder.toString());
		System.err.println(builder.toString());
		System.exit(-1);
	}

}
