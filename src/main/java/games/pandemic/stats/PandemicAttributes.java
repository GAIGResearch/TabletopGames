package games.pandemic.stats;

import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.interfaces.IGameMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameListener;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import utilities.Hash;

import java.util.function.BiFunction;

import static games.pandemic.PandemicConstants.infectionHash;
import static games.pandemic.PandemicConstants.playerDeckHash;

public enum PandemicAttributes implements IGameMetric {
    PLAYER_CARDS_LEFT((l, e) -> ((Deck<Card>) ((PandemicGameState)e.state).getComponent(playerDeckHash)).getSize()),
    INFECTION_DECK_SIZE((l, e) -> ((Deck<Card>) ((PandemicGameState)e.state).getComponent(infectionHash)).getSize()),
    INFECTION_RATE_COUNTER((l, e) -> ((Counter) ((PandemicGameState)e.state).getComponent(PandemicConstants.infectionRateHash)).getValue()),
    OUTBREAK_COUNTER((l, e) -> ((Counter) ((PandemicGameState)e.state).getComponent(PandemicConstants.outbreaksHash)).getValue()),
    YELLOW_DISEASE((l, e) -> ((Counter) ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash("Disease yellow"))).getValue()),
    RED_DISEASE((l, e) -> ((Counter) ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash("Disease red"))).getValue()),
    BLUE_DISEASE((l, e) -> ((Counter) ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash("Disease blue"))).getValue()),
    BLACK_DISEASE((l, e) -> ((Counter) ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash("Disease black"))).getValue()),
    YELLOW_CUBE_COUNTER((l, e) -> ((Counter) ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash("Disease Cube yellow"))).getValue()),
    RED_CUBE_COUNTER((l, e) -> ((Counter) ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash("Disease Cube red"))).getValue()),
    BLUE_CUBE_COUNTER((l, e) -> ((Counter) ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash("Disease Cube blue"))).getValue()),
    BLACK_CUBE_COUNTER((l, e) -> ((Counter) ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash("Disease Cube black"))).getValue()),
    ;
//    ACTION_DESCRIPTION((l, e) ->  e.action == null ? "NONE" : e.action.getString(s)),;

    private final BiFunction<GameListener, Event, Object> lambda;

    PandemicAttributes(BiFunction<GameListener, Event, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda.apply(listener, event);
    }

    @Override
    public boolean listens(Event.GameEvent eventType) {
        return eventType == Event.GameEvent.ACTION_CHOSEN;
    }

    @Override
    public boolean isRecordedPerPlayer() {
        return false;
    }

}
