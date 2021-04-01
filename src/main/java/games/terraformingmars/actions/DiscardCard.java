package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

import java.util.Objects;

public class DiscardCard extends TMAction {

    public DiscardCard(int player, int cardID) {
        super(player, true);
        setCardID(cardID);
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        TMGameState gs = (TMGameState) gameState;
        TMGameParameters gp = (TMGameParameters) gameState.getGameParameters();
        int player = this.player;
        if (player == -1) player = gs.getCurrentPlayer();
        TMCard card = (TMCard) gs.getComponentById(getCardID());
        if (card != null) {
            if (card.cardType != TMTypes.CardType.Corporation) {
                gs.getDiscardCards().add(card);
            }
            gs.getPlayerCardChoice()[player].remove(card);
        }
        return super.execute(gs);
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Discard " + gameState.getComponentById(getCardID()).getComponentName();
    }

    @Override
    public String toString() {
        return "Discard card id " + getCardID();
    }

}
