package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.awt.event.KeyEvent;
import java.util.Locale;

/**
 * Blender-style "type a number" while a transform (handle drag) runs. Mirrors
 * Blender's simple numeric input: digits and a decimal point are inserted at a
 * text cursor, {@code -} toggles negation, {@code /} toggles the inverse,
 * Backspace / Delete edit at the cursor (with Ctrl: clear), Left / Right / Home
 * / End move the cursor. While edited, the typed value replaces the
 * mouse-driven value; with empty text the mouse drives the value again
 * (negation / inverse still apply). Expression mode ({@code =} / {@code *} in
 * Blender) is not supported.
 */
final class NumericInput
{
    private final StringBuilder text = new StringBuilder();
    private int cursor;
    private boolean negate;
    private boolean inverse;

    /** Whether a key typed during a transform starts numeric input. */
    static boolean starts(int keyCode, char keyChar) {
        return Character.isDigit(keyChar) || keyChar == '.' || keyChar == ',' || keyChar == '-' || keyChar == '/'
                || keyCode == KeyEvent.VK_DECIMAL || keyCode == KeyEvent.VK_SUBTRACT || keyCode == KeyEvent.VK_DIVIDE;
    }

    /**
     * Whether a key starts typing a value from the keyboard (no drag running):
     * digits and the decimal point only. '-' and '/' work once typing, but don't
     * start it, so WorldPainter's own shortcuts on them (e.g. '-' to zoom out) keep
     * working.
     */
    static boolean startsTyping(int keyCode, char keyChar) {
        return Character.isDigit(keyChar) || keyChar == '.' || keyChar == ',' || keyCode == KeyEvent.VK_DECIMAL;
    }

    /** @return whether the key belongs to numeric input */
    boolean handleKey(int keyCode, char keyChar, boolean control) {
        switch (keyCode) {
            case KeyEvent.VK_BACK_SPACE -> {
                if (control) {
                    text.delete(0, cursor);
                    cursor = 0;
                } else if (cursor > 0)
                    text.deleteCharAt(--cursor);
                return true;
            }
            case KeyEvent.VK_DELETE -> {
                if (control)
                    text.setLength(cursor);
                else if (cursor < text.length())
                    text.deleteCharAt(cursor);
                return true;
            }
            case KeyEvent.VK_LEFT, KeyEvent.VK_KP_LEFT -> {
                cursor = Math.max(0, cursor - 1);
                return true;
            }
            case KeyEvent.VK_RIGHT, KeyEvent.VK_KP_RIGHT -> {
                cursor = Math.min(text.length(), cursor + 1);
                return true;
            }
            case KeyEvent.VK_HOME -> {
                cursor = 0;
                return true;
            }
            case KeyEvent.VK_END -> {
                cursor = text.length();
                return true;
            }
            default -> {
            }
        }
        if (Character.isDigit(keyChar))
            text.insert(cursor++, keyChar);
        else if (keyChar == '.' || keyChar == ',' || keyCode == KeyEvent.VK_DECIMAL) {
            // numpad decimal yields ',' on some layouts; like Blender it means '.'
            if (text.indexOf(".") < 0)
                text.insert(cursor++, '.');
        } else if (keyChar == '-' || keyCode == KeyEvent.VK_SUBTRACT)
            negate = !negate;
        else if (keyChar == '/' || keyCode == KeyEvent.VK_DIVIDE)
            inverse = !inverse;
        else
            return false;
        return true;
    }

    /**
     * Whether anything was entered: the value then no longer simply follows the
     * mouse.
     */
    boolean edited() {
        return text.length() > 0 || negate || inverse;
    }

    /** The typed number, or {@code null} while the text is empty or just ".". */
    Double typed() {
        String value = text.toString();
        if (value.isEmpty() || value.equals("."))
            return null;
        return Double.parseDouble(value.startsWith(".") ? "0" + value : value);
    }

    /**
     * Typed number (or the mouse value if none typed) with inverse and negation
     * applied.
     */
    double apply(double mouseValue) {
        Double typed = typed();
        double value = typed != null ? typed : mouseValue;
        if (inverse)
            value = value == 0 ? 0 : 1 / value;
        if (negate)
            value = -value;
        return value;
    }

    /**
     * Header text: the typed characters with a {@code |} at the cursor, wrapped
     * like Blender as {@code 1/(..)} for inverse and {@code -(..)} for negation.
     * Without typed text the (formatted) mouse value is shown instead of a cursor.
     */
    String display(String mouseValue) {
        String shown = text.length() == 0
                ? mouseValue + (edited() ? "|" : "")
                : text.substring(0, cursor) + "|" + text.substring(cursor);
        if (inverse)
            shown = "1/(" + shown + ")";
        if (negate)
            shown = "-(" + shown + ")";
        return shown;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "NumericInput[%s, cursor=%d, negate=%b, inverse=%b]", text, cursor, negate,
                inverse);
    }
}
