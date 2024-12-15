package com.matteo.HTTPServer.gui;

import java.util.Date;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import com.matteo.HTTPServer.enums.LogType;

/**
 * Classe che implementa la JPanel per i log del server
 * 
 * @author Matteo Basso
 */
public class LogJPanel extends JPanel {
	private JScrollTextPane scrollTextArea = new JScrollTextPane(700, 300, false);
	private boolean isWriting = false;
	
	/**
	 * Costruttore
	 * 
	 * @param guiVars oggetto contenente le variabili condivise tra la GUI
	 */
    public LogJPanel(GuiVars guiVars) {
    	super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(Box.createVerticalStrut(10));

        JLabel titleLabel = new JLabel("Log");
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);
        add(titleLabel);

        add(Box.createVerticalStrut(10));
        add(scrollTextArea);

        add(Box.createVerticalStrut(10));
        
        JButton stopBtn = new JButton("STOP");
        stopBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	guiVars.getServer().stopServer();
            }
        });
        add(stopBtn);
    }
    
    /**
     * Questo metodo acquisisce il lock del JScrollTextPane
     */
    private synchronized void acquireLock() {
    	while(isWriting) {
    		try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	isWriting = true;
    }
    
    /**
     * Questo metodo rilascia il lock del JScrollTextPane
     */
    private synchronized void releaseLock() {
    	isWriting = false;
    	notify();
    }
    
    /**
     * Questo metodo scrive un log nella JScrollTextPane ed è thread safe
     * 
     * @param logType la tipologia di log
     * @param log il log
     */
    public void writeLog(LogType logType, String log) {
    	acquireLock();
    	if(logType.equals(LogType.NORMAL)) {
    		scrollTextArea.append(new Date() + " :  " + log + "\n");
    	} else if(logType.equals(LogType.ERROR)){
    		scrollTextArea.append(new Date() + " :  " + log + "\n", Color.RED);
    	}
    	releaseLock();
    }
}
