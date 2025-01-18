package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.Gui.GradientDisplay;
import org.ironsight.wpplugin.expandLayerTool.Gui.GradientEditor;
import org.ironsight.wpplugin.expandLayerTool.pathing.RingFinder;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;
import org.pepsoft.worldpainter.selection.SelectionBlock;
import org.pepsoft.worldpainter.selection.SelectionChunk;

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
    private static final String DESCRIPTION = "Select the edge of all blocks of th laye and expand/reduce them " +
            "with a spraypaint gradient, then paint it on the map as output layer.";
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

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        main.add(panel);
        {   //SPINNER WIDTH
            {
                JLabel label = new JLabel("width");
                panel.add(label);
                // Create a SpinnerNumberModel for numeric input
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
                spinner.setToolTipText("how wide the output edge should be");
                panel.add(spinner);
            }
        }

        {
            // Create a JComboBox with options
            String[] listOptions = {"Outwards", "Inwards", "Both", "Out and keep"};
            JComboBox<String> dropdown = new JComboBox<>(listOptions);

// Map the list options to the corresponding directions
            Map<String, SelectEdgeOptions.DIRECTION> directionMap = new HashMap<>();
            directionMap.put("Outwards", SelectEdgeOptions.DIRECTION.OUTWARD);
            directionMap.put("Inwards", SelectEdgeOptions.DIRECTION.INWARD);
            directionMap.put("Both", SelectEdgeOptions.DIRECTION.BOTH);
            directionMap.put("Out and keep", SelectEdgeOptions.DIRECTION.OUT_AND_KEEP);

// Reverse map to find the key by value
            Map<SelectEdgeOptions.DIRECTION, String> reverseMap = new HashMap<>();
            directionMap.forEach((key, value) -> reverseMap.put(value, key));

// Add an action listener to handle option selection
            dropdown.addActionListener(e -> {
                String selectedOption = (String) dropdown.getSelectedItem();
                if (selectedOption != null) {
                    options.dir = directionMap.get(selectedOption);
                }
            });

// Set the selected item based on the current direction
            dropdown.setSelectedItem(reverseMap.get(options.dir));

            panel.add(new JLabel("direction"));
            dropdown.setToolTipText("in which direction the tool will grow the input layer");
            panel.add(dropdown);
        }


        {   // INPUT
            JButton button = new JButton();
            button.setText(options.inputSelection ? "selection" : "cyan annotation");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    options.inputSelection = !options.inputSelection;
                    button.setText(options.inputSelection ? "selection" : "cyan annotation");
                }
            });
            button.setToolTipText("the layer to be used as an input");
            panel.add(new JLabel("input"));
            panel.add(button);
        }

        {   // OUTPUT
            JButton button = new JButton();
            button.setText(options.asSelection ? "selection" : "cyan annotation");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    options.asSelection = !options.asSelection;
                    button.setText(options.asSelection ? "selection" : "cyan annotation");

                }
            });
            panel.add(new JLabel("output"));
            button.setToolTipText("the layer to be used as output. Will be painted on the map when the tool is run.");
            panel.add(button);
        }

        {
            JButton button3 = new JButton("edit");
            // Add action listeners to handle button click events
            button3.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showArrayEditorDialog();
                }
            });
            panel.add(new JLabel("gradient"));
            button3.setToolTipText("the gradient that is used when the layer is expanded.");
            panel.add(button3);
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
            button3.setToolTipText("execute the tool operation and place down the expanded output layer");
            panel.add(button3);
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

        JPanel gridPanel = new JPanel(new GridLayout(0, 2));
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
            if ((options.inputSelection && tile.hasLayer(SelectionBlock.INSTANCE) || tile.hasLayer(SelectionChunk.INSTANCE)) || tile.hasLayer(Annotations.INSTANCE)) {
                for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                    for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                        final int x = xInTile + (tile.getX() << TILE_SIZE_BITS), y =
                                yInTile + (tile.getY() << TILE_SIZE_BITS);
                        if (options.inputSelection) {
                            if (tile.getBitLayerValue(SelectionBlock.INSTANCE, xInTile, yInTile) || getDimension().getBitLayerValueAt(SelectionChunk.INSTANCE, x, y))
                                matches.add(new Point(x, y));
                        } else {
                            int annotation = tile.getLayerValue(Annotations.INSTANCE, xInTile, yInTile);
                            if (annotation == annotationMatch) {
                                matches.add(new Point(x, y));
                            }
                        }

                    }
                }
                if (!options.asSelection) tile.clearLayerData(Annotations.INSTANCE);
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
                if (options.asSelection) getDimension().setBitLayerValueAt(SelectionBlock.INSTANCE, p.x, p.y, true);
                else getDimension().setLayerValueAt(Annotations.INSTANCE, p.x, p.y, CYAN);
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
        boolean inputSelection = false;
        Gradient gradient = new Gradient(new float[]{0.01f, 0.15f, 0.25f, 0.5f, 1f}, new float[]{1f, 0.4f, 0.2f, 0.1f
                , 0.03f});

        enum DIRECTION {
            OUTWARD, INWARD, BOTH, OUT_AND_KEEP
        }
    }
}
