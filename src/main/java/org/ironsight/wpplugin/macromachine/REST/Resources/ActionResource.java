package org.ironsight.wpplugin.macromachine.REST.Resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.ironsight.wpplugin.macromachine.REST.DTOs.ActionDTO;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/actions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ActionResource
{
    private MappingActionContainer actionContainer = MappingActionContainer.getInstance();

    @GET
    public List<ActionDTO> getAll() {
        return actionContainer.queryAll().stream().map(ActionDTO::new).collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public ActionDTO get(@PathParam("id") UUID id) {
        var macro = actionContainer.queryById(id);
        if (macro == null) {
            throw new NotFoundException("Action not found for uuid=: " + id);
        }
        return new ActionDTO(macro);
    }

    @POST
    public ActionDTO create(ActionDTO dto) {
        MappingAction macro = dto.toAction();
        StringBuilder err = new StringBuilder();
        actionContainer.updateMapping(macro, err::append);
        if (!err.isEmpty()) {
            throw new InternalServerErrorException(err.toString());
        }

        return dto;
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") UUID id) {
        if (!actionContainer.queryContains(id))
            throw new NotFoundException("Action not found for uuid=: " + id);
        actionContainer.deleteMapping(id);
    }
}
