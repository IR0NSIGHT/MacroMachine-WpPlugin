package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JPanel;

public class CellPreviewPanel extends JPanel
{
    public static final int CELL_SIZE = 6;
    public static final int HEIGHT = 48;
    /** Dark checker cells, the drawn ones lavender (the dialogs' look). */
    static final Color EMPTY = new Color(0x16, 0x16, 0x16), EMPTY_ALTERNATE = new Color(0x1C, 0x1C, 0x1C),
            DRAWN = new Color(205, 199, 255), DRAWN_ALTERNATE = new Color(185, 178, 245);
    private final CellDrawer cellDrawer;

    public CellPreviewPanel(CellDrawer cellDrawer) {
        this.cellDrawer = cellDrawer;
        setPreferredSize(new Dimension(0, HEIGHT));
        setMinimumSize(new Dimension(0, HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
    }

    public int getCellColumns() {
        return getWidth() / CELL_SIZE;
    }

    public int getCellRows() {
        return getHeight() / CELL_SIZE;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int columns = getCellColumns();
        int rows = getCellRows();
        g.setColor(EMPTY);
        g.fillRect(0, 0, getWidth(), getHeight());

        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                boolean alternate = (x + y) % 2 == 1;
                g.setColor(cellDrawer.drawCell(x, y)
                        ? (alternate ? DRAWN_ALTERNATE : DRAWN)
                        : (alternate ? EMPTY_ALTERNATE : EMPTY));
                g.fillRect(x * CELL_SIZE, getHeight() - (y + 1) * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
    }

    @FunctionalInterface
    public interface CellDrawer
    {
        boolean drawCell(int x, int y);
    }
}
