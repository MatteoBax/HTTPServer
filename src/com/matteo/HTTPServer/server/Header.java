package com.matteo.HTTPServer.server;

/**
 * Classe Header
 * @author Matteo Basso
 */
public class Header {
	private String type; // tipo di header
	private String content; // contenuto header
	
	/**
	 * Construttore oggetto Header
	 * 
	 * @param type stringa contenente il tipo di header
	 * @param content stringa contenente il contenuto dell'header
	 */
	public Header(String type, String content) {
		this.type = type;
		this.content = content;
	}
	
	/**
	 * Restituisce il tipo di header
	 * 
	 * @return una stringa contenente il tipo di header
	 */
	public String getType() {
		return this.type;
	}
	
	/**
	 * Restituisce il contenuto dell'header
	 * 
	 * @return una stringa contenente il contenuto dell'header
	 */
	public String getContent() {
		return this.content;
	}
	
	@Override
	public String toString() {
		//return type + ": " + content + "\r\n";
		return type + ": " + content;
	}
}
