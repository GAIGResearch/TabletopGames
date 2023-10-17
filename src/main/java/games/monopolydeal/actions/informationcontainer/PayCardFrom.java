package games.monopolydeal.actions.informationcontainer;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.actions.BoardType;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.SetType;

import java.util.Objects;

/**
 * <p>A simple action which does not execute any command but acts as an information container for other EAS.</p>
 */
public class PayCardFrom extends AbstractAction {

    public final CardType cardType;
    public SetType from;
    public BoardType type;

    public PayCardFrom(CardType cardType, SetType from){
        this.cardType = cardType;
        this.from = from;
        this.type = BoardType.PropertySet;
    }
    public PayCardFrom(CardType cardType){
        this.cardType = cardType;
        this.type = BoardType.Bank;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }
    @Override
    public PayCardFrom copy() {
        PayCardFrom action = new PayCardFrom(cardType);
        action.type = type;
        action.from = from;
        return action;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PayCardFrom that = (PayCardFrom) o;
        return Objects.equals(cardType, that.cardType) && from == that.from && type == that.type;
    }
    @Override
    public int hashCode() {
        return Objects.hash(cardType, from, type);
    }
    @Override
    public String toString() {
        return "Pay with " + cardType;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
