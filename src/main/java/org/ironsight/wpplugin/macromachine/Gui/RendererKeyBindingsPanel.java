package org.ironsight.wpplugin.macromachine.Gui;

import java.awt.BorderLayout;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.ironsight.wpplugin.macromachine.Gui.HelpDialog.HelpItem;

public class RendererKeyBindingsPanel extends JPanel
{
    public static final String HELP_TITLE = "3D Preview";
    public static final String HELP_EXPLANATION = """
            Use the 3D Preview to inspect schematics and terrain as voxel cubes in a standalone window.
            The preview hot-swaps data when a new selection is rendered and keeps the camera position.
            Keys below control the 3D view once the window is open.
            """;

    public static List<HelpItem> getHelpItems() {
        try {
            List<Binding> bindings = loadBindings();
            if (bindings.isEmpty()) {
                return List.of(new HelpItem("No bindings", "No keybindings available"));
            }
            List<HelpItem> items = new ArrayList<>(bindings.size());
            for (Binding b : bindings) {
                String effect = b.description != null && !b.description.isBlank()
                        ? b.description
                        : formatEnumName(b.enumName);
                items.add(new HelpItem(b.keyName, effect));
            }
            return List.copyOf(items);
        } catch (ClassNotFoundException e) {
            return List.of(new HelpItem("No CubeArray", "CubeArray dependency not found on classpath"));
        } catch (Exception e) {
            return List.of(new HelpItem("Error", "Failed to load keybindings: " + e.getMessage()));
        }
    }

    public RendererKeyBindingsPanel() {
        super(new BorderLayout());

        String html = buildHtml();
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setText(html);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    private String buildHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><style>")
                .append("body { font-family: sans-serif; padding: 12px; }")
                .append("table { border-collapse: collapse; width: 100%; }")
                .append("th, td { border: 1px solid #ccc; padding: 6px 10px; text-align: left; }")
                .append("th { background-color: #f0f0f0; }")
                .append("</style></head><body>");
        sb.append("<h2>3D Renderer Keybindings</h2>");
        sb.append("<table><tr><th>Action</th><th>Key</th></tr>");

        try {
            List<Binding> bindings = loadBindings();
            if (bindings.isEmpty()) {
                sb.append("<tr><td colspan='2'>No keybindings available</td></tr>");
            } else {
                for (Binding b : bindings) {
                    String actionLabel = formatEnumName(b.enumName);
                    sb.append("<tr><td>")
                            .append(escapeHtml(actionLabel))
                            .append("</td><td>")
                            .append(escapeHtml(b.keyName))
                            .append("</td></tr>");
                }
            }
        } catch (ClassNotFoundException e) {
            sb.append("<tr><td colspan='2'>CubeArray dependency not found on classpath</td></tr>");
        } catch (Exception e) {
            sb.append("<tr><td colspan='2'>Failed to load keybindings: ")
                    .append(escapeHtml(e.getMessage()))
                    .append("</td></tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private static String formatEnumName(String enumName) {
        String spaced = enumName.replace('_', ' ').toLowerCase();
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private record Binding(String enumName, String keyName, String description) {
    }

    private static List<Binding> loadBindings() throws ClassNotFoundException, ReflectiveOperationException {
        Class<?> keyBindingClass = Class.forName("org.ironsight.cubearray.render.KeyBinding");
        Object[] values = (Object[]) keyBindingClass.getMethod("values").invoke(null);
        Field keyNameField = keyBindingClass.getField("keyName");
        Field descriptionField;
        try {
            descriptionField = keyBindingClass.getField("description");
        } catch (NoSuchFieldException e) {
            descriptionField = null;
        }
        List<Binding> result = new ArrayList<>(values.length);
        for (Object kb : values) {
            String enumName = ((Enum<?>) kb).name();
            String keyName = (String) keyNameField.get(kb);
            String description = null;
            if (descriptionField != null) {
                Object d = descriptionField.get(kb);
                if (d instanceof String s)
                    description = s;
            }
            result.add(new Binding(enumName, keyName, description));
        }
        return result;
    }
}
