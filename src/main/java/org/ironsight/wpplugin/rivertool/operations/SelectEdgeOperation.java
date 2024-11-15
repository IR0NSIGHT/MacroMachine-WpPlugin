package org.ironsight.wpplugin.rivertool.operations;

import org.ironsight.wpplugin.rivertool.Gui.OptionsLabel;
import org.ironsight.wpplugin.rivertool.pathing.RingFinder;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.*;

import static org.ironsight.wpplugin.rivertool.Gui.OptionsLabel.numericInput;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class SelectEdgeOperation extends MouseOrTabletOperation {
    static final int CYAN = 9;
    private static final String NAME = "Select Edge";
    private static final String DESCRIPTION = "Select the edge to select";
    private static final String ID = "select_edge_operation";
    private final SelectEdgeOptions options = new SelectEdgeOptions();
    Random r = new Random();

    public SelectEdgeOperation() {
        super(NAME, DESCRIPTION, ID);
    }

    @Override
    public JPanel getOptionsPanel() {
        // Create a new JPanel with a 2x2 grid layout
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));

        {   //SPINNER WIDTH
            OptionsLabel l = numericInput("width", "how wide the edge should be",
                    new SpinnerNumberModel(options.width, 1, 100, 1.), f -> options.width = f.intValue(), () -> {
                    });
            panel.add(l.getLabels()[0]);
        }

    /*    {   //SPINNER GRADIENT
            OptionsLabel l = numericInput("gradient", "how wide the gradient should be",
                    new SpinnerNumberModel(options.gradient, 1, 100, 1.), f -> options.gradient = f.intValue(), () -> {
                    });
            panel.add(l.getLabels()[0]);
        }
     */

        {   //EXECUTE BUTTON
            JButton button3 = new JButton("Run");
            // Add action listeners to handle button click events
            button3.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    run();
                }
            });

            panel.add(button3);
        }

        {
            // Create a JComboBox with options
            String[] listOptions = {"Out", "In", "Both", "Out and keep"};
            JComboBox<String> dropdown = new JComboBox<>(listOptions);

            // Add an action listener to handle option selection
            dropdown.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String selectedOption = (String) dropdown.getSelectedItem();
                    switch (selectedOption) {
                        case "Out":
                            options.dir = SelectEdgeOptions.DIRECTION.OUTWARD;
                            break;
                        case "In":
                            options.dir = SelectEdgeOptions.DIRECTION.INWARD;
                            break;
                        case "Both":
                            options.dir = SelectEdgeOptions.DIRECTION.BOTH;
                            break;
                        case "Out and keep":
                            options.dir = SelectEdgeOptions.DIRECTION.OUT_AND_KEEP;
                    }
                }
            });
            panel.add(dropdown);
        }

        return panel;
    }

    @Override
    protected void activate() throws PropertyVetoException {

    }

    private void run() {
        this.getDimension().setEventsInhibited(true);

        options.gradient = new Gradient(new float[]{0.1f, 0.25f, 0.5f, 1f}, new float[]{0.4f, 0.2f, 0.1f, 0.03f});

        int annotationMatch = CYAN;

        LinkedList<Point> matches = new LinkedList<>();

        Iterator<? extends Tile> t = getDimension().getTiles().iterator();
        while (t.hasNext()) {
            Tile tile = t.next();
            if (tile.hasLayer(Annotations.INSTANCE)) {
                for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                    for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                        final int x = xInTile + (tile.getX() << TILE_SIZE_BITS), y =
                                yInTile + (tile.getY() << TILE_SIZE_BITS);
                        int annotation = tile.getLayerValue(Annotations.INSTANCE, xInTile, yInTile);
                        if (annotation == annotationMatch) {
                            matches.add(new Point(x, y));
                        }
                    }
                }
                tile.clearLayerData(Annotations.INSTANCE);
            }
        }

        HashMap<Point, Float> edge = new HashMap<>(matches.size());
        for (Point p : matches) {
            edge.put(p, 1f);
        }
        int amountRings = options.width;

        RingFinder start = new RingFinder(edge, 3);

        Set<Point> restrictions = new HashSet<>();
        switch (options.dir) {
            case BOTH:
                //no restrictions
                edge = start.ring(1);
                break;
            case OUTWARD:
                edge = start.ring(1);
                restrictions = start.ring(0).keySet();
                break;
            case INWARD:
                edge = start.ring(1);
                restrictions = start.ring(2).keySet();
                break;
            case OUT_AND_KEEP:
                //edge stays the same
                //no restrictions
                break;
        }
        start = new RingFinder(edge, amountRings, restrictions);

        int totalWidth = options.width;
        for (int w = 0; w < amountRings; w++) {
            float chance = options.gradient.getValue((float) w / totalWidth);
            applyWithStrength(start.ring(w).keySet(), chance);
        }

        this.getDimension().setEventsInhibited(false);
    }

    private void applyWithStrength(Collection<Point> points, float strength) {
        for (Point p : points) {
            if (strength > r.nextFloat()) {
                getDimension().setLayerValueAt(Annotations.INSTANCE, p.x, p.y, CYAN);
            }
        }
    }

    @Override
    protected void deactivate() {

    }

    @Override
    protected void tick(int i, int i1, boolean b, boolean b1, float v) {


    }

    private static class SelectEdgeOptions {
        int width = 3;
        DIRECTION dir = DIRECTION.OUTWARD;

        Gradient gradient = new Gradient(new float[0], new float[0]);


        enum DIRECTION {
            OUTWARD, INWARD, BOTH, OUT_AND_KEEP
        }
    }
}
