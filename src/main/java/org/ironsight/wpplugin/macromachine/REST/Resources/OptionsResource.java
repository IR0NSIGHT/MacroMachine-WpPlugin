package org.ironsight.wpplugin.macromachine.REST.Resources;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/")
public class OptionsResource
{

    @OPTIONS
    public Response options() {
        return Response.ok().build();
    }
}
