package com.matteo.HTTPServer.gui;

import com.matteo.HTTPServer.server.Server;

/**
 * Classe che contiene le variabili di scambio della GUI
 * 
 * @author Matteo Basso
 */
public class GuiVars {
	private String port = null;
	private String documentRoot = null;
	private Server server = null;
	
	/**
	 * 
	 * @return la porta inserita
	 */
	public synchronized String getPort() {
		while(port == null) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return port;
	}
	
	/**
	 * 
	 * @return la document root inserita
	 */
	public synchronized String getDocumentRoot() {
		while(documentRoot == null) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return documentRoot;
	}
	
	/**
	 * Imposta la porta
	 * 
	 * @param port la stringa contenente la porta
	 */
	public synchronized void setPort(String port) {
		this.port = port;
		notifyAll();
	}
	
	/**
	 * Imposta la document root
	 * 
	 * @param documentRoot la stringa contenente la document root
	 */
	public synchronized void setDocumentRoot(String documentRoot) {
		this.documentRoot = documentRoot;
		notifyAll();
	}
	
	/**
	 * 
	 * @param server l'oggetto di classe Server
	 */
	public synchronized void setServer(Server server) {
		this.server = server;
		notifyAll();
	}
	
	/**
	 * 
	 * @return l'oggetto di classe Server
	 */
	public synchronized Server getServer() {
		while(server == null) {
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return server;
	}
}
