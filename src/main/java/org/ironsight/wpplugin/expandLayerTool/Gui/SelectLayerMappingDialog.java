package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;
import org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

public class SelectLayerMappingDialog extends JDialog {
    public SelectLayerMappingDialog(Frame owner, LayerMapping[] layerMappings, Consumer<LayerMapping> onSubmit) {
        super(owner);
        setTitle("Select Layer Mapping");
        init(layerMappings, onSubmit);
        setModal(true);
        setLocationRelativeTo(owner);  // Position the dialog relative to the owner
        this.pack();
        this.setVisible(true);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setTitle("Select Layer Mapping");
        for (int i = 0; i < 20; i++)
            LayerMappingContainer.addDefaultMappings(LayerMappingContainer.INSTANCE);
        new SelectLayerMappingDialog(frame, LayerMappingContainer.INSTANCE.queryMappingsAll(), System.out::println);
    }

    private void init(LayerMapping[] layerMappings, Consumer<LayerMapping> onSubmit) {
        JList<LayerMapping> list = new JList<>();
        DefaultListModel<LayerMapping> listModel = new DefaultListModel<>();
        list.setModel(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new MappingTableCellRenderer());

        Arrays.sort(layerMappings, new Comparator<LayerMapping>() {

            @Override
            public int compare(LayerMapping o1, LayerMapping o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        for (LayerMapping mapping : layerMappings) {
            listModel.addElement(mapping);
        }
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            onSubmit.accept(list.getSelectedValue());
            this.dispose();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            this.dispose();
        });

        JPanel panel = new JPanel(new BorderLayout());
        JScrollPane pane = new JScrollPane(list);
        panel.add(pane, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        getContentPane().add(panel, BorderLayout.CENTER);
    }


}
