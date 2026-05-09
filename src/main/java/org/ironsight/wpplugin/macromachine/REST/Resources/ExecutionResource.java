package org.ironsight.wpplugin.macromachine.REST.Resources;

import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.ironsight.wpplugin.macromachine.REST.DTOs.ExecutionQueueDTO;
import org.ironsight.wpplugin.macromachine.REST.DTOs.ExecutionStateDTO;
import org.ironsight.wpplugin.macromachine.REST.DTOs.ExecutionStatus;

import java.util.List;

@Path("/execution")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExecutionResource
{
    @GET
    @Path("/state")
    public ExecutionStateDTO getCurrentState() {
        return new ExecutionStateDTO(null, List.of(), 0, ExecutionStatus.IDLE);
    }

    @POST
    @Path("/state")
    public ExecutionStateDTO updateQueue(@Valid ExecutionStateDTO request) {
        System.out.println("client posted execution-state request");
        return request;
    }

    // DELETE state left out on purpose

    @GET
    @Path("/queue")
    public ExecutionQueueDTO getQueue() {
        return new ExecutionQueueDTO(List.of());
    }

    @POST
    @Path("/queue")
    public ExecutionQueueDTO updateQueue(@Valid ExecutionQueueDTO request) {
        return request;
    }

    @DELETE
    @Path("/queue")
    public void clearQueue() {

    }
}
