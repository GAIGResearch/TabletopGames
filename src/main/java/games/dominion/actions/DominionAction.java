package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dominion.DominionConstants.DeckType;
import games.dominion.DominionGameState;
import games.dominion.DominionGameState.DominionGamePhase;
import games.dominion.cards.CardType;

import java.util.Objects;

public abstract class DominionAction extends AbstractAction {

    public final CardType type;
    public final int player;
    protected boolean dummyAction;

    public DominionAction(CardType type, int playerId) {
        this.type = type;
        this.player = playerId;
        this.dummyAction = false;
    }

    public DominionAction(CardType type, int playerId, boolean dummy) {
        this.type = type;
        this.player = playerId;
        this.dummyAction = dummy;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        if (state.getCurrentPlayer() != player) {
            System.out.println(gs);
            throw new AssertionError("Attempting to play an action out of turn : " + this);
        }
        if (!dummyAction && state.getActionsLeft() < 1) {
            System.out.println(gs);
            throw new AssertionError("Insufficient actions to play action card " + this);
        }
        if (state.getGamePhase() != DominionGamePhase.Play) {
            System.out.println(gs);
            throw new AssertionError("Should not be able to play Action Cards unless it is the Play Phase : " + this);
        }
        if (!dummyAction) {
            state.moveCard(type, player, DeckType.HAND, player, DeckType.TABLE);
            state.changeActions(-1);  // use up one action from playing this card
        }
        executeCoreCardTypeFunctionality(state);
        return _execute(state);
    }

    /**
     * Any standard functionality parameterised directly on the CardType. This effectively means:
     * - Plus Actions/Draws/Buys/Money
     */
    void executeCoreCardTypeFunctionality(DominionGameState state) {
        state.changeActions(type.plusActions);
        for (int i = 0; i < type.plusDraws; i++)
            state.drawCard(player);
        state.changeBuys(type.plusBuys);
        state.changeAdditionalSpend(type.plusMoney);
    }

    abstract boolean _execute(DominionGameState state);

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DominionAction other) {
            return type == other.type && player == other.player && dummyAction == other.dummyAction;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, player, dummyAction);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return type.name() + " : Player " + player;
    }
}
