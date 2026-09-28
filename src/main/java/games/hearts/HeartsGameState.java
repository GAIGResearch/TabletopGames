package games.hearts;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IGamePhase;
import games.GameType;
import games.hearts.heuristics.HeartsHeuristic;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.*;

import java.util.function.*;
import java.util.stream.IntStream;


/**
 * <p>The game state encapsulates all game information. It is a data-only class, with game functionality present
 * in the Forward Model or actions modifying the state of the game.</p>
 * <p>Most variables held here should be {@link Component} subclasses as much as possible.</p>
 * <p>No initialisation or game logic should be included here (not in the constructor either). This is all handled externally.</p>
 * <p>Computation may be included in functions here for ease of access, but only if this is querying the game state information.
 * Functions on the game state should never <b>change</b> the state of the game.</p>
 */
public class HeartsGameState extends AbstractGameState {
    List<Deck<FrenchCard>> playerDecks;
    Deck<FrenchCard> drawDeck;
    public List<Deck<FrenchCard>> trickDecks;
    public boolean heartsBroken;
    public int[] playerTricksTaken;
    public List<List<FrenchCard>> pendingPasses;
    public Map<Integer, Integer> playerPoints;
    public List<Map.Entry<Integer, FrenchCard>> currentPlayedCards = new ArrayList<>();
    public FrenchCard.Suite firstCardSuit;
    /**
     * For each player, the suits that they are publicly known to be void in; i.e. the suits that
     * were led in a trick this round to which they did not follow suit.
     */
    public List<Set<FrenchCard.Suite>> knownVoids;

    public HeartsGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Hearts;
    }

    @Override
    protected List<Component> _getAllComponents() {

        List<Component> retValue = new ArrayList<>(playerDecks);
        playerDecks.stream().flatMap(e -> e.getComponents().stream()).forEach(retValue::add);
        retValue.add(drawDeck);
        retValue.addAll(drawDeck.getComponents());
        retValue.addAll(trickDecks);
        trickDecks.stream().flatMap(e -> e.getComponents().stream()).forEach(retValue::add);
        currentPlayedCards.forEach(e -> retValue.add(e.getValue()));

        return retValue;
    }

    public enum Phase implements IGamePhase {
        PASSING,
        PLAYING
    }

    /**
     * Passing happens one card at a time, all players at once: everyone commits their first card,
     * then everyone their second, and so on. This is the number of cards committed by the players
     * who have committed fewest, i.e. the sub-turn the passing phase is currently on.
     */
    public int fewestPendingPasses() {
        return pendingPasses.stream().mapToInt(List::size).min().orElse(0);
    }

    /**
     * The players who have not yet committed a card in the current passing sub-turn: those who have
     * committed only as many cards as the player who has committed fewest. Empty once everyone has
     * committed all the cards the round requires.
     */
    public List<Integer> getPlayersStillToPass() {
        int fewest = fewestPendingPasses();
        if (fewest >= ((HeartsParameters) gameParameters).cardsPassedPerRound)
            return List.of();
        return IntStream.range(0, getNPlayers())
                .filter(p -> pendingPasses.get(p).size() == fewest)
                .boxed()
                .toList();
    }

    /**
     * Passing is simultaneous; trick play is sequential. This is the switch between the two: while
     * passing, everyone still to commit a card this sub-turn decides at once; otherwise it is just
     * the current player, as for any sequential game.
     */
    @Override
    public List<Integer> getCurrentSimultaneousPlayers() {
        if (isActionInProgress() || getGamePhase() != Phase.PASSING)
            return super.getCurrentSimultaneousPlayers();
        List<Integer> toDecide = getPlayersStillToPass();
        if (toDecide.isEmpty())
            throw new AssertionError("Every player has passed but the passes have not been resolved");
        return toDecide;
    }

    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    /**
     * returns a List in playerID order of the player hands
     */
    public List<Deck<FrenchCard>> getPlayerDecks() {
        return playerDecks;
    }

    /**
     * The suits that the specified player is publicly known to be void in, from having failed to
     * follow suit earlier in the current round. This is information available to all players.
     */
    public Set<FrenchCard.Suite> getKnownVoids(int playerId) {
        return knownVoids.get(playerId);
    }

    public void scorePointsAtEndOfRound() {
        HeartsParameters params = (HeartsParameters) getGameParameters();
        for (int playerId = 0; playerId < getNPlayers(); playerId++) {

            // Get the trick deck for the player
            Deck<FrenchCard> trickDeck = trickDecks.get(playerId);
            if (trickDeck != null) {
                int points = 0;

                // Iterate over all cards in the trick deck
                for (FrenchCard card : trickDeck.getComponents()) {
                    if (card.suite == FrenchCard.Suite.Hearts) {
                        points += params.heartCard;
                    }
                    // The queen of spades is worth 13 points
                    else if (card.equals(params.qosCard)) {
                        points += params.queenOfSpades;
                    }
                }

                if (points == params.shootTheMoon) {
                    // all other players get the points instead
                    for (int i = 0; i < getNPlayers(); i++) {
                        if (i != playerId) {
                            playerPoints.put(i, playerPoints.getOrDefault(i, 0) + points);
                        }
                    }
                } else {
                    // Add the points to the player's score
                    playerPoints.put(playerId, playerPoints.getOrDefault(playerId, 0) + points);
                }


                // Clear the trick deck after its points have been added
                trickDeck.clear();
            }
        }
    }

    // The player's points from all previous hands
    public int getPlayerPoints(int playerID) {
        return playerPoints.getOrDefault(playerID, 0);
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        HeartsGameState copy = new HeartsGameState(gameParameters.copy(), getNPlayers());

        // Deep Copy player decks
        copy.playerDecks = new ArrayList<>();
        for (Deck<FrenchCard> d : playerDecks) {
            copy.playerDecks.add(d.copy());
        }

        // Deep Copy draw deck
        copy.drawDeck = drawDeck.copy();

        // Deep Copy trickDecks
        copy.trickDecks = new ArrayList<>();
        for (Deck<FrenchCard> d : trickDecks) {
            copy.trickDecks.add(d.copy());
        }

        copy.heartsBroken = heartsBroken;

        // Deep Copy playerTricksTaken
        copy.playerTricksTaken = Arrays.copyOf(playerTricksTaken, playerTricksTaken.length);

        // Deep Copy pendingPasses
        copy.pendingPasses = new ArrayList<>();
        for (List<FrenchCard> list : pendingPasses) {
            copy.pendingPasses.add(new ArrayList<>(list));
        }

        // Deep Copy playerPoints
        copy.playerPoints = new HashMap<>(playerPoints);


        // Deep Copy currentRoundCards
        copy.currentPlayedCards = new ArrayList<>();
        for (Map.Entry<Integer, FrenchCard> entry : currentPlayedCards) {
            copy.currentPlayedCards.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().copy()));
        }

        copy.firstCardSuit = firstCardSuit;

        // Deep Copy knownVoids
        copy.knownVoids = new ArrayList<>();
        for (Set<FrenchCard.Suite> voids : knownVoids) {
            Set<FrenchCard.Suite> voidCopy = EnumSet.noneOf(FrenchCard.Suite.class);
            voidCopy.addAll(voids);
            copy.knownVoids.add(voidCopy);
        }

        // Hidden information is dealt with in redeterminise(), which the superclass calls on the
        // copy when appropriate.
        return copy;
    }

    @Override
    public void redeterminise(int playerId) {
        // We cannot see the cards the other players have committed to pass, but we can see how many.
        // Put their pending passes back into their hands, reshuffle everything we cannot see, and then
        // draw the same number of (now unknown) cards back out into their pending passes. Our own
        // pending passes are kept: getPlayersStillToPass() counts them to decide who is still to pass,
        // so losing them would have us asked to pass the same card again.
        int[] pendingCounts = new int[getNPlayers()];
        for (int i = 0; i < getNPlayers(); i++) {
            if (i == playerId) continue;
            pendingCounts[i] = pendingPasses.get(i).size();
            playerDecks.get(i).add(pendingPasses.get(i));
            pendingPasses.get(i).clear();
        }

        // Reshuffle everything we cannot see across the other hands and the draw deck.
        // A player who has failed to follow suit is known to hold no cards of that suit, so we
        // must not deal them any.
        List<Deck<FrenchCard>> decksToShuffle = new ArrayList<>(playerDecks);
        decksToShuffle.add(drawDeck);
        BiPredicate<Deck<FrenchCard>, FrenchCard> voidConstraint =
                ((HeartsParameters) gameParameters).rememberVoids
                        ? (deck, card) -> deck.getOwnerId() < 0 || !knownVoids.get(deck.getOwnerId()).contains(card.suite)
                        : null;  // a null constraint gives the unconstrained reshuffle
        DeterminisationUtilities.reshuffle(playerId, decksToShuffle, c -> true, redeterminisationRnd, voidConstraint);

        for (int i = 0; i < getNPlayers(); i++) {
            for (int k = 0; k < pendingCounts[i]; k++) {
                pendingPasses.get(i).add(playerDecks.get(i).draw());
            }
        }

        // Passing is simultaneous, so while it lasts every player sees themselves as the current
        // player; that is what the 2-argument computeAvailableActions() reads. Trick play is
        // sequential, and there the turn owner is the real current player and must be left alone.
        if (getGamePhase() == Phase.PASSING)
            setTurnOwner(playerId);
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return new HeartsHeuristic().evaluateState(this, playerId);
    }

    /**
     * For Hearts a lower score is better than a high one. So we return the negative of the player's points.
     */
    @Override
    public double getGameScore(int playerId) {
        return -playerPoints.getOrDefault(playerId, 0);
    }

    public void setPlayerPoints(int playerId, int points) {
        playerPoints.put(playerId, points);
    }

    public CoreConstants.GameResult getPlayerResult(int playerIdx) {
        return playerResults[playerIdx];
    }

    @Override
    protected List<Integer> _getUnknownComponentsIds(int playerId) {
        List<Integer> retValue = new ArrayList<>();
        retValue.add(drawDeck.getComponentID());
        for (Component c : drawDeck.getComponents()) {
            retValue.add(c.getComponentID());
        }
        // the cards the other players have committed to pass are hidden until the passes resolve
        for (int p = 0; p < getNPlayers(); p++) {
            if (p == playerId) continue;
            for (Component c : pendingPasses.get(p)) {
                retValue.add(c.getComponentID());
            }
        }
        return retValue;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HeartsGameState)) return false;
        if (!super.equals(o)) return false;
        HeartsGameState that = (HeartsGameState) o;
        return heartsBroken == that.heartsBroken &&
                Arrays.equals(playerTricksTaken, that.playerTricksTaken) &&
                Objects.equals(playerDecks, that.playerDecks) &&
                Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(trickDecks, that.trickDecks) &&
                Objects.equals(pendingPasses, that.pendingPasses) &&
                Objects.equals(playerPoints, that.playerPoints) &&
                Objects.equals(currentPlayedCards, that.currentPlayedCards) &&
                Objects.equals(firstCardSuit, that.firstCardSuit) &&
                Objects.equals(knownVoids, that.knownVoids);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), playerDecks, drawDeck, heartsBroken,
                firstCardSuit, trickDecks,
                pendingPasses, playerPoints, currentPlayedCards, knownVoids);
        result = 31 * result + Arrays.hashCode(playerTricksTaken);
        return result;
    }

    /**
     * A player's trick deck is the Deck of cards they have won (only the ones with points) in
     * previous tricks this round.
     */
    public List<Deck<FrenchCard>> getPlayerTrickDecks() {
        return trickDecks;
    }

}








