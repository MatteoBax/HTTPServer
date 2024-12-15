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
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import com.matteo.HTTPServer.enums.Protocol;
import com.matteo.HTTPServer.utility.Queue;
import com.matteo.HTTPServer.utility.Utility;
import com.matteo.MavenUtility.loadResourceFromClassLoader.FileResourcesUtils;


/**
 * Thread che ha il compito di gestire il socket
 * 
 * @author Matteo Basso
 */
public class HandleSocketConnectionThread implements Runnable {
	private Server server; // l'oggetto server
	private Queue<DataFromSocketHandler> actionQueue; // coda delle elaborazioni delle richieste verso le route create dall'utente
	private Socket socket;
	private Protocol protocol;
	private ClassLoader classLoader; // il classLoader da dove caricare le risorse (se è null allora verranno caricate dal file system)
	private Vector<Route> registeredRoutes; // le route create dall'utente
	private Vector<Session> sessions; // le sessioni aperte lato server
	
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
		new Thread(this).start();
	}
	
	/**
	 * Questo metodo restituisce un'ArrayList contenente i parametri della richiesta data la linea della richiesta
	 * 
	 * @param requestLine la linea della richiesta (il body nel caso il metodo utillizzato sia POST, altrimenti se il metodo utilizzato è GET va passata solo la parte della query dell'url)
	 * @return un'ArrayList contenente i parametri della richiesta
	 */
	private ArrayList<RequestParameter> getRequestParameters(String requestLine) {
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
		final String sessionIDHeaderName = "sessionID";
		String setCookieHeaderContent = request.getHeaderContent("Cookie");
		if(setCookieHeaderContent == null) {
			final String id = generateUniqueSessionId();
			Session session = new Session(id);
			setCookieHeaderContent = sessionIDHeaderName + "=" + id;
			sessions.add(session);
			correctId = id;
		} else {
			String[] splittedHeader = setCookieHeaderContent.split(";");
			String sessionId = null;
			for (String attr : splittedHeader) {
				if(attr.contains(sessionIDHeaderName + "=")) {
					sessionId = attr.replaceFirst(sessionIDHeaderName + "=", "");
					break;
				}
			}
			Session foundSession = null;
			
			Session toCmp = new Session(sessionId);
			if(sessionId != null && sessions.contains(toCmp)) {
				foundSession = sessions.get(sessions.indexOf(toCmp));
			}
			
			if(foundSession == null || !foundSession.isStarted()) {
				if(foundSession != null) {
					// se la sessione è stata trovata ed è entrato in questo if vuol dire che la sessione non è stata avviata quindi la elimino
					foundSession.destroy();
				}
				// creo un cookie sessionID scaduto in modo tale da sovrascrivere quello lato client con quello nuovo
				String expiredCookie = sessionIDHeaderName + "=; Expires=" + Utility.formatDateAsUTCDateString(new Date(0L))+ "; Path=/";
		        response.addHeader(new Header("Set-Cookie", expiredCookie));
				final String id = generateUniqueSessionId();
				Session session = new Session(id);
				setCookieHeaderContent = sessionIDHeaderName + "=" + id;
				sessions.add(session);
				correctId = id;
			} else {
				setCookieHeaderContent = sessionIDHeaderName + "=" + sessionId;
				correctId = sessionId;
			}
			
		}
		request.setSession(sessions.elementAt(sessions.indexOf(new Session(correctId))));
		
		response.addHeader(new Header("Set-Cookie", setCookieHeaderContent + ";Path=/"));
	}
	
	private void sendFile(File f, Request request, Response response) {
		response.addHeader(new Header("Last-Modified", Utility.formatDateAsUTCDateString(new Date(f.lastModified()))));
		String path = f.getAbsolutePath();
		if(path.endsWith(".html")) {
			//initSession(request, response);
		}
		response.send(new File(path));
		response.close();
	}
	
	private void sendFile(InputStream iSForDetectingMimeType, InputStream isForDetectingContentLength, InputStream isForSendingFile, Request request, Response response, boolean needAsession) {
		if(needAsession) {
			//initSession(request, response);
		}
		// TODO implement PHP
		response.send(iSForDetectingMimeType, isForDetectingContentLength, isForSendingFile);
		response.close();
	}
	
	private boolean handleFileSend(String resource, Request request, Response response) {
		final String reqResource = resource;
		// cerco se il file esiste e lo invio
		resource = resource.substring(1); // rimuovo la prima /
		String documentRoot = Server.serverConfig.getDocumentRoot();
		boolean found = false;
		if(classLoader == null) {
			if(documentRoot != null) {
				if(File.separator.equals("\\")) {
					// sostituisco il separator della resource con \ solo se il sistema operativo ha come file separator il carattere \
					resource = resource.replace("/", "\\");
				}
				File f = new File(documentRoot + resource);
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
		
		
		return found;
	}
	
	public void setHeaderForHeadMethod(File file, Response response) throws IOException {
		response.addHeader(new Header("Content-Length", String.valueOf(file.length())));
		response.addHeader(new Header("Last-Modified", Utility.formatDateAsUTCDateString(new Date(file.lastModified()))));
		String mimeType = Utility.guessMimeTypeOfFile(file);
		response.addHeader(new Header("Content-Type", mimeType));
		response.status(200).send().close();
	}
	@Override
	public void run() {
		BufferedInputStream bi = null; // legge i dati dal socket
		int responseStatus = -1;
		Response response = null;
		Request request = null;
		try {
			bi = new BufferedInputStream(socket.getInputStream());
			if(Utility.isRawHTTPSTraffic(bi)) {
				responseStatus = 400;
			} else {
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
					String originalResource = resource;
					String httpVersion;
					long contentLength = -1L;
					
					if(method.equals("GET")) {
						request = new GetRequest();
					} else if (method.equals("POST")) {
						request = new PostRequest();
					}
					
					response = new Response(socket, request);
					
					if(requestParam.length == 2) { // richiesta HTTP/0.9 senza campo version
						response.setHTTPversion(HTTPVersion.HTTP0_9);
						// HTTP/0.9 supporta solo il metodo GET e non supporta gli header
						if(method.equals("GET")) {
							boolean found = handleFileSend(resource, request, response);
							if(!found) {
								response.status(404).send();
							}
						} else {
							responseStatus = 400;
						}
						
						response.close();
					} else if(requestParam.length == 3) { // versioni protocollo HTTP successive
						httpVersion = requestParam[2];
						response.setHTTPversion(httpVersion);
						
						response.addHeader(new Header("Date", Utility.formatDateAsUTCDateString(new Date())));
						
						// Aggiungo gli Header di default
						Vector<Header> defaultHeaders = server.getDefaultHeaders();
						synchronized(defaultHeaders) {
							for(Header defaultHeader : defaultHeaders) {
								response.addHeader(defaultHeader);
							}
						}
						
						// le uniche versioni di HTTP supportate da questo server sono la 1.0 e la 1.1, le altre le considero invalide
						if(!httpVersion.equals(HTTPVersion.HTTP1_0) && !httpVersion.equals(HTTPVersion.HTTP1_1)) {
							responseStatus = 505;
						} else {
							// gli unici metodi attualmente supportati dal server sono GET e POST, gli altri li considero invalidi
							if(method.equals("GET") || method.equals("POST")) {
								
								URL url = null;
								try {
									url = new URI("http://localhost" + resource).toURL();
								} catch (URISyntaxException e) {
									e.printStackTrace();
								}
								
								if(url != null) {
									resource = url.getPath(); // toglo i parametri dalla resource in modo tale da poter gestire le route
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
								
								if(updateInsecureRequestHeader != null && updateInsecureRequestHeader.equals("1") && hostHeader != null && !hostHeader.trim().isEmpty() && protocol == Protocol.HTTP && server.usesHTTPS()) {
									String newPath = "https://" + hostHeader.trim().substring(0, hostHeader.indexOf(":"));
									newPath += ":" + Server.serverConfig.getHTTPS_Port() + originalResource;
									response.redirect(newPath);
								} else {
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
									        int c;
									        while (byteArrayOutputStream.size() < contentLength && (c = bi.read()) != -1) {
									        	byteArrayOutputStream.write(c);
									        }
									        String body = byteArrayOutputStream.toString();
											request.addRequestParams(getRequestParameters(body));
										} else if(contentTypeHeader.contains("multipart/form-data")) {
											responseStatus = 415;
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
									
									/*
									 * Se la risorsa richiesta è una route creata dall'utente, l'aggiungo nella coda delle elaborazioni
									 * delle richieste della route
									 */
									if(registeredRoutes.contains(new Route(method, resource))) {
										initSession(request, response);
										DataFromSocketHandler dt = new DataFromSocketHandler(resource, request, response) ;
										actionQueue.add(dt);
									} else {
										boolean found = handleFileSend(resource, request, response);
										if(!found) {
											responseStatus = 404;
										}
										
									}
								}
							} else if(method.equals("HEAD")){
								URL url = null;
								try {
									url = new URI("http://localhost" + resource).toURL();
								} catch (URISyntaxException e) {
									e.printStackTrace();
								}
								
								if(url != null) {
									resource = url.getPath(); // tolgo i parametri dalla resource
								}
								
								// cerco se il file esiste e lo invio
								resource = resource.substring(1); // rimuovo la prima /
								if(File.separator.equals("\\")) {
									// sostituisco il separator della resource con \ solo se il sistema operativo ha come file separator il carattere \
									resource = resource.replace("/", "\\");
								}
								String documentRoot = Server.serverConfig.getDocumentRoot();
								if(documentRoot != null) {
									File f = new File(documentRoot + resource);
									File f1 = new File(documentRoot + resource + "index.html");
									if(f.isFile()) {
										setHeaderForHeadMethod(f, response);
									} else if (f1.isFile()) {
										setHeaderForHeadMethod(f1, response);
									} else {
										response.status(404).send().close();
									}
								} else {
									response.status(404).send().close();
								}
							} else {
								responseStatus = 400;
							}
						}
					} else {
						System.err.println("Bad request");
						responseStatus = 500;
					}
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			if(response != null) {
				if(responseStatus != -1) {
					response.status(responseStatus).send().close();
				}
			} else {
				try {
					socket.close();
				} catch (IOException e) { }
			}
		}
	}

}
