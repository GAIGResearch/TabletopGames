package games.serveTheKing;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import games.GameType;
import games.serveTheKing.components.PlateCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p>The game state encapsulates all game information. It is a data-only class, with game functionality present
 * in the Forward Model or actions modifying the state of the game.</p>
 * <p>Most variables held here should be {@link Component} subclasses as much as possible.</p>
 * <p>No initialisation or game logic should be included here (not in the constructor either). This is all handled externally.</p>
 * <p>Computation may be included in functions here for ease of access, but only if this is querying the game state information.
 * Functions on the game state should never <b>change</b> the state of the game.</p>
 */
public class STKGameState extends AbstractGameState {
    /**
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players in the game
     */
    protected Deck<PlateCard> mainDeck;
    protected Deck<PlateCard> discardPile;
    protected List<PartialObservableDeck<PlateCard>> playersHands;
    protected List<PartialObservableDeck<PlateCard>> playersPlates;
    protected int playerCalledServe;


    public STKGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    public enum STKGamePhase implements IGamePhase {
        Draw,
        Play,
    }

    public void setPlayerCalledServe(int player){
        playerCalledServe=player;
    }
    public List<PartialObservableDeck<PlateCard>> getPlayersPlates(){
        return this.playersPlates;
    }
    public List<PartialObservableDeck<PlateCard>> getPlayersHands(){
        return this.playersHands;
    }

    public Deck<PlateCard> getDiscardPile() {
        return discardPile;
    }

    /**
     * @return the enum value corresponding to this game, declared in {@link GameType}.
     */
    @Override
    protected GameType _getGameType() {
        return GameType.ServeTheKing;
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
        components.add(mainDeck);
        components.add(discardPile);
        components.addAll(playersHands);
        components.addAll(playersPlates);
        // TODO: add all components to the list
        return components;
    }

    /**
     * <p>Create a deep copy of the game state containing only those components the given player can observe.</p>
     * <p>If the playerID is NOT -1 and If any components are not visible to the given player (e.g. cards in the hands
     * of other players or a face-down deck), then these components should instead be randomized (in the previous examples,
     * the cards in other players' hands would be combined with the face-down deck, shuffled together, and then new cards drawn
     * for the other players).</p>
     * <p>If the playerID passed is -1, then full observability is assumed and the state should be faithfully deep-copied.</p>
     *
     * <p>Make sure the return type matches the class type, and is not AbstractGameState.</p>
     *
     * @param playerId - player observing this game state.
     */
    @Override
    protected STKGameState _copy(int playerId) {
        STKGameState copy = new STKGameState(gameParameters, getNPlayers());
        copy.playersPlates = playersPlates;
        copy.playerCalledServe = playerCalledServe;
        copy.mainDeck = mainDeck;
        copy.playersHands = playersHands;
        copy.discardPile = discardPile;

        return copy;
    }

    /**
     * @param playerId - player observing the state.
     * @return a score for the given player approximating how well they are doing (e.g. how close they are to winning
     * the game); a value between 0 and 1 is preferred, where 0 means the game was lost, and 1 means the game was won.
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            // TODO calculate an approximate value
            return 0;
        } else {
            // The game finished, we can instead return the actual result of the game for the given player.
            return getPlayerResults()[playerId].value;
        }
    }

    /**
     * @param playerId - player observing the state.
     * @return the true score for the player, according to the game rules. May be 0 if there is no score in the game.
     */
    @Override
    public double getGameScore(int playerId) {
        // TODO: What is this player's score (if any)?
        return 0;
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof STKGameState
                && ((STKGameState) o).discardPile == discardPile
                && ((STKGameState) o).mainDeck == mainDeck
                && ((STKGameState) o).playerCalledServe==playerCalledServe
                && ((STKGameState) o).playersHands==playersHands
                && ((STKGameState) o).playersPlates==playersPlates;
    }

    @Override
    public int hashCode() {
        int hash= Objects.hash(mainDeck,discardPile,playersPlates,playersHands,playerCalledServe);

        return hash;
    }

    // TODO: Consider the methods below for possible implementation
    // TODO: These all have default implementations in AbstractGameState, so are not required to be implemented here.
    // TODO: If the game has 'teams' that win/lose together, then implement the next two nethods.
    /**
     * Returns the number of teams in the game. The default is to have one team per player.
     * If the game does not have 'teams' that win/lose together, then ignore these two methods.
     */
   // public int getNTeams();
    /**
     * Returns the team number the specified player is on.
     */
    //public int getTeam(int player);

    // TODO: If your game has multiple special tiebreak options, then implement the next two methods.
    // TODO: The default is to tie-break on the game score (if this is the case, ignore these)
    // public double getTiebreak(int playerId, int tier);
    // public int getTiebreakLevels();


    // TODO: If your game does not have a score of any type, and is an 'insta-win' type game which ends
    // TODO: as soon as a player achieves a winning condition, and has some bespoke method for determining 1st, 2nd, 3rd etc.
    // TODO: Then you *may* want to implement:.
    //public int getOrdinalPosition(int playerId);
}
