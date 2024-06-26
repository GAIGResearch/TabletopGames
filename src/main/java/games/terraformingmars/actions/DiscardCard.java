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
import java.util.Objects;

public class DiscardCard extends TMAction implements IExtendedSequence {
    boolean cardChoice;

    public DiscardCard() { super(); } // This is needed for JSON Deserializer

    public DiscardCard(int player, int cardID, boolean cardChoice) {
        super(player, true);
        setCardID(cardID);
        this.cardChoice = cardChoice;
    }

    public DiscardCard(int player, boolean cardChoice) {
        super(player, true);
        this.cardChoice = cardChoice;
    }

    @Override
    public boolean _execute(TMGameState gs) {
        TMGameParameters gp = (TMGameParameters) gs.getGameParameters();
        TMCard card = (TMCard) gs.getComponentById(getCardID());
        if (card != null) {
            if (card.cardType != TMTypes.CardType.Corporation) {
                gs.getDiscardCards().add(card);
            }
            if (cardChoice) {
                gs.getPlayerCardChoice()[player].remove(card);
            } else {
                gs.getPlayerHands()[player].remove(card);
            }
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
        if (cardChoice) {
            for (int i = 0; i < gs.getPlayerCardChoice()[player].getSize(); i++) {
                actions.add(new DiscardCard(player, gs.getPlayerCardChoice()[player].get(i).getComponentID(), cardChoice));
            }
        } else {
            for (int i = 0; i < gs.getPlayerHands()[player].getSize(); i++) {
                actions.add(new DiscardCard(player, gs.getPlayerHands()[player].get(i).getComponentID(), cardChoice));
            }
        }
        if (actions.size() == 0) actions.add(new TMAction(player));
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        setCardID(((DiscardCard)action).getCardID());
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return getCardID() != -1;
    }

    @Override
    public DiscardCard _copy() {
        return new DiscardCard(player, getCardID(), cardChoice);
    }

    @Override
    public DiscardCard copy() {
        return (DiscardCard) super.copy();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Component c = gameState.getComponentById(getCardID());
        if (c == null) return "Discard a card";
        return "Discard " + c.getComponentName();
    }

    @Override
    public String toString() {
        if (getCardID() == -1) return "Discard a card";
        return "Discard card id " + getCardID();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiscardCard)) return false;
        if (!super.equals(o)) return false;
        DiscardCard that = (DiscardCard) o;
        return cardChoice == that.cardChoice;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardChoice);
    }

    @Override
    public boolean canBePlayed(TMGameState gs) {
        return true;
    }
}
