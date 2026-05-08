package org.ironsight.wpplugin.macromachine.REST;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.ironsight.wpplugin.macromachine.REST.Resources.ActionResource;
import org.ironsight.wpplugin.macromachine.REST.Resources.MacroResource;

@ApplicationPath("/api")
public class MacroApplication extends ResourceConfig
{
    public MacroApplication() {
        register(MacroResource.class);
        register(ActionResource.class);
        register(JacksonFeature.class);
        register(OpenApiResource.class);
    }
}
