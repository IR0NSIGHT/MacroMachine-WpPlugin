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

class OptionsPanel extends JPanel implements Scrollable
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

    private final JList<WPObject> list = new JList<>();
    private final JLabel warningLabel = new JLabel("Please select a city layer");
    private JPanel checkboxPanel;
    private final JCheckBox randomMirroredCheckBox = new JCheckBox("random mirrored");
    private final JCheckBox randomSelectCheckBox = new JCheckBox("random select");
    private final JCheckBox randomRotateCheckBox = new JCheckBox("random rotate");
    private final JCheckBox useHighlightColorsCheckBox = new JCheckBox("use highlight colors");
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
        if (previewPanel != null)
            previewPanel.repaint();
    }

    int getObjectCount() {
        return list.getModel().getSize();
    }

    void setHighlightColorsSelected(boolean selected) {
        useHighlightColorsCheckBox.setSelected(selected);
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
        setLayout(new ResponsiveFlowLayout(FlowLayout.CENTER, 8, 8));

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
        checkboxPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
        checkboxPanel.setPreferredSize(new java.awt.Dimension(200, 150));
        checkboxPanel.setMinimumSize(new java.awt.Dimension(200, 150));
        checkboxPanel.setMaximumSize(new java.awt.Dimension(200, 150));
        checkboxPanel.add(getHelpButton(HELP_TITLE, HELP_TEXT), BorderLayout.NORTH);
        JPanel checkboxGrid = new JPanel(new GridLayout(0, 1));
        checkboxGrid.add(randomRotateCheckBox);
        checkboxGrid.add(randomSelectCheckBox);
        checkboxGrid.add(randomMirroredCheckBox);
        checkboxGrid.add(useHighlightColorsCheckBox);
        checkboxPanel.add(checkboxGrid, BorderLayout.CENTER);

        previewPanel = getPreviewPanel();
        previewPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
        listPanel = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        listPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
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

    @Override
    public java.awt.Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRectangle, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRectangle, int orientation, int direction) {
        return Math.max(visibleRectangle.height - 16, 16);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    private static final class ResponsiveFlowLayout extends FlowLayout
    {
        private ResponsiveFlowLayout(int align, int horizontalGap, int verticalGap) {
            super(align, horizontalGap, verticalGap);
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return calculateLayoutSize(parent, false);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            Insets insets = parent.getInsets();
            ArrayList<Component> components = visibleComponents(parent);
            int width = components.stream().mapToInt(component -> component.getMinimumSize().width).max().orElse(0);
            int height = components.stream().mapToInt(component -> component.getMinimumSize().height).sum()
                    + getVgap() * Math.max(0, components.size() - 1);
            return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
        }

        private Dimension calculateLayoutSize(Container parent, boolean minimum) {
            Insets insets = parent.getInsets();
            ArrayList<Component> components = visibleComponents(parent);
            if (components.isEmpty())
                return new Dimension(insets.left + insets.right, insets.top + insets.bottom);

            int availableWidth = parent.getWidth() - insets.left - insets.right;
            if (availableWidth <= 0)
                availableWidth = rowWidth(components, minimum);

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
            height += rowHeight + getVgap() * (rows - 1);

            int width = parent.getWidth() > 0 ? availableWidth : rowWidth(components, minimum);
            return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
        }

        private static ArrayList<Component> visibleComponents(Container parent) {
            ArrayList<Component> components = new ArrayList<>();
            for (Component component : parent.getComponents()) {
                if (component.isVisible())
                    components.add(component);
            }
            return components;
        }

        private int rowWidth(ArrayList<Component> components, boolean minimum) {
            int width = 0;
            for (Component component : components)
                width += componentSize(component, minimum).width;
            return width + getHgap() * Math.max(0, components.size() - 1);
        }

        private static Dimension componentSize(Component component, boolean minimum) {
            return minimum ? component.getMinimumSize() : component.getPreferredSize();
        }
    }

}
