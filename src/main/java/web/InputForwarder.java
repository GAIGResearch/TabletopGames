package web;

import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Map;
import java.util.Objects;

/**
 * Turns the browser's mouse and key events into AWT events on the frame, so the GUI sees what it would see on the
 * desktop.
 * <p>
 * Mouse events are posted to the frame itself, and Swing routes them to the component under the mouse, generating
 * enter and exit events as the mouse crosses components. A click is posted after a release that did not drag, as the
 * windowing system would. Key events go to the component last pressed, as the frame has no keyboard focus.
 * <p>
 * Swing's tooltips are switched off (see {@link GameSession#configureSwing}), so after each move the tooltip text of
 * the component under the mouse is sent to the browser to show.
 * <p>
 * Browser messages carry CSS pixel coordinates, which are the frame's coordinates, as the frame is sized to match.
 */
class InputForwarder {

    /** How far (in pixels) the mouse may move between press and release for the release to count as a click. */
    static final int CLICK_SLOP = 4;

    private final JFrame frame;
    private final Sender out;
    private boolean inside;
    private Point pressedAt;
    private boolean dragged;
    private Component keyTarget;
    private String tooltip;

    InputForwarder(JFrame frame, Sender out) {
        this.frame = frame;
        this.out = out;
    }

    void handle(String type, JsonObject msg) {
        switch (type) {
            case "mouse" -> mouse(msg);
            case "wheel" -> wheel(msg);
            case "key" -> key(msg);
            default -> System.out.println("Unknown message type from browser: " + type);
        }
    }

    private void mouse(JsonObject msg) {
        String kind = msg.get("kind").getAsString();
        Point p = new Point(msg.get("x").getAsInt(), msg.get("y").getAsInt());
        int mods = modifiers(msg);
        if (kind.equals("leave")) {
            if (inside) post(MouseEvent.MOUSE_EXITED, p, mods, 0, MouseEvent.NOBUTTON);
            inside = false;
            SwingUtilities.invokeLater(() -> sendTooltip(null));
            return;
        }
        // Swing tracks which component the mouse is over only once the mouse has entered the frame. (A release may come
        // from outside, ending a drag that left the canvas.)
        if (!inside && !kind.equals("up")) {
            post(MouseEvent.MOUSE_ENTERED, p, mods, 0, MouseEvent.NOBUTTON);
            inside = true;
        }
        int button = button(msg);
        int clicks = msg.has("clicks") ? Math.max(1, msg.get("clicks").getAsInt()) : 1;
        switch (kind) {
            case "move" -> {
                boolean buttonDown = (mods & (InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK)) != 0;
                if (buttonDown && pressedAt != null && pressedAt.distance(p) > CLICK_SLOP) dragged = true;
                post(buttonDown ? MouseEvent.MOUSE_DRAGGED : MouseEvent.MOUSE_MOVED, p, mods, 0, MouseEvent.NOBUTTON);
                updateTooltip(p);
            }
            case "down" -> {
                pressedAt = p;
                dragged = false;
                post(MouseEvent.MOUSE_PRESSED, p, mods, clicks, button);
                SwingUtilities.invokeLater(() -> keyTarget = componentAt(p));
                SwingUtilities.invokeLater(() -> sendTooltip(null));
            }
            case "up" -> {
                post(MouseEvent.MOUSE_RELEASED, p, mods, clicks, button);
                if (pressedAt != null && !dragged)
                    post(MouseEvent.MOUSE_CLICKED, p, mods, clicks, button);
                pressedAt = null;
            }
            case "enter" -> {
                // the enter event has already been posted above
            }
            default -> System.out.println("Unknown mouse event from browser: " + kind);
        }
    }

    private void post(int id, Point p, int mods, int clicks, int button) {
        boolean popupTrigger = id == MouseEvent.MOUSE_PRESSED && button == MouseEvent.BUTTON3;
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                new MouseEvent(frame, id, System.currentTimeMillis(), mods, p.x, p.y, clicks, popupTrigger, button));
    }

    private void wheel(JsonObject msg) {
        Point p = new Point(msg.get("x").getAsInt(), msg.get("y").getAsInt());
        double delta = msg.get("deltaY").getAsDouble();
        if (delta == 0) return;
        int rotation = delta > 0 ? 1 : -1;
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                new MouseWheelEvent(frame, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), modifiers(msg),
                        p.x, p.y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, rotation));
    }

    private void key(JsonObject msg) {
        String kind = msg.get("kind").getAsString();
        String key = msg.get("key").getAsString();
        int mods = modifiers(msg);
        int keyCode = keyCode(key);
        char keyChar = key.length() == 1 ? key.charAt(0) : KeyEvent.CHAR_UNDEFINED;
        SwingUtilities.invokeLater(() -> {
            Component target = keyTarget != null ? keyTarget : frame.getContentPane();
            long when = System.currentTimeMillis();
            if (kind.equals("down")) {
                target.dispatchEvent(new KeyEvent(target, KeyEvent.KEY_PRESSED, when, mods, keyCode, keyChar));
                if (keyChar != KeyEvent.CHAR_UNDEFINED)
                    target.dispatchEvent(new KeyEvent(target, KeyEvent.KEY_TYPED, when, mods, KeyEvent.VK_UNDEFINED, keyChar));
            } else if (kind.equals("up")) {
                target.dispatchEvent(new KeyEvent(target, KeyEvent.KEY_RELEASED, when, mods, keyCode, keyChar));
            }
        });
    }

    private void updateTooltip(Point p) {
        SwingUtilities.invokeLater(() -> {
            Component c = componentAt(p);
            String text = null;
            if (c instanceof JComponent jc) {
                Point local = SwingUtilities.convertPoint(frame, p, jc);
                text = jc.getToolTipText(new MouseEvent(jc, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0,
                        local.x, local.y, 0, false));
                // some components give an empty tooltip ("" or "<html></html>"), which is no tooltip
                if (text != null && text.replaceAll("<[^>]*>", "").isBlank()) text = null;
            }
            sendTooltip(text);
        });
    }

    /** On the Swing thread only, as that is where the tooltip is looked up. */
    private void sendTooltip(String text) {
        if (Objects.equals(text, tooltip)) return;
        tooltip = text;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "tooltip");
        msg.addProperty("text", text);
        out.sendText(msg.toString());
    }

    private Component componentAt(Point p) {
        Component c = SwingUtilities.getDeepestComponentAt(frame.getRootPane(), p.x, p.y);
        return c != null ? c : frame.getContentPane();
    }

    private static int button(JsonObject msg) {
        if (!msg.has("button")) return MouseEvent.NOBUTTON;
        return switch (msg.get("button").getAsInt()) {
            case 0 -> MouseEvent.BUTTON1;
            case 1 -> MouseEvent.BUTTON2;
            case 2 -> MouseEvent.BUTTON3;
            default -> MouseEvent.NOBUTTON;
        };
    }

    /**
     * The AWT modifier mask for the message's keys and mouse buttons. The browser's buttons mask has 1 for the primary
     * button, 2 for the secondary and 4 for the middle one.
     */
    private static int modifiers(JsonObject msg) {
        int mods = 0;
        if (flag(msg, "shift")) mods |= InputEvent.SHIFT_DOWN_MASK;
        if (flag(msg, "ctrl")) mods |= InputEvent.CTRL_DOWN_MASK;
        if (flag(msg, "alt")) mods |= InputEvent.ALT_DOWN_MASK;
        if (flag(msg, "meta")) mods |= InputEvent.META_DOWN_MASK;
        int buttons = msg.has("buttons") ? msg.get("buttons").getAsInt() : 0;
        if ((buttons & 1) != 0) mods |= InputEvent.BUTTON1_DOWN_MASK;
        if ((buttons & 2) != 0) mods |= InputEvent.BUTTON3_DOWN_MASK;
        if ((buttons & 4) != 0) mods |= InputEvent.BUTTON2_DOWN_MASK;
        return mods;
    }

    private static boolean flag(JsonObject msg, String name) {
        return msg.has(name) && msg.get(name).getAsBoolean();
    }

    private static final Map<String, Integer> namedKeys = Map.ofEntries(
            Map.entry("Enter", KeyEvent.VK_ENTER), Map.entry("Escape", KeyEvent.VK_ESCAPE),
            Map.entry("Backspace", KeyEvent.VK_BACK_SPACE), Map.entry("Tab", KeyEvent.VK_TAB),
            Map.entry("Delete", KeyEvent.VK_DELETE), Map.entry(" ", KeyEvent.VK_SPACE),
            Map.entry("ArrowLeft", KeyEvent.VK_LEFT), Map.entry("ArrowRight", KeyEvent.VK_RIGHT),
            Map.entry("ArrowUp", KeyEvent.VK_UP), Map.entry("ArrowDown", KeyEvent.VK_DOWN),
            Map.entry("Home", KeyEvent.VK_HOME), Map.entry("End", KeyEvent.VK_END),
            Map.entry("PageUp", KeyEvent.VK_PAGE_UP), Map.entry("PageDown", KeyEvent.VK_PAGE_DOWN),
            Map.entry("Shift", KeyEvent.VK_SHIFT), Map.entry("Control", KeyEvent.VK_CONTROL),
            Map.entry("Alt", KeyEvent.VK_ALT), Map.entry("Meta", KeyEvent.VK_META));

    private static int keyCode(String key) {
        Integer named = namedKeys.get(key);
        if (named != null) return named;
        if (key.length() == 1) return KeyEvent.getExtendedKeyCodeForChar(Character.toUpperCase(key.charAt(0)));
        return KeyEvent.VK_UNDEFINED;
    }
}
