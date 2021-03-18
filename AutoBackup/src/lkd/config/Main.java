package lkd.config;

import java.io.File;

import lkd.form.MainForm;

public class Main {
	public static void main(String[] args) {
		try {
			checkEnv();
		} catch (Exception e) {
			Config.handleError(e);
		}
	}

	private static void checkEnv() throws Exception {
		Cmd cmd = new Cmd();
		String command = cmd.inputCommand("set LKD");
		String env = cmd.execCommand(command);

		if (!(env == null || env.equals(""))) {
			env = env.replace("LKD=", "");
			String[] envSplit = env.split(",");

			String pathStr;
			for (String string : envSplit) {
				if(string.startsWith("abu_path:")) {
					pathStr = string.trim().substring(9).replace(";", "");
					
					File file = new File(pathStr);
					if(file.exists()) {
						Config.path = pathStr;
					}
				}
			}

			new MainForm().setVisible(true);
		}
	}

}
