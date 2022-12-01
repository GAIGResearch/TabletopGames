package games.pandemic;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import evaluation.GameListener;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.Event;
import utilities.Hash;

import java.util.LinkedHashMap;
import java.util.Map;

import static games.pandemic.PandemicConstants.infectionHash;
import static games.pandemic.PandemicConstants.playerDeckHash;

public class PandemicListener extends GameListener {

    public PandemicListener(IStatisticLogger logger) {
        super(logger, null);
    }

    @Override
    public void onEvent(Event event)
    {
        if(event.type == Event.GameEvent.GAME_OVER) {
            PandemicGameState state = (PandemicGameState) event.state;
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("Game", state.getGameType().name());
            data.put("GameID", state.getGameID());
            data.put("Seed", state.getGameParameters().getRandomSeed());
            data.put("Players", state.getNPlayers());
            //data.put("PlayerType", state.getPlayers().get(0).toString());
            data.put("Rounds", state.getTurnOrder().getRoundCounter());
            data.put("Turns", state.getTurnOrder().getTurnCounter());
            data.put("Ticks", state.getGameTick());
            data.put("GameStatus", state.getGameStatus());
            data.put("playerCardsLeft", ((Deck<Card>) state.getComponent(playerDeckHash)).getSize());
            data.put("infectionDeckSize", ((Deck<Card>) state.getComponent(infectionHash)).getSize());
            data.put("infectionRateCounter", ((Counter) state.getComponent(PandemicConstants.infectionRateHash)).getValue());
            data.put("outbreakCounter", ((Counter) state.getComponent(PandemicConstants.outbreaksHash)).getValue());
            data.put("yellowDisease", ((Counter) state.getComponent(Hash.GetInstance().hash("Disease yellow"))).getValue());
            data.put("redDisease", ((Counter) state.getComponent(Hash.GetInstance().hash("Disease red"))).getValue());
            data.put("blueDisease", ((Counter) state.getComponent(Hash.GetInstance().hash("Disease blue"))).getValue());
            data.put("blackDisease", ((Counter) state.getComponent(Hash.GetInstance().hash("Disease black"))).getValue());
            data.put("yellowCubeCounter", ((Counter) state.getComponent(Hash.GetInstance().hash("Disease Cube yellow"))).getValue());
            data.put("redCubeCounter", ((Counter) state.getComponent(Hash.GetInstance().hash("Disease Cube red"))).getValue());
            data.put("blueCubeCounter", ((Counter) state.getComponent(Hash.GetInstance().hash("Disease Cube blue"))).getValue());
            data.put("blackCubeCounter", ((Counter) state.getComponent(Hash.GetInstance().hash("Disease Cube black"))).getValue());
            logger.record(data);
        }
    }

}


