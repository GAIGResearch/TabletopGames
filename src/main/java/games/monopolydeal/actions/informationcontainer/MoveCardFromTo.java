package games.monopolydeal.actions.informationcontainer;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.SetType;

import java.util.Objects;

/**
 * <p>A simple action which does not execute any command but acts as an information container for other EAS.</p>
 */
public class MoveCardFromTo extends AbstractAction {
    final int player;
    public final SetType from,to;
    public final CardType cardType;

    public MoveCardFromTo(int playerId, CardType cardType, SetType from, SetType to){
        player = playerId;
        this.cardType = cardType;
        this.from = from;
        this.to = to;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }
    @Override
    public MoveCardFromTo copy() {
        return this;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MoveCardFromTo that = (MoveCardFromTo) o;
        return player == that.player && from == that.from && to == that.to && cardType == that.cardType;
    }
    @Override
    public int hashCode() {
        return Objects.hash(player, from, to, cardType.ordinal());
    }
    @Override
    public String toString() {
        return "Move "+ cardType.toString() + " from " + from.toString() + " to " + to.toString();
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
