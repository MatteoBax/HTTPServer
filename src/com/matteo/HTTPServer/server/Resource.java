package com.matteo.HTTPServer.server;

import com.matteo.HTTPServer.enums.ResourceExistStatus;

public class Resource {
    private ResourceExistStatus existStatus;
    private String resourcePath;
    private boolean isAPI;

    public Resource(ResourceExistStatus existStatus, String resourcePath, boolean isAPI) {
        this.existStatus = existStatus;
        this.resourcePath = resourcePath;
        this.isAPI = isAPI;
    }

    public Resource(ResourceExistStatus existStatus, String resourcePath) {
        this(existStatus, resourcePath, false);
    }

    public ResourceExistStatus getExistStatus() {
        return this.existStatus;
    }

    public String getResourcePath() {
        return this.resourcePath;
    }

    public boolean isAPI() {
        return this.isAPI;
    }
}
