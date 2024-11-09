package com.matteo.HTTPServer.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.matteo.HTTPServer.enums.LogType;

/**
 * Classe del Thread che legge da uno stream di input i log e li scrive sulla GUI
 * 
 * @author Matteo Basso
 */
public class HandleLogThread implements Runnable {
	private LogJPanel logJPanel;
	private LogType logType;
	private BufferedReader bfReader;
	
	/**
	 * Il thread viene avviato automaticamente
	 * 
	 * @param logJPanel la JPanel dei log
	 * @param stream lo stream da cui leggere
	 * @param logType la tipologia di log (errore, normale, ...)
	 */
	public HandleLogThread(LogJPanel logJPanel, InputStream stream, LogType logType) {
		this.logJPanel = logJPanel;
		this.bfReader = new BufferedReader(new InputStreamReader(stream));
		this.logType = logType;
		new Thread(this).start();
	}
	
	@Override
	public void run() {
		while(!Thread.interrupted()) {
			try {
				if(bfReader.ready()) {
					logJPanel.writeLog(logType, bfReader.readLine());
				} else {
					System.err.flush();
					System.out.flush();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			} catch (IOException e) {}
			
			
		}
	}
}
