package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;
import org.ironsight.wpplugin.macromachine.operations.SaveableAction;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;

public class SaveableActionPickerDialog extends JDialog {
    public SaveableActionPickerDialog(ArrayList<SaveableAction> layerMappings, Consumer<SaveableAction> onSubmit,
                                      @Nullable SaveableAction newAction, Component parent) {
        super();
        init(layerMappings, onSubmit, newAction);
        this.setModal(true);
        this.toFront();
        this.setAlwaysOnTop(true);
        this.pack();
        if (parent != null) {
            Point parentLocation = parent.getLocationOnScreen();
            this.setLocation(parentLocation);
        }

    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setTitle("Select Layer Mapping");
        for (int i = 0; i < 20; i++)
            MappingActionContainer.addDefaultMappings(MappingActionContainer.getInstance());
        ArrayList<SaveableAction> layerMappings = new ArrayList<>(MappingActionContainer.getInstance().queryAll());
        Dialog dlg = new SaveableActionPickerDialog(layerMappings , System.out::println,
                MappingAction.getNewEmptyAction(), frame);
        dlg.setVisible(true);
    }

    private void init(ArrayList<SaveableAction> items, Consumer<SaveableAction> onSubmit,  @Nullable SaveableAction specialTopAction) {
        JList<SaveableAction> list = new JList<>();
        DefaultListModel<SaveableAction> listModel = new DefaultListModel<>();
        list.setModel(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new SaveableActionRenderer());



        items.sort(Comparator.comparing(o -> o.getName().toLowerCase()));
        if (specialTopAction != null)
            listModel.addElement(specialTopAction);
        for (SaveableAction mapping : items) {
            listModel.addElement(mapping);
        }
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            SaveableAction selected = list.getSelectedValue();
            onSubmit.accept(selected);
            this.dispose();
        });
        okButton.setEnabled(false);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            this.dispose();
        });
        list.addListSelectionListener(
                e -> {
                    okButton.setEnabled(list.getSelectedValue() != null);

                }
        );
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
