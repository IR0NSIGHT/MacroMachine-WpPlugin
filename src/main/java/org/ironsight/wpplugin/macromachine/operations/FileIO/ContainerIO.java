package org.ironsight.wpplugin.macromachine.operations.FileIO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ironsight.wpplugin.macromachine.operations.Macro;
import org.ironsight.wpplugin.macromachine.operations.MacroContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.ILayerGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.LayerProvider;
import org.pepsoft.worldpainter.layers.Layer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

import static org.ironsight.wpplugin.macromachine.operations.AbstractOperationContainer.createBackup;

public class ContainerIO {
    public static MacroJsonWrapper fromMacro(Macro macro) {
        return new MacroJsonWrapper(macro.getName(), macro.getDescription(), macro.getExecutionUUIDs(), macro.getUid(),
                macro.getActiveActions());
    }

    public static Macro toMacro(MacroJsonWrapper json) {
        return new Macro(json.getMacroName(), json.getDescription(), json.getStepIds(), json.getSelfId(),
                json.getActiveIds());
    }

    public static MappingAction toAction(ActionJsonWrapper json) {
        return MappingAction.fromJsonWrapper(json);
    }

    public static ActionJsonWrapper fromAction(MappingAction action) {
        return new ActionJsonWrapper(action);
    }

    public static void writeContainerToFile(ExportContainer container, File file) throws IOException {
        System.out.println("WRITE FILE");
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs(); // Create all necessary parent directories
        }
        assert file.getParentFile().exists();
        assert file.getParentFile().isDirectory() : "regression: if its a file, saving will fail";
        file.createNewFile();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(container);
            Files.write(file.toPath(), jsonString.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw e;
        }
    }

    public static ExportContainer readFromFile(File file) throws IOException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            ExportContainer obj = objectMapper.readValue(content, ExportContainer.class);

            return obj;
        } catch (JsonMappingException ex) {
            throw ex;
        } catch (JsonProcessingException ex) {
            throw ex;
        }
    }

    public static ExportContainer toExportContainer(MappingActionContainer actionContainer,
                                                    MacroContainer macroContainer,
                                                    ImportExportPolicy policy, Consumer<String> onImportError,
                                                    LayerProvider layerProvider) {
        LinkedList<MacroJsonWrapper> macroData = new LinkedList<>();
        for (Macro macro : macroContainer.queryAll()) {
            if (policy.allowImportExport(macro))
                macroData.add(fromMacro(macro));
        }
        LinkedList<ActionJsonWrapper> actionData = new LinkedList<>();
        Collection<MappingAction> actionDataMappings = new LinkedList<>();
        for (MappingAction action : actionContainer.queryAll()) {
            if (policy.allowImportExport(action)) {
                actionData.add(fromAction(action));
                actionDataMappings.add(action);
            }
        }

        LinkedList<Layer> layerData =
                new LinkedList<>();
        for (Layer l : getUsedLayers(actionDataMappings, layerProvider, onImportError)) {
            if (policy.allowImportExport(l))
                layerData.add(l);
        }


        ExportContainer container = new ExportContainer(generateISO8601TimestampWithTimeZone(), "no comment",
                macroData.toArray(new MacroJsonWrapper[0]), actionData.toArray(new ActionJsonWrapper[0]
        ), layerData.toArray(new Layer[0]));
        return container;
    }

    public static void exportToFile(MappingActionContainer actionContainer, MacroContainer macroContainer, File file,
                                    ImportExportPolicy policy, Consumer<String> onImportError,
                                    LayerProvider layerProvider) {
        ExportContainer container =
                toExportContainer(actionContainer, macroContainer, policy, onImportError, layerProvider);
        try {
            writeContainerToFile(container, file);

        } catch (IOException e) {
            onImportError.accept(e.getMessage());
        }
    }

    private static HashSet<Layer> getUsedLayers(Collection<MappingAction> actions, LayerProvider layerProvider,
                                                Consumer<String> onMissingLayerError) {
        HashSet<Layer> layers = new HashSet<>();
        for (MappingAction a : actions) {
            String inputLayer = getLayerIdFromInputOutput(a.getInput());
            String outputLayer = getLayerIdFromInputOutput(a.getOutput());
            if (inputLayer != null)
                layers.add(layerProvider.getLayerById(inputLayer, onMissingLayerError));
            if (outputLayer != null)
                layers.add(layerProvider.getLayerById(outputLayer, onMissingLayerError));
        }
        return layers;
    }

    private static String getLayerIdFromInputOutput(Object io) {
        if (io instanceof ILayerGetter) {
            return ((ILayerGetter) io).getLayerId();
        }
        return null;
    }

    private static boolean containsUnknownActions(ExportContainer data,
                                                  HashSet<UUID> knownActions,
                                                  HashSet<UUID> knownMacros, Consumer<String> onImportError) {
        //test all child actions
        for (MacroJsonWrapper macroData : data.getMacros()) {
            for (UUID child : macroData.getStepIds()) {
                if (knownMacros.contains(child))
                    continue; //we dont care about nested macros
                if (!knownActions.contains(child)) {
                    onImportError.accept("Macro " + macroData.getMacroName() + " is using an action that can not be " +
                            "found: " + child);
                    return true;
                }
            }

        }
        return false;
    }

    /**
     * check if one action is used by two or more macros. illegal behaviour, only macros can be shared between other
     * macros.
     *
     * @param data
     * @param onImportError
     * @return
     */
    private static boolean containsSharedActions(ExportContainer data, Consumer<String> onImportError) {
        HashSet<UUID> macrosSet = new HashSet<>();
        HashSet<UUID> seenActions = new HashSet<>();
        //collect all macro uids
        for (MacroJsonWrapper macroData : data.getMacros()) {
            Macro macro = toMacro(macroData);
            macrosSet.add(macro.getUid());
        }
        //test all child actions
        for (MacroJsonWrapper macroData : data.getMacros()) {
            for (UUID child : macroData.getStepIds()) {
                if (macrosSet.contains(child))
                    continue; //we dont care about nested macros
                if (seenActions.contains(child)) {
                    onImportError.accept("Illegal: action " + child + " is used by multiple macros.");
                    return true;
                }
                seenActions.add(child);
            }

        }
        return false;
    }


    public static void importFile(MappingActionContainer actionContainer, MacroContainer macroContainer, File file,
                                  ImportExportPolicy policy, Consumer<String> onImportError) {
        try {
            ExportContainer data = readFromFile(file);
            if (containsSharedActions(data, onImportError)) {
                assert false : "actions were used by multiple macros which goes against policy " +
                        "and" +
                        " will lead to undefined behaviour.";
                createBackup(file.getPath());
                return;
            }
            // collect everything that should be imported
            HashSet<UUID> macrosToImport = new HashSet<>();
            HashSet<UUID> actionsInMacros = new HashSet<>();
            HashSet<UUID> actionsToImport = new HashSet<>();
            for (MacroJsonWrapper macroData : data.getMacros()) {
                Macro macro = toMacro(macroData);
                if (policy.allowImportExport(macro)) {
                    macrosToImport.add(macro.getUid());
                    actionsInMacros.addAll(Arrays.asList(macro.getExecutionUUIDs()));
                }
            }
            for (ActionJsonWrapper actionData : data.getActions()) {
                MappingAction action = toAction(actionData);
                if (actionsInMacros.contains(action.getUid()) && policy.allowImportExport(action)) {
                    actionsToImport.add(action.getUid());
                }
            }

            if (containsUnknownActions(data, actionsToImport, macrosToImport, onImportError)) {
                assert false;
                createBackup(file.getPath());
                return;
            }

            {            // do import
                Macro[] macros = new Macro[macrosToImport.size()];
                int idx = 0;
                for (MacroJsonWrapper macroData : data.getMacros()) {
                    Macro macro = toMacro(macroData);
                    if (macrosToImport.contains(macro.getUid())) {
                        macros[idx++] = macro;
                    }
                }
                macroContainer.updateMapping(onImportError, macros);
            }

            {
                MappingAction[] actions = new MappingAction[actionsToImport.size()];
                int idx = 0;
                for (ActionJsonWrapper actionData : data.getActions()) {
                    MappingAction action = toAction(actionData);
                    if (actionsToImport.contains(action.getUid())) {
                        assert action.getUid() != null;
                        actions[idx++] = action;
                    }
                }
                actionContainer.updateMapping(onImportError, actions);
            }
        } catch (NoSuchFileException e) {
            ; // not an error.
        } catch (IOException e) {
            onImportError.accept(e.getMessage());
        }
    }

    public static String generateISO8601TimestampWithTimeZone() {
        ZonedDateTime now = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        return now.format(formatter);
    }
}
