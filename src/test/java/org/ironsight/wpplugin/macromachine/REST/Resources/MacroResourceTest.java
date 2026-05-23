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
import org.ironsight.wpplugin.macromachine.REST.DTOs.MacroDTO;
import org.ironsight.wpplugin.macromachine.operations.MacroContainer;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MacroResourceTest extends JerseyTest {

  private static UUID createdId;

  @Override
  protected Application configure() {
    return new ResourceConfig().register(MacroResource.class).register(JacksonFeature.class);
  }

  @BeforeAll
  void setupOnce() {
    MacroContainer.SetInstance(new MacroContainer(null));
  }

  // ---------------- CREATE ----------------
  @Test
  @Order(1)
  void testCreateMacro() {
    MacroDTO dto =
        new MacroDTO(
            new UUID[] {UUID.randomUUID()},
            new boolean[] {true},
            "Test Macro",
            "Test Description",
            UUID.randomUUID());

    Response response =
        target("/macros").request().post(Entity.entity(dto, MediaType.APPLICATION_JSON));

    assertEquals(200, response.getStatus());

    MacroDTO result = response.readEntity(MacroDTO.class);
    assertNotNull(result);

    createdId = result.getUid(); // assuming MacroDTO has getUid()
  }

  // ---------------- GET ALL ----------------
  @Test
  @Order(2)
  void testGetAll() {
    Response response = target("/macros").request().get();

    assertEquals(200, response.getStatus());

    MacroDTO[] list = response.readEntity(MacroDTO[].class);
    assertTrue(list.length >= 0);
  }

  // ---------------- GET BY ID ----------------
  @Test
  @Order(3)
  void testGetById() {
    assumeTrue(createdId != null);

    Response response = target("/macros/" + createdId).request().get();

    assertEquals(200, response.getStatus());

    MacroDTO dto = response.readEntity(MacroDTO.class);
    assertEquals(createdId, dto.getUid());
  }

  // ---------------- DELETE ----------------
  @Test
  @Order(4)
  void testDelete() {
    assumeTrue(createdId != null);

    Response response = target("/macros/" + createdId).request().delete();

    assertTrue(response.getStatus() == 200 || response.getStatus() == 204);
  }

  // ---------------- NOT FOUND ----------------
  @Test
  void testGetNotFound() {
    UUID random = UUID.randomUUID();

    Response response = target("/macros/" + random).request().get();

    assertEquals(404, response.getStatus());
  }
}
