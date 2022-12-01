package games.dicemonastery;

import core.interfaces.IGameMetric;
import evaluation.GameListener;
import evaluation.metrics.Event;
import games.dicemonastery.actions.*;

import java.util.function.BiFunction;

public enum DiceMonasteryActionAttributes implements IGameMetric {

    GAME_ID((l, e) -> e.state.getGameID()),
    SEASON((l, e) -> ((DiceMonasteryTurnOrder) e.state.getTurnOrder()).getSeason()),
    YEAR((l, e) -> ((DiceMonasteryTurnOrder) e.state.getTurnOrder()).getYear()),
    PLAYER((l, e) -> e.state.getCurrentPlayer()),
    ACTION_TYPE((l, e) -> e.action == null ? "NONE" : e.action.getClass().getSimpleName()),
    ACTION_DESCRIPTION((l, e) -> e.action == null ? "NONE" : e.action.getString(e.state)),
    PIETY((l, e) -> {
        if (e.action == null) return 0;
        if (e.action instanceof ChooseMonk) return ((ChooseMonk) e.action).piety;
        if (e.action instanceof PromoteMonk) return ((PromoteMonk) e.action).pietyLevelToPromote;
        return 0;
    }),
    LOCATION((l, e) -> {
        if (e.action instanceof PlaceMonk) return ((PlaceMonk) e.action).destination.name();
        if (e.action instanceof ChooseMonk) return ((ChooseMonk) e.action).destination.name();
        if (e.action instanceof PromoteMonk) return ((PromoteMonk) e.action).location.name();
        return ((DiceMonasteryTurnOrder) e.state.getTurnOrder()).currentAreaBeingExecuted.name();
    }),
    THING((l, e) -> {
        if (e.action == null) return "";
        if (e.action instanceof Buy) return ((Buy) e.action).resource.name();
        if (e.action instanceof Sell) return ((Sell) e.action).resource.name();
        if (e.action instanceof BuyTreasure) return ((BuyTreasure) e.action).treasure.getComponentName();
        if (e.action instanceof WriteText) return ((WriteText) e.action).textType.getComponentName();
        if (e.action instanceof TakeToken) return ((TakeToken) e.action).token.name();
        if (e.action instanceof GoOnPilgrimage) return ((GoOnPilgrimage) e.action).destination.destination;
        return "";
    }),
    VALUE((l, e) -> {
        if (e.action == null) return 0;
        if (e.action instanceof Buy) return ((Buy) e.action).cost;
        if (e.action instanceof Sell) return ((Sell) e.action).price;
        if (e.action instanceof SummerBid)
            return ((SummerBid) e.action).beer + 2 * ((SummerBid) e.action).mead;
        if (e.action instanceof Pray) return ((Pray) e.action).prayerCount;
        if (e.action instanceof TakeToken)
            return 2 - ((DiceMonasteryGameState)e.state).availableBonusTokens(((TakeToken) e.action).fromArea).size();
        if (e.action instanceof UseMonk) return ((UseMonk) e.action).getActionPoints();
        return 0;
    }),
    ACTIONS_LEFT((l, e) -> ((DiceMonasteryTurnOrder) e.state.getTurnOrder()).getActionPointsLeft())
    ;

    private final BiFunction<GameListener, Event, Object> lambda;

    DiceMonasteryActionAttributes(BiFunction<GameListener, Event, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda.apply(listener, event);
    }

}
