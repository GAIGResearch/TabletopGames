package games.monopolydeal.actions.informationcontainer;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.actions.BoardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.SetType;

import java.util.Objects;

/**
 * <p>A simple action which does not execute any command but acts as an information container for other EAS.</p>
 */
public class PayCardFrom extends AbstractAction {

    public final MonopolyDealCard card;
    public SetType from;
    public BoardType type;

    public PayCardFrom(MonopolyDealCard card, SetType from){
        this.card = card;
        this.from = from;
        this.type = BoardType.PropertySet;
    }
    public PayCardFrom(MonopolyDealCard card){
        this.card = card;
        this.type = BoardType.Bank;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }
    @Override
    public PayCardFrom copy() {
        PayCardFrom action = new PayCardFrom(card);
        action.type = type;
        action.from = from;
        return action;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PayCardFrom that = (PayCardFrom) o;
        return Objects.equals(card, that.card) && from == that.from && type == that.type;
    }
    @Override
    public int hashCode() {
        return Objects.hash(card, from, type);
    }
    @Override
    public String toString() {
        return "Pay with " + card;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
