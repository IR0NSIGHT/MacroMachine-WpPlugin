package org.ironsight.wpplugin.macromachine.Gui.ItemPicker;

public class PickerFilterOption<T> {
    public String getDisplayName() {
        return displayName;
    }

    public String getTooltip() {
        return tooltip;
    }

    private final String displayName;
    private final String tooltip;

    public PickerFilterOption(String displayName, String tooltip) {
        this.displayName = displayName;
        this.tooltip = tooltip;
    }

    /**
     * test if this item matches the filter.
     * @param item
     * @return
     */
    public boolean block(T item) {
        return true;
    }

    public boolean isActive() {
        return active;
    }

    private boolean active;
    public void setActive(boolean active) {
        this.active = active;
    }
}
