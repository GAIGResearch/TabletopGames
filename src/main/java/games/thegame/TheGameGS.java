package games.thegame;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.interfaces.IGamePhase;
import games.GameType;
import games.thegame.components.TheGameCard;
import games.thegame.components.TheGameDeck;

import java.util.*;

/**
 * <p>The game state encapsulates all game information. It is a data-only class, with game functionality present
 * in the Forward Model or actions modifying the state of the game.</p>
 * <p>Most variables held here should be {@link Component} subclasses as much as possible.</p>
 * <p>No initialisation or game logic should be included here (not in the constructor either). This is all handled externally.</p>
 * <p>Computation may be included in functions here for ease of access, but only if this is querying the game state information.
 * Functions on the game state should never <b>change</b> the state of the game.</p>
 */

public class TheGameGS extends AbstractGameState{

    public enum TheGamePhase implements IGamePhase{
        SelectingRow,
        PlayingCards
    }



    public List<Deck<TheGameCard>> playerHands;
    public List<TheGameDeck<TheGameCard>> cardRows;
    public Deck<TheGameCard> drawDeck;

    public Map<Integer, Integer> selectedRows;
    public TheGamePhase gamePhase;

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players in the game
     */
    public TheGameGS(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);

    }

    /**
     * @return the enum value corresponding to this game, declared in {@link GameType}.
     */
    @Override
    protected GameType _getGameType() {
        return GameType.TheGame;
    }

    /**
     * Returns all Components used in the game and referred to by componentId from actions or rules.
     * This method is called after initialising the game state, so all components will be initialised already.
     *
     * @return - List of Components in the game.
     */
    @Override
    protected List<Component> _getAllComponents() {
        ArrayList<Component> components = new ArrayList<>();
        components.addAll(playerHands);
        components.addAll(cardRows);
        components.add(drawDeck);
        return components;
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
    protected TheGameGS _copy(int playerId) {
        TheGameGS copy = new TheGameGS(gameParameters, getNPlayers());
        boolean visiblePlayerHands = ((TheGameParameters)gameParameters).playerHandVisibility == CoreConstants.VisibilityMode.VISIBLE_TO_ALL;

        copy.gamePhase = this.gamePhase;

        copy.cardRows = new ArrayList<>();
        copy.selectedRows = new HashMap<>();
        copy.playerHands = new ArrayList<>();

        // Fully observable bits
        for(int i = 0; i < cardRows.size(); ++i)
            copy.cardRows.add(this.cardRows.get(i).copy());
        for(int i = 0; i < selectedRows.size(); ++i)
            copy.selectedRows.put(i, this.selectedRows.get(i));

        // PO bits

        //First all copied as if fully observable
        copy.drawDeck = this.drawDeck.copy();
        for(int i = 0; i < playerHands.size(); ++i)
            copy.playerHands.add(this.playerHands.get(i).copy());

        if(playerId != -1){
            //Redeterminization of the PO bits

            if(visiblePlayerHands)
                //Only shuffle the draw deck.
                copy.drawDeck.shuffle(redeterminisationRnd);
            else{

                //Shuffle both the draw deck and the player hands.
                for (int i = 0; i < getNPlayers(); i++) {
                    if (i != playerId) {
                        copy.drawDeck.add(copy.playerHands.get(i));
                        copy.playerHands.get(i).clear();
                    }
                }
                copy.drawDeck.shuffle(redeterminisationRnd);
                for (int i = 0; i < getNPlayers(); i++) {
                    if (i != playerId) {
                        for (int j = 0; j < playerHands.get(i).getSize(); j++) {
                            copy.playerHands.get(i).add(copy.drawDeck.draw());
                        }
                    }
                }
            }
        }

        return copy;
    }

    /**
     * @param playerId - player observing the state.
     * @return a score for the given player approximating how well they are doing (e.g. how close they are to winning
     * the game); a value between 0 and 1 is preferred, where 0 means the game was lost, and 1 means the game was won.
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        TheGameParameters params = (TheGameParameters) gameParameters;
        int originalCardsInDrawDeck = params.maxCardNumber - params.minCardNumber - 1;
        int cardsInHands = 0;
        for(int i = 0; i < getNPlayers(); ++i)
            cardsInHands += playerHands.get(i).getSize();

        if (isNotTerminal()) {
            // This is the number of cards missing to be placed in rows.
            return originalCardsInDrawDeck - (drawDeck.getSize() + cardsInHands);
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
        return _getHeuristicScore(0);
    }

    @Override
    public boolean _equals(Object o) {
        if(o == this) return true;
        if(o == null) return false;
        if(o instanceof TheGameGS theGameGS) {
            return Objects.equals(playerHands, theGameGS.playerHands) &&
                    Objects.equals(cardRows, theGameGS.cardRows) && Objects.equals(drawDeck, theGameGS.drawDeck) &&
                    Objects.equals(selectedRows, theGameGS.selectedRows);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerHands, cardRows, drawDeck, selectedRows);
    }

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
