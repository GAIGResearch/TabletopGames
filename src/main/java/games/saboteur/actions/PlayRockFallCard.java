package games.saboteur.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.grid.SquareCoord;
import games.saboteur.SaboteurGameState;
import games.saboteur.components.ActionCard;
import games.saboteur.components.SaboteurCard;

import java.util.Arrays;

public class PlayRockFallCard extends AbstractAction {
    private final SquareCoord position;

    public PlayRockFallCard(SquareCoord position) {
        this.position = position;
    }

    public boolean execute(AbstractGameState gs) {
        SaboteurGameState sgs = (SaboteurGameState) gs;
        SaboteurCard deletedCard = sgs.getGridBoard().remove(position);

        Deck<SaboteurCard> currentDeck = sgs.getPlayerDecks().get(sgs.getCurrentPlayer());
        int idx = -1;
        for (int i = 0; i < currentDeck.getSize(); i++) {
            SaboteurCard card = currentDeck.getComponents().get(i);
            if (card instanceof ActionCard && ((ActionCard) card).actionType == ActionCard.ActionCardType.RockFall) {
                idx = i;
                break;
            }
        }
        // reveal identity to all players if they Rockfalled a Path
        if (deletedCard.type == SaboteurCard.SaboteurCardType.Path) {
            for (int p = 0; p < sgs.getNPlayers(); p++)
                sgs.getRoleDeck().setVisibilityOfComponent(sgs.getCurrentPlayer(), p, true);
        }
        sgs.getDiscardDeck().add(currentDeck.pick(idx));
        // the card taken off the board is discarded face up: everyone has seen it
        boolean[] seenByAll = new boolean[sgs.getNPlayers()];
        Arrays.fill(seenByAll, true);
        sgs.getDiscardDeck().add(deletedCard, seenByAll);
        return true;
    }

    @Override
    public PlayRockFallCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayRockFallCard that && position.equals(that.position);
    }

    @Override
    public int hashCode() {
        return position.hashCode() + 67;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public String toString() {
        return "RockFall at " + position;
    }
}
