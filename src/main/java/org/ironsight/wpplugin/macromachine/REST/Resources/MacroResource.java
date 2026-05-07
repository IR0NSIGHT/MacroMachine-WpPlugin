package org.ironsight.wpplugin.macromachine.REST.Resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.ironsight.wpplugin.macromachine.REST.DTOs.MacroDTO;
import org.ironsight.wpplugin.macromachine.operations.Macro;
import org.ironsight.wpplugin.macromachine.operations.MacroContainer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/macros")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MacroResource
{
    private MacroContainer macroContainer = MacroContainer.getInstance();

    @GET
    public List<MacroDTO> getAll() {
        return macroContainer.queryAll().stream().map(MacroDTO::new).collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public MacroDTO get(@PathParam("id") UUID id) {
        var macro = macroContainer.queryById(id);
        if (macro == null) {
            throw new NotFoundException("Macro not found for uuid=: " + id);
        }
        return new MacroDTO(macro);
    }

    @POST
    public MacroDTO create(MacroDTO dto) {
        Macro macro = dto.toMacro();
        StringBuilder err = new StringBuilder();
        macroContainer.updateMapping(macro, err::append);
        if (!err.isEmpty()) {
            throw new InternalServerErrorException(err.toString());
        }

        return new MacroDTO(macro);
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") UUID id) {
        if (macroContainer.queryContains(id))
            throw new NotFoundException("Macro not found for uuid=: " + id);
        macroContainer.deleteMapping(id);
    }
}
