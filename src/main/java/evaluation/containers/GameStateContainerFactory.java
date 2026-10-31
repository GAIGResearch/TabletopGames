package evaluation.containers;

import core.AbstractGameState;
import core.AbstractGameStateContainer;
import games.GameType;

import games.cantstop.CantStopGameState;
import games.cantstop.CantStopGameStateContainer;
import games.catan.CatanGameState;
import games.catan.CatanGameStateContainer;
import games.connect4.Connect4GameState;
import games.connect4.Connect4GameStateContainer;
import games.dominion.DominionGameState;
import games.dominion.DominionGameStateContainer;
import games.dotsboxes.DBGameState;
import games.dotsboxes.DBGameStateContainer;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.SSPGameStateContainer;
import games.uno.UnoGameState;
import games.uno.UnoGameStateContainer;
import games.wonders7.Wonders7GameState;
import games.wonders7.Wonders7GameStateContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Factory class to convert AbstractGameState to its corresponding AbstractGameStateContainer.
 * Not all game states have a corresponding container - an UnsupportedOperationException is thrown
 * if no container exists for the given game state.
 */
public class GameStateContainerFactory {

    private static final Map<GameType, Function<AbstractGameState, AbstractGameStateContainer>> containerFactories = new HashMap<>();

    static {
        containerFactories.put(GameType.CantStop, gs -> new CantStopGameStateContainer((CantStopGameState) gs));
        containerFactories.put(GameType.Catan, gs -> new CatanGameStateContainer((CatanGameState) gs));
        containerFactories.put(GameType.Connect4, gs -> new Connect4GameStateContainer((Connect4GameState) gs));
        containerFactories.put(GameType.Dominion, gs -> new DominionGameStateContainer((DominionGameState) gs));
        containerFactories.put(GameType.DominionSizeDistortion, gs -> new DominionGameStateContainer((DominionGameState) gs));
        containerFactories.put(GameType.DominionImprovements, gs -> new DominionGameStateContainer((DominionGameState) gs));
        containerFactories.put(GameType.DotsAndBoxes, gs -> new DBGameStateContainer((DBGameState) gs));
        containerFactories.put(GameType.SeaSaltPaper, gs -> new SSPGameStateContainer((SeaSaltPaperGameState) gs));
        containerFactories.put(GameType.Uno, gs -> new UnoGameStateContainer((UnoGameState) gs));
        containerFactories.put(GameType.Wonders7, gs -> new Wonders7GameStateContainer((Wonders7GameState) gs));
    }

    /**
     * Creates an AbstractGameStateContainer from the given AbstractGameState.
     *
     * @param gs the game state to convert
     * @return the corresponding container
     * @throws UnsupportedOperationException if no container exists for this game type
     */
    public static AbstractGameStateContainer createContainer(AbstractGameState gs) {
        GameType gameType = gs.getGameType();
        Function<AbstractGameState, AbstractGameStateContainer> factory = containerFactories.get(gameType);
        
        if (factory == null) {
            throw new UnsupportedOperationException(
                "No AbstractGameStateContainer available for game type: " + gameType.name() + 
                ". Not all games have a corresponding container implementation.");
        }
        
        return factory.apply(gs);
    }

    /**
     * Checks if a container exists for the given game type.
     *
     * @param gameType the game type to check
     * @return true if a container exists, false otherwise
     */
    public static boolean hasContainer(GameType gameType) {
        return containerFactories.containsKey(gameType);
    }

    /**
     * Checks if a container exists for the given game state.
     *
     * @param gs the game state to check
     * @return true if a container exists, false otherwise
     */
    public static boolean hasContainer(AbstractGameState gs) {
        return hasContainer(gs.getGameType());
    }
}
