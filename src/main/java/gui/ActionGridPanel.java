package gui;

import javax.swing.*;
import java.awt.*;

/**
 * The panel holding the action buttons: a FlowGridLayout that, inside a JScrollPane, always matches the viewport's
 * width, so the buttons wrap into rows and the pane scrolls only vertically.
 */
public class ActionGridPanel extends JPanel implements Scrollable {

    public ActionGridPanel(int preferredWidth) {
        super(new FlowGridLayout(4, 4, preferredWidth));
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        // one row of buttons
        for (Component c : getComponents())
            if (c.isVisible())
                return c.getHeight() + 4;
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
