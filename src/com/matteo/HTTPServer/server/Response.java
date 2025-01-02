package com.matteo.HTTPServer.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.matteo.HTTPServer.Adapters.LocalDateAdapter;
import com.matteo.HTTPServer.Adapters.LocalTimeAdapter;
import com.matteo.HTTPServer.utility.Utility;
import com.wuyufeng.open.client.FCGIClient;
import com.wuyufeng.open.response.FCGIResponse;

/**
 * Classe Response
 * 
 * @author Matteo Basso
 */
public final class Response {
	private Vector<Header> headers = new Vector<Header>(); // gli header della response
	private int statusCode = 200;
	private Socket socket;
	private Request request;
	private PrintWriter pw = null;
	private StringBuilder buffer = new StringBuilder();
	private String HTTPversion = HTTPVersion.HTTP1_1;
	private boolean sendingBoolean = false; // indica se il dato che sta per essere inviato è un booleano o no
	private DataOutputStream outputStreamForWriteMethod = null; //outputstream per il metodo write (deve tenere aperta la connessione)
	private boolean invokedWriteMethod = false; //indica se è stato invocato il metodo write
	private boolean writedResponseHeaderWithWriteMethod = false; // indice se sono stati giè scritti i response header con il metodo write
	private boolean invokedPipeMethod = false; // indica se è stato invocato il metodo pipe
	/**
	 * Costruttore della classe Response
	 * 
	 * @param socket il socket
	 * @param request la richiesta effettuata (mi serve per i file php)
	 */
	public Response(Socket socket, Request request) {
		this.socket = socket;
		this.request = request;
	}
	
	public void setHTTPversion(String HTTPversion) {
		this.HTTPversion = HTTPversion;
	}

	public String getHTTPversion() {
		return this.HTTPversion;
	}

	/**
	 * Controlla se è impostato o meno l'header Content-Type
	 * 
	 * @return <b>true</b> se è impostato, <b>false</b> altrimenti
	 */
	private boolean isSetContentType() {
		return getHeaderContent("Content-Type") != null;
	}
	
	/**
	 * Prepara l'head da aggiungere alla risposta
	 * @return la stringa
	 */
	private String stringifyfiResponseHeader() {
		String statusHeaderContent = getHeaderContent("Status");
		if(statusHeaderContent != null) {
			String[] splitted = statusHeaderContent.split(" ");
			if(splitted.length > 0) {
				try {
					statusCode = Integer.parseInt(splitted[0]);
				} catch (NumberFormatException e) {}
			}
		}
		StringBuilder toWrite =  new StringBuilder("HTTP/1.1 " + statusCode + "\r\n");
		
		for(Header header : headers) {
			toWrite.append(header.toString() + "\r\n");
		}
		toWrite.append("\r\n");
		
		return toWrite.toString();
	}
	/**
	 * Imposta gli header della risposta
	 * 
	 * @param pw lo stream (PrintWriter) su cui scrivere
	 * @throws IOException in caso di errori di I/O
	 */
	private void setResponseHeader(PrintWriter pw) throws IOException {
		String toWrite = stringifyfiResponseHeader();
		pw.write(toWrite);
	}
	
	/**
	 * Imposta gli header della risposta
	 * 
	 * @param out lo stream (DataOutputStream) su cui scrivere
	 * @throws IOException in caso di errori di I/O
	 */
	private void setResponseHeader(DataOutputStream out) throws IOException {
		String toWrite = stringifyfiResponseHeader();
		out.write(toWrite.getBytes());
	}
	
	/**
	 * Aggiunge un header alla risposta
	 * 
	 * @param header l'header da aggiungere
	 */
	public void addHeader(Header header) {
		headers.add(header);
	}
	
	public void overrideHeader(Header header) {
		synchronized(headers) {
			for(int i = 0; i < headers.size(); i++) {
				Header h = headers.get(i);
				if(h.getType().equalsIgnoreCase(header.getType())) {
					headers.set(i, header);
				}
			}
		}
	}
	
	public void removeHeader(String headerName) {
		for(int i = 0; i < headers.size(); i++) {
			if(headers.get(i).getType().equals(headerName)) {
				headers.remove(i);
				i--;
			}
		}
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
	 * Imposta lo stato della risposta
	 * 
	 * @param statusCode un intero contenente lo stato della risposta
	 * @return la Response
	 */
	public Response status(int statusCode) {
		this.statusCode = statusCode;
		if(HTTPversion.equals(HTTPVersion.HTTP0_9) && statusCode != 200) {
			send("<html>" + statusCode + "</html>\n");
		}
		return this;
	}
	
	/**
	 * Invia la risposta al client
	 * 
	 * @param string la stringa da inviare
	 */
	public Response send(String string) {
		if(!HTTPversion.equals(HTTPVersion.HTTP0_9) && !isSetContentType()) {
			addHeader(new Header("Content-Type", "text/plain; charset=utf-8"));
		}

		// converto i \n in \r\n come da protocollo
		StringBuilder correctString = new StringBuilder(string.length());
		char[] charArr = string.toCharArray();
		Character precChar = null;
		for(int i = 0; i < charArr.length; i++) {
			if(charArr[i] == '\n' && precChar != '\r')  {
				correctString.append("\r\n");
			} else {
				correctString.append(charArr[i]);
			}
			precChar = charArr[i];
		}
		buffer.append(correctString.toString());
		return this;
	}
	
	private Map<String, String> getPhpFastCgiVariables(File phpFile) {
		 Map<String, String> env = new HashMap<String, String>();
		 Vector<Cookie> cookies = request.getCookies();
		 StringBuilder stringCookies = new StringBuilder("");
		 for(Cookie cookie : cookies) {
			 stringCookies.append(cookie.getName() + "=" + cookie.getValue() + "; ");
		 }
		 env.put("HTTP_COOKIE", stringCookies.toString());
		 String queryString = request.getQueryStringForCGI();
		 if(request.getMethod().equals("POST")) {
			 env.put("CONTENT_TYPE", "application/x-www-form-urlencoded");
			 env.put("CONTENT_LENGTH", String.valueOf(queryString.length()));
		 }
		 
		 env.put("REQUEST_METHOD", request.getMethod());
		 env.put("QUERY_STRING", queryString);
		 env.put("REDIRECT_STATUS", "1");
		 env.put("SCRIPT_FILENAME", phpFile.getAbsolutePath());
		 return env;
	}
	
	private Response sendPHPfile(File phpFile) {
		System.out.println("SENDING PHP");
		String serverHeaderContent = getHeaderContent("Server");
		String dateHeaderContent = getHeaderContent("Date");
		headers.clear();
		addHeader(new Header("Date", dateHeaderContent));
		addHeader(new Header("Server", serverHeaderContent));
		FCGIClient client = new FCGIClient("127.0.0.1", Server.serverConfig.getPhpFastCGIserverPort(), false, 3000);
		FCGIResponse res = client.request(getPhpFastCgiVariables(phpFile), request.getQueryStringForCGI());
		BufferedInputStream bi = new BufferedInputStream(new ByteArrayInputStream(res.getResponseContent()));
		int c = -1;
		StringBuilder sb = new StringBuilder("");
		// leggo tutti gli header della risposta di php
		while(true) {
			String line = "";
			try {
				line = Utility.readLineFromBufferedInputStream(bi);
			} catch (IOException e) {
				e.printStackTrace();
			}
			// gli header sono separati dal body con una riga vuota
			if(line.equals("")) {
				break;
			} else {
				String headerType = line.substring(0, line.indexOf(':'));
				String content = line.substring(line.indexOf(':') + 1).trim();
				addHeader(new Header(headerType, content)); // salvo gli header
			}
		}
		
		try {
			while((c = bi.read()) != -1) {
				sb.append((char)c);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		send(sb.toString());
		close();
		return this;
	}
	
	public Response send(boolean value) {
		sendingBoolean = true;
		buffer.append(value + "");
		this.json();
		return this;
	}
	
	public Response send(Object object) {
		json(object);
		return this;
	}
	
	/**
	 * Invia la risposta al client (HTTP/0.9)
	 * 
	 * @param file il file da inviare
	 */
	private Response sendHTTP0_9(File file) {
		DataInputStream in = null;
		DataOutputStream out = null;
		byte[] buffer = new byte[4096];
		int count;
		try {
			in = new DataInputStream(new FileInputStream(file));
			out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			while((count = in.read(buffer)) > 0) {
				out.write(buffer, 0, count);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(in != null) {
					in.close();
				}
				
				if(out != null) {
					out.flush();
					out.close();
				}
				
				close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return this;
	}
	/**
	 * Invia la risposta al client
	 * 
	 * @param file il file da inviare
	 */
	public Response send(File file) {
		try {
			socket.setKeepAlive(true);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(file.exists()) {
			if(Server.serverConfig.isPHPEnabled() && file.getName().endsWith(".php")) {
				sendPHPfile(file);
			} else {
				String mimeType = "";
				try {
					mimeType = Utility.guessMimeTypeOfFile(file);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(HTTPversion.equals(HTTPVersion.HTTP0_9)) {
					if(mimeType.equals("text/html")) {
						sendHTTP0_9(file);
					} else {
						status(404).send().close();
					}
					
				} else {
					if(!isSetContentType()) {
						addHeader(new Header("Content-Type", mimeType));
					}
					addHeader(new Header("Content-Length", String.valueOf(file.length())));
					DataInputStream in = null;
					DataOutputStream out = null;
					byte[] buffer = new byte[4096];
					int count;
					try {
						in = new DataInputStream(new FileInputStream(file));
						out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
						setResponseHeader(out);
						while((count = in.read(buffer)) > 0) {
							out.write(buffer, 0, count);
						}
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							if(in != null) {
								in.close();
							}
							
							if(out != null) {
								out.flush();
								out.close();
							}
							
							close();
							
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		} else {
			this.status(404).send().close();
		}
		
		
		return this;
	}
	
	/**
	 * Invia la risposta al client (HTTP/0.9)
	 * 
	 * @param iS l'inputStream del file da inviare
	 */
	private Response sendHTTP0_9(InputStream iS) {
		DataInputStream in = null;
		DataOutputStream out = null;
		byte[] buffer = new byte[4096];
		int count;
		try {
			in = new DataInputStream(iS);
			out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			while((count = in.read(buffer)) > 0) {
				out.write(buffer, 0, count);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(in != null) {
					in.close();
				}
				
				if(out != null) {
					out.flush();
					out.close();
				}
				
				close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return this;
	}
	
	/**
	 * Invia la risposta al client
	 * 
	 * @param iSForDetectingMimeType l'inputStream per rilevare il mime type
	 * @param isForDetectingContentLength un secondo input stream che punta alla stessa risorsa ma è un InputStream diverso e serve per rilevare la content length
	 * @param isForSendingFile un terzo input stream che punta alla stessa risorsa ma è un InputStream diverso e serve per l'invio del file
	 */
	protected Response send(InputStream iSForDetectingMimeType, InputStream isForDetectingContentLength, InputStream isForSendingFile) {
		try {
			socket.setKeepAlive(true);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String mimeType = "text/plain";
		
		try {
			mimeType = Utility.guessMimeTypeOfFile(iSForDetectingMimeType);
			iSForDetectingMimeType.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(HTTPversion.equals(HTTPVersion.HTTP0_9)) {
			if(mimeType.equals("text/html")) {
				sendHTTP0_9(isForSendingFile);
			} else {
				status(404).send().close();
			}
			
			try {
				isForSendingFile.close();
				isForDetectingContentLength.close();
			} catch (IOException e) {}
			
		} else {
			if(!isSetContentType()) {
				addHeader(new Header("Content-Type", mimeType));
			}
			
			try {
				addHeader(new Header("Content-Length", String.valueOf(Utility.getInputStreamLength(isForDetectingContentLength))));
				isForDetectingContentLength.close();
			} catch (IOException e) {};
			
			DataInputStream in = null;
			DataOutputStream out = null;
			byte[] buffer = new byte[4096];
			int count;
			try {
				in = new DataInputStream(isForSendingFile);
				out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
				setResponseHeader(out);
				while((count = in.read(buffer)) > 0) {
					out.write(buffer, 0, count);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if(in != null) {
						in.close();
					}
					
					if(out != null) {
						out.flush();
						out.close();
					}
					
					close();
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return this;
	}
	
	/**
	 * Invia una risposta vuota al client
	 */
	public Response send() {
		return send("");
	}
	
	public Response json() {
		if(buffer.length() != 0) { // se il buffer non è vuoto
			if(isSetContentType())  {
				removeHeader("Content-Type");
			}
			addHeader(new Header("Content-Type", "application/json"));
			Gson gson = initGson();
			String s = buffer.toString();
			if(sendingBoolean) {
				if(s.equals("true")) {
					buffer = new StringBuilder(gson.toJson(true));
				} else if (s.equals("false")) {
					buffer = new StringBuilder(gson.toJson(false));
				} else {
					buffer = new StringBuilder(gson.toJson(s));
				}
			} else {
				buffer = new StringBuilder(gson.toJson(s));
			}
			
			
		}
		return this;
	}
	
	private Response json(Object object) {
		if(isSetContentType())  {
			removeHeader("Content-Type");
		}
		addHeader(new Header("Content-Type", "application/json"));
		Gson gson = initGson();
		buffer = new StringBuilder(gson.toJson(object));
		return this;
	}
	
	public Response write(byte[] buffer) {
		invokedWriteMethod = true;
		try {
			if(!socket.getKeepAlive()) {
				socket.setKeepAlive(true);
			}
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DataInputStream in = null;
		
		byte[] sendBuffer = new byte[4096];
		int count;
		try {
			in = new DataInputStream(new ByteArrayInputStream(buffer));
			if(outputStreamForWriteMethod == null) {
				outputStreamForWriteMethod = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
				if(!writedResponseHeaderWithWriteMethod) {
					setResponseHeader(outputStreamForWriteMethod);
					writedResponseHeaderWithWriteMethod = true;
				}
			}
			while((count = in.read(sendBuffer)) != -1) {
				outputStreamForWriteMethod.write(sendBuffer, 0, count);
				outputStreamForWriteMethod.flush();
			}
		} catch (IOException e) {
			
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return this;
	}
	
	// TODO nn funzionerè in HTTP/2
	public Response pipe(InputStream inputStream) throws IOException {
		invokedPipeMethod = true;
		DataInputStream dataInput = new DataInputStream(inputStream);
		DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());
		addHeader(new Header("Transfer-Encoding", "chunked"));
		setResponseHeader(dataOutput);
		byte[] buffer = new byte[4096];
		int readByte;
		while((readByte = dataInput.read(buffer)) != -1) {
			dataOutput.writeBytes(Integer.toHexString(readByte) + "\r\n"); // scrivo la dimensione del chunk in formato esadecimale
			dataOutput.write(buffer, 0, readByte); // scrivo il chunk
			dataOutput.writeBytes("\r\n"); // termina il chunk
			dataOutput.flush();
		}
		dataOutput.writeBytes("0\r\n\r\n"); // scrivo 0 per indicare che è finito lo stream
	    dataOutput.flush();
		dataInput.close();
		dataOutput.close();
		this.close();
		return this;
	}
	
	/**
	 * Chiude la connessione con il client
	 */
	public void close() {
		if(!socket.isClosed()) {
			if(!invokedWriteMethod && !invokedPipeMethod) {
				String bodyOfResponse = "";
				if(buffer.length() != 0) { // se ci sono caratteri
					bodyOfResponse = buffer.toString();
					int contentLength = bodyOfResponse.getBytes(StandardCharsets.UTF_8).length;
					addHeader(new Header("Content-Length", String.valueOf(contentLength)));
				}
				
				try {
					pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
					if(!HTTPversion.equals(HTTPVersion.HTTP0_9)) {
						setResponseHeader(pw);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if(pw != null) {
					if(bodyOfResponse.length() != 0) { // se ci sono caratteri
						pw.write(bodyOfResponse);
					}
					pw.flush();
					pw.close();
					pw = null;
					buffer = new StringBuilder();
				}
			} else if(outputStreamForWriteMethod != null) {
				try {
					outputStreamForWriteMethod.flush();
					outputStreamForWriteMethod.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Fa il redirect della richiesta alla risorsa specificata
	 * 
	 * @param newPath il percorso della risorsa
	 */
	public void redirect(String newPath) {
		headers.clear();
		if(!HTTPversion.equals(HTTPVersion.HTTP0_9)) {
			addHeader(new Header("Location", newPath));
		}
		status(301).send().close();
	}
	
	private Gson initGson() {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
		builder.registerTypeAdapter(LocalTime.class, new LocalTimeAdapter());
		return builder.create();
	}
}
