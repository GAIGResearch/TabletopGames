package gui.views;

import games.GameType;
import gui.RulesPages;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.html.HTML;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * A Rules tab: an HTML page in a column of readable width, which wraps to the column and scrolls only downwards.
 * A link to an anchor on the page ({@code <a href='#scoring'>} to {@code <a name='scoring'>}, or to an element with
 * that id, as a Markdown heading has) scrolls to it.
 */
public class RulesView extends JPanel {

    // about 80 characters a line at the font below
    public static final int COLUMN_WIDTH = 660;

    final JEditorPane pane;
    final String bodyHtml;

    /**
     * @param bodyHtml the page, without the html and body tags
     * @param height   the height of the column; the page scrolls within it
     */
    public RulesView(String bodyHtml, int height) {
        this.bodyHtml = bodyHtml;
        pane = new JEditorPane("text/html",
                "<html><body style='font-family:sans-serif; font-size:11pt; margin:8px'>" + bodyHtml + "</body></html>");
        pane.setEditable(false);
        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getDescription() != null
                    && e.getDescription().startsWith("#"))
                scrollTo(e.getDescription().substring(1));
        });
        pane.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(pane,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(COLUMN_WIDTH, height));
        // the default FlowLayout centres the column in a wider tab
        setOpaque(false);
        add(scroll);
    }

    /**
     * Adds a tab for each page of the game's rules (see {@link RulesPages}), filled in from the parameters.
     *
     * @param height the height of each column
     */
    public static void addTabs(JTabbedPane tabs, GameType game, Object params, int height) {
        for (RulesPages.Page page : RulesPages.load(game, params))
            tabs.add(page.title(), new RulesView(page.html(), height));
    }

    /**
     * The page, without the html and body tags.
     */
    public String getBodyHtml() {
        return bodyHtml;
    }

    private void scrollTo(String anchor) {
        // Swing's own scrollToReference finds only <a name=...>
        ElementIterator it = new ElementIterator(pane.getDocument());
        for (Element e = it.next(); e != null; e = it.next()) {
            if (anchor.equals(e.getAttributes().getAttribute(HTML.Attribute.ID))) {
                try {
                    Rectangle2D r = pane.modelToView2D(e.getStartOffset());
                    if (r != null) {
                        Rectangle visible = pane.getVisibleRect();
                        pane.scrollRectToVisible(new Rectangle(0, (int) r.getY(), 1, visible.height));
                    }
                } catch (javax.swing.text.BadLocationException ex) {
                    // not on the page
                }
                return;
            }
        }
        pane.scrollToReference(anchor);
    }
}
