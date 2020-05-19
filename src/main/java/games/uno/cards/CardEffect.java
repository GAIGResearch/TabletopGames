package games.uno.cards;

import core.actions.IAction;
import core.AbstractGameState;
import core.components.Card;

public abstract class CardEffect implements IAction {
    public abstract boolean execute(AbstractGameState gs);

    @Override
    public Card getCard() {
        return null;
    }
}
