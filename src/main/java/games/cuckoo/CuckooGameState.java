package games.cuckoo;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import utilities.DeterminisationUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>Game state for Cuckoo. Data-only: all initialisation and rule logic lives in {@link CuckooForwardModel}.</p>
 *
 * <p>Components tracked:</p>
 * <ul>
 *     <li>playerCards - one Deck per player holding their single card, VISIBLE_TO_OWNER. Empty once the player is
 *     out of the game</li>
 *     <li>drawDeck - the cards left undealt, HIDDEN_TO_ALL</li>
 *     <li>lives - the lives each player has left</li>
 *     <li>roundEliminated - the round in which each player lost their last life, or -1 while they are in the game</li>
 *     <li>dealer - the player who dealt this round</li>
 *     <li>knowledge - which players' cards each player knows this round</li>
 * </ul>
 */
public class CuckooGameState extends AbstractGameState {

    List<Deck<FrenchCard>> playerCards;
    Deck<FrenchCard> drawDeck;
    int[] lives;
    int[] roundEliminated;
    int dealer;
    CuckooKnowledge knowledge;

    public CuckooGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Cuckoo;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(playerCards);
        components.add(drawDeck);
        return components;
    }

    public List<Deck<FrenchCard>> getPlayerCards() {
        return playerCards;
    }

    /**
     * The card the player holds, or null if they are out of the game.
     */
    public FrenchCard getPlayerCard(int player) {
        Deck<FrenchCard> deck = playerCards.get(player);
        return deck.getSize() == 0 ? null : deck.get(0);
    }

    public Deck<FrenchCard> getDrawDeck() {
        return drawDeck;
    }

    public int getLives(int player) {
        return lives[player];
    }

    /**
     * The round in which the player lost their last life, or -1 while they are in the game.
     */
    public int getRoundEliminated(int player) {
        return roundEliminated[player];
    }

    public boolean isInGame(int player) {
        return lives[player] > 0;
    }

    public int getNPlayersInGame() {
        int count = 0;
        for (int p = 0; p < getNPlayers(); p++) {
            if (isInGame(p))
                count++;
        }
        return count;
    }

    public int getDealer() {
        return dealer;
    }

    /**
     * The next player on the left (clockwise, the next player number) who is still in the game.
     */
    public int nextPlayerInGame(int player) {
        int next = (player + 1) % getNPlayers();
        while (!isInGame(next) && next != player)
            next = (next + 1) % getNPlayers();
        return next;
    }

    public CuckooKnowledge getKnowledge() {
        return knowledge;
    }

    /**
     * Whether the observer knows which card the holder has.
     */
    public boolean knowsCard(int observer, int holder) {
        return knowledge.knows(observer, holder);
    }

    @Override
    protected CuckooGameState _copy(int playerId) {
        CuckooGameState copy = new CuckooGameState(gameParameters, getNPlayers());
        copy.playerCards = new ArrayList<>();
        for (Deck<FrenchCard> deck : playerCards)
            copy.playerCards.add(deck.copy());
        copy.drawDeck = drawDeck.copy();
        copy.lives = lives.clone();
        copy.roundEliminated = roundEliminated.clone();
        copy.dealer = dealer;
        copy.knowledge = knowledge.copy();

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // The cards of players the observer knows stay where they are; the rest are shuffled with the draw deck
            Set<FrenchCard> known = new HashSet<>();
            for (int p = 0; p < getNPlayers(); p++) {
                if (knowsCard(playerId, p))
                    known.addAll(playerCards.get(p).getComponents());
            }
            List<Deck<FrenchCard>> decks = new ArrayList<>(copy.playerCards);
            decks.add(copy.drawDeck);
            DeterminisationUtilities.reshuffle(playerId, decks, c -> !known.contains(c), redeterminisationRnd);
        }
        return copy;
    }

    /**
     * The final result once the game is over; otherwise the lives the player has left as a fraction of those they
     * started with.
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!isNotTerminal())
            return getPlayerResults()[playerId].value;
        return lives[playerId] / (double) ((CuckooParameters) gameParameters).nLives;
    }

    /**
     * The lives the player has left.
     */
    @Override
    public double getGameScore(int playerId) {
        return lives[playerId];
    }

    /**
     * Players out of the game are ranked by the round in which they went out: the later, the better.
     */
    @Override
    public double getTiebreak(int playerId, int tier) {
        return roundEliminated[playerId];
    }

    @Override
    public int getTiebreakLevels() {
        return 1;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CuckooGameState that)) return false;
        return dealer == that.dealer &&
                Objects.equals(playerCards, that.playerCards) &&
                Objects.equals(drawDeck, that.drawDeck) &&
                Arrays.equals(lives, that.lives) &&
                Arrays.equals(roundEliminated, that.roundEliminated) &&
                Objects.equals(knowledge, that.knowledge);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerCards, drawDeck, dealer, knowledge)
                + 31 * Arrays.hashCode(lives) + 961 * Arrays.hashCode(roundEliminated);
    }
}
