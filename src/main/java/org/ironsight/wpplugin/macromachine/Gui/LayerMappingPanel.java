package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Consumer;

public abstract class LayerMappingPanel extends JPanel {
    protected MappingAction mapping;
    private boolean allowEvents = true;
    private Consumer<MappingAction> onUpdate = f -> {
    };

    public LayerMappingPanel() {
        allowEvents = false;
        initComponents();
        allowEvents = true;
    }

    public boolean isAllowEvents() {
        return allowEvents;
    }

    protected abstract void updateComponents();

    protected abstract void initComponents();

    /**
     * internal way to signalize "i changed the mapping, do the events" will set mapping, call onUpdate and trigger
     * internal update()
     *
     * @param mapping
     */
    protected final void updateMapping(MappingAction mapping) {
        System.out.println("attempt update mapping to points: " + Arrays.toString(mapping.getMappingPoints()));
        if (mapping == null || this.mapping == null || this.mapping.equals(mapping)) {
            System.out.println("skip update: mapping didnt change");
            return;
        }
        if (!allowEvents) {
            System.out.println("block update");
            return;
        }
        if (mapping.input != this.mapping.input || mapping.output != this.mapping.output) {
            if (mapping.input.isDiscrete()) {
                //ensure all values have mapping points.
                HashMap<Integer, MappingPoint> inputToMapping = new HashMap<>();
                for (MappingPoint mappingPoint : mapping.getMappingPoints()) {
                    inputToMapping.put(mappingPoint.input, mappingPoint);
                }
                MappingPoint[] newPoints =
                        new MappingPoint[mapping.input.getMaxValue() - mapping.input.getMinValue() + 1];
                for (int i = mapping.input.getMinValue(); i <= mapping.input.getMaxValue(); i++) {
                    newPoints[i - mapping.input.getMinValue()] =
                            inputToMapping.getOrDefault(i, new MappingPoint(i, mapping.map(i)));
                }
                mapping = mapping.withNewPoints(newPoints);
            } else if (mapping.getMappingPoints().length == 0) { //interpol input, discrete output
                //input or output changed, wipe control points
                mapping =
                        mapping.withNewPoints(new MappingPoint[]{new MappingPoint(
                                (mapping.input.getMinValue() + mapping.input.getMaxValue()) / 2,
                                (mapping.output.getMinValue() + mapping.output.getMaxValue()) / 2)});
            }
        }


        setMapping(mapping);
        if (onUpdate != null) onUpdate.accept(mapping);
    }

    public final void setMapping(MappingAction mapping) {
        assert mapping != null;
        assert mapping.getMappingPoints() != null;
        assert mapping.input != null;
        assert mapping.output != null;
        if (this.mapping != null && this.mapping.equals(mapping)) {
            return;
        }
        allowEvents = false;
        this.mapping = mapping;
        updateComponents();
        this.revalidate();
        this.repaint();
        allowEvents = true;
    }

    public final void setOnUpdate(Consumer<MappingAction> onUpdate) {
        this.onUpdate = onUpdate;
    }
}
