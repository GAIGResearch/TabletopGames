package web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import core.Game;
import core.actions.AbstractAction;
import gui.AbstractGUIManager;
import gui.ClickRegion;
import gui.RulesPages;
import gui.views.RulesView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Moves a GUI's standard parts (its action buttons, game state information and history; see
 * {@link AbstractGUIManager#getActionPanel()}) from the streamed image into the page, where the browser draws them
 * natively. A GUI that does not use a standard panel keeps that part in the image. It also sends the GUI's click
 * regions (see {@link AbstractGUIManager#getClickRegions()}), the parts of the frame that take the mouse wheel, and the
 * game's rules: its GUI's rules tabs, taken out of the image, and any rules written in Markdown.
 * <p>
 * Everything here runs on the Swing thread.
 */
class ChromeReader {

    private static final Pattern HISTORY_LINE = Pattern.compile("Player (\\d+) : (.*)", Pattern.DOTALL);
    private static final Pattern SCORES = Pattern.compile("Player Scores: (.*)");
    private static final Pattern PHASE = Pattern.compile("Game phase: (.*)");
    private static final Pattern TURN = Pattern.compile("Turn: (\\d+); Round: (\\d+)");
    private static final Pattern CURRENT = Pattern.compile("Current player: (\\d+)");

    private final AbstractGUIManager gui;
    private final Game game;
    private final JRootPane root;
    private final Sender out;
    private final boolean hasActions, hasInfo;

    private JsonArray sentActions;
    private JsonObject sentInfo;
    private List<String> sentHistory = List.of();
    private boolean historyReset;
    // the regions last sent, and the version number the page knows them by
    private String sentRegions;
    private List<ClickRegion> regions = List.of();
    private int regionsVersion;
    // the game's pages of rules
    private final List<RulesPages.Page> rules = new ArrayList<>();
    private boolean sentRules;
    private String sentWheelAreas;

    /**
     * @param dataPath the game's data directory, for any rules written in Markdown (see {@link RulesPages}); may be null
     */
    ChromeReader(AbstractGUIManager gui, Game game, JRootPane root, String dataPath, Sender out) {
        this.gui = gui;
        this.game = game;
        this.root = root;
        this.out = out;
        hasActions = gui.getActionPanel() != null && gui.getActionButtons().length > 0;
        hasInfo = gui.getInfoPanel() != null;
        // hidden, not removed: update still reads them, and choose clicks their buttons
        if (hasActions) gui.getActionPanel().setVisible(false);
        if (hasInfo) gui.getInfoPanel().setVisible(false);
        takeRulesTabs(root.getContentPane());
        for (RulesPages.Page page : RulesPages.fromDataDirectory(dataPath))
            if (rules.stream().noneMatch(p -> p.title().equals(page.title())))
                rules.add(page);
    }

    /**
     * Removes the GUI's rules tabs, keeping their pages to send to the page. A rules tab is a {@link RulesView}, or a
     * tab titled Rules or How to Play that holds HTML or text.
     */
    private void takeRulesTabs(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JTabbedPane tabs)
                for (int i = tabs.getTabCount() - 1; i >= 0; i--) {
                    Component tab = tabs.getComponentAt(i);
                    String title = tabs.getTitleAt(i);
                    String html = tab instanceof RulesView || RULES_TAB.matcher(title).matches() ? rulesHtml(tab) : null;
                    if (html != null) {
                        rules.add(0, new RulesPages.Page(title, html));
                        tabs.removeTabAt(i);
                    }
                }
            // A tab strip with one tab left only takes room, so the tab replaces the tabbed pane where the layout
            // allows it (a BorderLayout, which the GUIs with rules tabs use).
            if (child instanceof JTabbedPane tabs && tabs.getTabCount() == 1
                    && container.getLayout() instanceof BorderLayout layout) {
                Object where = layout.getConstraints(tabs);
                Component only = tabs.getComponentAt(0);
                container.remove(tabs);
                container.add(only, where);
                child = only;
            }
            if (child instanceof Container c) takeRulesTabs(c);
        }
    }

    private static final Pattern RULES_TAB = Pattern.compile("(?i).*\\b(rules|how to play)\\b.*");

    /**
     * The rules a component shows, as HTML (which may be a whole document), or null if it shows none.
     */
    static String rulesHtml(Component c) {
        if (c instanceof RulesView view) return view.getBodyHtml();
        if (c instanceof JEditorPane pane && pane.getContentType().contains("html")) return pane.getText();
        // a label holding a page of text, which a Swing label may hold as HTML
        if (c instanceof JLabel label && label.getText() != null && label.getText().length() >= 100) return label.getText();
        if (c instanceof JTextArea area && area.getText().length() >= 100)
            return "<div style='white-space: pre-wrap'>" + escapeHtml(area.getText()) + "</div>";
        if (c instanceof Container container)
            for (Component child : container.getComponents()) {
                String html = rulesHtml(child);
                if (html != null) return html;
            }
        return null;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
     * Sends everything on the next update, for a page that has reconnected.
     */
    void resend() {
        sentActions = null;
        sentInfo = null;
        sentHistory = List.of();
        historyReset = true;
        sentRegions = null;
        sentRules = false;
        sentWheelAreas = null;
    }

    /**
     * Sends whatever changed since the last update.
     */
    void update() {
        if (!sentRules) {
            sentRules = true;
            if (!rules.isEmpty()) {
                JsonArray pages = new JsonArray();
                for (RulesPages.Page p : rules) {
                    JsonObject page = new JsonObject();
                    page.addProperty("title", p.title());
                    page.addProperty("html", p.html());
                    pages.add(page);
                }
                JsonObject msg = new JsonObject();
                msg.addProperty("type", "rules");
                msg.add("pages", pages);
                out.sendText(msg.toString());
            }
        }
        if (hasActions) {
            JsonArray actions = new JsonArray();
            JButton[] buttons = gui.getActionButtons();
            for (int i = 0; i < buttons.length; i++)
                if (offers(buttons[i])) {
                    JsonObject a = new JsonObject();
                    a.addProperty("i", i);
                    a.addProperty("label", buttons[i].getText());
                    String kind = kind(gui.getButtonAction(i));
                    if (kind != null) a.addProperty("kind", kind);
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
            JsonObject info = info(gui.getGameStateInfo());
            if (!info.equals(sentInfo)) {
                sentInfo = info;
                JsonObject msg = info.deepCopy();
                msg.addProperty("type", "info");
                out.sendText(msg.toString());
            }
            sendHistory(gui.getHistory());
        }
        sendRegions();
        sendWheelAreas();
    }

    /**
     * Sends the parts of the frame that take the mouse wheel themselves.
     */
    private void sendWheelAreas() {
        JsonArray areas = new JsonArray();
        addWheelAreas(root.getContentPane(), areas);
        String text = areas.toString();
        if (text.equals(sentWheelAreas)) return;
        sentWheelAreas = text;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "wheelAreas");
        msg.add("areas", areas);
        out.sendText(msg.toString());
    }

    private void addWheelAreas(Component c, JsonArray areas) {
        if (!c.isShowing()) return;
        // a scroll pane that can scroll, or a view with a wheel listener of its own (a board that zooms, say)
        boolean takesWheel = c instanceof JScrollPane pane
                ? pane.getVerticalScrollBar().isVisible() || pane.getHorizontalScrollBar().isVisible()
                : c.getMouseWheelListeners().length > 0;
        if (takesWheel && c instanceof JComponent jc) {
            Rectangle r = SwingUtilities.convertRectangle(jc, jc.getVisibleRect(), root);
            if (!r.isEmpty()) {
                JsonArray a = new JsonArray();
                a.add(r.x);
                a.add(r.y);
                a.add(r.width);
                a.add(r.height);
                areas.add(a);
            }
            return;
        }
        if (c instanceof Container container)
            for (Component child : container.getComponents())
                addWheelAreas(child, areas);
    }

    /**
     * Sends the lines added since the last update or, if the history was replaced rather than added to, all of it.
     */
    private void sendHistory(List<String> history) {
        int sent = sentHistory.size();
        boolean appended = !historyReset && history.size() >= sent
                && (sent == 0 || Objects.equals(history.get(sent - 1), sentHistory.get(sent - 1)));
        if (appended && history.size() == sent) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "history");
        msg.addProperty("reset", !appended);
        JsonArray lines = new JsonArray();
        for (String line : appended ? history.subList(sent, history.size()) : history)
            lines.add(historyLine(line));
        msg.add("lines", lines);
        sentHistory = history;
        historyReset = false;
        out.sendText(msg.toString());
    }

    private void sendRegions() {
        List<ClickRegion> showing = new ArrayList<>();
        JsonArray json = new JsonArray();
        for (ClickRegion region : gui.getClickRegions()) {
            JComponent view = region.view();
            // a view in a tab not chosen is not showing
            if (!view.isShowing() || region.actions().isEmpty()) continue;
            Shape shape = region.shape();
            Rectangle visible = view.getVisibleRect();
            if (!visible.contains(shape.getBounds2D())) {
                Area area = new Area(shape);
                area.intersect(new Area(visible));
                if (area.isEmpty()) continue;
                shape = area;
            }
            Point offset = SwingUtilities.convertPoint(view, 0, 0, root);
            JsonObject r = new JsonObject();
            r.addProperty("path", svgPath(shape, offset.x, offset.y));
            JsonArray options = new JsonArray();
            for (AbstractAction a : region.actions())
                options.add(a.getString(game.getGameState()));
            r.add("options", options);
            json.add(r);
            showing.add(region);
        }
        String text = json.toString();
        if (text.equals(sentRegions)) return;
        sentRegions = text;
        regions = showing;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "regions");
        msg.addProperty("v", ++regionsVersion);
        msg.add("regions", json);
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
     * Chooses option o of click region r, if the page's regions (version v) are still the ones offered.
     */
    void chooseRegion(int v, int r, int o) {
        if (v != regionsVersion || r < 0 || r >= regions.size()) return;
        List<? extends AbstractAction> actions = regions.get(r).actions();
        if (o >= 0 && o < actions.size())
            gui.chooseClicked(actions.get(o));
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

    /**
     * The name of the action's class, or of the class an anonymous one extends; null for no action.
     */
    static String kind(AbstractAction action) {
        if (action == null) return null;
        Class<?> c = action.getClass();
        while (c.isAnonymousClass()) c = c.getSuperclass();
        return c.getSimpleName();
    }

    /**
     * The info panel's lines: the standard ones (see {@link AbstractGUIManager#getGameStateInfo()}) as fields, where
     * they have their standard form, and any others as lines.
     */
    static JsonObject info(List<String> info) {
        JsonObject result = new JsonObject();
        JsonArray lines = new JsonArray();
        for (String line : info) {
            Matcher m;
            if (line.startsWith("Game status: ")) {
                // the page shows the game's progress itself
                continue;
            } else if ((m = SCORES.matcher(line)).matches() && scores(m.group(1)) != null) {
                result.add("scores", scores(m.group(1)));
            } else if ((m = PHASE.matcher(line)).matches()) {
                result.addProperty("phase", m.group(1));
            } else if ((m = TURN.matcher(line)).matches()) {
                result.addProperty("turn", Integer.parseInt(m.group(1)));
                result.addProperty("round", Integer.parseInt(m.group(2)));
            } else if ((m = CURRENT.matcher(line)).matches()) {
                result.addProperty("current", Integer.parseInt(m.group(1)));
            } else {
                lines.add(line);
            }
        }
        result.add("lines", lines);
        return result;
    }

    /**
     * The scores in "1, 2, 3", or null if that is not a list of numbers.
     */
    private static JsonArray scores(String text) {
        JsonArray scores = new JsonArray();
        for (String s : text.split(",")) {
            try {
                scores.add(Double.parseDouble(s.trim()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return scores;
    }

    /**
     * A history line: its text, and the player it is about (from {@code "Player 2 : ..."}), or -1 if none.
     */
    static JsonObject historyLine(String line) {
        JsonObject result = new JsonObject();
        Matcher m = HISTORY_LINE.matcher(line);
        result.addProperty("player", m.matches() ? Integer.parseInt(m.group(1)) : -1);
        result.addProperty("text", m.matches() ? m.group(2) : line);
        return result;
    }

    /**
     * The shape as SVG path data, its curves made straight lines, moved by (dx, dy).
     */
    static String svgPath(Shape shape, double dx, double dy) {
        StringBuilder path = new StringBuilder();
        double[] c = new double[6];
        for (PathIterator it = shape.getPathIterator(null, 0.5); !it.isDone(); it.next()) {
            switch (it.currentSegment(c)) {
                case PathIterator.SEG_MOVETO -> path.append('M').append(number(c[0] + dx)).append(' ').append(number(c[1] + dy));
                case PathIterator.SEG_LINETO -> path.append('L').append(number(c[0] + dx)).append(' ').append(number(c[1] + dy));
                case PathIterator.SEG_CLOSE -> path.append('Z');
            }
        }
        return path.toString();
    }

    private static String number(double v) {
        long tenths = Math.round(v * 10);
        return tenths % 10 == 0 ? Long.toString(tenths / 10) : Double.toString(tenths / 10.0);
    }
}
