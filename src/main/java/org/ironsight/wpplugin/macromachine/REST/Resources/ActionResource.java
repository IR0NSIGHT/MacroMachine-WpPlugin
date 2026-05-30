package org.ironsight.wpplugin.macromachine.REST.Resources;

import static org.ironsight.wpplugin.macromachine.operations.MappingAction.getNewEmptyAction;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.ironsight.wpplugin.macromachine.REST.DTOs.ActionDTO;
import org.ironsight.wpplugin.macromachine.operations.ActionType;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;

@Path("/actions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ActionResource {
  private final InputOutputProvider ioProvider;

  public ActionResource(InputOutputProvider ioProvider, MappingActionContainer actionContainer) {
    this.ioProvider = ioProvider;
    this.actionContainer = actionContainer;
  }

  private final MappingActionContainer actionContainer;

  @GET
  public List<ActionDTO> getAllActions() {
    return actionContainer.queryAll().stream().map(ActionDTO::new).collect(Collectors.toList());
  }

  @GET
  @Path("/{id}")
  public ActionDTO getActionById(@PathParam("id") UUID id) {
    var macro = actionContainer.queryById(id);
    if (macro == null) {
      throw new NotFoundException("Action not found for uuid=: " + id);
    }
    return new ActionDTO(macro);
  }

  private List<MappingAction> getDefaultFilters() {
    var filterOutput = new ActionFilterIO();
    return ioProvider.getters.stream()
        .filter(IPositionValueGetter.class::isInstance)
        .map(IPositionValueGetter.class::cast)
        .map(
            input -> {
              var mappingPoints =
                  input.isDiscrete()
                      ? Arrays.stream(input.getAllInputValues())
                          .mapToObj(
                              v ->
                                  new MappingPoint(
                                      v,
                                      v == input.getMinValue()
                                          ? ActionFilterIO.IGNORE_VALUE
                                          : ActionFilterIO.BLOCK_VALUE))
                          .toArray(MappingPoint[]::new)
                      : new MappingPoint[] {
                        new MappingPoint(
                            Math.round((input.getMinValue() + input.getMaxValue()) / 2f) - 1,
                            ActionFilterIO.BLOCK_VALUE),
                        new MappingPoint(
                            Math.round((input.getMinValue() + input.getMaxValue()) / 2f),
                            ActionFilterIO.IGNORE_VALUE),
                      };
              var filterAction =
                  getNewEmptyAction(UUID.randomUUID())
                      .withInput(input)
                      .withOutput(filterOutput)
                      .withType(ActionType.SET)
                      .withNewPoints(mappingPoints);
              return filterAction
                  .withDescription("Default simple filter")
                  .withName(
                      "Filter by: "
                          + input
                              .getName()); // FIXME maybe add some preset to block certain things?
            })
        .toList();
  }

  private List<MappingAction> getDefaultAppliers() {
    var filterOutput = new ActionFilterIO();
    return ioProvider.setters.stream()
        .filter(IPositionValueSetter.class::isInstance)
        .map(IPositionValueSetter.class::cast)
        .map(
            output -> {
              return getNewEmptyAction(UUID.randomUUID())
                  .withInput(new AlwaysIO())
                  .withOutput(output)
                  .withDescription("Default simple applier.")
                  .withName("Set: " + output.getName())
                  .withNewPoints(
                      new MappingPoint[] {
                        new MappingPoint(
                            AlwaysIO.instance.getMinValue(),
                            Math.round((output.getMaxValue() + output.getMinValue()) / 2f))
                      }) // halfway point
              ; // FIXME maybe add some preset to block certain things?
            })
        .toList();
  }

  @GET
  @Path("/filters")
  public List<ActionDTO> getFilters() {
    var filterList = getDefaultFilters();
    var dtos = filterList.stream().map(ActionDTO::new).toList();
    return dtos;
  }

  @GET
  @Path("/appliers")
  public List<ActionDTO> getAppliers() {
    var filterList = getDefaultAppliers();
    var dtos = filterList.stream().map(ActionDTO::new).toList();
    return dtos;
  }

  @POST
  public ActionDTO postAction(ActionDTO dto) {
    System.out.println("POST RECEVIED action dto" + dto.toString());
    try {
      MappingAction macro = dto.toAction();
      StringBuilder err = new StringBuilder();
      actionContainer.updateMapping(macro, err::append);
      if (!err.isEmpty()) {
        throw new InternalServerErrorException(err.toString());
      }
    } catch (NullPointerException ex) {
      System.err.println("DTO produces nullpointer:\n"+ex+"\n"+dto);
      throw ex;
    }
    return dto;
  }

  @DELETE
  @Path("/{id}")
  public void deleteAction(@PathParam("id") UUID id) {
    if (!actionContainer.queryContains(id))
      throw new NotFoundException("Action not found for uuid=: " + id);
    actionContainer.deleteMapping(id);
  }
}
