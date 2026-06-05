package org.ironsight.wpplugin.macromachine.REST.Resources;

import static org.ironsight.wpplugin.macromachine.operations.MappingAction.getNewEmptyAction;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.ironsight.wpplugin.macromachine.REST.DTOs.ActionDTO;
import org.ironsight.wpplugin.macromachine.REST.DTOs.LayerDTO;
import org.ironsight.wpplugin.macromachine.operations.*;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;

@Path("/layers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LayerResource {
    private final InputOutputProvider ioProvider;
    private final MacroContainer macroContainer;
    private final MappingActionContainer actionContainer;

    public LayerResource(InputOutputProvider ioProvider,  MappingActionContainer actionContainer, MacroContainer macros) {
        this.ioProvider = ioProvider;
        this.actionContainer = actionContainer;
        this.macroContainer = macros;
    }


    @GET
    public List<LayerDTO> getAllLayers() {
        return ioProvider.getLayers().stream().map(LayerDTO::new).toList(); //FIXME this is all in project, not all-known-to-mm
    }

    @GET
    @Path("/{id}")
    public boolean existsLayerInProject(@PathParam("id") String id) {
        return true;
    }
}
