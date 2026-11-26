package org.ironsight.wpplugin.macromachine.Gui;

import java.awt.event.KeyEvent;
import java.util.function.BooleanSupplier;

/**
 * Double-tapping Ctrl switches a map tool's add mode on and off: the standard
 * across the plugin's map tools (Path Tool, City Tool). A tap is a press and
 * release of Ctrl alone within {@link #MAX_HOLD_MS}; two taps within
 * {@link #DOUBLE_TAP_MS} make a double tap. Any other key or a mouse press in
 * between ({@link #interrupt()}) cancels it, so Ctrl shortcuts and Ctrl+click
 * never toggle the mode.
 */
public final class CtrlDoubleTap
{
    public static final long MAX_HOLD_MS = 300, DOUBLE_TAP_MS = 400;

    private final BooleanSupplier allowed;
    private final Runnable action;
    private boolean held;
    private boolean interrupted;
    private long pressedAt;
    private long lastTapAt;

    /**
     * @param allowed
     *            checked when the second tap ends (e.g. cursor over the map, no
     *            drag running)
     * @param action
     *            what a double tap does (e.g. toggle add mode)
     */
    public CtrlDoubleTap(BooleanSupplier allowed, Runnable action) {
        this.allowed = allowed;
        this.action = action;
    }

    /** Feed every key event the tool sees (pressed and released). */
    public void onKey(KeyEvent e) {
        boolean ctrl = e.getKeyCode() == KeyEvent.VK_CONTROL;
        long now = e.getWhen() != 0 ? e.getWhen() : System.currentTimeMillis();
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (!ctrl) {
                interrupt();
                return;
            }
            if (held)
                return; // auto-repeat
            held = true;
            interrupted = false;
            pressedAt = now;
        } else if (e.getID() == KeyEvent.KEY_RELEASED && ctrl) {
            held = false;
            boolean tap = !interrupted && now - pressedAt <= MAX_HOLD_MS;
            if (tap && lastTapAt != 0 && now - lastTapAt <= DOUBLE_TAP_MS && allowed.getAsBoolean()) {
                lastTapAt = 0;
                action.run();
            } else
                lastTapAt = tap ? now : 0;
        }
    }

    /** Something else happened (a mouse press, another key): no double tap. */
    public void interrupt() {
        interrupted = true;
        lastTapAt = 0;
    }
}
