package org.ironsight.wpplugin.macromachine.REST.DTOs;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.pepsoft.worldpainter.layers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Schema(
        description =
                "A layer used in worldpainter.")

public record LayerDTO(  @Schema(description = "Display name of the layer", requiredMode = Schema.RequiredMode.REQUIRED)
                         String name,
                         @Schema(description = "Description of the layer", requiredMode = Schema.RequiredMode.REQUIRED)
                         String description,
                         @Schema(description = "Amount of possible values the layer can store", requiredMode = Schema.RequiredMode.REQUIRED)
                         Layer.DataSize dataSize,
                         @Schema(description = "IDK? Possible export application priority?", requiredMode = Schema.RequiredMode.REQUIRED)
                         int priority,
                         @Schema(description = "Unique, stable ID of layer", requiredMode = Schema.RequiredMode.REQUIRED)
                         String id,
                         @Schema(description = "Discrete. IDK about the semantic", requiredMode = Schema.RequiredMode.REQUIRED)
                         boolean discrete,
                         @Schema(description = "Specific layer category", requiredMode = Schema.RequiredMode.REQUIRED, examples = {"Custom Objects","CityLayer","Annotations"})
                         String type,
                         @Schema(description = "is Custom layer", requiredMode = Schema.RequiredMode.REQUIRED)
                         boolean custom,
                         @ArraySchema(
                                 schema = @Schema(
                                         implementation = UUID.class,
                                         format = "uuid"
                                 ),
                                 arraySchema = @Schema(
                                         description = "IDs of macros that use this layer",
                                         requiredMode = Schema.RequiredMode.REQUIRED
                                 )
                         )
                         List<UUID> macrosUsingLayer,
                       @Schema(description = "Input", requiredMode = Schema.RequiredMode.REQUIRED)
                    boolean presentInProject
) {
    public LayerDTO(Layer layer) {
        this(layer.getName(), layer.getDescription(), layer.getDataSize(), layer.getPriority(), layer.getId(), layer.discrete, GetType(layer), layer instanceof CustomLayer,
                new ArrayList<>(), true);
    }



    public static String GetType(Layer layer) {
        if (layer instanceof CustomLayer) {
            return ((CustomLayer) layer).getType();
        } else {
            return layer.getClass().getSimpleName();
        }
    }
};

