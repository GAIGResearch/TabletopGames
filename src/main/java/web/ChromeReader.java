package web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gui.AbstractGUIManager;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;

/**
 * Moves a GUI's standard parts (its action buttons, game state information and history; see
 * {@link AbstractGUIManager#getActionPanel()}) from the streamed image into the page, where the browser draws them
 * natively. A GUI that does not use a standard panel keeps that part in the image.
 * <p>
 * Everything here runs on the Swing thread.
 */
class ChromeReader {

    private final AbstractGUIManager gui;
    private final Sender out;
    private final boolean hasActions, hasInfo;

    private JsonArray sentActions = new JsonArray();
    private List<String> sentInfo = List.of();
    private List<String> sentHistory = List.of();

    ChromeReader(AbstractGUIManager gui, Sender out) {
        this.gui = gui;
        this.out = out;
        hasActions = gui.getActionPanel() != null && gui.getActionButtons().length > 0;
        hasInfo = gui.getInfoPanel() != null;
        // hidden, not removed: update still reads them, and choose clicks their buttons
        if (hasActions) gui.getActionPanel().setVisible(false);
        if (hasInfo) gui.getInfoPanel().setVisible(false);
    }

    /**
     * Whether the page shows the actions (otherwise they stay in the image).
     */
    boolean hasActions() {
        return hasActions;
    }

    /**
     * Whether the page shows the game state information and history (otherwise they stay in the image).
     */
    boolean hasInfo() {
        return hasInfo;
    }

    /**
     * Sends whatever changed since the last update.
     */
    void update() {
        if (hasActions) {
            JsonArray actions = new JsonArray();
            JButton[] buttons = gui.getActionButtons();
            for (int i = 0; i < buttons.length; i++)
                if (offers(buttons[i])) {
                    JsonObject a = new JsonObject();
                    a.addProperty("i", i);
                    a.addProperty("label", buttons[i].getText());
                    actions.add(a);
                }
            if (!actions.equals(sentActions)) {
                sentActions = actions;
                JsonObject msg = new JsonObject();
                msg.addProperty("type", "actions");
                msg.add("actions", actions);
                out.sendText(msg.toString());
            }
        }
        if (hasInfo) {
            List<String> info = gui.getGameStateInfo();
            if (!info.equals(sentInfo)) {
                sentInfo = info;
                JsonObject msg = new JsonObject();
                msg.addProperty("type", "info");
                msg.add("lines", array(info));
                out.sendText(msg.toString());
            }
            sendHistory(gui.getHistory());
        }
    }

    /**
     * Sends the lines added since the last update or, if the history was replaced rather than added to, all of it.
     */
    private void sendHistory(List<String> history) {
        int sent = sentHistory.size();
        boolean appended = history.size() >= sent && (sent == 0 || Objects.equals(history.get(sent - 1), sentHistory.get(sent - 1)));
        if (appended && history.size() == sent) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "history");
        msg.addProperty("reset", !appended);
        msg.add("lines", array(appended ? history.subList(sent, history.size()) : history));
        sentHistory = history;
        out.sendText(msg.toString());
    }

    /**
     * Chooses the action on button i, if that button still offers the action the page showed.
     */
    void choose(int i, String label) {
        JButton[] buttons = gui.getActionButtons();
        if (i < 0 || i >= buttons.length || !offers(buttons[i]) || !buttons[i].getText().equals(label)) return;
        // a click on the Swing button, rather than the action itself, so whatever the game does on a click happens
        buttons[i].doClick(0);
    }

    /**
     * Passes the mouse entering or leaving the page's button for action i on to the Swing button, so a game's hover
     * effects (highlights on the board, say) still happen.
     */
    void hover(int i, boolean enter) {
        JButton[] buttons = gui.getActionButtons();
        if (i < 0 || i >= buttons.length) return;
        JButton b = buttons[i];
        b.dispatchEvent(new MouseEvent(b, enter ? MouseEvent.MOUSE_ENTERED : MouseEvent.MOUSE_EXITED,
                System.currentTimeMillis(), 0, b.getWidth() / 2, b.getHeight() / 2, 0, false));
    }

    private static boolean offers(JButton b) {
        return b.isVisible() && b.getText() != null && !b.getText().isEmpty();
    }

    private static JsonArray array(List<String> lines) {
        JsonArray a = new JsonArray();
        lines.forEach(a::add);
        return a;
    }
}
