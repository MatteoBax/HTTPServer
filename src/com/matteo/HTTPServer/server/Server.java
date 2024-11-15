package com.matteo.HTTPServer.server;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.Vector;
import java.util.function.BiConsumer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import com.matteo.HTTPServer.enums.Protocol;
import com.matteo.HTTPServer.utility.Queue;
import com.matteo.HTTPServer.utility.Utility;

/**
 * Classe Server
 * 
 * @author Matteo Basso
 */
public class Server {
	public static final String SERVER_VERSION = Utility.getServerVersion();
	public static final String OS_NAME = System.getProperty("os.name");
	public static final String OS_ARCH = System.getProperty("os.arch");
	
	private ServerSocket HTTPserverSocket;
	private SSLServerSocket HTTPSserverSocket;
	public static final ServerConfig serverConfig = new ServerConfig();
	private ClassLoader classLoader = null;
	private Queue<DataFromSocketHandler> queue = new Queue<DataFromSocketHandler>(500);
	private Vector<Route> registeredRoutes = new Vector<Route>();
	private Vector<Session> sessions = new Vector<Session>();
	private Thread HTTPserverThread = null;
	private Thread HTTPSserverThread = null;
	private SessionCleanerThread sessionCleanerThread = null;
	private Process phpFastCGIserverProcess = null;
	private Server server = this;
	
	/**
	 * Costruttore del server
	 * 
	 * Carica la configurazione dal file config.properties
	 */
	public Server() {
		FileReader reader = null;
		try {
			reader = new FileReader("config.properties");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Properties prop = new Properties();
		try {
			prop.load(reader);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		String documentRoot = prop.getProperty("DocumentRoot").trim();
		if(!documentRoot.isEmpty()) {
			if(!documentRoot.endsWith(File.separator)) {
				documentRoot += File.separator;
			}
			serverConfig.setDocumentRoot(documentRoot);
		}
		
		String httpPortStr = prop.getProperty("HTTP_Port").trim();
		if(!httpPortStr.isEmpty()) {
			try {
				serverConfig.setHTTP_Port(Integer.parseInt(httpPortStr));
			} catch (NumberFormatException e) {
				System.err.println("Nel file di configurazione il numero di porta http � invalido!");
			}
		} else {
			System.err.println("Nel file di configurazione il numero di porta http � invalido!");
		}
		
		// HTTPS
		String httpsPortStr = prop.getProperty("HTTPS_Port").trim();
		if(!httpsPortStr.isEmpty()) {
			try {
				serverConfig.setHTTPS_Port(Integer.parseInt(httpsPortStr));
			} catch (NumberFormatException e) {
				System.err.println("Nel file di configurazione il numero di porta https � invalido!");
			}
			
			String SSLkeyStorePassword = prop.getProperty("SSLkeyStorePassword");
			if(!SSLkeyStorePassword.isEmpty()) {
				serverConfig.setSSLkeyStorePassword(SSLkeyStorePassword);
			}
			
			String SSLkeyStorePath = prop.getProperty("SSLkeyStorePath");
			if(!SSLkeyStorePath.isEmpty()) {
				serverConfig.setSSLkeyStorePath(SSLkeyStorePath);
			}
		}
		String phpCgiExecutablePath = prop.getProperty("PHP-CGIexecutablePath").trim();
		if(!phpCgiExecutablePath.isEmpty()) {
			serverConfig.setPhpCgiExecutablePath(phpCgiExecutablePath);
		}
		
		String loadResourcesFromClassLoaderStr = prop.getProperty("LoadResourcesFromClassLoader").trim();
		if(!loadResourcesFromClassLoaderStr.isEmpty()) {
			serverConfig.setLoadResourcesFromClassLoader(Boolean.parseBoolean(loadResourcesFromClassLoaderStr));
		}
	}
	
	public Server(int HTTP_Port, String documentRoot) {
		serverConfig.setHTTP_Port(HTTP_Port);
		if(documentRoot != null && !documentRoot.endsWith(File.separator)) {
			documentRoot += File.separator;
		}
		serverConfig.setDocumentRoot(documentRoot);
	}
	
	public Server(int HTTP_Port, int HTTPS_Port, String SSLkeyStorePassword, String SSLkeyStorePath, String documentRoot) {
		this(HTTP_Port, documentRoot);
		serverConfig.setHTTPS_Port(HTTPS_Port);
		serverConfig.setSSLkeyStorePassword(SSLkeyStorePassword);
		serverConfig.setSSLkeyStorePath(SSLkeyStorePath);
	}
	
	public Server(int HTTP_Port, String documentRoot, String phpCgiBinaryPath) {
		this(HTTP_Port, documentRoot);
		serverConfig.setPhpCgiExecutablePath(phpCgiBinaryPath);
	}
	
	public Server(int HTTP_Port, int HTTPS_Port, String SSLkeyStorePassword, String SSLkeyStorePath, String documentRoot, String phpCgiBinaryPath) {
		this(HTTP_Port, HTTPS_Port, SSLkeyStorePassword, SSLkeyStorePath, documentRoot);
		serverConfig.setPhpCgiExecutablePath(phpCgiBinaryPath);
	}
	
	public Server(int HTTP_Port, String documentRoot, boolean loadResourcesFromClassLoader) {
		this(HTTP_Port, documentRoot);
		if(loadResourcesFromClassLoader) {
			classLoader = getClass().getClassLoader();
			if(documentRoot != null) {
				documentRoot.replace("\\", "/");
				if(!documentRoot.endsWith("/")) {
					documentRoot += "/";
				}
				
				if(documentRoot.startsWith("/") && documentRoot.length() > 1) {
					documentRoot.substring(1);
				}
			}
			
			serverConfig.setDocumentRoot(documentRoot);
		}
		
	}
	
	public Server(int HTTP_Port, int HTTPS_Port, String SSLkeyStorePassword, String SSLkeyStorePath, String documentRoot, boolean loadResourcesFromClassLoader) {
		this(HTTP_Port, documentRoot, loadResourcesFromClassLoader);
		serverConfig.setHTTPS_Port(HTTPS_Port);
		serverConfig.setSSLkeyStorePassword(SSLkeyStorePassword);
		serverConfig.setSSLkeyStorePath(SSLkeyStorePath);
	}
	
	public Server(int HTTP_Port, String documentRoot, String phpCgiBinaryPath, boolean loadResourcesFromClassLoader) {
		this(HTTP_Port, documentRoot, loadResourcesFromClassLoader);
		serverConfig.setPhpCgiExecutablePath(phpCgiBinaryPath);
	}
	
	public Server(int HTTP_Port, int HTTPS_Port, String SSLkeyStorePassword, String SSLkeyStorePath, String documentRoot, String phpCgiBinaryPath, boolean loadResourcesFromClassLoader) {
		this(HTTP_Port, documentRoot, phpCgiBinaryPath, loadResourcesFromClassLoader);
		serverConfig.setHTTPS_Port(HTTPS_Port);
		serverConfig.setSSLkeyStorePassword(SSLkeyStorePassword);
		serverConfig.setSSLkeyStorePath(SSLkeyStorePath);
	}
	
	public Server(int HTTP_Port, boolean loadResourcesFromClassLoader) {
		this(HTTP_Port, null, loadResourcesFromClassLoader);
	}
	
	public Server(int HTTP_Port, int HTTPS_Port, String SSLkeyStorePassword, String SSLkeyStorePath, boolean loadResourcesFromClassLoader) {
		this(HTTP_Port, HTTPS_Port, SSLkeyStorePassword, SSLkeyStorePath, null, loadResourcesFromClassLoader);
	}
	
	public Server(int HTTP_Port) {
		serverConfig.setHTTP_Port(HTTP_Port);
	}
	
	public Server(int HTTP_Port, int HTTPS_Port, String SSLkeyStorePassword, String SSLkeyStorePath) {
		this(HTTP_Port);
		serverConfig.setHTTPS_Port(HTTPS_Port);
		serverConfig.setSSLkeyStorePassword(SSLkeyStorePassword);
		serverConfig.setSSLkeyStorePath(SSLkeyStorePath);
	}
	
	private SSLContext initSSLContext() {
		KeyStore keyStore = null;
		try {
			keyStore = KeyStore.getInstance("PKCS12");
		} catch (KeyStoreException e) {
			System.err.println("KeyStore PKCS12 non supportato");
		}
		
		boolean keyStoreOk = false;
		if(keyStore != null) {
			try {
				keyStore.load(new FileInputStream(serverConfig.getSSLkeyStorePath()), serverConfig.getSSLkeyStorePassword().toCharArray());
				keyStoreOk = true;
			} catch (IOException e) {
				System.err.println("Impossibile caricare il Keystore specificato!");
				System.err.println("Verificare che la password sia corretta e che il Keystore non sia corrotto o inesistente");
			} catch (NoSuchAlgorithmException e) {
				System.err.println("L'algoritmo per verificare l'integrit� del Keystore non � stato trovato!");
			} catch (CertificateException e) {
				System.err.println("I certificati del Keystore non possono essere caricati!");
				System.err.println("Analizzare lo stacktrace");
				e.printStackTrace();
			}
		}
		
		boolean keyManagerFactoryOk = false;
		KeyManagerFactory keyManagerFactory = null;
		
		if(keyStoreOk) {
			try {
				keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			} catch (NoSuchAlgorithmException e) {
				System.err.println("L'algoritmo per KeyManagerFactory non � supportato!");
			}
			if(keyManagerFactory != null) {
				try {
					keyManagerFactory.init(keyStore, serverConfig.getSSLkeyStorePassword().toCharArray());
					keyManagerFactoryOk = true;
				} catch (UnrecoverableKeyException e) {
					System.err.println("Le chiavi non possono essere ripristinate dal Keystore. La password potrebbe essere errata");
				} catch (KeyStoreException e) {
					System.err.println("Errore di inizializzazione di KeyManagerFactory\nControllare lo stacktrace");
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					System.err.println("L'algoritmo del Keystore non � disponibile!");
				}
			}
		}
		
		if(keyManagerFactoryOk) {
			SSLContext sslContext = null;
			try {
				sslContext = SSLContext.getInstance("TLS");
			} catch (NoSuchAlgorithmException e) {
				System.err.println("TLS non � supportato!");
			}
			
			if(sslContext != null) {
				try {
					sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
					return sslContext;
				} catch (KeyManagementException e) {
					System.err.println("Errore di inizializzazione di SSLContext");
					return null;
				}
			}
		}
		
		return null;
	}
	
	public void startServer() throws IOException{
		if(serverConfig.getPhpCgiExecutablePath() != null) {
			File sessionSaveDir = new File("sessions");
			if(!sessionSaveDir.isDirectory()) {
				sessionSaveDir.mkdir();
			}
			ProcessBuilder pb = new ProcessBuilder(serverConfig.getPhpCgiExecutablePath(), "-b", "127.0.0.1:" + serverConfig.getPhpFastCGIserverPort(), "-c", serverConfig.getPhpCgiExecutablePath().replace("php-cgi.exe", "").replace("php-cgi", "") + "php.ini-development", "-d", "doc_root=\"" + serverConfig.getDocumentRoot() + "\"");
			
			File currentClassFile = null;
			try {
				currentClassFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			
			if(currentClassFile != null) {
				/*
				do {
					phpSessionSavePath = new File(currentClassFile.getAbsolutePath() + File.separator + "sessions" + Utility.generateRandomString(10, new char[]{'\\', '/', ':', '*', '?', '"', '<', '>', '|', ',', '{', '}'}));
				} while (!phpSessionSavePath.mkdir());
				phpSessionSavePath.setWritable(true, false);
			    phpSessionSavePath.setReadable(true, false);
			    phpSessionSavePath.setExecutable(true, false);
				pb.command().add("-d");
				pb.command().add("session.save_path=\"" + phpSessionSavePath.getAbsolutePath().replace("\\", "\\\\") + "\"");
				*/
			}
			// nn worka, devo fare un installer
			pb.command().add("-d");
			pb.command().add("session.save_path=" + System.getProperty("user.dir") + File.separator + "sessions");
			pb.environment().put("DOCUMENT_ROOT", serverConfig.getDocumentRoot());
			System.out.println(String.join(" ", pb.command()));
			phpFastCGIserverProcess = pb.start();
		}
		sessionCleanerThread = new SessionCleanerThread(sessions);
		HTTPserverSocket = new ServerSocket(serverConfig.getHTTP_Port());
		
		// gestione Server HTTPS
		if(usesHTTPS()) {
			SSLContext sslContext = initSSLContext();
			if(sslContext != null) {
				SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
				try {
					HTTPSserverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(serverConfig.getHTTPS_Port());
				} catch (IOException e) {
					System.err.println("Verificare che la porta HTTPS non sia gi� utilizzata. Se il keystore specificato non esiste, va creato con il seguente comando:");
					System.err.println("keytool -genkeypair -v -keystore nomeKeyStore.p12 -storetype PKCS12 -keyalg RSA -keysize 2048 -storepass laPassword -validity validit�InGG -alias JavaHTTPserver");
				}
				
				if(HTTPSserverSocket != null) {
					HTTPSserverThread = new Thread(new Runnable() {
						@Override
						public void run() {
							while(!HTTPSserverSocket.isClosed()) {
								Socket socket = null;;
								try {
									socket = HTTPSserverSocket.accept();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								if(socket != null) {
									new HandleSocketConnectionThread(server, Protocol.HTTPS, registeredRoutes, queue, sessions, socket, classLoader);
								}
								
							}
							
							if(!HTTPSserverSocket.isClosed()) {
								try {
									HTTPSserverSocket.close();
								} catch (IOException e) {
									System.err.println("Impossibile chiudere il socket del server HTTPS");
								}
							} else {
								System.out.println("Il server HTTPS � stato terminato!");
							}
						}
					});
					
					HTTPSserverThread.start();
				}
			}
		}
		
		// gestione Server HTTP
		HTTPserverThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(!HTTPserverSocket.isClosed()) {
					Socket socket = null;;
					try {
						socket = HTTPserverSocket.accept();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
					
					if(socket != null) {
						new HandleSocketConnectionThread(server, Protocol.HTTP, registeredRoutes, queue, sessions, socket, classLoader);
					}
					
				}
				
				if(!HTTPserverSocket.isClosed()) {
					try {
						HTTPserverSocket.close();
					} catch (IOException e) {
						System.err.println("Impossibile chiudere il socket del server HTTP");
					}
				} else {
					System.out.println("Il server HTTP � stato terminato!");
				}
			}
		});
		
		HTTPserverThread.start();
		
		System.out.println("HostName server: " + InetAddress.getLocalHost().getHostName());
		System.out.println("IP server: " + InetAddress.getLocalHost().getHostAddress());
		System.out.println("Porta in cui � in ascolto il server HTTP: " + serverConfig.getHTTP_Port());
		if(usesHTTPS()) {
			System.out.println("Porta in cui � in ascolto il server HTTPS: " + serverConfig.getHTTPS_Port());
		}
	}
	
	public void stopServer() {
		if(sessionCleanerThread != null) {
			sessionCleanerThread.interrupt();
		}
		
		if(phpFastCGIserverProcess != null) {
			phpFastCGIserverProcess.destroy();
			//Utility.deleteDirectory(phpSessionSavePath);
		}
		
		if(HTTPserverThread != null) {
			try {
				HTTPserverSocket.close();
			} catch (IOException e) {
				System.err.println("Impossibile chiudere il socket del server HTTP");
			}
		}
		
		if(HTTPSserverThread != null) {
			try {
				HTTPSserverSocket.close();
			} catch (IOException e) {
				System.err.println("Impossibile chiudere il socket del server HTTPS");
			}
		}
		
	}
	
	public boolean usesHTTPS() {
		return (serverConfig.getHTTPS_Port() != -1 && serverConfig.getSSLkeyStorePassword() != null && serverConfig.getSSLkeyStorePath() != null);
	}
	public void get(String path, BiConsumer<Request, Response> handler) {
		registeredRoutes.add(new Route("GET", path));
		new RequestHandlerThread(queue, path, "GET", handler);
	}
	
	public void post(String path, BiConsumer<Request, Response> handler) {
		registeredRoutes.add(new Route("POST", path));
		new RequestHandlerThread(queue, path, "POST", handler);
	}
}
