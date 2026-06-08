package org.ironsight.wpplugin.macromachine.REST;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CorsFilter implements ContainerResponseFilter
{

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {

        String origin = requestContext.getHeaderString("Origin");

        if (origin != null && isLocalOrigin(origin)) {
            responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", origin);
        }

        responseContext.getHeaders()
                .putSingle("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");

        responseContext.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");

        responseContext.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }

    private boolean isLocalOrigin(String origin) {

        return origin.startsWith("http://localhost:") || origin.startsWith("http://127.0.0.1:")
                || origin.startsWith("http://192.168.") || origin.startsWith("http://10.")
                || origin.startsWith("http://172.");
    }
}
