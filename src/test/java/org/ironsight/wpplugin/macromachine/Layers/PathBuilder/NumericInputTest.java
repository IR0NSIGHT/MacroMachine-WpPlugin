package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.event.KeyEvent;
import org.junit.jupiter.api.Test;

class NumericInputTest
{
    private static NumericInput typed(String keys) {
        var input = new NumericInput();
        for (char c : keys.toCharArray())
            assertTrue(input.handleKey(KeyEvent.VK_UNDEFINED, c, false), "key " + c);
        return input;
    }

    private static void key(NumericInput input, int code) {
        assertTrue(input.handleKey(code, KeyEvent.CHAR_UNDEFINED, false));
    }

    @Test
    void typedNumberReplacesMouseValueAndShowsCursor() {
        var input = typed("2.5");
        assertTrue(input.edited());
        assertEquals(2.5, input.apply(7));
        assertEquals("2.5|", input.display("7"));
    }

    @Test
    void emptyInputFollowsMouse() {
        var input = new NumericInput();
        assertFalse(input.edited());
        assertEquals(1.3, input.apply(1.3));
        assertEquals("1.30", input.display("1.30"));
        var cleared = typed("4");
        key(cleared, KeyEvent.VK_BACK_SPACE);
        assertFalse(cleared.edited(), "backspacing to empty hands control back to the mouse");
        assertEquals(1.3, cleared.apply(1.3));
    }

    @Test
    void minusNegatesAndSlashInvertsLikeBlender() {
        var negated = typed("-2");
        assertEquals(-2, negated.apply(0));
        assertEquals("-(2|)", negated.display("0"));
        assertEquals(2, typed("-2-").apply(0), "minus toggles");
        var inverse = typed("4/");
        assertEquals(0.25, inverse.apply(0));
        assertEquals("1/(4|)", inverse.display("0"));
        assertEquals(-0.25, typed("4/-").apply(0));
        // negation without digits applies to the mouse value
        assertEquals(-1.5, typed("-").apply(1.5));
        assertEquals("-(1.50|)", typed("-").display("1.50"));
    }

    @Test
    void decimalPointCommaAndNumpad() {
        assertEquals(0.5, typed(".5").apply(0));
        assertEquals(1.25, typed("1,25").apply(0), "numpad decimal may yield ','");
        assertEquals(1.25, typed("1.2.5").apply(0), "only one decimal point");
        var numpad = new NumericInput();
        numpad.handleKey(KeyEvent.VK_NUMPAD3, '3', false);
        numpad.handleKey(KeyEvent.VK_DECIMAL, KeyEvent.CHAR_UNDEFINED, false);
        numpad.handleKey(KeyEvent.VK_NUMPAD5, '5', false);
        assertEquals(3.5, numpad.apply(0));
        assertNull(typed(".").typed());
    }

    @Test
    void cursorEditing() {
        var input = typed("125");
        key(input, KeyEvent.VK_LEFT);
        key(input, KeyEvent.VK_LEFT);
        assertEquals("1|25", input.display(""));
        input.handleKey(KeyEvent.VK_UNDEFINED, '.', false);
        assertEquals(1.25, input.apply(0));
        key(input, KeyEvent.VK_DELETE);
        assertEquals("1.|5", input.display(""));
        key(input, KeyEvent.VK_HOME);
        assertEquals("|1.5", input.display(""));
        key(input, KeyEvent.VK_END);
        key(input, KeyEvent.VK_BACK_SPACE);
        assertEquals("1.|", input.display(""));
        assertTrue(input.handleKey(KeyEvent.VK_BACK_SPACE, KeyEvent.CHAR_UNDEFINED, true));
        assertEquals("", input.display(""));
        assertFalse(input.handleKey(KeyEvent.VK_A, 'a', false), "letters are not numeric input");
    }

    @Test
    void startKeys() {
        for (char c : "0123456789.,-/".toCharArray())
            assertTrue(NumericInput.starts(KeyEvent.VK_UNDEFINED, c), String.valueOf(c));
        assertTrue(NumericInput.starts(KeyEvent.VK_SUBTRACT, KeyEvent.CHAR_UNDEFINED));
        assertFalse(NumericInput.starts(KeyEvent.VK_A, 'a'));
        assertFalse(NumericInput.starts(KeyEvent.VK_ENTER, '\n'));
        // starting from the keyboard (no drag): digits and decimal point only
        for (char c : "0123456789.,".toCharArray())
            assertTrue(NumericInput.startsTyping(KeyEvent.VK_UNDEFINED, c), String.valueOf(c));
        assertTrue(NumericInput.startsTyping(KeyEvent.VK_DECIMAL, KeyEvent.CHAR_UNDEFINED));
        assertFalse(NumericInput.startsTyping(KeyEvent.VK_MINUS, '-'));
        assertFalse(NumericInput.startsTyping(KeyEvent.VK_SLASH, '/'));
    }
}
