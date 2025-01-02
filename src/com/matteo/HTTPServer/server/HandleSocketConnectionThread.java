package com.matteo.HTTPServer.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.matteo.HTTPServer.enums.DefaultRequestHandlerReturnCode;
import com.matteo.HTTPServer.enums.Protocol;
import com.matteo.HTTPServer.enums.ResourceExistStatus;
import com.matteo.HTTPServer.utility.Queue;
import com.matteo.HTTPServer.utility.Utility;
import com.matteo.MavenUtility.loadResourceFromClassLoader.FileResourcesUtils;
import com.optimaize.langdetect.cybozu.util.Util;


/**
 * Thread che ha il compito di gestire il socket
 * 
 * @author Matteo Basso
 */
public class HandleSocketConnectionThread implements Runnable {
	private Server server; // l'oggetto server
	private Queue<DataFromSocketHandler> actionQueue; // coda delle elaborazioni delle richieste verso le route create dall'utente
	private Socket socket;
	private BufferedInputStream bi = null; // legge i dati dal socket
	private Protocol protocol;
	private ClassLoader classLoader; // il classLoader da dove caricare le risorse (se è null allora verranno caricate dal file system)
	private Vector<Route> registeredRoutes; // le route create dall'utente
	private Vector<Session> sessions; // le sessioni aperte lato server

	private static final String SESSIONID_HEADER_NAME_PREFIX = "sessionID=";
	private final HashMap<String, BiConsumer<Request, Response>> methods = new HashMap<String, BiConsumer<Request, Response>>();
	
	private boolean handlingAnAPI = false; // indica se la richiesta in gestione è un'API

	// gli unici metodi attualmente supportati dal server sono GET, POST, OPTIONS, PUT, gli altri li considero invalidi
	private void initMethodsHashMap() {
		methods.put("GET", this::handleGET);
		methods.put("POST", this::handlePOST);
		methods.put("HEAD", this::handleHEAD);
		methods.put("OPTIONS", this::handleOPTIONS);
		methods.put("PUT", this::handlePUT);
		methods.put("DELETE", this::handleDELETE);
	}
	
	/**
	 * Costruttore del thread (lo avvia)
	 * 
	 * @param protocol il tipo di protocollo usato (HTTP / HTTPS)
	 * @param registeredRoutes la Vector contenente le route create dall'utente tramite i metodi server.get() e server.post()
	 * @param actionQueue coda delle elaborazioni delle richieste verso le route create dall'utente
	 * @param socket il socket da gestire
	 * @param classLoader il classLoader da dove caricare le risorse (se è null allora verranno caricate dal file system)
	 */
	public HandleSocketConnectionThread(Server server, Protocol protocol, Vector<Route> registeredRoutes, Queue<DataFromSocketHandler> actionQueue, Vector<Session> sessions, Socket socket, ClassLoader classLoader) {
		this.server = server;
		this.protocol = protocol;
		this.socket = socket;
		this.classLoader = classLoader;
		this.actionQueue = actionQueue;
		this.registeredRoutes = registeredRoutes;
		this.sessions = sessions;
		initMethodsHashMap();
		new Thread(this).start();
	}
	
	/**
	 * Questo metodo restituisce un'ArrayList contenente i parametri della richiesta dato il body in JSON della richiesta
	 * 
	 * @param jsonRequest il body in JSON della richiesta
	 * @return un'ArrayList contenente i parametri della richiesta
	 */
	private static ArrayList<RequestParameter> getRequestParameters(JsonObject jsonRequest) {
		ArrayList<RequestParameter> requestParameters = new ArrayList<RequestParameter>();
		if(jsonRequest != null) {
			Set<String> keys = jsonRequest.keySet();
			for(String key : keys) {
				requestParameters.add(new RequestParameter(key, jsonRequest.get(key).getAsString()));
			}
		}
		return requestParameters;
	}

	/**
	 * Questo metodo restituisce un'ArrayList contenente i parametri della richiesta data la linea della richiesta
	 * 
	 * @param requestLine la linea della richiesta (il body nel caso il metodo utillizzato sia POST, altrimenti se il metodo utilizzato è GET va passata solo la parte della query dell'url)
	 * @return un'ArrayList contenente i parametri della richiesta
	 */
	private static ArrayList<RequestParameter> getRequestParameters(String requestLine) {
		ArrayList<RequestParameter> requestParameters = new ArrayList<RequestParameter>();
		if(requestLine != null) {
			String[] params = requestLine.split("&");
			for(String param : params) {
				String[] splitted = param.split("=");
				if(splitted.length == 2) {
					String name;
					String value;
					try {
						name = URLDecoder.decode(splitted[0], "UTF-8");
						value = URLDecoder.decode(splitted[1], "UTF-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
						break;
					}
					requestParameters.add(new RequestParameter(name, value));
				}
			}
		}
		return requestParameters;
	}
	
	/**
	 * Metodo che restituisce un id univoco per la sessione
	 * 
	 * @return una stringa contenente l'id della sessione
	 */
	private String generateUniqueSessionId() {
		String generatedSessionId = "";
		boolean found = false;
		final char[] toExclude = {';', '=', '\\'};
		do {
			found = false;
			generatedSessionId = Utility.generateSecureRandomString(256, toExclude);
			synchronized(sessions) {
				for(Session session : sessions) {
					String sessionID = session.getSessionID();
					if(session != null && sessionID != null && sessionID.equals(generatedSessionId)) {
						found = true;
						break;
					}
				}
			}
		} while (found);
		
		return generatedSessionId;
	}
	
	private void initSession(Request request, Response response) {
		String correctId;
		String setCookieHeaderContent = request.getHeaderContent("Cookie");
		if(setCookieHeaderContent == null) {
			final String id = generateUniqueSessionId();
			Session session = new Session(id);
			setCookieHeaderContent = SESSIONID_HEADER_NAME_PREFIX + id;
			sessions.add(session);
			correctId = id;
		} else {
			String[] splittedHeader = setCookieHeaderContent.split(";");
			String sessionId = null;
			for (String attr : splittedHeader) {
				if(attr.contains(SESSIONID_HEADER_NAME_PREFIX)) {
					sessionId = attr.replaceFirst(SESSIONID_HEADER_NAME_PREFIX, "");
					break;
				}
			}
			Session foundSession = null;
			
			Session toCmp = new Session(sessionId);
			synchronized(sessions) {
				if(sessionId != null && sessions.contains(toCmp)) {
					foundSession = sessions.get(sessions.indexOf(toCmp));
				}
			}
			
			if(foundSession == null || !foundSession.isStarted()) {
				if(foundSession != null) {
					// se la sessione è stata trovata ed è entrato in questo if vuol dire che la sessione non è stata avviata quindi la elimino
					foundSession.destroy();
				}
				// creo un cookie sessionID scaduto in modo tale da sovrascrivere quello lato client con quello nuovo
				String expiredCookie = SESSIONID_HEADER_NAME_PREFIX + "; Expires=" + Utility.formatDateAsUTCDateString(new Date(0L))+ "; Path=/";
		        response.addHeader(new Header("Set-Cookie", expiredCookie));
				final String id = generateUniqueSessionId();
				Session session = new Session(id);
				setCookieHeaderContent = SESSIONID_HEADER_NAME_PREFIX + id;
				sessions.add(session);
				correctId = id;
			} else {
				setCookieHeaderContent = SESSIONID_HEADER_NAME_PREFIX + sessionId;
				correctId = sessionId;
			}
			
		}
		
		synchronized(sessions) {
			request.setSession(sessions.elementAt(sessions.indexOf(new Session(correctId))));
		}
		
		response.addHeader(new Header("Set-Cookie", setCookieHeaderContent + ";Path=/"));
	}
	
	private void sendFile(File f, Request request, Response response) {
		if(request.getMethod().equals("GET") || request.getMethod().equals("POST")) {
			response.addHeader(new Header("Last-Modified", Utility.formatDateAsUTCDateString(new Date(f.lastModified()))));
			if(f.getAbsolutePath().endsWith(".html")) {
				//initSession(request, response);
			}
			response.send(f);
		}
	}
	
	private void sendFile(InputStream iSForDetectingMimeType, InputStream isForDetectingContentLength, InputStream isForSendingFile, Request request, Response response, boolean needAsession) {
		if(request.getMethod().equals("GET") || request.getMethod().equals("POST")) {
			if(needAsession) {
				//initSession(request, response);
			}
			// TODO implement PHP
			response.send(iSForDetectingMimeType, isForDetectingContentLength, isForSendingFile);
		}
	}
	
	private Resource findAResource(Request request) {
		final String reqResource = request.getResource();
		String resource = reqResource;
		// cerco se l'API esiste
		synchronized(registeredRoutes) {
			for(Route route : registeredRoutes) {
				if(route.getPath().equals(resource)) {
					return new Resource(ResourceExistStatus.EXIST, resource, true);
				}
			}
		}

		// cerco se il file esiste
		resource = resource.substring(1); // rimuovo la prima /
		String documentRoot = server.serverConfig.getDocumentRoot();
		if(classLoader == null) {
			if(documentRoot != null) {
				if(File.separator.equals("\\")) {
					// sostituisco il separator della resource con \ solo se il sistema operativo ha come file separator il carattere \
					resource = resource.replace("/", "\\");
				}
				File f = new File(documentRoot + resource);
				File f1 = new File(documentRoot + resource + "index.html");
				File f2 = new File(documentRoot + resource + "index.php");

				if(f.isFile() || f1.isFile() || f2.isFile()) {
					return new Resource(ResourceExistStatus.EXIST, reqResource);
				} else if (!reqResource.endsWith("/") && f.isDirectory() && (new File(documentRoot + resource + File.separator + "index.html").isFile() || new File(documentRoot + resource + File.separator + "index.php").isFile())) {
					return new Resource(ResourceExistStatus.NEED_A_REDIRECT, reqResource + "/");
				} else {
					return new Resource(ResourceExistStatus.NOT_EXIST, null);
				}
			}
		} else {
			if(documentRoot != null) {
				if(documentRoot.endsWith("/")) {
					resource = documentRoot + resource;
				} else {
					resource = documentRoot + "/" + resource;
				}
			}
			if(FileResourcesUtils.isFile(classLoader, resource)) {
				return new Resource(ResourceExistStatus.EXIST, resource);
			} else if(!resource.endsWith("/") && FileResourcesUtils.isDirectory(classLoader, resource + "/")) {
				return new Resource(ResourceExistStatus.NEED_A_REDIRECT, resource + "/");
			} else if(resource.endsWith("/") && (FileResourcesUtils.isFile(classLoader, resource + "index.html") || FileResourcesUtils.isFile(classLoader, resource + "index.php"))) {
				return new Resource(ResourceExistStatus.EXIST, resource);
			}
		}	
		
		return new Resource(ResourceExistStatus.NOT_EXIST, null);
	}
	private ResourceExistStatus handleFileSend(String resource, Request request, Response response) throws IOException {
		final String reqResource = resource;
		// cerco se il file esiste e lo invio
		resource = resource.substring(1); // rimuovo la prima /
		String documentRoot = server.serverConfig.getDocumentRoot();
		boolean found = false;
		if(classLoader == null) {
			if(documentRoot != null) {
				if(File.separator.equals("\\")) {
					// sostituisco il separator della resource con \ solo se il sistema operativo ha come file separator il carattere \
					resource = resource.replace("/", "\\");
				}
				File f = new File(documentRoot + resource);
				
				if(!f.getCanonicalPath().startsWith(documentRoot.substring(0, documentRoot.length()-1))) {
					return ResourceExistStatus.CANNOT_ACCESS;
				}
				File f1 = new File(documentRoot + resource + "index.html");
				File f2 = new File(documentRoot + resource + "index.php");
				if(f.isFile()) {
					sendFile(f, request, response);
					found = true;
				} else if (f1.isFile()) {
					sendFile(f1, request, response);
					found = true;
				} else if (f2.isFile()) {
					sendFile(f2, request, response);
					found = true;
				} else if (!resource.endsWith("/") && (new File(documentRoot + resource + File.separator + "index.html").isFile() || new File(documentRoot + resource + File.separator + "index.php").isFile() ) && f.isDirectory()) {
					response.redirect(reqResource + "/");
					found = true;
				}
			}
		} else {
			boolean needAsession = resource.endsWith(".html");
			if(documentRoot != null) {
				if(documentRoot.endsWith("/")) {
					resource = documentRoot + resource;
				} else {
					resource = documentRoot + "/" + resource;
				}
			}
			if(FileResourcesUtils.isFile(classLoader, resource)) {
				found = true;
			} else if(!resource.endsWith("/") && FileResourcesUtils.isDirectory(classLoader, resource + "/")) {
				response.redirect(reqResource + "/");
				found = true;
			} else if(resource.endsWith("/") && FileResourcesUtils.isFile(classLoader, resource + "index.html")) {
				needAsession = true;
				resource += "index.html";
				found = true;
			} else if(resource.endsWith("/") && FileResourcesUtils.isFile(classLoader, resource + "index.php")) {
				resource += "index.php";
				found = true;
			}

			if(found) {
				try {
					InputStream s1 = FileResourcesUtils.getFileFromResourceAsStream(classLoader, resource);
					InputStream s2 = FileResourcesUtils.getFileFromResourceAsStream(classLoader, resource);
					InputStream s3 = FileResourcesUtils.getFileFromResourceAsStream(classLoader, resource);
					sendFile(s1, s2, s3, request, response, needAsession);
					try {
						s1.close();
						s2.close();
					} catch (IOException e) {
						
					};
					
					System.gc();
				} catch (FileNotFoundException e) {
					found = false;
				}
			}
		}
		
		
		return found ? ResourceExistStatus.EXIST : ResourceExistStatus.NOT_EXIST;
	}

	private void addDefaultHeadersForOPTIONS(Response response, boolean isAPI) {
		response.addHeader(new Header("Access-Control-Allow-Methods", "HEAD, GET, POST, OPTIONS" + ((!isAPI) ? ", PUT, DELETE" : "")));
		response.addHeader(new Header("Access-Control-Allow-Headers", "Content-Type, Content-Length, Upgrade-Insecure-Requests, Cookie, Host"));
		response.addHeader(new Header("Access-Control-Max-Age", "0"));
		response.addHeader(new Header("Connection", "Keep-Alive"));
		response.addHeader(new Header("Content-Length", "0"));
	}

	private DefaultRequestHandlerReturnCode defaultRequestHandler(Request request, Response response) throws IOException{
		String resource = request.getResource();
		String originalResource = resource;

		response.addHeader(new Header("Date", Utility.formatDateAsUTCDateString(new Date())));
						
		// Aggiungo gli Header di default
		Vector<Header> defaultHeaders = server.getDefaultHeaders();
		synchronized(defaultHeaders) {
			for(Header defaultHeader : defaultHeaders) {
				response.addHeader(defaultHeader);
			}
		}
		String httpVersion = request.getHTTPversion();
		// le uniche versioni di HTTP supportate da questo server sono la 1.0 e la 1.1, le altre le considero invalide
		if(!httpVersion.equals(HTTPVersion.HTTP1_0) && !httpVersion.equals(HTTPVersion.HTTP1_1)) {
			response.status(505).send();
			return DefaultRequestHandlerReturnCode.INVALID_HTTP_VERSION;
		}

		URL url = null;
		try {
			url = new URI("http://localhost" + resource).toURL();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		if(url != null) {
			resource = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8.name()); // toglo i parametri dalla resource in modo tale da poter gestire le route
			request.setResource(resource);
			request.addRequestParams(getRequestParameters(url.getQuery()));
		}
			
		
		// leggo tutti gli header della richiesta
		while(true) {
			String line = Utility.readLineFromBufferedInputStream(bi);
			// gli header sono separati dal body con una riga vuota
			if(line.equals("")) {
				break;
			} else {
				String headerType = line.substring(0, line.indexOf(':'));
				String content = line.substring(line.indexOf(':') + 1).trim();
				request.addHeader(new Header(headerType, content)); // salvo gli header
			}
		}

		String hostHeader = request.getHeaderContent("Host");
		String updateInsecureRequestHeader = request.getHeaderContent("Upgrade-Insecure-Requests");
		
		// Gestisce la CORS
		if(server.isCORSallowed()) {
			String origin = request.getHeaderContent("Origin");
			if(origin != null) {
				response.overrideHeader(new Header("Access-Control-Allow-Origin", origin));
			}
		}

		if(!request.getMethod().equals("PUT") && updateInsecureRequestHeader != null && updateInsecureRequestHeader.equals("1") && hostHeader != null && !hostHeader.trim().isEmpty() && protocol == Protocol.HTTP && server.usesHTTPS()) {
			hostHeader = hostHeader.trim();
			int index = hostHeader.indexOf(":");
			String newPath = "https://";
			if(index != -1) {
				newPath += hostHeader.substring(0, index);
			} else {
				newPath += hostHeader;
			}
			newPath += ":" + server.serverConfig.getHTTPS_Port() + originalResource;
			response.redirect(newPath);
			return DefaultRequestHandlerReturnCode.REDIRECTED_REQUEST;
		}

		return DefaultRequestHandlerReturnCode.OK;
	}

	private void handleGETandPOST(Request request, Response response) throws IOException {
		long contentLength = -1L;
		String contentLengthHeaderContent = request.getHeaderContent("Content-Length");

		if(contentLengthHeaderContent != null) {
			try {
				contentLength = Long.parseLong(contentLengthHeaderContent);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
			
		if(contentLength > 0) {
			String contentTypeHeader = request.getHeaderContent("Content-Type");
			if(contentTypeHeader.contains("application/x-www-form-urlencoded")) {
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				byte[] buffer = new byte[4096];
				int bytesRead;
				while (byteArrayOutputStream.size() < contentLength && (bytesRead = bi.read(buffer)) != -1) {
					byteArrayOutputStream.write(buffer, 0, bytesRead);
				}
				String body = byteArrayOutputStream.toString();
				request.addRequestParams(getRequestParameters(body));
			} else if(contentTypeHeader.contains("multipart/form-data")) {
				response.status(415);
			} else if(contentTypeHeader.toLowerCase().contains("application/json")) {
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				byte[] buffer = new byte[4096];
				int bytesRead;
				while (byteArrayOutputStream.size() < contentLength && (bytesRead = bi.read(buffer)) != -1) {
					byteArrayOutputStream.write(buffer, 0, bytesRead);
				}

				String body = byteArrayOutputStream.toString();
				JsonElement json = null;
				try {
					json = JsonParser.parseString(body);
				} catch (JsonParseException e)  {}

				if(json != null && json.isJsonObject()) {
					request.addRequestParams(getRequestParameters(json.getAsJsonObject()));
				} else {
					response.status(400).send("Body invalido!");
				}
			} else {
				// upload
				File file = new File("prova" + Utility.getFileExtensionFromMimeType(contentTypeHeader));
				BufferedOutputStream  bos = new BufferedOutputStream (new FileOutputStream(file));
				byte[] buffer = new byte[4096];
				int bytesRead;
				long totalBytes = 0L;
				while(totalBytes < contentLength) {
					bytesRead = bi.read(buffer);
					if(bytesRead != -1) {
						bos.write(buffer, 0, bytesRead);
						totalBytes += bytesRead;
					} else {
						break;
					}
					
				}
				bos.flush();
				bos.close();
			}
		}
		
		String resource = request.getResource();
		/*
		 * Se la risorsa richiesta è una route creata dall'utente, l'aggiungo nella coda delle elaborazioni
		 * delle richieste della route
		*/
		if(registeredRoutes.contains(new Route(request.getMethod(), resource))) {
			initSession(request, response);
			DataFromSocketHandler dt = new DataFromSocketHandler(resource, request, response) ;
			actionQueue.add(dt);
			handlingAnAPI = true;
		} else {
			switch(handleFileSend(resource, request, response)) {
				case NOT_EXIST:
					response.status(404).send();
					break;
				case CANNOT_ACCESS:
					response.status(403).send();
					break;
				default:
					break;
			}
		}
	}

	private void handleGET(Request request, Response response) {
		// TODO
		String resource = request.getResource();
		if(request.getHTTPversion().equals(HTTPVersion.HTTP0_9)) {
			try {
				switch(handleFileSend(resource, request, response)) {
					case NOT_EXIST:
						response.status(404).send();
						return;
					case CANNOT_ACCESS:
						response.status(403).send();
						return;
					default:
						break;
				}
			} catch (IOException e) {
				response.status(500).send();
			}
		}

		try {
			if(defaultRequestHandler(request, response) == DefaultRequestHandlerReturnCode.OK) {
				handleGETandPOST(request, response);
			}
		} catch (IOException e) {
			response.status(500).send();
		}
	}

	private void handlePOST(Request request, Response response) {
		// TODO
		if(request.getHTTPversion().equals(HTTPVersion.HTTP0_9)) {
			response.status(405).send();
			return;
		}

		try {
			if(defaultRequestHandler(request, response) == DefaultRequestHandlerReturnCode.OK) {
				handleGETandPOST(request, response);
			}
		} catch (IOException e) {
			response.status(500).send();
		}
	}
	
	private void handleHEAD(Request request, Response response) {
		// TODO
		if(request.getHTTPversion().equals(HTTPVersion.HTTP0_9)) {
			response.status(405).send();
			return;
		}

		try {
			if(defaultRequestHandler(request, response) == DefaultRequestHandlerReturnCode.OK) {
				Resource foundResource = findAResource(request);
				if(foundResource.getExistStatus() == ResourceExistStatus.NOT_EXIST) {
					response.status(404);
				} else {
					if(foundResource.isAPI()) {
						response.status(405);
						return;
					}
					
					String resource = foundResource.getResourcePath();
					resource = resource.substring(1); // rimuovo la prima /
					if(File.separator.equals("\\")) {
						// sostituisco il separator della resource con \ solo se il sistema operativo ha come file separator il carattere \
						resource = resource.replace("/", "\\");
					}

					String documentRoot = server.serverConfig.getDocumentRoot();
					if(documentRoot != null) {
						File f = new File(documentRoot + resource);
						File f1 = new File(documentRoot + resource + "index.html");
						if(f.isFile()) {
							setHeaderForHeadMethod(f, response);
							return;
						} else if (f1.isFile()) {
							setHeaderForHeadMethod(f1, response);
							return;
						}
					}
				}
			}
		} catch (IOException e) {
			response.status(500).send();
		}
	}

	private void handleOPTIONS(Request request, Response response) {
		if(request.getHTTPversion().equals(HTTPVersion.HTTP0_9) || request.getHTTPversion().equals(HTTPVersion.HTTP1_0)) {
			response.status(405);
			return;
		}
		
		try {
			if(request.getResource().equals("*")) {
				addDefaultHeadersForOPTIONS(response, false);
				response.status(204);
			} else if(defaultRequestHandler(request, response) == DefaultRequestHandlerReturnCode.OK) {
				Resource resource = findAResource(request);
				boolean isAPI = resource.isAPI();
				ResourceExistStatus status = resource.getExistStatus();
				if(status == ResourceExistStatus.EXIST) {
					addDefaultHeadersForOPTIONS(response, isAPI);
					response.status(204);
				} else if (status == ResourceExistStatus.NEED_A_REDIRECT) {
					/* 
					gli dico che la resource è un'API in modo da non aggiungere i metodi HTTP non supportati (es PUT, DELETE, PATCH) 
					dato che neanche le API li supportano
					*/
					addDefaultHeadersForOPTIONS(response, true);
					response.addHeader(new Header("Location", resource.getResourcePath()));
					response.status(301);
				} else if (status == ResourceExistStatus.NOT_EXIST) {
					response.status(404);
				}
			} else {

			}
		} catch (IOException e) {
			response.status(500);
		}
	}

	private void handlePUT(Request request, Response response) {
		if(request.getHTTPversion().equals(HTTPVersion.HTTP0_9) || request.getHTTPversion().equals(HTTPVersion.HTTP1_0)) {
			Utility.clearInputStream(bi);
			response.status(405);
			return;
		}

		try {
			if(defaultRequestHandler(request, response) == DefaultRequestHandlerReturnCode.OK) {
				String contentLengthStr = request.getHeaderContent("Content-Length");
				if(contentLengthStr != null) {
					long contentLength = -1L;
					try {
						contentLength = Long.parseLong(contentLengthStr);
					} catch (NumberFormatException e) {}

					if(contentLength > 0) {
						String documentRoot = server.serverConfig.getDocumentRoot();
						if(documentRoot != null) {
							if(!documentRoot.endsWith(File.separator)) {
								documentRoot += File.separator;
							}

							String resource = request.getResource();
							resource = resource.substring(1); // rimuovo la prima /
							if(File.separator.equals("\\")) {
								// sostituisco il separator della resource con \ solo se il sistema operativo ha come file separator il carattere \
								resource = resource.replace("/", "\\");
							}
							
							File file = new File(documentRoot + resource);
							if(file.isDirectory()) {
								Utility.clearInputStream(bi);
								response.status(409);
								return;
							} else if(file.isFile()) {
								if(file.canWrite() && file.canRead()) {
									response.status(204);
								} else {
									Utility.clearInputStream(bi);
									response.status(403);
									return;
								}
							} else {
								response.status(201);
							}

							try {
								BufferedOutputStream  bos = new BufferedOutputStream(new FileOutputStream(file));
								byte[] buffer = new byte[4096];
								int bytesRead;
								long totalBytes = 0L;
								while(totalBytes < contentLength) {
									bytesRead = bi.read(buffer);
									if(bytesRead != -1) {
										bos.write(buffer, 0, bytesRead);
										totalBytes += bytesRead;
									} else {
										break;
									}
								}

								bos.flush();
								bos.close();
								response.addHeader(new Header("Content-Location", request.getResource()));
							} catch (IOException e1) {
								response.status(500);
							}
						} else {
							response.status(405);
						}
					} else {
						Utility.clearInputStream(bi);
						response.status(400);
					}
				} else {
					Utility.clearInputStream(bi);
					response.status(411);
				}
			}
		} catch (IOException e) {
			response.status(500);
		}
	}

	private void handleDELETE(Request request, Response response) {
		if(request.getHTTPversion().equals(HTTPVersion.HTTP0_9) || request.getHTTPversion().equals(HTTPVersion.HTTP1_0)) {
			Utility.clearInputStream(bi);
			response.status(405);
			return;
		}

		try{
			if(defaultRequestHandler(request, response) == DefaultRequestHandlerReturnCode.OK) {
				String documentRoot = server.serverConfig.getDocumentRoot();
				if(documentRoot != null) {
					if(!documentRoot.endsWith(File.separator)) {
						documentRoot += File.separator;
					}

					String resource = request.getResource();
					resource = resource.substring(1); // rimuovo la prima /
					if(File.separator.equals("\\")) {
						// sostituisco il separator della resource con \ solo se il sistema operativo ha come file separator il carattere \
						resource = resource.replace("/", "\\");
					}
					
					File file = new File(documentRoot + resource);
					if(file.isDirectory()) {
						response.status(409); // TODO
						return;
					} else if(file.isFile()) {
						server.deleteQueue.add(file.getAbsolutePath());
						response.status(202).send(
							"<html>\n" +
							"	<body>\n" +
							"		<h1>Eliminazione di \"" + resource.replace("\\", "/") + "\" accettata.</h1>\n" +
							"	</body>\n" +
							"</html>"
						);
					} else {
						response.status(404);
					}
				} else {
					response.status(405);
				}
			}
		} catch (IOException e) {
			response.status(500);
		}
	}

	public void setHeaderForHeadMethod(File file, Response response) throws IOException {
		response.addHeader(new Header("Content-Length", String.valueOf(file.length())));
		response.addHeader(new Header("Last-Modified", Utility.formatDateAsUTCDateString(new Date(file.lastModified()))));
		String mimeType = Utility.guessMimeTypeOfFile(file);
		response.addHeader(new Header("Content-Type", mimeType));
		response.status(200).send();
	}

	private Request initRequest(String method) {
		Request request;
		switch (method) {
			case "GET":
				request = new GetRequest();
				break;
			
			case "POST":
				request = new PostRequest();
				break;
			
			case "HEAD":
				request = new HeadRequest();
				break;
			
			case "OPTIONS":
				request = new OptionsRequest();
				break;
			
			case "PUT":
				request = new PutRequest();
				break;
			
			case "DELETE":
				request = new DeleteRequest();
				break;
			
			default:
				request = null;
				break;
		}

		return request;
	}

	@Override
	public void run() {
		Response response = null;
		Request request = null;
		try {
			bi = new BufferedInputStream(socket.getInputStream());
			if(!Utility.isRawHTTPSTraffic(bi)) {
				/*
				 * la prima riga nel pacchetto è sempre la richiesta effettuata.
				 * Nel protocollo HTTP/0.9 la richiesta è nel formato METODO RISORSA
				 * nelle versioni successive è METODO RISORSA VERSIONE_PROTOCOLLO
				 */
				String httpRequest = Utility.readLineFromBufferedInputStream(bi);
				// controllo se effettivamente è stata ricevuta una richiesta http valida in quanto il client potrebbe non aver mandato nulla
				if(httpRequest != null && !httpRequest.trim().isEmpty()) {
					String[] requestParam = httpRequest.split(" ");
					String method = requestParam[0];
					String resource = requestParam[1];
					request = initRequest(method);
					response = new Response(server, socket, request);
					
					if(request != null) {
						if(requestParam.length == 2) { // richiesta HTTP/0.9 senza campo version. HTTP/0.9 supporta solo il metodo GET e non supporta gli header
							request.setHTTPversion(HTTPVersion.HTTP0_9);
							response.setHTTPversion(HTTPVersion.HTTP0_9);
						} else if(requestParam.length == 3) { // versioni protocollo HTTP successive
							String httpVersion = requestParam[2];
							request.setHTTPversion(httpVersion);
							response.setHTTPversion(httpVersion);
						} else {
							response.status(500).send();
							return;
						}
					}

					BiConsumer<Request, Response> requestMethodHandler = methods.get(method);
					if(requestMethodHandler != null) {
						request.setResource(resource);
						requestMethodHandler.accept(request, response);
					} else {
						response.status(405).send();
					}
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			if(response != null) {	
				if(!handlingAnAPI) {
					response.close();
				}
			} else {
				try {
					socket.close();
				} catch (IOException e) { }
			}
		}
	}

}
