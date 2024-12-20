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
public class ChooseCardFrom extends AbstractAction {

    public final CardType take;
    public final SetType from;
    final int actionType;

    public ChooseCardFrom(CardType take, SetType from, int actionType){
        this.take = take;
        this.from = from;
        this.actionType = actionType;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }

    @Override
    public ChooseCardFrom copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ChooseCardFrom that = (ChooseCardFrom) o;
        return actionType == that.actionType && take == that.take && from == that.from;
    }

    @Override
    public int hashCode() {
        return Objects.hash(take, from, actionType);
    }

    @Override
    public String toString() {
        if(actionType == 0)
            return "Take card: " + take.toString();
        else return "Give card: " + take.toString();
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
