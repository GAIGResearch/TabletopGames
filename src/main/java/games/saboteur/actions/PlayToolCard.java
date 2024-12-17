package games.saboteur.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.saboteur.SaboteurGameState;
import games.saboteur.components.ActionCard;
import games.saboteur.components.SaboteurCard;

import java.util.Objects;

public class PlayToolCard extends AbstractAction {

    private final int playerID;
    private final int cardIdx;
    private final boolean isFunctional;
    private final ActionCard.ToolCardType toolType;

    public PlayToolCard(int cardIdx, int playerID, ActionCard.ToolCardType toolType, boolean isFunctional)
    {
        this.cardIdx = cardIdx;
        this.playerID = playerID;
        this.toolType = toolType;
        this.isFunctional = isFunctional;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SaboteurGameState sgs = (SaboteurGameState) gs;
        Deck<SaboteurCard> currentPlayerDeck = sgs.getPlayerDecks().get(sgs.getCurrentPlayer());
        sgs.getDiscardDeck().add(currentPlayerDeck.pick(cardIdx));
        sgs.setToolFunctional(playerID, toolType, isFunctional);
        return true;
    }

    @Override
    public PlayToolCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayToolCard that)) return false;
        return playerID == that.playerID && cardIdx == that.cardIdx && isFunctional == that.isFunctional && toolType == that.toolType;
    }

    public int getCardIdx() {
        return cardIdx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardIdx, isFunctional, toolType);
    }

    public String toString() {
        return (isFunctional? "Fix " : "Break ") + toolType + " to " + playerID;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return (isFunctional? "Fix " : "Break ") + toolType + " to player " + playerID;
    }
}
