package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.dicemonastery.DiceMonasteryConstants.ActionArea;
import games.dicemonastery.DiceMonasteryConstants.BONUS_TOKEN;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.components.Monk;

import java.util.List;

import static games.dicemonastery.DiceMonasteryConstants.Resource.PRAYER;
import static games.dicemonastery.DiceMonasteryConstants.Resource.SHILLINGS;
import static java.util.stream.Collectors.toList;

public class TakeToken extends AbstractAction implements IExtendedSequence {

    public final BONUS_TOKEN token;
    public final ActionArea fromArea;
    public final int player;
    boolean monkPromoted;

    public TakeToken(BONUS_TOKEN token, ActionArea area, int player) {
        this.token = token;
        this.fromArea = area;
        this.player = player;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        switch (token) {
            case DEVOTION:
                state.addResource(player, PRAYER, 1);
                break;
            case PRESTIGE:
                state.addVP(1, player);
                break;
            case DONATION:
                state.addResource(player, SHILLINGS, 3);
                break;
            case PROMOTION:
                state.setActionInProgress(this);
                break;
            default:
                throw new AssertionError("Unknown Token type: " + token);
        }
        state.removeToken(token, fromArea);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        return state.monksIn(fromArea, player).stream()
                .mapToInt(Monk::getPiety)
                .distinct()
                .mapToObj(piety -> new PromoteMonk(piety, fromArea))
                .collect(toList());
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState gs, AbstractAction action) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        if (action instanceof PromoteMonk) {
            monkPromoted = true;
            if (((PromoteMonk) action).pietyLevelToPromote < 6)
                state.addActionPoints(1);
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return monkPromoted;
    }

    @Override
    public TakeToken copy() {
        if (token == BONUS_TOKEN.PROMOTION) {
            TakeToken retValue = new TakeToken(token, fromArea, player);
            retValue.monkPromoted = monkPromoted;
            return retValue;
        } else {
            // in this case the Action is immutable
            return this;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TakeToken) {
            TakeToken other = (TakeToken) obj;
            return other.monkPromoted == monkPromoted && other.token == token && other.fromArea == fromArea;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // deliberately exclude fromArea
        return token.ordinal() * -3163 + (monkPromoted ? 3559 : 877);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Takes %s token", token);
    }
}
