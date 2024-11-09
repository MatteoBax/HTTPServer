package com.matteo.HTTPServer.gui;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

/**
 * Classe che implementa un DirectoryChooser sfruttando la classe JFileChooser
 * 
 * @author Matteo Basso
 */
public class DirectoryChooser extends JFileChooser {
	public DirectoryChooser() {
		super(FileSystemView.getFileSystemView().getHomeDirectory());
		setDialogTitle("Seleziona una cartella");
		setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	}
}

