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
	private boolean started = false;
	private boolean expiryDisabled = false;
	private boolean isIsExpiredMethodUnlocked = false;
	
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
	
	public void start() {
		this.started = true;
	}
	
	public boolean isStarted() {
		return this.started;
	}
	
	public void disableExpiry() {
		this.expiryDisabled = true;
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
	 * @throws RuntimeException nel caso in cui la sessione non sia stata avviata
	 */
	public void addSessionVariable(SessionVariable sessionVariable) {
		if(!started) {
			throw new RuntimeException("La sessione non è avviata!");
		}
		
		if(sessionID != null && !sessionVariables.contains(sessionVariable)) {
			sessionVariables.add(sessionVariable);
		}
	}
	
	/**
	 * Restituisce la variabile di sessione dato il nome della variabile
	 * 
	 * @param variableName il nome della variabile
	 * @return null se la variabile non esiste, altrimenti la SessionVariable
	 * @throws RuntimeException nel caso in cui la sessione non sia stata avviata
	 */
	public SessionVariable getSessionVariable(String variableName) {
		if(!started) {
			throw new RuntimeException("La sessione non è avviata!");
		}
		
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
	 * @throws RuntimeException nel caso in cui la sessione non sia stata avviata
	 */
	public boolean overrideSessionVariable(SessionVariable newSessionVariable) {
		if(!started) {
			throw new RuntimeException("La sessione non è avviata!");
		}
		
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
	 * @throws RuntimeException nel caso in cui la sessione non sia stata avviata
	 */
	public boolean changeSessionVariableValue(String varName, Object newValue) {
		if(!started) {
			throw new RuntimeException("La sessione non è avviata!");
		}
		
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
	 * @throws RuntimeException nel caso in cui la sessione non sia stata avviata
	 */
	public boolean removeSessionVariable(String varName) {
		if(!started) {
			throw new RuntimeException("La sessione non è avviata!");
		}
		
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
	 * Questo metodo restituisce un valore booleano che indica se la sessione è scaduta o no
	 * 
	 * @return <b>true</b> se la sessione è scaduta, altrimenti <b>false</b>
	 */
	protected boolean isExpired() {
		if(expiry == null || !started) {
			return true;
		}
		
		if(expiryDisabled) {
			return false;
		}
		
		return LocalDateTime.now().compareTo(expiry) >= 0;
	}
	
	protected void unlockIsExpiredMethod() {
		isIsExpiredMethodUnlocked = true;
	}
	
	protected boolean isIsExpiredMethodUnlocked() {
		return isIsExpiredMethodUnlocked;
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
