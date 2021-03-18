package lkd.form;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.border.LineBorder;

import lkd.config.Config;

@SuppressWarnings("serial")
public class Base extends JFrame {

	public Base(int width, int height) {
		setTitle(Config.title);
		setSize(width, height);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
	}

	public JButton createButton(String text, ActionListener e, Font font) {
		JButton button = new JButton(text);
		button.addActionListener(e);
		button.setFont(font);
		button.setBorder(new LineBorder(Color.black));
		return button;
	}

}
