package games.tickettoride;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.*;
import games.GameType;
import games.pandemic.PandemicGameState;


import java.util.*;

import static core.CoreConstants.playerHandHash;


/**
 * <p>The game state encapsulates all game information. It is a data-only class, with game functionality present
 * in the Forward Model or actions modifying the state of the game.</p>
 * <p>Most variables held here should be {@link Component} subclasses as much as possible.</p>
 * <p>No initialisation or game logic should be included here (not in the constructor either). This is all handled externally.</p>
 * <p>Computation may be included in functions here for ease of access, but only if this is querying the game state information.
 * Functions on the game state should never <b>change</b> the state of the game.</p>
 */
public class  TicketToRideGameState extends AbstractGameState {
    /**
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players in the game
     */

    HashMap<Integer, Area> areas;
    Deck<Card> tempDeck;
    GraphBoard world;

    protected int[] scores;

    List<Map<TicketToRideParameters.TrainCar, Counter>> playerTrainCars;
    HashMap<TicketToRideParameters.TrainCar, Counter> trainCarPool;



    public GraphBoard getWorld() {
        return world;
    }


    public TicketToRideGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    /**
     * @return the enum value corresponding to this game, declared in {@link GameType}.
     */
    @Override
    protected GameType _getGameType() {
        // TODO: replace with game-specific enum value declared in GameType
        return GameType.TicketToRide ;
    }

    /**
     * Returns all Components used in the game and referred to by componentId from actions or rules.
     * This method is called after initialising the game state, so all components will be initialised already.
     *
     * @return - List of Components in the game.
     */
    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(areas.values());
        components.add(tempDeck);
        components.add(world);
        return components;
    }

    public Map<TicketToRideParameters.TrainCar, Counter> getPlayerTrainCars(int playerID) {
        return playerTrainCars.get(playerID);
    }

    public Component getComponentActingPlayer(int playerId, int componentId) {
        return areas.get(playerId).getComponent(componentId);
    }

    /**
     * <p>Create a deep copy of the game state containing only those components the given player can observe.</p>
     * <p>If the playerID is NOT -1 and If any components are not visible to the given player (e.g. cards in the hands
     * of other players or a face-down deck), then these components should instead be randomized (in the previous examples,
     * the cards in other players' hands would be combined with the face-down deck, shuffled together, and then new cards drawn
     * for the other players). This process is also called 'redeterminisation'.</p>
     * <p>There are some utilities to assist with this in utilities.DeterminisationUtilities. One firm is guideline is
     * that the standard random number generator from getRnd() should not be used in this method. A separate Random is provided
     * for this purpose - redeterminisationRnd.
     *  This is to avoid this RNG stream being distorted by the number of player actions taken (where those actions are not themselves inherently random)</p>
     * <p>If the playerID passed is -1, then full observability is assumed and the state should be faithfully deep-copied.</p>
     *
     * <p>Make sure the return type matches the class type, and is not AbstractGameState.</p>
     *
     *
     * @param playerId - player observing this game state.
     */
    @Override
    protected TicketToRideGameState _copy(int playerId) {
        TicketToRideGameState copy = new TicketToRideGameState(gameParameters, getNPlayers());
        copy.areas = areas;
        copy.tempDeck = tempDeck.copy();
        copy.world = world.copy();
        copy.scores = scores;
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
        return scores[playerId];
    }
    public int[] getScores() {
        return scores;
    }

    public void addScore(int playerID, int score) {
        if (playerID < scores.length) {
            scores[playerID] += score;
        }
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PandemicGameState)) return false;
        if (!super.equals(o)) return false;
        // TODO: compare all variables in the state
        return o instanceof TicketToRideGameState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), areas, tempDeck, world);
    }
    public Component getComponent(int componentId, int playerId) {
        return areas.get(playerId).getComponent(componentId);
    }

    public Component getComponent(int componentId) {
        return getComponent(componentId, -1);
    }
    public Area getArea(int playerId) {
        return areas.get(playerId);
    }

    protected void _reset() {
        areas = null;
        scores = null;
//        tempDeck = null;
        world = null;
//        quietNight = false;
//        nCardsDrawn = 0;
//        researchStationLocations = new ArrayList<>();
    }
    void addComponents() {
        super.addAllComponents();
    }

    public int getNumberOfSpecificTrainCard(int playerId, String cardColor){
        Area gameArea = this.getArea(-1);
        Deck<Card> playerTrainCardHandDeck = (Deck<Card>) this.getComponentActingPlayer(playerId, playerHandHash);
        int count = 0;

        for (Card card : playerTrainCardHandDeck) {
            if (card.toString().equalsIgnoreCase(cardColor)) {
                count++;
            }
        }

        return count;

    }

//    public boolean checkCost(HashMap<CatanParameters.Resource, Integer> cost, int playerId) {
//        for (Map.Entry<TicketToRideParameters.Resource, Integer> e: cost.entrySet()) {
//            if (playerResources.get(playerId).get(e.getKey()).getValue() < cost.get(e.getKey())) return false;
//        }
//        return true;
//    }
//    public boolean checkCost(CatanParameters.Resource resource, int nRequired, int playerId) {
//        return playerResources.get(playerId).get(resource).getValue() >= nRequired;
//    }
//
//    public boolean spendResourcesIfPossible(HashMap<CatanParameters.Resource, Integer> cost, int playerId) {
//        if (!checkCost(cost, playerId)) return false;
//        for (Map.Entry<CatanParameters.Resource, Integer> e: cost.entrySet()) {
//            playerResources.get(playerId).get(e.getKey()).decrement(e.getValue());
//        }
//        return true;
//    }



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

    // TODO: Review the methods below...these are all supported by the default implementation in AbstractGameState
    // TODO: So you do not (and generally should not) implement your own versions - take advantage of the framework!
    // public Random getRnd() returns a Random number generator for the game. This will be derived from the seed
    // in game parameters, and will be updated correctly on a reset

    // Ths following provide access to the id of the current player; the first player in the Round (if that is relevant to a game)
    // and the current Turn and Round numbers.
    // public int getCurrentPlayer()
    // public int getFirstPlayer()
    // public int getRoundCounter()
    // public int getTurnCounter()
    // also make sure you check out the standard endPlayerTurn() and endRound() methods in StandardForwardModel

    // This method can be used to log a game event (e.g. for something game-specific that you want to include in the metrics)
    // public void logEvent(IGameEvent...)
}


