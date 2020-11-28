package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.Arrays;

public class MoatReaction extends AbstractAction implements IDominionReaction {

    final int player;

    public MoatReaction(int playerId) {
        player = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        PartialObservableDeck<DominionCard> hand = (PartialObservableDeck<DominionCard>) state.getDeck(DominionConstants.DeckType.HAND, player);
        for (int pos = 0; pos < hand.getSize(); pos++) {
            if (hand.get(pos).cardType() == CardType.MOAT) {
                boolean[] allTrue = new boolean[state.getNPlayers()];
                Arrays.fill(allTrue, true);
                hand.setVisibilityOfComponent(pos, allTrue);
                state.setDefended(player);
                return true;
            }
        }
        throw new AssertionError("No MOAT card found in hand");
    }

    @Override
    public AbstractAction copy() {
        // immutable state
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MoatReaction && ((MoatReaction) obj).player == player;
    }

    @Override
    public int hashCode() {
        return player * 49234;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Player " + player + " reveals a MOAT";
    }

    @Override
    public CardType getCardType() {
        return CardType.MOAT;
    }

    @Override
    public int getPlayer() {
        return player;
    }
}

