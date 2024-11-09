package com.matteo.HTTPServer.server;

import java.time.LocalDateTime;
import java.util.Vector;

/**
 * Classe Session
 * 
 * @author Matteo Basso
 */
public class Session implements Comparable<Session>{
	protected String sessionID;
	protected Vector<SessionVariable> sessionVariables = new Vector<SessionVariable>();
	private final short SESSION_DURATION = 24; // 24h 
	private LocalDateTime expiry;
	
	/**
	 * Costruttore dell'oggetto Session
	 */
	public Session() {
		this.sessionID = null;
		expiry = LocalDateTime.now().plusHours(SESSION_DURATION);
	}
	
	/**
	 * Costruttore dell'oggetto Session
	 * 
	 * @param sessionID l'id della sessione
	 */
	public Session(String sessionID) {
		this();
		this.sessionID = sessionID;
	}
	
	/**
	 * Restituisce l'id della sessione
	 * 
	 * @return l'id della sessione
	 */
	public String getSessionID() {
		return this.sessionID;
	}
	
	/**
	 * Aggiunge una variabile di sessione (verrà aggiunta soltanto se è impostato sessionID)
	 * 
	 * @param sessionVariable la variabile da aggiungere
	 */
	public void addSessionVariable(SessionVariable sessionVariable) {
		if(sessionID != null && !sessionVariables.contains(sessionVariable)) {
			sessionVariables.add(sessionVariable);
		}
	}
	
	/**
	 * Restituisce la variabile di sessione dato il nome della variabile
	 * 
	 * @param variableName il nome della variabile
	 * @return null se la variabile non esiste, altrimenti la SessionVariable
	 */
	public SessionVariable getSessionVariable(String variableName) {
		for(SessionVariable var : sessionVariables) {
			if(var.getName().equals(variableName)) {
				return var;
			}
		}
		return null;
	}
	
	/**
	 * Sovrascrive la variabile di sessione con quella nuova in base al nome dato alla variabile
	 * 
	 * @param newSessionVariable l'oggetto SessionVariable
	 * @return <b>true</b> se è stata sovrascritta, <b>false</b> se non è stata sovrascritta in quanto non esiste
	 */
	public boolean overrideSessionVariable(SessionVariable newSessionVariable) {
		for(int i = 0; i < sessionVariables.size(); i++) {
			if(sessionVariables.get(i).getName().equals(newSessionVariable.getName())) {
				sessionVariables.set(i, newSessionVariable);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Cambia il valore della variabile dato il nome
	 * 
	 * @param varName il nome della variabile
	 * @param newValue il nuovo valore della variabile
	 * @return <b>true</b> se il valore è stato cambiato, <b>false</b> se la variabile non esiste
	 */
	public boolean changeSessionVariableValue(String varName, Object newValue) {
		for(SessionVariable var : sessionVariables) {
			if(var.getName().equals(varName)) {
				var.setValue(newValue);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Rimuove la variabile di sessione
	 * 
	 * @param varName il nome della variabile da rimuovere
	 * @return <b>true</b> se è stata rimossa, <b>false</b> se non è stata rimossa in quanto inesistente
	 */
	public boolean removeSessionVariable(String varName) {
		for(int i = 0; i < sessionVariables.size(); i++) {
			if(sessionVariables.get(i).getName().equals(varName)) {
				sessionVariables.remove(i);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Distrugge la sessione
	 */
	public void destroy() {
		this.sessionID = null;
		this.sessionVariables = null;
		this.expiry = LocalDateTime.now().minusMinutes(1);
	}
	
	/**
	 * Questo metodo restituice la scandenza della sessione
	 * 
	 * @return la scadenza della sessione
	 */
	protected LocalDateTime getExpiry() {
		return this.expiry;
	}
	
	/**
	 * Questo metodo restituisce un valore booleano che indica se la sessione è scaduta o no
	 * 
	 * @return <b>true</b> se la sessione è scaduta, altrimenti <b>false</b>
	 */
	protected boolean isExpired() {
		if(expiry == null) {
			return true;
		}
		
		return LocalDateTime.now().compareTo(expiry) >= 0;
	}
	
	/**
	 * Questo metodo ricostruisce l'oggetto
	 * 
	 * @param session il nuovo oggetto
	 */
	protected void reInitClass(Session session) {
		this.sessionID = session.sessionID;
		this.sessionVariables = session.sessionVariables;
		this.expiry = session.expiry;
	}
	
	@Override
	public int compareTo(Session toCompare) {
		return this.sessionID.compareTo(toCompare.getSessionID());
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Session) {
			Session session = (Session)obj;
			return this.sessionID.equals(session.sessionID);
		} else {
			return false;
		}
		
	}
	
	@Override
	public String toString() {
		return sessionID;
	}
}
