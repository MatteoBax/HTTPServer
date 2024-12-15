package com.matteo.HTTPServer.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import com.matteo.HTTPServer.utility.Utility;

/**
 * Classe che modella la JPanel per la schermata di configurazione
 * 
 * @author Matteo Basso
 */
public class ConfigJPanel extends JPanel {
    private final int MAX_PORT = 65535;
    private final int MIN_PORT = 49152;
    private String documentRoot = "";
    
    /**
     * Costruttore della JPanel per la schemata di configurazione
     * 
     * @param guiVars l'oggetto contenente la variabili di scambio nella GUI
     */
    public ConfigJPanel(GuiVars guiVars) {
    	super();
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JLabel label = new JLabel("Porta:");
        add(label);

        JTextField textField = new JTextField(10);
        add(textField);

        JLabel label1 = new JLabel("Document root:");
        add(label1);
        
        JButton chooseDirBtn = new JButton("Scegli");
        chooseDirBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DirectoryChooser dirChooser = new DirectoryChooser();
                boolean correct = false;
            	while(!correct) {
            		int returnValue = dirChooser.showOpenDialog(null);
                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = dirChooser.getSelectedFile();
                        if(selectedFile.exists() && selectedFile.isDirectory()) {
                        	documentRoot = selectedFile.getAbsolutePath();
                        	correct = true;
                        }
                    } else if(returnValue == JFileChooser.CANCEL_OPTION) {
                    	break;
                    }
            	}
            }
        });
        add(chooseDirBtn);

        JButton b = new JButton("Avvia");
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String insertedPort = textField.getText();
                int portNumber = 0;
                try {
                    portNumber = Integer.parseInt(insertedPort);
                } catch (NumberFormatException exc) {
                    showErrorDialog("Non è stato inserito un intero");
                    return;
                }

                if (portNumber >= MIN_PORT && portNumber <= MAX_PORT) {
                	if(Utility.isPortAvailable(portNumber)) {
                		if(!documentRoot.equals("")) {
                    		guiVars.setPort(insertedPort);
                    		guiVars.setDocumentRoot(documentRoot);
                    	} else {
                    		 showErrorDialog("La document root non è stata impostata!");
                    	}
                	} else {
                		showErrorDialog("La porta è giè in uso!\nSi prega di sceglierne un'altra.");
                	}
                	
                    
                } else {
                    showErrorDialog("Numero porta invalido.\nSi prega di inserire un numero di porta compreso tra " + MIN_PORT + " e " + MAX_PORT);
                }
            }
        });
        add(b);
        setSize(200,150);
    }

    /**
     * Mostra un dialog di errore
     * 
     * @param error il messaggio di errore
     */
    private void showErrorDialog(String error) {
        JOptionPane.showMessageDialog(this, error, "Errore", JOptionPane.ERROR_MESSAGE);
    }
}
