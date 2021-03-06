package games.dicemonastery;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionAttribute;
import games.dicemonastery.actions.*;

import java.util.function.BiFunction;

public enum DiceMonasteryActionAttributes implements IActionAttribute<Object> {

    GAME_ID((s, a) -> s.getGameID()),
    SEASON((s, a) -> ((DiceMonasteryTurnOrder) s.getTurnOrder()).getSeason()),
    YEAR((s, a) -> ((DiceMonasteryTurnOrder) s.getTurnOrder()).getYear()),
    PLAYER((s, a) -> s.getCurrentPlayer()),
    ACTION_TYPE((s, a) -> a == null ? "NONE" : a.getClass().getSimpleName()),
    ACTION_DESCRIPTION((s, a) -> a == null ? "NONE" : a.getString(s)),
    PIETY((s, a) -> {
        if (a == null) return 0;
        if (a instanceof ChooseMonk) return ((ChooseMonk) a).piety;
        if (a instanceof PromoteMonk) return ((PromoteMonk) a).pietyLevelToPromote;
        return 0;
    }),
    LOCATION((s, a) -> {
        if (a == null) return "";
        if (a instanceof PlaceMonk) return ((PlaceMonk) a).destination.name();
        if (a instanceof ChooseMonk) return ((ChooseMonk) a).destination.name();
        if (a instanceof PromoteMonk) return ((PromoteMonk) a).location.name();
        return "";
    }),
    RESOURCE((s, a) -> {
        if (a == null) return "";
        if (a instanceof Buy) return ((Buy) a).resource.name();
        if (a instanceof Sell) return ((Sell) a).resource.name();
        return "";
    }),
    PRICE((s, a) -> {
        if (a == null) return 0;
        if (a instanceof Buy) return ((Buy) a).cost;
        if (a instanceof Sell) return ((Sell) a).price;
        return 0;
    }),
    REWARD((s, a) -> {
        if (a == null) return false;
        if (a instanceof PromoteMonk) return ((PromoteMonk) a).areaReward;
        if (a instanceof GainVictoryPoints) return  ((GainVictoryPoints) a).asReward;
        return false;
    }),
    ACTIONS_LEFT((s, a) -> ((DiceMonasteryTurnOrder)s.getTurnOrder()).getActionPointsLeft())
    ;

    private final BiFunction<DiceMonasteryGameState, AbstractAction, Object> lambda;

    DiceMonasteryActionAttributes(BiFunction<DiceMonasteryGameState, AbstractAction, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(AbstractGameState state, AbstractAction action) {
        return lambda.apply((DiceMonasteryGameState) state, action);
    }

}
