package org.ironsight.wpplugin.macromachine.REST.Resources;

import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.ironsight.wpplugin.macromachine.REST.DTOs.ExecutionQueueDTO;
import org.ironsight.wpplugin.macromachine.REST.DTOs.ExecutionStateDTO;
import org.ironsight.wpplugin.macromachine.operations.MacroApplicator;
import org.ironsight.wpplugin.macromachine.operations.MacroContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;

@Path("/execution")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExecutionResource {
  private final MacroApplicator applicator;
  private final MacroContainer macros;
  private final MappingActionContainer actions;

  public ExecutionResource(
      MacroApplicator applicator, MappingActionContainer actions, MacroContainer macros) {
    this.applicator = applicator;
    this.actions = actions;
    this.macros = macros;
  }

  @GET
  @Path("/state")
  public ExecutionStateDTO getCurrentState() {
    return applicator.getCurrentState();
  }

  // DELETE and POST state left out on purpose

  @GET
  @Path("/queue")
  public ExecutionQueueDTO getQueue() {
    return new ExecutionQueueDTO(applicator.getQueue());
  }

  @POST
  @Path("/queue")
  public ExecutionQueueDTO updateQueue(@Valid ExecutionQueueDTO request) {
    var existingMacros = request.queuedMacroIds().stream().filter(macros::queryContains).toList();
    for (var uid : existingMacros) applicator.queueMacro(uid);
    return new ExecutionQueueDTO(applicator.getQueue());
  }
}
