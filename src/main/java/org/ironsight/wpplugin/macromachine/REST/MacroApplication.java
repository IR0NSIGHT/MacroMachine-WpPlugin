package org.ironsight.wpplugin.macromachine.REST;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.ironsight.wpplugin.macromachine.REST.Resources.ActionResource;
import org.ironsight.wpplugin.macromachine.REST.Resources.ExecutionResource;
import org.ironsight.wpplugin.macromachine.REST.Resources.MacroResource;
import org.ironsight.wpplugin.macromachine.operations.MacroApplicator;
import org.ironsight.wpplugin.macromachine.operations.MacroContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;

@ApplicationPath("/api")
public class MacroApplication extends ResourceConfig {
    public MacroApplication(MacroApplicator applicator, MappingActionContainer actions, MacroContainer macros) {
        register(PreflightRequestFilter.class);
        register(CorsFilter.class);

        register(MacroResource.class);
        register(ActionResource.class);
        register(new ExecutionResource(applicator, actions, macros));

        register(JacksonFeature.class);
        register(OpenApiResource.class);
    }
}
