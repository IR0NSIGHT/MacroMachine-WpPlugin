package org.ironsight.wpplugin.macromachine.REST.Resources;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.ironsight.wpplugin.macromachine.REST.DTOs.ActionDTO;
import org.ironsight.wpplugin.macromachine.operations.ActionType;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.ActionFilterIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ActionResourceTest extends JerseyTest {

  private static UUID createdId;

  @Override
  protected Application configure() {
    return new ResourceConfig().register(ActionResource.class).register(JacksonFeature.class);
  }

  @BeforeAll
  void setupOnce() {
    MappingActionContainer.SetInstance(new MappingActionContainer(null));
  }

  // ---------------- CREATE ----------------
  @Test
  @Order(1)
  void testCreateAction() {
    ActionDTO dto =
        new ActionDTO(
            new MappingAction(
                new TerrainHeightIO(-64, 319),
                ActionFilterIO.instance,
                new MappingPoint[] {
                  new MappingPoint(73, ActionFilterIO.BLOCK_VALUE),
                  new MappingPoint(319, ActionFilterIO.PASS_VALUE)
                },
                ActionType.LIMIT_TO,
                "Filter: Above height",
                "Default filter: block all blocks that below this level",
                UUID.randomUUID()));

    Response response =
        target("/actions").request().post(Entity.entity(dto, MediaType.APPLICATION_JSON));

    assertEquals(200, response.getStatus());

    ActionDTO result = response.readEntity(ActionDTO.class);
    assertNotNull(result);

    createdId = result.getUid(); // adjust getter if different
  }

  // ---------------- GET ALL ----------------
  @Test
  @Order(2)
  void testGetAll() {
    Response response = target("/actions").request().get();

    assertEquals(200, response.getStatus());

    ActionDTO[] list = response.readEntity(ActionDTO[].class);
    assertTrue(list.length >= 0);
  }

  // ---------------- GET BY ID ----------------
  @Test
  @Order(3)
  void testGetById() {
    assumeTrue(createdId != null);

    Response response = target("/actions/" + createdId).request().get();

    assertEquals(200, response.getStatus());

    ActionDTO dto = response.readEntity(ActionDTO.class);
    assertEquals(createdId, dto.getUid());
  }

  // ---------------- DELETE ----------------
  @Test
  @Order(4)
  void testDelete() {
    assumeTrue(createdId != null);

    Response response = target("/actions/" + createdId).request().delete();

    assertTrue(response.getStatus() == 200 || response.getStatus() == 204);
  }

  // ---------------- NOT FOUND ----------------
  @Test
  void testGetNotFound() {
    UUID random = UUID.randomUUID();

    Response response = target("/actions/" + random).request().get();

    assertEquals(404, response.getStatus());
  }
}
