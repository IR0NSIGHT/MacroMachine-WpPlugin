package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.LayerMapping;
import org.ironsight.wpplugin.macromachine.operations.UniqueList;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IDisplayUnit;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class MappingValueTable extends JPanel {
    private JTable table;
    private final ArrayList<LayerMapping> mappings = new ArrayList<>();

    public MappingValueTable() {

        initComponents();
    }

    public void setMappings(ArrayList<LayerMapping> mappings) {
        this.mappings.clear();
        this.mappings.addAll(mappings);
        updateComponents();
    }

    protected void updateComponents() {
        UniqueList<IPositionValueGetter> inputs = new UniqueList<>();
        UniqueList<IPositionValueSetter> outputs = new UniqueList<>();
        for (LayerMapping action : mappings) {
            inputs.add(action.input);
            outputs.add(action.output);
        }

        table.setModel(new DefaultTableModel());
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setColumnCount(inputs.getList().size() + outputs.getList().size());

        Object[] header = new Object[inputs.getList().size() + outputs.getList().size()];
        int i = 0;
        String[] inputNames = inputs.getList().stream().map(IDisplayUnit::getName).toArray(String[]::new);
        String[] outputNames = outputs.getList().stream().map(IDisplayUnit::getName).toArray(String[]::new);
        for (String inputName : inputNames)
            header[i++] = inputName;
        for (String outputName : outputNames)
            header[i++] = outputName;
        model.setColumnIdentifiers(header);

        int rowCount = 1;
        for (IPositionValueGetter input : inputs.getList()) {
            rowCount = rowCount * IMappingValue.range(input);
        }
       // model.setRowCount(rowCount);
    }

    protected void initComponents() {
        VirtualTableModel model = new VirtualTableModel();

        // Create a JTable with the custom model
        JTable table = new JTable(model);

        table.setCellEditor(new MappingPointCellEditor());
        table.setDefaultRenderer(MappingPointValue.class, new MappingPointCellRenderer());
        table.setRowHeight(new MappingPointCellRenderer().getPreferredHeight());
        table.getTableHeader().setReorderingAllowed(true);


        JScrollPane scrollPane = new JScrollPane(table);

        this.setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);
    }
}
