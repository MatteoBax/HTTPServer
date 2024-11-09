package com.matteo.HTTPServer.server;

/**
 * Classe SessionVariable
 * 
 * @author Matteo Basso
 */
public class SessionVariable {
	private String name;
	private Object value;
	
	/**
	 * Costruttore oggetto SessionVariable
	 * 
	 * @param name il nome della variabile
	 * @param value il valore della variabile
	 */
	public SessionVariable(String name, Object value) {
		this.name = name;
		this.value = value;
	}
	
	/**
	 * Cambia il nome alla variabile
	 * @param name il nuovo nome
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Cambia il valore alla variabile
	 * @param value il nuovo valore
	 */
	public void setValue(Object value) {
		this.value = value;
	}
	
	/**
	 * Restituisce il nome della variabile
	 * 
	 * @return il nome della variabile
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Restituisce il valore della variabile
	 * 
	 * @return il valore della variabile
	 */
	public Object getValue() {
		return this.value;
	}
	
	@Override
	public String toString() {
		return this.name + "=" + this.value;
	}
}
