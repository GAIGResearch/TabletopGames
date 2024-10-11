package games.conquest;

import core.actions.AbstractAction;
import games.conquest.actions.*;
import games.conquest.components.Command;
import utilities.Vector2D;

public class CQUtility {
    public static boolean compareHighlight(AbstractAction action, Vector2D highlight, Command cmdHighlight) {
        if (action instanceof EndTurn) return true;
        assert action instanceof CQAction; // Only EndTurn is a basic AbstractAction; the rest extend CQAction
        if (action instanceof ApplyCommand) return ((CQAction) action).checkHighlight(highlight, cmdHighlight);
        else return ((CQAction) action).checkHighlight(highlight);
    }
}
