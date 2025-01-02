package com.matteo.HTTPServer.server;

public class HTTPResponseStatusCode {
    private int code;
    private String description;

    public HTTPResponseStatusCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }
}
