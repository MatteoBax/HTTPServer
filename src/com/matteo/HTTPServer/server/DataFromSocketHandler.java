package com.matteo.HTTPServer.server;

/**
 * Questa classe contiene i dati emessi dal socket handler
 * 
 * @author Matteo Basso
 */
public class DataFromSocketHandler {
	private String requestPath; // l'url della richiesta senza hostname
	private Request request; // la richiesta
	private Response response; // la risposta
	private String method; // il metodo usato nella richiesta (GET, POST ....)
	
	/**
	 * Costruttore della classe DataFromSocketHandler
	 * 
	 * @param requestPath l'url della richiesta senza hostname
	 * @param request la richiesta
	 * @param response la risposta
	 */
	public DataFromSocketHandler(String requestPath, Request request, Response response) {
		this.requestPath = requestPath;
		this.request = request;
		this.response = response;
		this.method = request.getMethod();
	}
	
	/**
	 * Restituisce la requestPath
	 * 
	 * @return una stringa contenente l'url della richiesta senza hostname
	 */
	public String getRequestPath() {
		return requestPath;
	}
	
	/**
	 * Restituisce la Request
	 * 
	 * @return l'oggetto Request contenente la richiesta del client
	 */
	public Request getRequest() {
		return request;
	}
	
	/**
	 * Restituisce la Response
	 * 
	 * @return l'oggetto Response contenente la risposta del server
	 */
	public Response getResponse() {
		return response;
	}
	
	/**
	 * Restituisce il metodo usato nella richiesta (GET, POST ...)
	 * 
	 * @return una string contenente il metodo usato nella richiesta (GET, POST ...)
	 */
	public String getMethod() {
		return this.method;
	}
}
