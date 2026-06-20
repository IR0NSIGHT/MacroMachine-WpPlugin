package org.ironsight.wpplugin.macromachine.REST.Resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;

@Path("/docs")
public class DocsResource
{

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getDocs() {
        InputStream html = getClass().getResourceAsStream("/docs/index.html");

        if (html == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Swagger UI not found").build();
        }

        return Response.ok(html).build();
    }
}
