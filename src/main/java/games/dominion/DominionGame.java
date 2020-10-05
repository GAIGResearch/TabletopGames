package games.dominion;

import core.*;
import games.GameType;

import java.util.List;

public class DominionGame extends Game {
    /**
     * Game constructor. Receives a list of players, a forward model and a game state. Sets unique and final
     * IDs to all players in the game, and performs initialisation of the game state and forward model objects.
     *
     * @param type
     * @param players   - players taking part in this game.
     * @param realModel - forward model used to apply game rules.
     * @param gameState - object used to track the state of the game in a moment in time.
     */
    public DominionGame(GameType type, List<AbstractPlayer> players, AbstractForwardModel realModel, AbstractGameState gameState) {
        super(type, players, realModel, gameState);
    }

    /**
     * Game constructor. Receives a forward model and a game state.
     * Performs initialisation of the game state and forward model objects.
     *
     * @param type
     * @param model     - forward model used to apply game rules.
     * @param gameState - object used to track the state of the game in a moment in time.
     */
    public DominionGame(GameType type, AbstractForwardModel model, AbstractGameState gameState) {
        super(type, model, gameState);
    }
}
