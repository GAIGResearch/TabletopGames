package games.pickomino;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import games.GameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>The game state encapsulates all game information. It is a data-only class, with game functionality present
 * in the Forward Model or actions modifying the state of the game.</p>
 * <p>Most variables held here should be {@link Component} subclasses as much as possible.</p>
 * <p>No initialisation or game logic should be included here (not in the constructor either). This is all handled externally.</p>
 * <p>Computation may be included in functions here for ease of access, but only if this is querying the game state information.
 * Functions on the game state should never <b>change</b> the state of the game.</p>
 */
public class PickominoGameState extends AbstractGameState {

    // Sorted list of remaining tiles
    List<PickominoTile> remainingTiles;
    // Tile stacks for each player
    List<Deck<PickominoTile>> playerTiles;
    // Number of dices that have already been assigned:
    // assignedDices[0] is the number of "1" assigned, assignedDices[1] is the number of "2" assigned, etc.
    int[] assignedDices = new int[6];
    // Dices that have just been rolled but not yet assigned
    int[] currentRoll = new int[6];
    // Number of dices that have not been assigned yet
    int remainingDices;

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players in the game
     */
    public PickominoGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    /**
     * @return the enum value corresponding to this game, declared in {@link GameType}.
     */
    @Override
    protected GameType _getGameType() {
        return GameType.Pickomino;
    }

    /**
     * Returns all Components used in the game and referred to by componentId from actions or rules.
     * This method is called after initialising the game state, so all components will be initialised already.
     *
     * @return - List of Components in the game.
     */
    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.addAll(remainingTiles);
        components.addAll(playerTiles);
        return components;
    }

    /**
     * Create a deep copy of the game state containing only those components the given player can observe.
     * In the case of Pickomino, the game state is fully observable.
     *
     * @param playerId - player observing this game state.
     */
    @Override
    protected PickominoGameState _copy(int playerId) {
        PickominoGameState copy = new PickominoGameState(gameParameters, getNPlayers());
        copy.remainingTiles = new ArrayList<>(remainingTiles);
        copy.playerTiles = playerTiles.stream().map(Deck::copy).collect(Collectors.toList());
        copy.assignedDices = assignedDices.clone();
        copy.currentRoll = currentRoll.clone();
        copy.remainingDices = remainingDices;
        return copy;
    }

    /**
     * @param playerId - player observing the state.
     * @return a score for the given player approximating how well they are doing (e.g. how close they are to winning
     * the game); a value between 0 and 1 is preferred, where 0 means the game was lost, and 1 means the game was won.
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        return getGameScore(playerId) / ((PickominoParameters) gameParameters).getMaxScore();
    }

    /**
     * @param playerId - player observing the state.
     * @return the true score for the player, according to the game rules. May be 0 if there is no score in the game.
     */
    @Override
    public double getGameScore(int playerId) {
        int score = 0;
        for (PickominoTile tile : playerTiles.get(playerId)) {
            score += tile.getScore();
        }
        return (double) score;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PickominoGameState that = (PickominoGameState) o;

        if(!super.equals(o)) return false;

        // Compare remainingTiles
        if (!this.remainingTiles.equals(that.remainingTiles)) return false;

        // Compare playerTiles (list of Decks)
        if (this.playerTiles.size() != that.playerTiles.size()) return false;
        for (int i = 0; i < this.playerTiles.size(); i++) {
            if (!this.playerTiles.get(i).equals(that.playerTiles.get(i))) return false;
        }

        // Compare assignedDices
        if (this.assignedDices.length != that.assignedDices.length) return false;
        for (int i = 0; i < this.assignedDices.length; i++) {
            if (this.assignedDices[i] != that.assignedDices[i]) return false;
        }

        // Compare currentRoll
        if (this.currentRoll.length != that.currentRoll.length) return false;
        for (int i = 0; i < this.currentRoll.length; i++) {
            if (this.currentRoll[i] != that.currentRoll[i]) return false;
        }

        // Compare remainingDices
        if (this.remainingDices != that.remainingDices) return false;

        // If all variables are equal, return true
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + remainingTiles.hashCode();
        result = 31 * result + playerTiles.hashCode();
        result = 31 * result + Arrays.hashCode(assignedDices);
        result = 31 * result + Arrays.hashCode(currentRoll);
        result = 31 * result + remainingDices;
        return result;
    }

    public double getTiebreak(int playerId, int tier){
        // Winning player has the tile with the highest value
        int maxPickomino = 0;
        for(PickominoTile tile : playerTiles.get(playerId)){
            if(tile.getValue() > maxPickomino) maxPickomino = tile.getValue();
        }
        return (double) maxPickomino;
    }

}

