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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class SellProjects extends TMAction implements IExtendedSequence {
    HashSet<Integer> cardIDsSold;
    boolean complete;

    public SellProjects() { super(); } // This is needed for JSON Deserializer

    public SellProjects(int player) {
        super(player, false);
        actionType = TMTypes.ActionType.StandardProject;
        cardIDsSold = new HashSet<>();
    }
    public SellProjects(int player, int cardID) {
        super(player, true);
        setCardID(cardID);
    }

    @Override
    public boolean _execute(TMGameState gs) {
        if (getCardID() != -1) {
            TMGameParameters gp = (TMGameParameters) gs.getGameParameters();
            int currentMC = gs.getPlayerResources()[player].get(TMTypes.Resource.MegaCredit).getValue();
            TMCard card = (TMCard) gs.getComponentById(getCardID());
            if (card != null) {
                gs.getDiscardCards().add(card);
                gs.getPlayerHands()[player].remove(card);
            }
            gs.getPlayerResources()[player].get(TMTypes.Resource.MegaCredit).setValue(currentMC + 1);
            return true;
        }
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        // Choose which card in hand to sell
        List<AbstractAction> actions = new ArrayList<>();
        TMGameState gs = (TMGameState) state;
        for (int i = 0; i < gs.getPlayerHands()[player].getSize(); i++) {
            int id = gs.getPlayerHands()[player].get(i).getComponentID();
            if (!cardIDsSold.contains(id)) {
                actions.add(new SellProjects(player, id));
            }
        }
        if (!cardIDsSold.isEmpty()) {
            actions.add(new TMAction(player));  // Pass to stop the action, unless you haven't picked any card yet
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (((TMAction)action).pass) complete = true;
        else cardIDsSold.add(((SellProjects)action).getCardID());
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public SellProjects _copy() {
        SellProjects copy = new SellProjects(player, getCardID());
        copy.complete = complete;
        if (cardIDsSold != null) {
            copy.cardIDsSold = new HashSet<>(cardIDsSold);
        }
        return copy;
    }

    @Override
    public SellProjects copy() {
        return (SellProjects) super.copy();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if (getCardID() == -1) return "Sell projects";
        Component c = gameState.getComponentById(getCardID());
        return "Sell " + c.getComponentName();
    }

    @Override
    public String toString() {
        if (getCardID() == -1) return "Sell projects";
        return "Sell card id " + getCardID();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SellProjects)) return false;
        if (!super.equals(o)) return false;
        SellProjects that = (SellProjects) o;
        return complete == that.complete && Objects.equals(cardIDsSold, that.cardIDsSold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardIDsSold, complete);
    }
}
