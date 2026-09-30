package gui.views;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;

/**
 * A Rules tab: an HTML page in a column {@link #COLUMN_WIDTH} pixels wide, which wraps to the column, scrolls only
 * vertically, opens at the top and follows in-page links (<code>&lt;a href='#x'&gt;</code> to
 * <code>&lt;a name='x'&gt;</code>). A wider pane centres the column rather than widening the lines.
 */
public class RulesView extends JScrollPane {

    public static final int COLUMN_WIDTH = 660;

    /**
     * @param bodyHtml the page body, without the html and body tags
     * @param height   the preferred height of the view
     */
    public RulesView(String bodyHtml, int height) {
        JEditorPane pane = new JEditorPane("text/html", "<html><body>" + bodyHtml + "</body></html>") {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        pane.setEditable(false);
        pane.setOpaque(true);
        pane.setBackground(Color.WHITE);
        pane.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getDescription() != null
                    && e.getDescription().startsWith("#"))
                pane.scrollToReference(e.getDescription().substring(1));
        });

        // The column keeps its width; any extra width goes either side of it.
        JPanel column = new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(COLUMN_WIDTH, pane.getPreferredSize().height);
            }
        };
        column.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.VERTICAL;
        c.weighty = 1;
        JPanel sized = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                pane.setSize(COLUMN_WIDTH, Short.MAX_VALUE);
                return new Dimension(COLUMN_WIDTH, pane.getPreferredSize().height);
            }
        };
        sized.add(pane, BorderLayout.CENTER);
        column.add(sized, c);

        setViewportView(column);
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        getVerticalScrollBar().setUnitIncrement(16);
        setPreferredSize(new Dimension(COLUMN_WIDTH + 20, height));
        SwingUtilities.invokeLater(() -> getVerticalScrollBar().setValue(0));
        pane.setCaretPosition(0);
    }
}
