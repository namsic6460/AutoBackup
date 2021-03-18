package lkd.form;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import lkd.config.Cmd;
import lkd.config.Config;

@SuppressWarnings("serial")
public class MainForm extends Base {
	
	private final static int MINUTE = 60000;
	private static boolean isBackup = false;
	private static boolean isLoading = false;
	
	//자동 작동 시 에러 메세지 표시 없앰을 위함
	private static boolean isAuto = false;
	
	//로드 시 백업도 진행하며 에러가 뜨기 때문에 이를 제거하기 위함
	private static boolean isLoad = false;

	enum Status {
		WAITING("대기중..."), RUNNING("동작중입니다"), BACKUP("백업을 실행하고 있습니다!");

		public String text;

		Status(String name) {
			this.text = name;
		}
	}

	String initPath = "C:/Users/" + System.getProperty("user.name") + "/Desktop";
	String fontName;
	Font buttonFont;
	int backupTime = 60;
	JTextField backupField;
	List<JButton> buttons = new ArrayList<>();
	JLabel statusLabel;
	JButton findButton;
	JButton runButton;
	JButton stopButton;
	JButton saveButton;
	JButton loadButton;
	JComboBox<String> comboBox;
	
	int remainTime;

	Timer timer = new Timer();
	TimerTask task;

	public MainForm() {
		super(500, 265);
		System.out.println("PATH : " + Config.path);

		JPanel rootPanel = new JPanel(new BorderLayout());
		rootPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		JPanel centerPanel = new JPanel(new FlowLayout());
		centerPanel.setBorder(new LineBorder(Color.black, 2));

		// Left
		JPanel textPanel = new JPanel(new GridLayout(3, 1, 0, 24));
//		textPanel.setBorder(new MatteBorder(2, 2, 2, 0, Color.black));

		JLabel pathLabel = new JLabel("경로:");
		JLabel backupLabel = new JLabel("백업 시간:");
		JLabel leftStatusLabel = new JLabel("상태:");

		fontName = pathLabel.getFont().getName();
		Font labelFont = new Font(fontName, Font.BOLD, 16);
		pathLabel.setFont(labelFont);
		backupLabel.setFont(labelFont);
		leftStatusLabel.setFont(labelFont);

		pathLabel.setHorizontalAlignment(JLabel.RIGHT);
		backupLabel.setHorizontalAlignment(JLabel.RIGHT);
		leftStatusLabel.setHorizontalAlignment(JLabel.RIGHT);

		textPanel.add(pathLabel);
		textPanel.add(backupLabel);
		textPanel.add(leftStatusLabel);

		// Right
		JPanel settingPanel = new JPanel(new GridLayout(3, 1, 0, 5));

		JPanel rightPathPanel = new JPanel(new FlowLayout());

		JTextField pathField = new JTextField(Config.path);
		pathField.setPreferredSize(new Dimension(280, 24));
		pathField.setFont(new Font(fontName, Font.BOLD, 16));
		pathField.setBorder(new LineBorder(Color.black));
		pathField.setEditable(false);

		buttonFont = new Font(fontName, Font.BOLD, 14);
		findButton = createButton("파일 검색", e -> {
			JFileChooser chooser = new JFileChooser(initPath);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			String filePath;
			int value = chooser.showOpenDialog(null);

			if (value == JFileChooser.APPROVE_OPTION) {
				filePath = chooser.getSelectedFile().getAbsolutePath();
			} else {
				return;
			}
			
			if(!Config.path.equals(filePath)) {
				Config.path = filePath;
				pathField.setText(filePath);
				
				comboBox.removeAllItems();
				
				List<File> backups = getBackups();
				for(File backup : backups) {
					comboBox.addItem(backup.getName());
				}
				
				Cmd cmd = new Cmd();
				String env = cmd.execCommand(cmd.inputCommand("set LKD")).replace("LKD=", "").trim();
				System.out.println("env : " + env);
				String command;

				if (env != null && !env.equals("")) {
					command = cmd.inputCommand("setx LKD " + env + ",abu_path:" + Config.path);
				} else {
					command = cmd.inputCommand("setx LKD " + "abu_path:" + Config.path);
				}
				
				System.out.println("command : " + command);
				cmd.execCommand(command);
			}
		}, buttonFont);
		findButton.setBorder(new LineBorder(Color.black));
		findButton.setPreferredSize(new Dimension(85, 30));

		rightPathPanel.add(pathField);
		rightPathPanel.add(findButton);

		JPanel timePanel = new JPanel(new FlowLayout());

		backupField = new JTextField("60 분");
		backupField.setBorder(new LineBorder(Color.black));
		backupField.setEditable(false);
		backupField.setFont(new Font(fontName, Font.BOLD, 16));
		backupField.setHorizontalAlignment(JLabel.CENTER);
		backupField.setPreferredSize(new Dimension(60, 30));

		timePanel.add(timeButton("-", 100));
		timePanel.add(timeButton("-", 10));
		timePanel.add(timeButton("-", 1));
		timePanel.add(backupField);
		timePanel.add(timeButton("+", 1));
		timePanel.add(timeButton("+", 10));
		timePanel.add(timeButton("+", 100));

		JPanel statusPanel = new JPanel(new FlowLayout());

		statusLabel = new JLabel(Status.WAITING.text);
		statusLabel.setFont(labelFont);
		statusLabel.setHorizontalAlignment(JLabel.LEFT);
		statusLabel.setPreferredSize(new Dimension(250, 30));

		runButton = createButton("시작", e -> {
			File dir = new File(Config.path);
			if (!dir.exists()) {
				JOptionPane.showMessageDialog(null, "폴더가 존재하지 않습니다");
				return;
			}

			runButton.setEnabled(false);
			for(JButton button : buttons) {
				button.setEnabled(false);
			}

			task = getTask();
			timer = new Timer();
			timer.scheduleAtFixedRate(task, 0, backupTime * MINUTE);
		}, buttonFont);
		runButton.setPreferredSize(new Dimension(50, 30));

		stopButton = createButton("중지", e -> stopProcess(), buttonFont);
		stopButton.setPreferredSize(new Dimension(50, 30));
		stopButton.setEnabled(false);

		statusPanel.add(statusLabel);
		statusPanel.add(runButton);
		statusPanel.add(stopButton);

		settingPanel.add(rightPathPanel);
		settingPanel.add(timePanel);
		settingPanel.add(statusPanel);
		
		//Bottom
		JPanel bottomPanel = new JPanel(new GridLayout(2, 1, 0, 5));
		
		JPanel bottomButtonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
		
		saveButton = createButton("백업 시작하기 (수동 백업)", e -> {
			new Thread(new Runnable() {
				@Override
				public void run() {
					saveButton.setEnabled(false);
					startBackup();
					saveButton.setEnabled(true);
				}
			}).start();
		}, buttonFont);
		saveButton.setPreferredSize(new Dimension(220, 30));
		
		loadButton = createButton("백업 로드하기 (기존 월드 삭제)", e ->  {
			new Thread(new Runnable() {
				@Override
				public void run() {
					loadButton.setEnabled(false);
					loadBackup();
					loadButton.setEnabled(true);
				}
			}).start();
		}, buttonFont);
		loadButton.setPreferredSize(new Dimension(220, 30));
		
		bottomButtonPanel.add(saveButton);
		bottomButtonPanel.add(loadButton);
		
		comboBox = new JComboBox<String>();
		
		isAuto = true;
		List<File> backups = getBackups();
		for(File backup : backups) {
			comboBox.addItem(backup.getName());
		}
		
		isAuto = false;
		
		bottomPanel.add(bottomButtonPanel);
		bottomPanel.add(comboBox);
		
		// Add
		centerPanel.add(textPanel);
		centerPanel.add(settingPanel);
		centerPanel.add(bottomPanel);
		rootPanel.add(centerPanel, BorderLayout.CENTER);
		add(rootPanel, BorderLayout.CENTER);
	}
	
	private List<File> getBackups() {
		String saveDirName = Config.path + "\\backup";
		File saveDir = new File(saveDirName);
		if(!saveDir.exists()) {
			if(!isAuto) {
				JOptionPane.showMessageDialog(null, "폴더가 존재하지 않습니다");
			}
			
			return new ArrayList<>();
		}
		
		File[] backups = saveDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return !file.isDirectory() && file.getName().endsWith(".zip");
			}
		});
		
		return new ArrayList<>(Arrays.asList(backups));
	}
	
	private void loadBackup() {
		if(isBackup) {
			JOptionPane.showMessageDialog(null, "백업이 진행중입니다");
			return;
		}

		isLoading = true;

		try {
			File dir = new File(Config.path);
			if(!dir.exists()) {
				JOptionPane.showMessageDialog(null, "폴더가 존재하지 않습니다");
				return;
			}
			
			String saveDirName = Config.path + "\\backup";
			File saveDir = new File(saveDirName);
			if(!saveDir.exists()) {
				JOptionPane.showMessageDialog(null, "폴더가 존재하지 않습니다");
				return;
			}
			
			File[] deleteFiles = dir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					return !file.getAbsolutePath().startsWith(saveDirName);
				}
			});
			
			if(deleteFiles.length > 0) {
				for(File deleteFile : deleteFiles) {
					if(!deleteFile.renameTo(deleteFile)) {
						JOptionPane.showMessageDialog(null, deleteFile.getAbsoluteFile() + "파일이 사용중입니다");
						return;
					}
				}
				
				isLoad = true;
				startBackup();
				isLoad = false;
				
				for(File deleteFile : deleteFiles) {
					deleteFile.delete();
				}
			}
			
			File zipFile = new File(saveDirName + "\\" + comboBox.getSelectedItem().toString());
		
			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
			ZipEntry zipEntry;
			while((zipEntry = zis.getNextEntry()) != null) {
				File unzipFile = new File(Config.path, zipEntry.getName());
				
				if(zipEntry.isDirectory()) {
					unzipFile.mkdirs();
				} else {
					File parentDir = unzipFile.getParentFile();
					if(!parentDir.exists()) {
						parentDir.mkdirs();
					}
					
					FileOutputStream fos = new FileOutputStream(unzipFile);
					
					byte[] buffer = new byte[4096];
					int size;
					while((size = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, size);
					}
					
					fos.close();
				}
			}
			
			zis.close();
			JOptionPane.showMessageDialog(null, "백업 로드를 성공하였습니다!");
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "백업 로드를 실패하였습니다 - " + e.getMessage());
			return;
		} finally {
			isLoading = false;
		}
	}

	private void stopProcess() {
		timer.cancel();

		stopButton.setEnabled(false);
		runButton.setEnabled(true);
		setEnables(true);

		statusLabel.setText(Status.WAITING.text);
		statusLabel.revalidate();
	}
	
	private void startBackup() {
		if(isBackup) {
			if(!isAuto) {
				JOptionPane.showMessageDialog(null, "백업이 진행중입니다");
			}
			
			return;
		} else if(isLoading) {
			if(!(isAuto || isLoad)) {
				JOptionPane.showMessageDialog(null, "로드가 진행중입니다");
			}
			
			return;
		}

		isBackup = true;
		
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
			
			File dir = new File(Config.path);
			if (!dir.exists()) {
				JOptionPane.showMessageDialog(null, "폴더가 존재하지 않습니다");
				
				if(isAuto) {
					stopProcess();
				}

				return;
			}
	
			String saveDirName = Config.path + "\\backup";
			File saveDir = new File(saveDirName);
			if (!saveDir.exists()) {
				saveDir.mkdir();
			}
			
			statusLabel.setText(Status.BACKUP.text);
			statusLabel.revalidate();
	
			String time = LocalDateTime.now().format(formatter);
			File zipFile = new File(saveDirName + "\\" + time + ".zip");
			
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
			
			File[] fileList = dir.listFiles();
			byte[] buf = new byte[4096];
			FileInputStream in = null;
			
			for (File file : fileList) {
				if(file.getAbsolutePath().startsWith(saveDirName)) {
					continue;
				}
				
				in = new FileInputStream(file.getAbsoluteFile());
				zos.putNextEntry(new ZipEntry(file.getName()));
				
				int len;
				while ((len = in.read(buf)) > 0) {
					zos.write(buf, 0, len);
				}
				
				zos.closeEntry();
				in.close();
			}
	
			zos.close();
			comboBox.addItem(zipFile.getName());
			System.out.println(zipFile.getAbsolutePath() + "생성완료");
		} catch (Exception e) {
			Config.handleError(e);
		} finally {
			if(isAuto) {
				statusLabel.setText(Status.RUNNING.text + " (남은 시간 : " + remainTime + "분)");
			} else {
				statusLabel.setText(Status.WAITING.text);
			}
			statusLabel.revalidate();
			
			isBackup = false;
		}
	}

	private JButton timeButton(String plusMinus, int time) {
		JButton button = createButton(plusMinus + time, e -> {
			int changeTime = plusMinus.equals("-") ? time * -1 : time;
			backupTime += changeTime;
			backupTime = backupTime < 1 ? 1 : backupTime;
			backupTime = backupTime > 9999 ? 9999 : backupTime;
			backupField.setText(String.valueOf(backupTime) + " 분");
			backupField.revalidate();
		}, buttonFont);
		button.setPreferredSize(new Dimension(45, 30));

		buttons.add(button);

		return button;
	}
	
	private TimerTask getTask() {
		return new TimerTask() {
			@Override
			public void run() {
				remainTime = backupTime;
				
				statusLabel.setText(Status.RUNNING.text + " (남은 시간 : " + remainTime + "분)");
				statusLabel.revalidate();

				setEnables(false);
				stopButton.setEnabled(true);

				try {
					while (true) {
						Thread.sleep(MINUTE);
						remainTime--;

						if (remainTime == 0) {
							isAuto = true;
							startBackup();
							isAuto = false;
							
							return;
						} else {
							statusLabel.setText(Status.RUNNING.text + " (남은 시간 : " + remainTime + "분)");
							statusLabel.revalidate();
						}
					}
				} catch (Exception e) {
					Config.handleError(e);
				}
			}
		};
	}
	
	private void setEnables(boolean enable) {
		findButton.setEnabled(enable);
		for(JButton button : buttons) {
			button.setEnabled(enable);
		}
	}

}
