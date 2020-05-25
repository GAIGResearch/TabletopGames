package games.uno.cards;

import core.actions.AbstractAction;
import core.AbstractGameState;
import core.components.Card;

public abstract class CardEffect extends AbstractAction {
    public abstract boolean execute(AbstractGameState gs);


}
