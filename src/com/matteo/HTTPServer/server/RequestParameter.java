package com.matteo.HTTPServer.server;

public class RequestParameter {
	private String name;
	private String value;
	
	public RequestParameter(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getValue() {
		return this.value;
	}
	
	@Override
	public String toString() {
		return name + "=" + value;
	}
}
