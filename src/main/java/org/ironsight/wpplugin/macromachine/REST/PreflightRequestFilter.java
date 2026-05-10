package org.ironsight.wpplugin.macromachine.REST;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ContainerRequest;

import java.io.IOException;

@Provider
public class PreflightRequestFilter
        implements jakarta.ws.rs.container.ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {

        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {

            requestContext.abortWith(
                    Response.ok()
                            .header("Access-Control-Allow-Origin", "http://localhost:5173")
                            .header("Access-Control-Allow-Headers",
                                    "origin, content-type, accept, authorization")
                            .header("Access-Control-Allow-Credentials", "true")
                            .header("Access-Control-Allow-Methods",
                                    "GET, POST, PUT, DELETE, OPTIONS, HEAD")
                            .build()
            );
        }
    }
}