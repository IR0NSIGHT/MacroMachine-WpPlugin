package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.Gui.GradientDisplay;
import org.ironsight.wpplugin.expandLayerTool.Gui.GradientEditor;
import org.ironsight.wpplugin.expandLayerTool.pathing.RingFinder;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;
import org.pepsoft.worldpainter.selection.SelectionBlock;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.*;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class SelectEdgeOperation extends MouseOrTabletOperation {
    static final int CYAN = 9;
    private static final String NAME = "Select Edge Operation";
    private static final String DESCRIPTION = "Select the edge of all cyan annotated blocks and expand/reduce them " +
            "with a spraypaint gradient.";
    private static final String ID = "select_edge_operation";
    private final SelectEdgeOptions options = new SelectEdgeOptions();
    Random r = new Random();

    public SelectEdgeOperation() {
        super(NAME, DESCRIPTION, ID);
    }

    @Override
    public JPanel getOptionsPanel() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.add(new JLabel(NAME));

        JTextArea textArea = new JTextArea(DESCRIPTION);
        // Enable word wrapping
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);

        // Make the text area non-editable (optional)
        textArea.setEditable(false);
        main.add(textArea);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        main.add(panel);
        {   //SPINNER WIDTH
            JPanel input = new JPanel();
            input.setLayout(new BoxLayout(input, BoxLayout.X_AXIS));
            {
                JLabel label = new JLabel("width");
                label.setToolTipText("how wide the edge should be");
                input.add(label);
            }
            { // Create a SpinnerNumberModel for numeric input
                SpinnerNumberModel model = new SpinnerNumberModel(options.width, 1, 100, 1f); // initialValue, min,
                // max, step
                JSpinner spinner = new JSpinner(model);
                // Add a change listener to capture value changes
                spinner.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        // Retrieve the current value (im to lazy to figure out when its a double or a float)
                        options.width = ((Double) spinner.getValue()).intValue();
                    }
                });
                input.add(spinner);
            }

            panel.add(input);
        }

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
            JButton button3 = new JButton("Edit gradient");
            // Add action listeners to handle button click events
            button3.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showArrayEditorDialog();
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

        {
            JButton button = new JButton();
            button.setText(options.asSelection ? "apply as annotation" : "apply as selection");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    options.asSelection = !options.asSelection;
                    button.setText(options.asSelection ? "apply as annotation" : "apply as selection");

                }
            });
            panel.add(button);
        }
        return main;
    }

    public void showArrayEditorDialog() {
        // Create the dialog
        JDialog dialog = new JDialog((Frame) null, "Edit Arrays", true); // Modal dialog
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));

        // Create the PixelGrid instance with the gradient
        GradientDisplay pixelGrid = new GradientDisplay(options.gradient);
        // Create the JFrame to render the PixelGrid
        JFrame frame = new JFrame("Pixel Grid with Gradient");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(500, 500));

        frame.setLayout(new BorderLayout());

        JPanel gridPanel = new JPanel(new GridLayout(1, 2));
        gridPanel.add(pixelGrid);
        gridPanel.add(new GradientEditor(options.gradient, pixelGrid::setGradient, grad -> {
            this.options.gradient = grad;
            dialog.dispose();
        }));

        // Add components to the dialog
        dialog.add(gridPanel, BorderLayout.NORTH);

        // Set dialog size and make it visible
        dialog.pack();
        dialog.setLocationRelativeTo(null); // Center on screen
        dialog.setVisible(true);
    }


    @Override
    protected void activate() throws PropertyVetoException {

    }

    private void run() {
        this.getDimension().setEventsInhibited(true);

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
                if (!options.asSelection)
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
                edge = start.ring(1);   //initial first outer layer
                restrictions = start.ring(2).keySet(); //initial second outwards layer

                //walk inwards once
                start = new RingFinder(edge, 1, restrictions);
                restrictions = edge.keySet();
                edge = start.ring(1);   //first inwards layer

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
                if (options.asSelection)
                    getDimension().setBitLayerValueAt(SelectionBlock.INSTANCE, p.x, p.y, true);
                else
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
        boolean asSelection = true;
        Gradient gradient = new Gradient(
                new float[]{0.01f, 0.15f, 0.25f, 0.5f, 1f},
                new float[]{1f, 0.4f, 0.2f, 0.1f, 0.03f});

        enum DIRECTION {
            OUTWARD, INWARD, BOTH, OUT_AND_KEEP
        }
    }
}
