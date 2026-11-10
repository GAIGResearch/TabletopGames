package gui;

import core.actions.AbstractAction;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * A part of a view, such as a province on a map or a card in a hand, that a human player may click to choose one of
 * the actions offered there. See {@link AbstractGUIManager#getClickRegions()}.
 *
 * @param view    the component the shape is drawn in
 * @param shape   the part of the view, in the view's coordinates
 * @param actions the actions a click there may choose, in the order to list them; each one of the offered actions
 *                (see {@link ClickableActions#matching})
 */
public record ClickRegion(JComponent view, Shape shape, List<? extends AbstractAction> actions) {

    public ClickRegion {
        actions = List.copyOf(actions);
    }
}
