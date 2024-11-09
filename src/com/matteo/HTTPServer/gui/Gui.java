package com.matteo.HTTPServer.gui;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

/**
 * JFrame della GUI del server
 * 
 * @author Matteo Basso
 */
public class Gui extends JFrame {
	public Gui(GuiVars guiVars) {
		super();
        setTitle("Server");
        //Container contentPane = getContentPane();
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int response = JOptionPane.showConfirmDialog(null, 
                    "Sei sicuro di voler terminare il server?", 
                    "Conferma chiusura", 
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
                
                if (response == JOptionPane.YES_OPTION) {
                	guiVars.getServer().stopServer();
                    dispose();
                    System.exit(0);
                }
            }
        });
        setVisible(true);
    }
	
	@Override
	public Component add(Component component) {
		super.add(component, BorderLayout.CENTER);
		return component;
	}
	
	/**
	 * Centra il JFrame
	 */
	public void center() {
		setLocationRelativeTo(null); // centra la finestra
	}
}
