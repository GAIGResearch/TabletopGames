package games.jaipurskeleton;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Counter;
import core.components.Deck;
import games.GameType;
import games.jaipurskeleton.components.JaipurCard;
import games.jaipurskeleton.components.JaipurToken;

import java.util.*;

/**
 * <p>The game state encapsulates all game information. It is a data-only class, with game functionality present
 * in the Forward Model or actions modifying the state of the game.</p>
 * <p>Most variables held here should be {@link Component} subclasses as much as possible.</p>
 * <p>No initialisation or game logic should be included here (not in the constructor either). This is all handled externally.</p>
 * <p>Computation may be included in functions here for ease of access, but only if this is querying the game state information.
 * Functions on the game state should never <b>change</b> the state of the game.</p>
 */
public class JaipurGameState extends AbstractGameState {

    List<Map<JaipurCard.GoodType, Counter>> playerHands;
    List<Counter> playerHerds;  // Camels go here
    Deck<JaipurCard> drawDeck;
    Map<JaipurCard.GoodType, Counter> market;

    Map<JaipurCard.GoodType, Deck<JaipurToken>> goodTokens;
    Counter nGoodTokensSold;
    Map<Integer, Deck<JaipurToken>> bonusTokens;

    List<Counter> playerScores;
    List<Counter> playerNRoundsWon;
    List<Counter> playerNBonusTokens, playerNGoodTokens;  // Tiebreak counts

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players in the game
     */
    public JaipurGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    /**
     * @return the enum value corresponding to this game, declared in {@link GameType}.
     */
    @Override
    protected GameType _getGameType() {
        return GameType.Jaipur;
    }

    /**
     * Returns all Components used in the game and referred to by componentId from actions or rules.
     * This method is called after initialising the game state, so all components will be initialised already.
     *
     * @return - List of Components in the game.
     */
    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            addAll(playerHerds);
            add(drawDeck);
            addAll(goodTokens.values());
            add(nGoodTokensSold);
            addAll(bonusTokens.values());
            addAll(playerScores);
            addAll(playerNRoundsWon);
            addAll(market.values());
            for (int i = 0; i < getNPlayers(); i++) {
                addAll(playerHands.get(i).values());
            }
        }};
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
    protected JaipurGameState _copy(int playerId) {
        JaipurGameState copy = new JaipurGameState(gameParameters, getNPlayers());
        // Fully observable: market, good tokens, n good tokens sold, player scores, herds, n rounds won, n bonus tokens, ngood tokens
        copy.market = new HashMap<>();
        for (JaipurCard.GoodType gt: market.keySet()) {
            copy.market.put(gt, market.get(gt).copy());
        }
        copy.goodTokens = new HashMap<>();
        for (JaipurCard.GoodType gt: goodTokens.keySet()) {
            copy.goodTokens.put(gt, goodTokens.get(gt).copy());
        }
        copy.nGoodTokensSold = nGoodTokensSold.copy();
        copy.playerScores = new ArrayList<>();
        copy.playerNRoundsWon = new ArrayList<>();
        copy.playerNBonusTokens = new ArrayList<>();
        copy.playerNGoodTokens = new ArrayList<>();
        copy.playerHerds = new ArrayList<>();
        for (int i = 0; i < getNPlayers(); i++) {
            copy.playerScores.add(playerScores.get(i).copy());
            copy.playerNRoundsWon.add(playerNRoundsWon.get(i).copy());
            copy.playerNBonusTokens.add(playerNBonusTokens.get(i).copy());
            copy.playerNGoodTokens.add(playerNGoodTokens.get(i).copy());
            copy.playerHerds.add(playerHerds.get(i).copy());
        }

        copy.playerHands = new ArrayList<>();
        copy.drawDeck = drawDeck.copy();
        copy.bonusTokens = new HashMap<>();
        for (int n: bonusTokens.keySet()) {
            copy.bonusTokens.put(n, bonusTokens.get(n).copy());
        }

        for (int i = 0; i < getNPlayers(); i++) {
            Map<JaipurCard.GoodType, Counter> playerHandCopy = new HashMap<>();
            for (JaipurCard.GoodType gt: playerHands.get(i).keySet()) {
                playerHandCopy.put(gt, playerHands.get(i).get(gt).copy());
            }
            copy.playerHands.add(playerHandCopy);
        }

        // Partial observable: player hands, draw deck, bonus tokens
        if (getCoreGameParameters().partialObservable && playerId != -1) {
            Random r = new Random(gameParameters.getRandomSeed());

            // Hide the bonus token order
            for (int n: bonusTokens.keySet()) {
                copy.bonusTokens.get(n).shuffle(r);
            }

            // Hide! Shuffle together opponent player hands and the deck
            // TODO: Iterate through the players. If they're not the `playerId` observing the state (passed as argument to this method), then:
            // TODO: Iterate through the good types in their hands. Set all to 0 *in the copy*.
            // TODO: Count how many cards each player has in their hands in total.
            // TODO: Add new JaipurCard objects of the corresponding type to the *copy draw deck*, as many as the player has in their hand.
            // TODO: After going through all the players, shuffle the *copy draw deck*.

            // Then draw new cards for opponent
            // TODO: Iterate through the players. If they're the `playerId` observing the state (passed as argument to this method), copy the exact hand of the player into the *copy game state*
            // TODO: Otherwise, draw new cards from the *copy draw deck* and update the *copy player hand* appropriately (you can check this same functionality in the round setup performed in the Forward Model for help)
            // TODO: Make sure to ignore camels, and put them back at the bottom of the *copy draw deck*, e.g. copy.drawDeck.add(card,copy.drawDeck.getSize()); Camels don't stay in player's hands, so we're only filling hands with non-camel cards
            // TODO: At the end of this process, reshuffle the *copy draw deck* to make sure any camels that were drawn and put back are randomly distributed too
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
        if (isNotTerminal()) {
            // No heuristic, just return game score
            return getGameScore(playerId);
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
        return playerScores.get(playerId).getValue();
    }

    @Override
    public double getTiebreak(int playerId, int tier) {
        // 2 tiebreaks: most bonus tokens; if still tied, tiebreak is most good tokens
        if (tier == 1) {
            return playerNBonusTokens.get(playerId).getValue();
        } else if (tier == 2) {
            return playerNGoodTokens.get(playerId).getValue();
        }
        return 0;
    }

    public List<Map<JaipurCard.GoodType, Counter>> getPlayerHands() {
        return playerHands;
    }

    public List<Counter> getPlayerHerds() {
        return playerHerds;
    }

    public Deck<JaipurCard> getDrawDeck() {
        return drawDeck;
    }

    public Map<JaipurCard.GoodType, Counter> getMarket() {
        return market;
    }

    public Map<JaipurCard.GoodType, Deck<JaipurToken>> getGoodTokens() {
        return goodTokens;
    }

    public Counter getnGoodTokensSold() {
        return nGoodTokensSold;
    }

    public Map<Integer, Deck<JaipurToken>> getBonusTokens() {
        return bonusTokens;
    }

    public List<Counter> getPlayerScores() {
        return playerScores;
    }

    public List<Counter> getPlayerNRoundsWon() {
        return playerNRoundsWon;
    }

    public List<Counter> getPlayerNBonusTokens() {
        return playerNBonusTokens;
    }

    public List<Counter> getPlayerNGoodTokens() {
        return playerNGoodTokens;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JaipurGameState)) return false;
        if (!super.equals(o)) return false;
        JaipurGameState that = (JaipurGameState) o;
        return Objects.equals(playerHands, that.playerHands) && Objects.equals(playerHerds, that.playerHerds) && Objects.equals(drawDeck, that.drawDeck) && Objects.equals(market, that.market) && Objects.equals(goodTokens, that.goodTokens) && Objects.equals(nGoodTokensSold, that.nGoodTokensSold) && Objects.equals(bonusTokens, that.bonusTokens) && Objects.equals(playerScores, that.playerScores) && Objects.equals(playerNRoundsWon, that.playerNRoundsWon) && Objects.equals(playerNBonusTokens, that.playerNBonusTokens) && Objects.equals(playerNGoodTokens, that.playerNGoodTokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerHands, playerHerds, drawDeck, market, goodTokens, nGoodTokensSold, bonusTokens, playerScores, playerNRoundsWon, playerNBonusTokens, playerNGoodTokens);
    }
}
