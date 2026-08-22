package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import static org.ironsight.wpplugin.macromachine.Gui.HelpDialog.getHelpButton;

import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.*;

import org.pepsoft.worldpainter.layers.bo2.WPObjectListCellRenderer;
import org.pepsoft.worldpainter.objects.WPObject;

class OptionsPanel extends JPanel
{
    private static final String HELP_TITLE = "City Editor";
    private static final String HELP_TEXT = """
            this tool is for editing City Layers, a new special type of Custom Object Layer.
            1. Create or import a city layer (make sure your schematic offsets are centered and not 0,0,0)
            2. select the city layer
            3. select the city editor tool
            4. select a custom brush (the one with the little arrow showing the rotation)
            - Left click to place a building
            - Right click to delete all buildings inside the brush area

            - CTRL + left click to select a building type on the map
            - CTRL + right click to move last placed building to new position

            - SHIFT + mousewheel to scroll the building type list
            - ALT + mousewheel to rotate brush

            - X key : mirror last selected building on map
            - C key : rotate last selected building on map
            - AWSD key : move last selected building on map

            Warning: This layer is NOT compatible with undo/redo. Do NOT use undo/redo with this layer.

            """;

    private final JPanel contentPanel = new JPanel();
    private final JList<WPObject> list = new JList<>();
    private final JLabel warningLabel = new JLabel("Please select a city layer");
    private final JCheckBox randomMirroredCheckBox = new JCheckBox("random mirrored");
    private final JCheckBox randomSelectCheckBox = new JCheckBox("random select");
    private final JCheckBox randomRotateCheckBox = new JCheckBox("random rotate");
    private final JCheckBox useHighlightColorsCheckBox = new JCheckBox("use highlight colors");
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
    }

    void setSelectedIndex(int index) {
        list.setSelectedIndex(index);
    }

    int getObjectCount() {
        return list.getModel().getSize();
    }

    void setHighlightColorsSelected(boolean selected) {
        useHighlightColorsCheckBox.setSelected(selected);
    }

    void showLayer(boolean hasLayer) {
        warningLabel.setVisible(!hasLayer);
        contentPanel.setVisible(hasLayer);
        revalidate();
        repaint();
    }

    private void init() {
        setLayout(new BorderLayout());
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        add(contentPanel, BorderLayout.CENTER);
        add(warningLabel, BorderLayout.SOUTH);

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

        contentPanel.add(getHelpButton(HELP_TITLE, HELP_TEXT));
        contentPanel.add(randomRotateCheckBox);
        contentPanel.add(randomSelectCheckBox);
        contentPanel.add(randomMirroredCheckBox);
        contentPanel.add(useHighlightColorsCheckBox);
        contentPanel.add(getPreviewPanel());
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setMaximumSize(new java.awt.Dimension(1000, 300));
        contentPanel.add(scrollPane);
    }

    private void notifyPlacementOptionsChanged() {
        placementOptionsChanged.accept(new CityEditToolOperation.PlacementOptions(randomRotateCheckBox.isSelected(),
                randomSelectCheckBox.isSelected(), randomMirroredCheckBox.isSelected()));
    }

    private JLabel getPreviewPanel() {
        return new JLabel() {
            private int width = 100;

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
                int scale = Math.max(100, getHeight()) / original.getHeight(null);
                Image image = original.getScaledInstance(original.getWidth(null) * scale,
                        original.getHeight(null) * scale, Image.SCALE_REPLICATE);
                width = image.getWidth(null);
                graphics.drawImage(image, 0, 0, null);
            }

            @Override
            public java.awt.Dimension getPreferredSize() {
                return new java.awt.Dimension(width, Math.max(100, getHeight()));
            }
        };
    }
}
