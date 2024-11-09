package com.matteo.HTTPServer.server;

public class ServerConfig {
	
	private String documentRoot = null;
	private int HTTP_Port = -1;
	private int HTTPS_Port = -1;
	private String SSLkeyStorePath = null;
	private String SSLkeyStorePassword = null;
	private String phpCgiExecutablePath = null;
	private final short phpFastCGIserverPort = 9123;
	private boolean loadResourcesFromClassLoader = false;
	
	public String getDocumentRoot() {
		return documentRoot;
	}
	
	protected void setDocumentRoot(String documentRoot) {
		this.documentRoot = documentRoot;
	}
	
	public int getHTTP_Port() {
		return HTTP_Port;
	}
	
	protected void setHTTP_Port(int HTTP_Port) {
		this.HTTP_Port = HTTP_Port;
	}
	
	public int getHTTPS_Port() {
		return HTTPS_Port;
	}
	
	protected void setHTTPS_Port(int HTTPS_Port) {
		this.HTTPS_Port = HTTPS_Port;
	}
	
	protected void setSSLkeyStorePath(String SSLkeyStorePath) {
		this.SSLkeyStorePath = SSLkeyStorePath;
	}
	
	public String getSSLkeyStorePath() {
		return SSLkeyStorePath;
	}
	
	protected void setSSLkeyStorePassword(String SSLkeyStorePassword) {
		this.SSLkeyStorePassword = SSLkeyStorePassword;
	}
	
	public String getSSLkeyStorePassword() {
		return this.SSLkeyStorePassword;
	}
	
	public String getPhpCgiExecutablePath() {
		return phpCgiExecutablePath;
	}
	
	protected void setPhpCgiExecutablePath(String phpCgiExecutablePath) {
		this.phpCgiExecutablePath = phpCgiExecutablePath;
	}
	
	public short getPhpFastCGIserverPort() {
		return this.phpFastCGIserverPort;
	}
	
	public boolean isLoadResourcesFromClassLoader() {
		return loadResourcesFromClassLoader;
	}
	
	protected void setLoadResourcesFromClassLoader(boolean loadResourcesFromClassLoader) {
		this.loadResourcesFromClassLoader = loadResourcesFromClassLoader;
	}
	
	
}
