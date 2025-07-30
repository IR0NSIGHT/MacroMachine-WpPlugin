package org.ironsight.wpplugin.macromachine.Gui.ItemPicker;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IDisplayUnit;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DisplayValueRowFilter extends RowFilter<TableModel, Integer> {

    private Pattern pattern= Pattern.compile("(?i)" + Pattern.quote(""));
    private PickerFilterOption[] filters = new PickerFilterOption[0];
    public DisplayValueRowFilter() {

    }
    public void setFilters( PickerFilterOption... filters) {
        this.filters = filters;
    }
    public void setString(String searchString) {
        this.pattern = Pattern.compile("(?i)" + Pattern.quote(searchString));
    }

    @Override
    public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
        TableModel model = entry.getModel();
        for (int i = 0; i < model.getColumnCount(); i++) {
            Object value = model.getValueAt(entry.getIdentifier(), i);
            String text = "";

            if (value instanceof IDisplayUnit) {
                text = ((IDisplayUnit) value).getName();
            } else {
                text = value.toString();
            }

            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                for (PickerFilterOption filter: filters) {
                    if (filter.block(value)) { //filter out anything the filters match on
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
