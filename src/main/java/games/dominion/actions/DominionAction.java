package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dominion.DominionConstants.DeckType;
import games.dominion.DominionGameState;
import games.dominion.DominionGameState.DominionGamePhase;
import games.dominion.cards.CardType;

import java.util.Objects;

public abstract class DominionAction extends AbstractAction {

    protected final CardType type;
    protected final int player;

    public DominionAction(CardType type, int playerId) {
        this.type = type;
        this.player = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        if (state.getCurrentPlayer() != player) {
            System.out.println(gs);
            throw new AssertionError("Attempting to play an action out of turn : " + this);
        }
        if (state.actionsLeft() < 1) {
            System.out.println(gs);
            throw new AssertionError("Insufficient actions to play action card " + this);
        }
        if (state.getGamePhase() != DominionGamePhase.Play) {
            System.out.println(gs);
            throw new AssertionError("Should not be able to play Action Cards unless it is the Play Phase : " + this);
        }
        if (!state.moveCard(type, player, DeckType.HAND, player, DeckType.TABLE)) {
            System.out.println(gs);
            throw new AssertionError(String.format("Moving %s card from HAND to TABLE failed for player %d", type, player));
        }
        state.changeActions(-1);  // use up one action from playing this card
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
        if (obj instanceof DominionAction) {
            DominionAction other = (DominionAction) obj;
            return type == other.type && player == other.player;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, player);
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
