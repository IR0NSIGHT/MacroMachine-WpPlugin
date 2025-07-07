package org.ironsight.wpplugin.macromachine.operations.FileIO;

import org.ironsight.wpplugin.macromachine.operations.*;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.PineForest;
import org.pepsoft.worldpainter.layers.plants.PlantLayer;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ContainerIOTest {
    public static final UUID emptyMacroUID = UUID.fromString("f448bd00-3cc7-4451-8bac-5b3bf36634c0");
    public static final UUID simpleMacroUID = UUID.fromString("fd69b6c6-1885-4d55-9c03-f73e9394cc7e");
    public static final UUID complexMacroUID = UUID.fromString("e9462d29-bb6c-4eed-8d2f-23c3c9014b4b");
    public static final UUID applyGrassAction01 = UUID.fromString("8378edd5-88ab-4b8e-b274-c05a7c7713c4");
    public static final UUID applyGrassAction02 = UUID.fromString("77b21f11-d454-47f8-8085-cfd9bf9a9f4b");

    static MappingAction applyGrassEverywhere() {
        MappingAction action = MappingAction.getNewEmptyAction(UUID.fromString("ffaefa8c-08d2-4abb-ba81-4691eab57044"))
                .withName("Apply: Grass")
                .withInput(new AlwaysIO())
                .withOutput(new TerrainProvider())
                .withType(ActionType.SET)
                .withNewPoints(new MappingPoint[]{new MappingPoint(0, 0)});
        return action;
    }

    public static void fillWithData(MappingActionContainer actions, MacroContainer macros) {
        // empty macro
        Macro emptyMacro =
                macros.addMapping(emptyMacroUID).withName("My empty macro").withDescription("this macro is empty");
        macros.updateMapping(emptyMacro, Assertions::fail);
        // macro with 2 actions
        Macro simpleMacro =
                macros.addMapping(simpleMacroUID).withName("My simple macro").withDescription("this macro executes" +
                        " 2 " + "actions");
        {
            MappingAction action01 =
                    actions.addMapping(applyGrassAction01).withValuesFrom(applyGrassEverywhere().withName("action 01"));
            MappingAction action02 =
                    actions.addMapping(applyGrassAction02).withValuesFrom(applyGrassEverywhere().withName("action 02"));
            macros.updateMapping(simpleMacro.withUUIDs(new UUID[]{action01.getUid(), action02.getUid()}),
                    Assertions::fail);
        }

        // macro with nested macro
        Macro complexMacroWithNesting = macros.addMapping(complexMacroUID)
                .withName("My complex macro")
                .withDescription("this macro " + "contains another macro")
                .withUUIDs(new UUID[]{simpleMacro.getUid(), emptyMacro.getUid()});
        macros.updateMapping(complexMacroWithNesting, Assertions::fail);
    }

    static ExportContainer getContainerWithData() {
        MacroJsonWrapper[] macros = new MacroJsonWrapper[3];
        ActionJsonWrapper[] actions = new ActionJsonWrapper[5];
        for (int i = 0; i < actions.length; i++) {
            actions[i] = new ActionJsonWrapper(MappingAction.getNewEmptyAction(UUID.randomUUID())
                    .withName("test-action-" + i));
        }
        UUID[][] actionsInMacro = new UUID[3][];
        actionsInMacro[0] = new UUID[]{actions[0].getUid()};
        actionsInMacro[1] = new UUID[]{actions[1].getUid()};
        actionsInMacro[2] = new UUID[]{actions[2].getUid(), actions[3].getUid(), actions[4].getUid()};

        for (int i = 0; i < macros.length; i++) {
            macros[i] = new MacroJsonWrapper("alpine-" + i, "uwu owo doing doing",
                    actionsInMacro[i],
                    UUID.randomUUID(),
                    new boolean[0]);
        }


        ExportContainer container = new ExportContainer("2025-06-14-17-04", "no comment", macros, actions,
                new Layer[0]);
        return container;
    }

    void generateUUIDs() {
        for (int i = 0; i < 10; i++) {
            System.out.println(UUID.randomUUID());
        }
    }

    @Test
    public void readWriteData() throws IOException {
        ExportContainer container = getContainerWithData();
        File tempFile = Files.createTempFile("test", ".tmp").toFile();
        ContainerIO.writeContainerToFile(container, tempFile);

        ExportContainer containerFromFile = ContainerIO.readFromFile(tempFile);
        assertEquals(container, containerFromFile);
    }

    @Test
    public void importFileToContainers() throws IOException {
        LayerProvider layerProvider = new LayerProvider() {
            HashSet<Layer> layers = new HashSet<>();
            @Override
            public Layer getLayerById(String layerId, Consumer<String> layerNotFoundError) {
                return null;
            }

            @Override
            public List<Layer> getLayers() {
                return new ArrayList<>(layers);
            }

            @Override
            public void addLayer(Layer layer) {
                layers.add(layer);
            }
        };

        MappingActionContainer actionContainer = new MappingActionContainer("./ioTestAction.json");
        MacroContainer macroContainer = new MacroContainer("./ioTestMacro.json");

        assertEquals(0, macroContainer.queryAll().size());
        assertEquals(0, actionContainer.queryAll().size());


        ExportContainer dataContainer = getContainerWithData();
        File tempFile = Files.createTempFile("test", ".tmp").toFile();
        ContainerIO.writeContainerToFile(dataContainer, tempFile);

        ContainerIO.importFile(actionContainer, macroContainer, tempFile, new ImportExportPolicy(), Assertions::fail, layerProvider);

        assertEquals(dataContainer.getMacros().length, macroContainer.queryAll().size());
        assertEquals(dataContainer.getActions().length, actionContainer.queryAll().size());
        for (MacroJsonWrapper macroData : dataContainer.getMacros()) {
            assertTrue(macroContainer.queryContains(macroData.getSelfId()));
        }
        for (ActionJsonWrapper actionData : dataContainer.getActions()) {
            assertTrue(actionContainer.queryContains(actionData.getUid()));
        }
    }

    private static byte[] serialize(Serializable obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        return bos.toByteArray();
    }

    @Test
    public void exportActionWithLayerToFile() throws IOException {
        MappingActionContainer actionContainer = new MappingActionContainer("./ioTestAction.json");
        MacroContainer macroContainer = new MacroContainer("./ioTestMacro.json");

        assertEquals(0, macroContainer.queryAll().size());
        assertEquals(0, actionContainer.queryAll().size());
        Layer plantsLayer =  new PlantLayer("Plants", "A custom collection of plants",
                Color.GREEN);

        LayerProvider mockProvider = new LayerProvider() {
            @Override
            public Layer getLayerById(String layerId, Consumer<String> layerNotFoundError) {
                return plantsLayer;
            }

            @Override
            public List<Layer> getLayers() {
                return Collections.singletonList(plantsLayer);
            }

            @Override
            public void addLayer(Layer layer) {
                Assertions.fail();
            }
        };

        UUID actionId = UUID.fromString("2d201b86-f765-4750-a5d0-301363537d68");
        {        //create action with a layer
            MappingAction a = actionContainer.addMapping(actionId)
                    .withName("apply pines")
                    .withInput(new TerrainHeightIO(0, 100))
                    .withOutput(new NibbleLayerSetter(plantsLayer, true));
            actionContainer.updateMapping(Assertions::fail, a);
        }

        // CREATE MACRO THAT USES ACTION
        UUID macroId = UUID.fromString("4ab1b1e3-7fc9-4dc1-9ae0-fc3d95378af4");
        macroContainer.updateMapping(Assertions::fail,
                macroContainer.addMapping(macroId).withName("paint pines macro").withUUIDs(new UUID[]{actionId}));

        ExportContainer c =  ContainerIO.toExportContainer(actionContainer, macroContainer, new ImportExportPolicy(),
                Assertions::fail,
                mockProvider);
        assertArrayEquals(new Layer[]{ plantsLayer }, c.getLayers());

        File tempFile = Files.createTempFile("test", ".tmp").toFile();
        ContainerIO.writeContainerToFile(c, tempFile);
        ExportContainer imported = ContainerIO.readFromFile(tempFile);
        assertEquals(0, c.getLayers()[0].compareTo(imported.getLayers()[0]));
        assertEquals(c.getLayers()[0].getId(),imported.getLayers()[0].getId());
    }

    @Test
    public void exportContainerToFile() throws IOException {
        MappingActionContainer actionContainer = new MappingActionContainer("./ioTestAction.json");
        MacroContainer macroContainer = new MacroContainer("./ioTestMacro.json");

        assertEquals(0, macroContainer.queryAll().size());
        assertEquals(0, actionContainer.queryAll().size());

        fillWithData(actionContainer, macroContainer);
        assertEquals(3, macroContainer.queryAll().size()); // empty, simple, complex
        assertEquals(2, actionContainer.queryAll().size());

        File tempFile = Files.createTempFile("test", ".tmp").toFile();
        ContainerIO.exportToFile(actionContainer, macroContainer, tempFile, new ImportExportPolicy(), Assertions::fail,
                InputOutputProvider.INSTANCE);

        String content = new String(Files.readAllBytes(tempFile.toPath()))
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("  \"exportDate\"\\s*:\\s*\".*?\"", "  \"exportDate\" : \"now\"");

        assertEquals("{\n" +
                "  \"exportDate\" : \"now\",\n" +
                "  \"comment\" : \"no comment\",\n" +
                "  \"macros\" : [ {\n" +
                "    \"macroName\" : \"My simple macro\",\n" +
                "    \"description\" : \"this macro executes 2 actions\",\n" +
                "    \"stepIds\" : [ \"8378edd5-88ab-4b8e-b274-c05a7c7713c4\", " +
                "\"77b21f11-d454-47f8-8085-cfd9bf9a9f4b\" ],\n" +
                "    \"selfId\" : \"fd69b6c6-1885-4d55-9c03-f73e9394cc7e\",\n" +
                "    \"activeIds\" : [ true, true ]\n" +
                "  }, {\n" +
                "    \"macroName\" : \"My complex macro\",\n" +
                "    \"description\" : \"this macro contains another macro\",\n" +
                "    \"stepIds\" : [ \"fd69b6c6-1885-4d55-9c03-f73e9394cc7e\", " +
                "\"f448bd00-3cc7-4451-8bac-5b3bf36634c0\" ],\n" +
                "    \"selfId\" : \"e9462d29-bb6c-4eed-8d2f-23c3c9014b4b\",\n" +
                "    \"activeIds\" : [ true, true ]\n" +
                "  }, {\n" +
                "    \"macroName\" : \"My empty macro\",\n" +
                "    \"description\" : \"this macro is empty\",\n" +
                "    \"stepIds\" : [ ],\n" +
                "    \"selfId\" : \"f448bd00-3cc7-4451-8bac-5b3bf36634c0\",\n" +
                "    \"activeIds\" : [ ]\n" +
                "  } ],\n" +
                "  \"actions\" : [ {\n" +
                "    \"inputId\" : \"HEIGHT\",\n" +
                "    \"inputData\" : [ -64, 319 ],\n" +
                "    \"outputId\" : \"ANNOTATION\",\n" +
                "    \"outputData\" : [ ],\n" +
                "    \"actionType\" : \"SET\",\n" +
                "    \"inputPoints\" : [ ],\n" +
                "    \"outputPoints\" : [ ],\n" +
                "    \"name\" : \"New Action\",\n" +
                "    \"description\" : \"description of the action\",\n" +
                "    \"uid\" : \"77b21f11-d454-47f8-8085-cfd9bf9a9f4b\"\n" +
                "  }, {\n" +
                "    \"inputId\" : \"HEIGHT\",\n" +
                "    \"inputData\" : [ -64, 319 ],\n" +
                "    \"outputId\" : \"ANNOTATION\",\n" +
                "    \"outputData\" : [ ],\n" +
                "    \"actionType\" : \"SET\",\n" +
                "    \"inputPoints\" : [ ],\n" +
                "    \"outputPoints\" : [ ],\n" +
                "    \"name\" : \"New Action\",\n" +
                "    \"description\" : \"description of the action\",\n" +
                "    \"uid\" : \"8378edd5-88ab-4b8e-b274-c05a7c7713c4\"\n" +
                "  } ],\n" +
                "  \"layers\" : \"rO0ABXVyAChbTG9yZy5wZXBzb2Z0LndvcmxkcGFpbnRlci5sYXllcnMuTGF5ZXI7BDpGR8pvArcCAAB4cAAAAAA=\"\n" +
                "}", content);
    }
}