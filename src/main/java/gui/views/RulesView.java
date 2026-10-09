package gui.views;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;

/**
 * A Rules tab: an HTML page in a column of readable width, which wraps to the column and scrolls only downwards.
 * Links to an anchor on the page ({@code <a href='#scoring'>} to {@code <a name='scoring'>}) scroll to it.
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
                pane.scrollToReference(e.getDescription().substring(1));
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
     * The page, without the html and body tags.
     */
    public String getBodyHtml() {
        return bodyHtml;
    }
}
