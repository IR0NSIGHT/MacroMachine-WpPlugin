package org.ironsight.wpplugin.macromachine.operations.FileIO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ironsight.wpplugin.macromachine.operations.Macro;
import org.ironsight.wpplugin.macromachine.operations.MacroContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;

import javax.crypto.Mac;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

        ExportContainer container = new ExportContainer(generateISO8601TimestampWithTimeZone(),"no comment",
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
            HashSet<UUID> macrosSet = new HashSet<>();
            HashSet<UUID> actionsSet = new HashSet<>();
            for (MacroJsonWrapper macroData : data.getMacros()) {
                Macro macro = toMacro(macroData);
                if (policy.allowImportExport(macro)) {
                    macrosSet.add(macro.getUid());
                }
            }
            for (ActionJsonWrapper actionData : data.getActions()) {
                MappingAction action = toAction(actionData);
                if (policy.allowImportExport(action)) {
                    actionsSet.add(action.getUid());
                }
            }

            {            // do import
                Macro[] macros = new Macro[macrosSet.size()];
                int idx = 0;
                for (MacroJsonWrapper macroData : data.getMacros()) {
                    Macro macro = toMacro(macroData);
                    if (macrosSet.contains(macro.getUid())) {
                        macros[idx++] = macro;
                    }
                }
                macroContainer.updateMapping(onImportError, macros);
            }

            {
                MappingAction[] actions = new MappingAction[actionsSet.size()];
                int idx = 0;
                for (ActionJsonWrapper actionData : data.getActions()) {
                    MappingAction action = toAction(actionData);
                    if (actionsSet.contains(action.getUid())) {
                        assert action.getUid() != null;
                        actions[idx++] = action;
                    }
                }
                actionContainer.updateMapping(onImportError, actions);
            }
        } catch (NoSuchFileException e) {
            ; // not an error.
        }
        catch (IOException e) {
            onImportError.accept(e.getMessage());
        }
    }

    public static String generateISO8601TimestampWithTimeZone() {
        ZonedDateTime now = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        return now.format(formatter);
    }
}
