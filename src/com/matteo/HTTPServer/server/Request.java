package com.matteo.HTTPServer.server;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Vector;

/**
 * Classe Request
 * 
 * @author Matteo Basso
 */
public abstract class Request {
	private Vector<Header> headers = new Vector<Header>();
	private Vector<Cookie> cookies = new Vector<Cookie>();
	public final Vector<RequestParameter> requestParameters = new Vector<RequestParameter>();
	private String method; // il metodo usato nella richiesta (GET, POST ....)
	private Session session;
	
	/**
	 * 
	 * @param method il metodo usato nella richiesta (GET, POST ....)
	 */
	public Request(String method) {
		this.method = method;
		this.session = new Session();
	}
	
	/**
	 * Restituisce il contenuto dell'header in base al tipo
	 * 
	 * @param type la stringa contenente il tipo di headere
	 * @return una stringa contenente il contenuto dell'header, null se non esiste
	 */
	public String getHeaderContent(String type) {
		for(Header header : headers) {
			if(header.getType().equalsIgnoreCase(type)) {
				return header.getContent();
			}
		}
		
		return null;
	}
	
	/**
	 * Aggiunge un header alla richiesta
	 * 
	 * @param header l'header da aggiungere
	 */
	public void addHeader(Header header) {
		headers.add(header);
		if(header.getType().equalsIgnoreCase("Cookie")) {
			String[] cookiesSplitted = header.getContent().split(";");
			for(String cookieS : cookiesSplitted) {
				String cookieName = cookieS.substring(0, cookieS.indexOf("="));
				String cookieValue = cookieS.substring(cookieName.length() + 1);
				cookies.add(new Cookie(cookieName, cookieValue));
			}
			
		}
	}
	
	/**
	 * Restituisce gli Header (non sono in sola lettura)
	 * 
	 * @return la Vector contenente gli header
	 */
	public Vector<Header> getHeaders() {
		return this.headers;
	}
	
	/**
	 * Imposta gli Header
	 * 
	 * @param headers la Vector contenente gli Header
	 */
	public void setHeaders(Vector<Header> headers) {
		this.headers.clear();
		for(Header header : headers) {
			addHeader(header);
		}
	}
	
	/**
	 * Restituisce i Cookie (non sono in sola lettura)
	 * 
	 * @return la Vector contenente i cookie
	 */
	public Vector<Cookie> getCookies() {
		return this.cookies;
	}
	/**
	 * Aggiunge un parametro alla richiesta
	 * 
	 * @param name il nome del parametro
	 * @param value il valore del parametro
	 */
	public void addRequestParam(String name, String value) {
		requestParameters.add(new RequestParameter(name, value));
	}
	
	/**
	 * Aggiunge i parametri della Collection alla richiesta 
	 * @param parameters la Collection
	 */
	public void addRequestParams(Collection<RequestParameter> parameters) {
		requestParameters.addAll(parameters);
	}
	/**
	 * Restituisce il valore del parametro dato il nome
	 * 
	 * @param name il nome del parametro
	 * @return null se non esiste, altrimenti il valore del parametro
	 */
	public String getRequestParamValue(String name) {
		for(RequestParameter param : requestParameters) {
			if(param.getName().equals(name)) {
				return param.getValue();
			}
		}
		return null;
	}
	
	/**
	 * Restituisce il metodo usato nella richiesta (GET, POST ...)
	 * 
	 * @return una string contenente il metodo usato nella richiesta (GET, POST ...)
	 */
	public String getMethod() {
		return this.method;
	}
	
	protected String getQueryStringForCGI() {
		StringBuilder sb = new StringBuilder("");
		for(RequestParameter param : requestParameters) {
			try {
				sb.append(URLEncoder.encode(param.getName(), "UTF-8") + "=" + URLEncoder.encode(param.getValue(), "UTF-8") + "&");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}
	/**
	 * Imposta la sessione
	 * 
	 * @param session la sessione
	 */
	protected void setSession(Session session) {
		this.session = session;
	}
	
	/**
	 * Restituisce la sessione associata
	 */
	public Session getSession() {
		return this.session;
	}
	
}
