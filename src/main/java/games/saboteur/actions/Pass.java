package games.saboteur.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.saboteur.SaboteurGameState;
import games.saboteur.components.SaboteurCard;

import java.util.Objects;

public class Pass extends AbstractAction
{
    private final int cardIdx;
    public Pass(int cardIdx) {
        this.cardIdx = cardIdx;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        SaboteurGameState sgs = (SaboteurGameState) gs;
        Deck<SaboteurCard> currentDeck = sgs.getPlayerDecks().get(sgs.getCurrentPlayer());
        sgs.getDiscardDeck().add(currentDeck.pick(cardIdx));
        return true;
    }

    @Override
    public Pass copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pass pass)) return false;
        return cardIdx == pass.cardIdx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardIdx);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Player " + gameState.getCurrentPlayer() + " pass using card " + cardIdx + ".";
    }

    public String toString() {
        return "Pass using card " + cardIdx + ".";
    }
}
