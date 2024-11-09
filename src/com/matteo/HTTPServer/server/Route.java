package com.matteo.HTTPServer.server;

public class Route {
	private String method;
	private String path;
	
	public Route(String method, String path) {
		this.method = method;
		this.path = path;
	}
	
	public String getMethod() {
		return this.method;
	}
	
	public String getPath() {
		return this.path;
	}
	
	@Override
	public String toString() {
		return "method: " + method + " path: " + path;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Route) {
			Route route = (Route)obj;
			return this.method.equals(route.getMethod()) && this.path.equals(route.getPath());
		} else {
			return false;
		}
		
	}
}
