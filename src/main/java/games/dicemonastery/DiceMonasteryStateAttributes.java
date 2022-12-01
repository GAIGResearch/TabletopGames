package games.dicemonastery;

import core.AbstractGameState;
import core.interfaces.IGameMetric;
import evaluation.GameListener;
import evaluation.metrics.Event;
import games.dicemonastery.components.Monk;

import java.util.function.BiFunction;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;

public enum DiceMonasteryStateAttributes implements IGameMetric {

    GAME_ID((l, e)-> e.state.getGameID()),
    SEASON((l, e)-> ((DiceMonasteryTurnOrder) e.state.getTurnOrder()).getSeason()),
    SPRING((l, e)-> ((DiceMonasteryTurnOrder) e.state.getTurnOrder()).getSeason() == DiceMonasteryConstants.Season.SPRING),
    AUTUMN((l, e)-> ((DiceMonasteryTurnOrder) e.state.getTurnOrder()).getSeason() == DiceMonasteryConstants.Season.AUTUMN),
    YEAR((l, e)-> ((DiceMonasteryTurnOrder) e.state.getTurnOrder()).getYear()),
    TURN((l, e)-> {
        DiceMonasteryTurnOrder dmto = (DiceMonasteryTurnOrder) e.state.getTurnOrder();
        return (dmto.getYear() - 1) * 4 + dmto.getSeason().ordinal() + 1;
    }),
    PLAYER((l, e)-> e.playerID),
    MONKS_IN_MEADOW((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(MEADOW, e.playerID).size()),
    MONKS_IN_KITCHEN((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(KITCHEN, e.playerID).size()),
    MONKS_IN_WORKSHOP((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(WORKSHOP, e.playerID).size()),
    MONKS_IN_GATEHOUSE((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(GATEHOUSE, e.playerID).size()),
    MONKS_IN_LIBRARY((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(LIBRARY, e.playerID).size()),
    MONKS_IN_CHAPEL((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(CHAPEL, e.playerID).size()),
    AP_IN_MEADOW((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(MEADOW, e.playerID).stream().mapToDouble(Monk::getPiety).sum()),
    AP_IN_KITCHEN((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(KITCHEN, e.playerID).stream().mapToDouble(Monk::getPiety).sum()),
    AP_IN_WORKSHOP((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(WORKSHOP, e.playerID).stream().mapToDouble(Monk::getPiety).sum()),
    AP_IN_GATEHOUSE((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(GATEHOUSE, e.playerID).stream().mapToDouble(Monk::getPiety).sum()),
    AP_IN_LIBRARY((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(LIBRARY, e.playerID).stream().mapToDouble(Monk::getPiety).sum()),
    SHILLINGS((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.SHILLINGS, STOREROOM)),
    BEER((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.BEER, STOREROOM)),
    PRE_BEER_1((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.PROTO_BEER_1, STOREROOM)),
    PRE_BEER_2((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.PROTO_BEER_2, STOREROOM)),
    MEAD((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.MEAD, STOREROOM)),
    PRE_MEAD_1((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.PROTO_MEAD_1, STOREROOM)),
    PRE_MEAD_2((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.PROTO_MEAD_2, STOREROOM)),
    GRAIN((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.GRAIN, STOREROOM)),
    BREAD((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.BREAD, STOREROOM)),
    HONEY((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.HONEY, STOREROOM)),
    WAX((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.WAX, STOREROOM)),
    CANDLE((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.CANDLE, STOREROOM)),
    CALF_SKIN((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.CALF_SKIN, STOREROOM)),
    VELLUM((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.VELLUM, STOREROOM)),
    WRITING_SETS((l, e)-> Math.min(((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.VELLUM, STOREROOM),
            ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.CANDLE, STOREROOM))),
    SKEP((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.SKEP, STOREROOM)),
    WHEAT((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.GRAIN, MEADOW)),
    HIVES((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.SKEP, MEADOW)),
    DEVOTION((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.PRAYER, STOREROOM)),
    PIGMENT((l, e)-> ((DiceMonasteryGameState)e.state).getStores(e.playerID, r -> r.isPigment).size()),
    INK((l, e)-> ((DiceMonasteryGameState)e.state).getStores(e.playerID, r -> r.isInk).size()),
    PALE_BLUE_PIG((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.PALE_BLUE_PIGMENT, STOREROOM)),
    PALE_RED_PIG((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.PALE_RED_PIGMENT, STOREROOM)),
    PALE_GREEN_PIG((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.PALE_GREEN_PIGMENT, STOREROOM)),
    PALE_BLUE_INK((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.PALE_BLUE_INK, STOREROOM)),
    PALE_RED_INK((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.PALE_RED_INK, STOREROOM)),
    PALE_GREEN_INK((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.PALE_GREEN_INK, STOREROOM)),
    VIVID_BLUE_PIG((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.VIVID_BLUE_PIGMENT, STOREROOM)),
    VIVID_RED_PIG((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.VIVID_RED_PIGMENT, STOREROOM)),
    VIVID_GREEN_PIG((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.VIVID_GREEN_PIGMENT, STOREROOM)),
    VIVID_PURPLE_PIG((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.VIVID_PURPLE_PIGMENT, STOREROOM)),
    VIVID_BLUE_INK((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.VIVID_BLUE_INK, STOREROOM)),
    VIVID_RED_INK((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.VIVID_RED_INK, STOREROOM)),
    VIVID_GREEN_INK((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.VIVID_GREEN_INK, STOREROOM)),
    VIVID_PURPLE_INK((l, e)-> ((DiceMonasteryGameState)e.state).getResource(e.playerID, DiceMonasteryConstants.Resource.VIVID_PURPLE_INK, STOREROOM)),
    MONKS_1((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(null, e.playerID).stream().filter(m -> m.getPiety() == 1).count()),
    MONKS_2((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(null, e.playerID).stream().filter(m -> m.getPiety() == 2).count()),
    MONKS_3((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(null, e.playerID).stream().filter(m -> m.getPiety() == 3).count()),
    MONKS_4((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(null, e.playerID).stream().filter(m -> m.getPiety() == 4).count()),
    MONKS_5((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(null, e.playerID).stream().filter(m -> m.getPiety() == 5).count()),
    MONKS_6((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(null, e.playerID).stream().filter(m -> m.getPiety() == 6).count()),
    MONKS((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(null, e.playerID).size()),
    PIETY((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(null, e.playerID).stream().mapToInt(Monk::getPiety).sum()),
    RETIRED((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(DiceMonasteryConstants.ActionArea.RETIRED, e.playerID).size()),
    PILGRIMS((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(PILGRIMAGE, e.playerID).size()),
    DEAD((l, e)-> ((DiceMonasteryGameState)e.state).monksIn(GRAVEYARD, e.playerID).size()),
    VP((l, e) -> ((DiceMonasteryGameState)e.state).getVictoryPoints(e.playerID)),
    TREASURE((l, e)-> ((DiceMonasteryGameState)e.state).getTreasures(e.playerID).stream().mapToInt(t -> t.vp).sum());

    private final BiFunction<GameListener, Event, Object> lambda;

    DiceMonasteryStateAttributes(BiFunction<GameListener, Event, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda.apply(listener, event);
    }

}
