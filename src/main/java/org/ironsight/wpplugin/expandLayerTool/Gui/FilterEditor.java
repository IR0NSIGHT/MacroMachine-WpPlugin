package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.MacroSelectionLayer;
import org.ironsight.wpplugin.expandLayerTool.operations.TileFilter;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.plugins.WPPluginManager;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class FilterEditor extends javax.swing.JPanel {

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("FilterEditor");
        frame.getContentPane().add(new FilterEditor());
        frame.pack();
        frame.setVisible(true);
    }

    private List<Layer> getLayers() {
        if (WPPluginManager.getInstance() == null) {
            return Arrays.asList(PineForest.INSTANCE, DeciduousForest.INSTANCE, MacroSelectionLayer.INSTANCE);
        }
        else
            return LayerManager.getInstance().getLayers();
    }

    public FilterEditor() {
        this.setLayout(new BorderLayout());

        JPanel settings = new JPanel();

        JPanel height = new JPanel();
        height.add(new JLabel("Height:"));
        height.add(new JComboBox<>(new String[]{"Ignore","Only On","Except On"}));
        height.add(new JSpinner(new SpinnerNumberModel(-10, -64, 365, 1)));
        height.add(new JSpinner(new SpinnerNumberModel(200, -64, 365, 1)));
        settings.add(height);

        JPanel layers = new JPanel();
        layers.add(new JLabel("Layers:"));
        layers.add(new JComboBox<>(new String[]{"Ignore","Only On","Except On"}));
        for (Layer l: getLayers()) {
            layers.add(new JCheckBox(l.getName()));
        }
        settings.add(layers);

        this.add(settings, BorderLayout.CENTER);

    }

    private TileFilter tileFilter;

    public TileFilter getFilter() {
        return tileFilter;
    }

    public void setFilter(TileFilter filter) {

    }

}
