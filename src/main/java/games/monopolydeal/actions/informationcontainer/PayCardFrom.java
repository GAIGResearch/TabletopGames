package games.monopolydeal.actions.informationcontainer;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.monopolydeal.cards.BoardType;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.SetType;

import java.util.Objects;

/**
 * <p>A simple action which does not execute any command but acts as an information container for other EAS.</p>
 */
public class PayCardFrom extends AbstractAction {

    public final CardType cardType;
    public final SetType from;
    public final BoardType type;

    public PayCardFrom(CardType cardType, SetType from){
        this.cardType = cardType;
        this.from = from;
        this.type = BoardType.PropertySet;
    }
    public PayCardFrom(CardType cardType){
        this.cardType = cardType;
        this.from = null;
        this.type = BoardType.Bank;
    }

    public PayCardFrom(CardType cardType, SetType from, BoardType type){
        this.cardType = cardType;
        this.from = from;
        this.type = type;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }

    @Override
    public PayCardFrom copy() {
        return this;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PayCardFrom that = (PayCardFrom) o;
        return cardType == that.cardType && from == that.from && type == that.type;
    }
    @Override
    public int hashCode() {
        return Objects.hash(cardType.ordinal(), from, type);
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
