package org.ironsight.wpplugin.macromachine.operations.FileIO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ironsight.wpplugin.macromachine.operations.Macro;
import org.ironsight.wpplugin.macromachine.operations.MacroContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.ironsight.wpplugin.macromachine.MacroMachinePlugin.error;

public class ContainerIO {
    public static MacroJsonWrapper fromMacro(Macro macro) {
        return new MacroJsonWrapper(macro.getName(), macro.getDescription(), macro.getExecutionUUIDs(), macro.getUid());
    }

    public static Macro toMacro(MacroJsonWrapper json){
        return new Macro(json.getMacroName(), json.getDescription(), json.getStepIds(), json.getSelfId());
    }

    public static MappingAction toAction(ActionJsonWrapper json) {
        return MappingAction.fromJsonWrapper(json);
    }

    public static ActionJsonWrapper fromAction(MappingAction action) {
        return new ActionJsonWrapper(action);
    }

    public static void writeContainerToFile(ExportContainer container, File file) throws IOException {
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
            ExportContainer obj = objectMapper.readValue(Files.readString(file.toPath(), StandardCharsets.UTF_8),
                    ExportContainer.class);

            return obj;
        }catch (JsonMappingException ex) {
            throw  ex;
        } catch (JsonProcessingException ex) {
            throw  ex;
        }
    }

    public static void exportFile(MappingActionContainer actionContainer, MacroContainer macroContainer, File file,
                                  ImportExportPolicy policy, Consumer<String> onImportError) {
        LinkedList<MacroJsonWrapper> macroData = new LinkedList<>();
        for (Macro macro: macroContainer.queryAll()) {
            if (policy.allowImportExport(macro))
                macroData.add(fromMacro(macro));
        }
        LinkedList<ActionJsonWrapper> actionData = new LinkedList<>();
        for (MappingAction action: actionContainer.queryAll()) {
            if (policy.allowImportExport(action))
                actionData.add(fromAction(action));
        }

        ExportContainer container = new ExportContainer("now","no comment",
                macroData.toArray(new MacroJsonWrapper[0]), actionData.toArray(new ActionJsonWrapper[0]));
        try {
            writeContainerToFile(container, file);

        } catch (IOException e) {
            onImportError.accept(e.getMessage());
        }
    }

    public static void importFile(MappingActionContainer actionContainer, MacroContainer macroContainer, File file,
                                  ImportExportPolicy policy, Consumer<String> onImportError) {
        try {
            ExportContainer data = readFromFile(file);

            // collect everything that should be imported
            HashSet<UUID> toBeImported = new HashSet<>();
            for (MacroJsonWrapper macroData : data.getMacros()) {
                Macro macro = toMacro(macroData);
                if (policy.allowImportExport(macro)) {
                    toBeImported.add(macro.getUid());
                    toBeImported.addAll(List.of(macro.getExecutionUUIDs()));
                }
            }
            for (ActionJsonWrapper actionData : data.getActions()) {
                MappingAction action = toAction(actionData);
                if (policy.allowImportExport(action)) {
                    toBeImported.add(action.getUid());
                }
            }

            // do import
            for (MacroJsonWrapper macroData : data.getMacros()) {
                Macro macro = toMacro(macroData);
                if (toBeImported.contains(macro.getUid())) {
                    macroContainer.updateMapping(macro, onImportError);
                }
            }
            for (ActionJsonWrapper actionData : data.getActions()) {
                MappingAction action = toAction(actionData);
                if (toBeImported.contains(action.getUid())) {
                    assert action.getUid() != null;
                    actionContainer.updateMapping(action, onImportError);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
