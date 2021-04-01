package games.dicemonastery;

import core.AbstractGameState;
import core.interfaces.IGameAttribute;

import java.util.function.BiFunction;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;

public enum DiceMonasteryStateAttributes implements IGameAttribute {

    GAME_ID((s, p) -> s.getGameID()),
    SEASON((s, p) -> ((DiceMonasteryTurnOrder) s.getTurnOrder()).getSeason()),
    YEAR((s, p) -> ((DiceMonasteryTurnOrder) s.getTurnOrder()).getYear()),
    TURN((s, p) -> {
        DiceMonasteryTurnOrder dmto = (DiceMonasteryTurnOrder) s.getTurnOrder();
        return (dmto.getYear() - 1) * 4 + dmto.getSeason().ordinal() + 1;
    }),
    PLAYER((s, p) -> p),
    SHILLINGS((s,p) -> s.getResource(p, DiceMonasteryConstants.Resource.SHILLINGS, STOREROOM)),
    BEER((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.BEER, STOREROOM)),
    PRE_BEER_1((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.PROTO_BEER_1, STOREROOM)),
    PRE_BEER_2((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.PROTO_BEER_2, STOREROOM)),
    MEAD((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.MEAD, STOREROOM)),
    PRE_MEAD_1((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.PROTO_MEAD_1, STOREROOM)),
    PRE_MEAD_2((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.PROTO_MEAD_2, STOREROOM)),
    GRAIN((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.GRAIN, STOREROOM)),
    BREAD((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.BREAD, STOREROOM)),
    HONEY((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.HONEY, STOREROOM)),
    BERRIES((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.BERRIES, STOREROOM)),
    WAX((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.WAX, STOREROOM)),
    CANDLE((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.CANDLE, STOREROOM)),
    CALF_SKIN((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.CALF_SKIN, STOREROOM)),
    VELLUM((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.VELLUM, STOREROOM)),
    SKEP((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.SKEP, STOREROOM)),
    WHEAT((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.GRAIN, MEADOW)),
    HIVES((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.SKEP, MEADOW)),
    DEVOTION((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.PRAYER, STOREROOM)),
    PIGMENT((s, p) -> s.getStores(p, r -> r.isPigment).size()),
    INK((s, p) -> s.getStores(p, r -> r.isInk).size()),
    PALE_BLUE_PIG((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.PALE_BLUE_PIGMENT, STOREROOM)),
    PALE_RED_PIG((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.PALE_RED_PIGMENT, STOREROOM)),
    PALE_GREEN_PIG((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.PALE_GREEN_PIGMENT, STOREROOM)),
    PALE_BLUE_INK((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.PALE_BLUE_INK, STOREROOM)),
    PALE_RED_INK((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.PALE_RED_INK, STOREROOM)),   
    PALE_GREEN_INK((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.PALE_GREEN_INK, STOREROOM)),
    VIVID_BLUE_PIG((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.VIVID_BLUE_PIGMENT, STOREROOM)),
    VIVID_RED_PIG((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.VIVID_RED_PIGMENT, STOREROOM)),
    VIVID_GREEN_PIG((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.VIVID_GREEN_PIGMENT, STOREROOM)),
    VIVID_PURPLE_PIG((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.VIVID_PURPLE_PIGMENT, STOREROOM)),
    VIVID_BLUE_INK((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.VIVID_BLUE_INK, STOREROOM)),
    VIVID_RED_INK((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.VIVID_RED_INK, STOREROOM)),
    VIVID_GREEN_INK((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.VIVID_GREEN_INK, STOREROOM)),
    VIVID_PURPLE_INK((s, p) -> s.getResource(p, DiceMonasteryConstants.Resource.VIVID_PURPLE_INK, STOREROOM)),
    MONKS_1((s,p) -> s.monksIn(null, p).stream().filter(m -> m.getPiety() == 1).count()),
    MONKS_2((s,p) -> s.monksIn(null, p).stream().filter(m -> m.getPiety() == 2).count()),
    MONKS_3((s,p) -> s.monksIn(null, p).stream().filter(m -> m.getPiety() == 3).count()),
    MONKS_4((s,p) -> s.monksIn(null, p).stream().filter(m -> m.getPiety() == 4).count()),
    MONKS_5((s,p) -> s.monksIn(null, p).stream().filter(m -> m.getPiety() == 5).count()),
    MONKS_6((s,p) -> s.monksIn(null, p).stream().filter(m -> m.getPiety() == 6).count()),
    MONKS((s,p) -> s.monksIn(null, p).size()),
    PIETY((s, p) -> s.monksIn(null, p).stream().mapToInt(Monk::getPiety).sum()),
    RETIRED((s, p) -> s.monksIn(DiceMonasteryConstants.ActionArea.RETIRED, p).size()),
    PILGRIMS((s, p) -> s.monksIn(PILGRIMAGE, p).size()),
    DEAD((s, p) -> s.monksIn(GRAVEYARD, p).size()),
    VP(DiceMonasteryGameState::getVictoryPoints)
    ;

    private final BiFunction<DiceMonasteryGameState, Integer, Object> lambda;

    DiceMonasteryStateAttributes(BiFunction<DiceMonasteryGameState, Integer, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(AbstractGameState state, int player) {
        return lambda.apply((DiceMonasteryGameState) state, player);
    }

}
