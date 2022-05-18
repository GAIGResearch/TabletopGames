package games.pandemic;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.interfaces.IGameListener;
import core.interfaces.IStatisticLogger;
import utilities.Hash;

import java.util.LinkedHashMap;
import java.util.Map;

import static games.pandemic.PandemicConstants.infectionHash;
import static games.pandemic.PandemicConstants.playerDeckHash;

public class PandemicListener implements IGameListener {
    IStatisticLogger logger;
    public PandemicListener(IStatisticLogger logger) {
        this.logger = logger;
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        if (type == CoreConstants.GameEvents.GAME_OVER) {
            PandemicGameState state = (PandemicGameState) game.getGameState();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("Game", game.getGameType().name());
            data.put("GameID", game.getGameState().getGameID());
            data.put("Seed", game.getGameState().getGameParameters().getRandomSeed());
            data.put("Players", state.getNPlayers());
            data.put("PlayerType", game.getPlayers().get(0).toString());
            data.put("Rounds", state.getTurnOrder().getRoundCounter());
            data.put("Turns", state.getTurnOrder().getTurnCounter());
            data.put("Ticks", game.getTick());
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

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
        // nothing
    }

    @Override
    public void allGamesFinished() {
        logger.processDataAndFinish();
    }
}


