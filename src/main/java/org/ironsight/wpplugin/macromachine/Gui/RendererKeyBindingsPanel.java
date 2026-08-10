package org.ironsight.wpplugin.macromachine.Gui;

import java.awt.BorderLayout;
import java.lang.reflect.Field;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class RendererKeyBindingsPanel extends JPanel
{

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
            Class<?> keyBindingClass = Class.forName("org.ironsight.cubearray.render.KeyBinding");
            Object[] values = (Object[]) keyBindingClass.getMethod("values").invoke(null);
            Field keyNameField = keyBindingClass.getField("keyName");

            for (Object kb : values) {
                String enumName = ((Enum<?>) kb).name();
                String keyName = (String) keyNameField.get(kb);
                String actionLabel = formatEnumName(enumName);
                sb.append("<tr><td>").append(actionLabel).append("</td><td>").append(keyName).append("</td></tr>");
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

    private String formatEnumName(String enumName) {
        String spaced = enumName.replace('_', ' ').toLowerCase();
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
