package org.ironsight.wpplugin.macromachine.REST.Resources;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;
import org.ironsight.wpplugin.macromachine.REST.DTOs.LayerDTO;
import org.ironsight.wpplugin.macromachine.operations.*;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;
import org.pepsoft.worldpainter.layers.Layer;

@Path("/layers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LayerResource {
  private final InputOutputProvider ioProvider;
  private final MacroContainer macroContainer;
  private final MappingActionContainer actionContainer;

  public LayerResource(
      InputOutputProvider ioProvider,
      MappingActionContainer actionContainer,
      MacroContainer macros) {
    this.ioProvider = ioProvider;
    this.actionContainer = actionContainer;
    this.macroContainer = macros;
  }

  @GET
  public List<LayerDTO> getAllLayers() {
    // return ioProvider.getLayers().stream().map(LayerDTO::new).toList(); //FIXME this is all in
    // project, not all-known-to-mm
    return collectLayers();
  }

  @GET
  @Path("/{id}")
  public boolean existsLayerInProject(@PathParam("id") String id) {
    return ioProvider.existsLayerWithId(id);
  }

  @GET
  @Path("/{id}/icon")
  @Produces("image/png")
  public Response getLayerIcon(@PathParam("id") String layerId) throws IOException {
    if (!ioProvider.existsLayerWithId(layerId))
      return Response.status(Response.Status.NOT_FOUND).build();

    var layer = ioProvider.getLayerById(layerId, System.err::println);
    if (layer == null) return Response.status(Response.Status.NOT_FOUND).build();

    BufferedImage image = layer.getIcon();
    if (image == null) return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, "png", baos);

    return Response.ok(baos.toByteArray()).header("Cache-Control", "public, max-age=86400").build();
  }

  private List<LayerDTO> collectLayers() {
    HashMap<String, LayerDTO> layerIdToDTO =
        new HashMap<>() {
          @Override
          public LayerDTO put(String key, LayerDTO value) {
            return super.put(key, value);
          }
        };
    Object2ObjectOpenCustomHashMap<LayerDTO, Set<UUID>> layerToUsingMacros =
        new Object2ObjectOpenCustomHashMap<>(
            new Hash.Strategy<>() {
              @Override
              public int hashCode(LayerDTO layer) {
                return layer.id().hashCode();
              }

              @Override
              public boolean equals(LayerDTO a, LayerDTO b) {
                if (a == null || b == null) return false;
                return Objects.equals(a.id(), b.id());
              }
            });
    // get layers in project
    ioProvider.getLayers().stream()
        .map(LayerDTO::new)
        .forEach(
            dto -> {
              layerToUsingMacros.put(dto, new HashSet<>());
              layerIdToDTO.put(dto.id(), dto);
            });

    macroContainer
        .queryAll()
        .forEach(
            macro ->
                Arrays.stream(macro.getExecutionUUIDs())
                    .filter(actionContainer::queryContains)
                    .map(actionContainer::queryById)
                    .map(a -> new IMappingValue[] {a.getInput(), a.getOutput()})
                    .flatMap(Arrays::stream)
                    .filter(a -> a instanceof ILayerGetter)
                    .map(a -> (ILayerGetter) a)
                    .forEach(
                        layerIo -> {
                          var dto = layerIdToDTO.getOrDefault(layerIo.getLayerId(), null);
                          if (dto == null) {
                            dto =
                                new LayerDTO(
                                    layerIo.getLayerName(),
                                    "unknown",
                                    Layer.DataSize.NONE,
                                    -1,
                                    layerIo.getLayerId(),
                                    false,
                                    "unknown",
                                    layerIo.isCustomLayer(),
                                    new ArrayList<>(),
                                    false);
                            layerToUsingMacros.put(dto, new HashSet<>());
                            layerIdToDTO.put(dto.id(), dto);
                          }
                          layerToUsingMacros.get(dto).add(macro.getUid());
                        }));

    layerToUsingMacros
        .entrySet()
        .forEach(
            entry -> {
              entry.getKey().macrosUsingLayer().addAll(entry.getValue());
            });

    return layerToUsingMacros.keySet().stream().toList();
  }
}
