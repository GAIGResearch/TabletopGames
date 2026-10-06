package gui;

import javax.swing.*;
import java.awt.*;

/**
 * Messages a GUI shows to the player (round over, your turn, ...). On the desktop these are modal dialogs, as
 * JOptionPane shows them. A GUI that is not on a desktop can have them shown another way by setting a handler: the web
 * package does, as a modal dialog would block the one Swing thread shared by every game it serves, and would never
 * reach the browser.
 */
public final class GUIMessages {

    /**
     * Shows a message from the GUI containing the given component.
     */
    public interface Handler {
        /**
         * @param title   the message's title, or null
         * @param message plain text, or HTML starting with "&lt;html&gt;"
         */
        void show(Component parent, String title, String message);
    }

    private static volatile Handler handler;

    private GUIMessages() {
    }

    /**
     * Shows messages through the given handler instead of dialogs (null for dialogs again).
     */
    public static void setHandler(Handler h) {
        handler = h;
    }

    /**
     * As JOptionPane.showMessageDialog(parent, message).
     */
    public static void show(Component parent, String message) {
        Handler h = handler;
        if (h != null) h.show(parent, null, message);
        else JOptionPane.showMessageDialog(parent, message);
    }

    /**
     * As JOptionPane.showConfirmDialog(parent, message, title, optionType, messageType), but with no answer, as the
     * message is for information only.
     */
    public static void showConfirm(Component parent, String message, String title, int optionType, int messageType) {
        Handler h = handler;
        if (h != null) h.show(parent, title, message);
        else JOptionPane.showConfirmDialog(parent, message, title, optionType, messageType);
    }
}
