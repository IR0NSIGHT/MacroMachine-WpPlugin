package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import static org.ironsight.wpplugin.macromachine.Gui.HelpDialog.getHelpButton;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.*;

import org.ironsight.wpplugin.macromachine.Gui.HelpDialog.HelpItem;
import org.pepsoft.worldpainter.layers.bo2.WPObjectListCellRenderer;
import org.pepsoft.worldpainter.objects.WPObject;

public class OptionsPanel extends JPanel
{
    public static final String HELP_TITLE = "City Editor";
    public static final String HELP_EXPLANATION = """
            Use the City Tool to place and edit buildings in a City Layer. Create or import a City Layer, select it and use this tool to place schematics in the map.
            Select buildings on the map, then use the interactions below to place, move, and edit them.
            The tool settings show you the current state of your selected building. You can change the building type by selecting a different one from the list.

            City Layers are not compatible with undo/redo. Do not use undo/redo while editing one.
            """;
    public static final List<HelpItem> HELP_ITEMS = List.of(new HelpItem("Ctrl + left click", "Place a new building"),
            new HelpItem("Left click", "Select or deselect the building under the cursor"),
            new HelpItem("Left-button drag", "Select buildings fully inside the drag rectangle"),
            new HelpItem("Right click", "Move the selection center to the cursor"),
            new HelpItem("Shift + mouse wheel", "Change the selected building type"),
            new HelpItem("Ctrl + A", "Select all buildings"), new HelpItem("Escape", "Clear the selection"),
            new HelpItem("Ctrl + C", "Copy selected buildings"), new HelpItem("Ctrl + X", "Cut selected buildings"),
            new HelpItem("Ctrl + V", "Paste buildings at the cursor"),
            new HelpItem("Q", "Randomize selected buildings using enabled random options"),
            new HelpItem("W/A/S/D", "Move selected buildings"),
            new HelpItem("C", "Rotate selected buildings around the selection center"),
            new HelpItem("X", "Mirror selected buildings"), new HelpItem("Delete", "Delete selected buildings"));

    private final JList<WPObject> list = new JList<>();
    private final JLabel warningLabel = new JLabel("Please select a city layer");
    private JPanel checkboxPanel;
    private final JCheckBox randomMirroredCheckBox = new JCheckBox("random mirrored");
    private final JCheckBox randomSelectCheckBox = new JCheckBox("random select");
    private final JCheckBox randomRotateCheckBox = new JCheckBox("random rotate");
    private final JCheckBox useHighlightColorsCheckBox = new JCheckBox("use highlight colors");
    private final JLabel selectionLabel = new JLabel("No objects selected");
    private final JLabel clipboardLabel = new JLabel("No objects in clipboard");
    private final JLabel statusLabel = new JLabel();
    private JLabel previewPanel;
    private JScrollPane listPanel;
    private final Consumer<CityEditToolOperation.PlacementOptions> placementOptionsChanged;
    private final Consumer<Integer> objectSelectionChanged;
    private final Consumer<Boolean> highlightColorsChanged;
    private final Supplier<CityLayer> selectedLayerSupplier;
    private final Supplier<ObjectState> selectedStateSupplier;

    OptionsPanel(Consumer<CityEditToolOperation.PlacementOptions> placementOptionsChanged,
            Consumer<Integer> objectSelectionChanged, Consumer<Boolean> highlightColorsChanged,
            Supplier<CityLayer> selectedLayerSupplier, Supplier<ObjectState> selectedStateSupplier) {
        this.placementOptionsChanged = Objects.requireNonNull(placementOptionsChanged);
        this.objectSelectionChanged = Objects.requireNonNull(objectSelectionChanged);
        this.highlightColorsChanged = Objects.requireNonNull(highlightColorsChanged);
        this.selectedLayerSupplier = Objects.requireNonNull(selectedLayerSupplier);
        this.selectedStateSupplier = Objects.requireNonNull(selectedStateSupplier);
        init();
    }

    void setPlacementOptions(CityEditToolOperation.PlacementOptions options) {
        randomRotateCheckBox.setSelected(options.randomRotate());
        randomSelectCheckBox.setSelected(options.randomSelect());
        randomMirroredCheckBox.setSelected(options.randomMirror());
    }

    void setObjects(ArrayList<WPObject> objects) {
        DefaultListModel<WPObject> model = new DefaultListModel<>();
        model.setSize(objects.size());
        for (int i = 0; i < objects.size(); i++)
            model.setElementAt(objects.get(i), i);
        list.setModel(model);
        if (previewPanel != null)
            previewPanel.repaint();
    }

    void setSelectedIndex(int index) {
        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);
        if (previewPanel != null)
            previewPanel.repaint();
    }

    int getObjectCount() {
        return list.getModel().getSize();
    }

    void setHighlightColorsSelected(boolean selected) {
        useHighlightColorsCheckBox.setSelected(selected);
    }

    void setSelectionCount(int count) {
        selectionLabel.setText(count == 1 ? "1 object selected" : count + " objects selected");
    }

    void setClipboardCount(int count) {
        clipboardLabel.setText(count == 1 ? "1 object in clipboard" : count + " objects in clipboard");
    }

    void setStatusMessage(String message) {
        statusLabel.setText(message == null ? "" : message);
    }

    void showLayer(boolean hasLayer) {
        checkboxPanel.setVisible(hasLayer);
        previewPanel.setVisible(hasLayer);
        listPanel.setVisible(hasLayer);
        warningLabel.setVisible(!hasLayer);
        revalidate();
        repaint();
    }

    private void init() {
        setLayout(new SizingFlowLayout(FlowLayout.CENTER, 8, 8));

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new WPObjectListCellRenderer());
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && list.getSelectedIndex() != -1)
                objectSelectionChanged.accept(list.getSelectedIndex());
        });

        randomRotateCheckBox.setToolTipText("Randomly rotate the brush after each use");
        randomSelectCheckBox.setToolTipText("Randomly select new schematic after each use");
        randomMirroredCheckBox.setToolTipText("Randomly select new schematic after each use");
        randomRotateCheckBox.addActionListener(event -> notifyPlacementOptionsChanged());
        randomSelectCheckBox.addActionListener(event -> notifyPlacementOptionsChanged());
        randomMirroredCheckBox.addActionListener(event -> notifyPlacementOptionsChanged());

        useHighlightColorsCheckBox.setToolTipText("Use the layers color instead of painting the actual schematics");
        useHighlightColorsCheckBox
                .addActionListener(event -> highlightColorsChanged.accept(useHighlightColorsCheckBox.isSelected()));

        checkboxPanel = new JPanel(new BorderLayout());
        checkboxPanel.setPreferredSize(new java.awt.Dimension(200, 190));
        checkboxPanel.setMinimumSize(new java.awt.Dimension(200, 190));
        checkboxPanel.setMaximumSize(new java.awt.Dimension(200, 190));
        JButton helpButton = getHelpButton(HELP_TITLE, HELP_EXPLANATION, HELP_ITEMS);
        checkboxPanel.add(helpButton, BorderLayout.NORTH);
        JPanel checkboxGrid = new JPanel(new GridLayout(0, 1));
        checkboxGrid.add(randomRotateCheckBox);
        checkboxGrid.add(randomSelectCheckBox);
        checkboxGrid.add(randomMirroredCheckBox);
        checkboxGrid.add(useHighlightColorsCheckBox);
        checkboxPanel.add(checkboxGrid, BorderLayout.CENTER);
        JPanel statusGrid = new JPanel(new GridLayout(0, 1));
        statusGrid.add(selectionLabel);
        statusGrid.add(clipboardLabel);
        statusGrid.add(statusLabel);
        checkboxPanel.add(statusGrid, BorderLayout.SOUTH);

        previewPanel = getPreviewPanel();
        listPanel = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        listPanel.setPreferredSize(new java.awt.Dimension(200, 200));
        listPanel.setMinimumSize(new java.awt.Dimension(200, 200));
        listPanel.setMaximumSize(new java.awt.Dimension(200, 200));

        add(checkboxPanel);
        add(previewPanel);
        add(listPanel);
        add(warningLabel);
        showLayer(false);
    }

    private void notifyPlacementOptionsChanged() {
        placementOptionsChanged.accept(new CityEditToolOperation.PlacementOptions(randomRotateCheckBox.isSelected(),
                randomSelectCheckBox.isSelected(), randomMirroredCheckBox.isSelected()));
    }

    private JLabel getPreviewPanel() {
        JLabel preview = new JLabel() {
            @Override
            public void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                CityLayer layer = selectedLayerSupplier.get();
                ObjectState state = selectedStateSupplier.get();
                if (layer == null || state == null)
                    return;
                Image original = layer.getSchematicImage(state);
                if (original == null)
                    return;

                int availableWidth = Math.max(1, getWidth() - 8);
                int availableHeight = Math.max(1, getHeight() - 8);
                double scale = Math.min((double) availableWidth / original.getWidth(null),
                        (double) availableHeight / original.getHeight(null));
                int imageWidth = Math.max(1, (int) Math.round(original.getWidth(null) * scale));
                int imageHeight = Math.max(1, (int) Math.round(original.getHeight(null) * scale));
                int x = (getWidth() - imageWidth) / 2;
                int y = (getHeight() - imageHeight) / 2;
                graphics.drawImage(original, x, y, imageWidth, imageHeight, null);
            }

            @Override
            public java.awt.Dimension getPreferredSize() {
                return new java.awt.Dimension(200, 200);
            }
        };
        preview.setPreferredSize(new java.awt.Dimension(200, 200));
        preview.setMinimumSize(new java.awt.Dimension(200, 200));
        preview.setMaximumSize(new java.awt.Dimension(200, 200));
        return preview;
    }

    private static final class SizingFlowLayout extends FlowLayout
    {
        private static final int EXTRA_PREFERRED_WIDTH = 10;

        private SizingFlowLayout(int align, int horizontalGap, int verticalGap) {
            super(align, horizontalGap, verticalGap);
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            ArrayList<Component> components = visibleComponents(parent);
            Insets insets = parent.getInsets();
            if (components.isEmpty())
                return new Dimension(insets.left + insets.right, insets.top + insets.bottom);

            int naturalWidth = rowWidth(components, false);
            int availableWidth = parent.getWidth() - insets.left - insets.right;
            int widestMinimumWidth = components.stream()
                    .mapToInt(component -> component.getMinimumSize().width)
                    .max()
                    .orElse(0);
            int layoutWidth = availableWidth > widestMinimumWidth ? availableWidth : naturalWidth;

            int height = wrappedHeight(components, layoutWidth, false);
            return new Dimension(Math.max(200, layoutWidth + insets.left + insets.right),
                    height + insets.top + insets.bottom);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            ArrayList<Component> components = visibleComponents(parent);
            Insets insets = parent.getInsets();
            int width = components.stream().mapToInt(component -> component.getMinimumSize().width).max().orElse(0);
            int height = components.stream().mapToInt(component -> component.getMinimumSize().height).sum()
                    + getVgap() * Math.max(0, components.size() - 1);
            return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
        }

        private int wrappedHeight(ArrayList<Component> components, int availableWidth, boolean minimum) {
            int rows = 1;
            int rowWidth = 0;
            int rowHeight = 0;
            int height = 0;
            for (Component component : components) {
                Dimension size = componentSize(component, minimum);
                if (rowWidth > 0 && rowWidth + getHgap() + size.width > availableWidth) {
                    height += rowHeight;
                    rows++;
                    rowWidth = 0;
                    rowHeight = 0;
                }
                rowWidth += rowWidth == 0 ? size.width : getHgap() + size.width;
                rowHeight = Math.max(rowHeight, size.height);
            }
            return height + rowHeight + getVgap() * (rows + 1);
        }

        private int rowWidth(ArrayList<Component> components, boolean minimum) {
            int width = 0;
            for (Component component : components)
                width += componentSize(component, minimum).width;
            return width + getHgap() * Math.max(0, components.size() - 1);
        }

        private static ArrayList<Component> visibleComponents(Container parent) {
            ArrayList<Component> components = new ArrayList<>();
            for (Component component : parent.getComponents()) {
                if (component.isVisible())
                    components.add(component);
            }
            return components;
        }

        private static Dimension componentSize(Component component, boolean minimum) {
            return minimum ? component.getMinimumSize() : component.getPreferredSize();
        }
    }

}
