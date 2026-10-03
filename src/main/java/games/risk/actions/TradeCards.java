package games.risk.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.risk.RiskGameState;
import games.risk.RiskParameters;
import games.risk.RiskTerritory;
import games.risk.RiskUtils;
import games.risk.components.RiskCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The current player trades in a set of 3 RISK cards from their hand for new armies to place. The k-th set traded
 * in the game (by anyone) is worth RiskParameters.tradeValue(k). If bonusTerritory is not null - a territory shown on
 * one of the cards that the player holds - territoryBonus extra armies go straight onto it (once per turn).
 */
public class TradeCards extends AbstractAction {

    /** the 3 cards, in the canonical order of RiskUtils.CARD_ORDER */
    public final List<RiskCard> cards;
    /** null when no bonus is taken */
    public final RiskTerritory bonusTerritory;

    public TradeCards(List<RiskCard> cards, RiskTerritory bonusTerritory) {
        this.cards = List.copyOf(cards);
        this.bonusTerritory = bonusTerritory;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RiskGameState state = (RiskGameState) gs;
        RiskParameters params = (RiskParameters) state.getGameParameters();
        int player = state.getCurrentPlayer();
        for (RiskCard card : cards) {
            state.getHand(player).remove(card);
            state.getDiscardPile().add(card);
        }
        state.setNSetsTraded(state.getNSetsTraded() + 1);
        state.setArmiesToPlace(player, state.getArmiesToPlace(player) + params.tradeValue(state.getNSetsTraded()));
        if (bonusTerritory != null) {
            // armies beyond maxArmiesPerTerritory are lost
            state.addArmies(bonusTerritory, Math.min(params.territoryBonus, state.getRoom(bonusTerritory)));
            state.setTerritoryBonusTaken(true);
        }
        return true;
    }

    /**
     * The trades open to the player.
     */
    public static List<AbstractAction> options(RiskGameState state, int player) {
        List<AbstractAction> actions = new ArrayList<>();
        for (List<RiskCard> set : RiskUtils.sets(state.getHand(player).getComponents())) {
            // each set, with a choice of bonus territory among its cards that show territories the player holds;
            // the bonus is null if they have had it this turn or hold none of them
            List<RiskTerritory> held = new ArrayList<>();
            if (!state.isTerritoryBonusTaken())
                for (RiskCard c : set)
                    if (!c.isWild() && state.getOwner(c.territory) == player)
                        held.add(c.territory);
            if (held.isEmpty())
                actions.add(new TradeCards(set, null));
            for (RiskTerritory t : held)
                actions.add(new TradeCards(set, t));
        }
        return actions;
    }

    @Override
    public TradeCards copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TradeCards other && other.cards.equals(cards)
                && Objects.equals(other.bonusTerritory, bonusTerritory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cards, bonusTerritory) + 731219;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "TradeCards(" + cards + (bonusTerritory == null ? "" : ", bonus " + bonusTerritory) + ")";
    }
}
