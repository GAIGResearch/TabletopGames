package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import core.interfaces.IExtendedSequence;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

import java.util.ArrayList;
import java.util.List;

public class DiscardCard extends TMAction implements IExtendedSequence {

    public DiscardCard(int player, int cardID) {
        super(player, true);
        setCardID(cardID);
    }

    public DiscardCard(int player) {
        super(player, true);
    }

    @Override
    public boolean _execute(TMGameState gs) {
        TMGameParameters gp = (TMGameParameters) gs.getGameParameters();
        TMCard card = (TMCard) gs.getComponentById(getCardID());
        if (card != null) {
            if (card.cardType != TMTypes.CardType.Corporation) {
                gs.getDiscardCards().add(card);
            }
            gs.getPlayerCardChoice()[player].remove(card);
        } else {
            gs.setActionInProgress(this);
        }
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        // Choose which card in hand to discard
        List<AbstractAction> actions = new ArrayList<>();
        TMGameState gs = (TMGameState) state;
        for (int i = 0; i < gs.getPlayerHands()[player].getSize(); i++) {
            actions.add(new DiscardCard(player, gs.getPlayerHands()[player].get(i).getComponentID()));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        setCardID(((DiscardCard)action).getCardID());
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return getCardID() != -1;
    }

    @Override
    public DiscardCard _copy() {
        return new DiscardCard(player, getCardID());
    }

    @Override
    public DiscardCard copy() {
        return (DiscardCard) super.copy();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if (getCardID() == -1) return "Discard a card";
        Component c = gameState.getComponentById(getCardID());
        return "Discard " + c.getComponentName();
    }

    @Override
    public String toString() {
        return "Discard card id " + getCardID();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DiscardCard)) return false;
        return super.equals(o);
    }
}
