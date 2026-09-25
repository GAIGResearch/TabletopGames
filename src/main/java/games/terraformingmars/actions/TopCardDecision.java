package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.components.TMCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TopCardDecision extends TMAction implements IExtendedSequence {
    int nCardsKept;

    public int nCardsLook;
    public int nCardsKeep;
    public boolean buy;

    public TopCardDecision() { super(); } // This is needed for JSON Deserializer

    public TopCardDecision(int nCardsLook, int nCardsKeep, boolean buy) {
        super(-1, true);
        this.nCardsKeep = nCardsKeep;
        this.nCardsLook = nCardsLook;
        this.buy = buy;
    }

    @Override
    public boolean _execute(TMGameState gameState) {
        for (int i = 0; i < nCardsLook; i++) {
            TMCard c = gameState.drawCard();
            if (c != null) {
                gameState.getPlayerCardChoice()[player].add(c);
            } else {
                nCardsLook = i;
                break;
            }
        }
        nCardsKept = 0;
        gameState.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        // for first card in the card choice, buy it or discard it
        List<AbstractAction> actions = new ArrayList<>();
        TMGameState gs = (TMGameState) state;
        int cardId = gs.getPlayerCardChoice()[player].get(0).getComponentID();
        if (nCardsLook == 1 || nCardsKept < nCardsKeep) {
            TMAction keep;
            if (buy) {
                int cost = ((TMGameParameters)gs.getGameParameters()).getProjectPurchaseCost();
                keep = new PayForAction(player, new BuyCard(player, cardId, cost));
            } else {
                keep = new BuyCard(player, cardId, 0);
            }
            if (keep.canBePlayed(gs)) actions.add(keep);  // player may not be able to afford it
        }
        // Can only discard while enough cards remain to still keep the required number
        int cardsRemaining = gs.getPlayerCardChoice()[player].getSize();
        if (actions.isEmpty() || nCardsLook == 1 || cardsRemaining > nCardsKeep - nCardsKept) {
            actions.add(new DiscardCard(player, cardId, true));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof BuyCard) nCardsKept++;

//        if (nCardsKept == nCardsKeep && stage != nCardsLook) {
//            TMGameState gs = (TMGameState) state;
//            // Discard the rest
//            for (TMCard card: gs.getPlayerCardChoice()[player].getComponents()) {
//                new DiscardCard(player, card.getComponentID(), true).execute(gs);
//            }
//        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return ((TMGameState)state).getPlayerCardChoice()[player].getSize() == 0;
    }

    @Override
    public TopCardDecision _copy() {
        TopCardDecision copy = new TopCardDecision(nCardsLook, nCardsKeep, buy);
        copy.nCardsKept = nCardsKept;
        return copy;
    }

    @Override
    public TopCardDecision copy() {
        return (TopCardDecision) super.copy();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TopCardDecision)) return false;
        if (!super.equals(o)) return false;
        TopCardDecision that = (TopCardDecision) o;
        return nCardsKept == that.nCardsKept && nCardsLook == that.nCardsLook && nCardsKeep == that.nCardsKeep && buy == that.buy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nCardsKept, nCardsLook, nCardsKeep, buy);
    }

    @Override
    public String toString() {
        String text = "Look at the top " + (nCardsLook > 1? nCardsLook + " cards." : "card.");
        if (buy) {
            text += " Buy " + (nCardsKeep > 1? nCardsKeep : "it") + " or discard all.";
        } else {
            text += " Take " + nCardsKeep + " of them into your hand and discard the rest.";
        }
        return text;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
