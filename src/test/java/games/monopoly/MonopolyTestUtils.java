package games.monopoly;

import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.monopoly.actions.Auction;
import games.monopoly.actions.DeclineProperty;
import games.monopoly.actions.PassBid;
import games.monopoly.actions.RollDice;
import games.monopoly.components.MonopolyCard;
import core.components.Deck;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 * Factories and arrange helpers for the Monopoly tests. Squares are named by their name on the UK board
 * (data/monopoly/ukBoard.json); MonopolySquare is equal by index and name, so a square from this board equals the
 * state's.
 */
class MonopolyTestUtils {

    /** The default (UK) board, for naming squares. */
    static final MonopolyBoard UK = new MonopolyParameters().getBoard();

    private MonopolyTestUtils() {
    }

    /** The square of the UK board with this name. */
    static MonopolySquare sq(String name) {
        return UK.square(name);
    }

    /** A game with random players, and default parameters when params is null. */
    static Game newGame(int nPlayers, long seed, MonopolyParameters params) {
        Game game = GameType.Monopoly.createGameInstance(nPlayers, seed,
                params == null ? new MonopolyParameters() : params);
        List<AbstractPlayer> players = IntStream.range(0, nPlayers)
                .mapToObj(p -> (AbstractPlayer) new RandomPlayer(new Random(seed + p))).toList();
        game.reset(players);
        return game;
    }

    static Game newGame(int nPlayers, long seed) {
        return newGame(nPlayers, seed, null);
    }

    /** A directly instantiated state, set up with the given seed (default parameters when params is null). */
    static MonopolyGameState newState(int nPlayers, long seed, MonopolyParameters params) {
        MonopolyParameters p = params == null ? new MonopolyParameters() : params;
        p.setRandomSeed(seed);
        MonopolyGameState state = new MonopolyGameState(p, nPlayers);
        new MonopolyForwardModel().setup(state);
        return state;
    }

    /**
     * Starts the player's turn with their token on the square: they become the turn owner, in phase ROLL, with no
     * doubles rolled and no further roll due. The first player is not changed.
     */
    static void startTurn(MonopolyGameState state, int player, MonopolySquare square) {
        state.setTurnOwner(player);
        state.setGamePhase(MonopolyGamePhase.ROLL);
        state.setNDoubles(0);
        state.setAnotherRoll(false);
        state.setPosition(player, square);
    }

    /** Puts the player in Jail (on the Jail square) with the given number of failed rolls for doubles so far. */
    static void putInJail(MonopolyGameState state, int player, int jailRolls) {
        state.setPosition(player, state.getBoard().jail());
        state.inJail[player] = true;
        state.setJailRolls(player, jailRolls);
    }

    /** Sets the last roll of the dice (as rollDice records it), without rolling. */
    static void setDice(MonopolyGameState state, int d1, int d2) {
        state.dice = new int[]{d1, d2};
    }

    /** Gives the properties to the player (unmortgaged). */
    static void give(MonopolyGameState state, int player, MonopolySquare... squares) {
        for (MonopolySquare s : squares)
            state.setOwner(s, player);
    }

    /** Marks the player as already out of the game, in the given place. */
    static void eliminate(MonopolyGameState state, int player, int finalPlace) {
        state.finalPlace[player] = finalPlace;
        state.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, player);
    }

    /** The current player rolls d1 and d2 (through the forward model). */
    static void roll(MonopolyGameState state, MonopolyForwardModel fm, int d1, int d2) {
        state.setNextRolls(d1, d2);
        fm.next(state, new RollDice());
    }

    /**
     * The player deciding declines to buy the property, and every bidder in the auction that follows passes, so the
     * property stays with the Bank.
     */
    static void decline(MonopolyGameState state, MonopolyForwardModel fm, MonopolySquare square) {
        fm.next(state, new DeclineProperty(square));
        assertEquals(square, passAll(state, fm));
        assertEquals(-1, state.getOwner(square));
    }

    /**
     * Every bidder still in the auction on top of the action stack passes until it ends (at most one pass per
     * player), and the auctioned square is returned.
     */
    static MonopolySquare passAll(MonopolyGameState state, MonopolyForwardModel fm) {
        assertTrue("no auction in progress: " + state.currentActionInProgress(),
                state.currentActionInProgress() instanceof Auction);
        Auction auction = (Auction) state.currentActionInProgress();
        for (int i = 0; i < state.getNPlayers() && state.currentActionInProgress() == auction; i++)
            fm.next(state, new PassBid());
        assertNotSame("the auction did not end", auction, state.currentActionInProgress());
        return auction.square;
    }

    /** Plays the actions in turn, asserting before each that it is available to the player deciding. */
    static void play(MonopolyGameState state, MonopolyForwardModel fm, AbstractAction... actions) {
        for (AbstractAction a : actions) {
            assertTrue(a + " not available to " + state.getCurrentPlayer() + ": " + fm.computeAvailableActions(state),
                    fm.computeAvailableActions(state).contains(a));
            fm.next(state, a);
        }
    }

    /** The available actions as a set (asserting there are no duplicates). */
    static Set<AbstractAction> actionSet(MonopolyForwardModel fm, MonopolyGameState state) {
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        Set<AbstractAction> set = new HashSet<>(actions);
        assertEquals("duplicate actions in " + actions, actions.size(), set.size());
        return set;
    }

    static int totalCash(MonopolyGameState state) {
        int total = 0;
        for (int p = 0; p < state.getNPlayers(); p++)
            total += state.getCash(p);
        return total;
    }

    /** The Chance card of the UK board whose text starts with this. */
    static MonopolyCard chance(String text) {
        return card(UK.chanceCards(), text);
    }

    /** The Community Chest card of the UK board whose text starts with this. */
    static MonopolyCard chest(String text) {
        return card(UK.communityChestCards(), text);
    }

    private static MonopolyCard card(List<MonopolyCard> cards, String text) {
        List<MonopolyCard> found = cards.stream().filter(c -> c.text.startsWith(text)).toList();
        assertEquals("cards starting " + text, 1, found.size());
        return found.get(0);
    }

    /** The state's pile the card belongs to. */
    static Deck<MonopolyCard> pile(MonopolyGameState state, MonopolyCard card) {
        return card.pile == MonopolyCard.Pile.CHANCE ? state.getChanceDeck() : state.getCommunityChestDeck();
    }

    /** Moves the cards to the top of their own piles, the first given on top. */
    static void onTop(MonopolyGameState state, MonopolyCard... cards) {
        for (int i = cards.length - 1; i >= 0; i--) {
            Deck<MonopolyCard> deck = pile(state, cards[i]);
            deck.remove(cards[i]);
            deck.add(cards[i]);
        }
    }

    /** Moves the Get Out of Jail Free card of the pile to the player's jail cards (conserves the cards). */
    static MonopolyCard holdJailCard(MonopolyGameState state, int player, MonopolyCard.Pile pile) {
        MonopolyCard card = pile == MonopolyCard.Pile.CHANCE ? chance("Get out of Jail") : chest("Get out of Jail");
        pile(state, card).remove(card);
        state.getJailCards(player).add(card);
        return card;
    }

    /** The last (bottom) card of the deck. */
    static MonopolyCard bottom(Deck<MonopolyCard> deck) {
        return deck.get(deck.getSize() - 1);
    }

    /**
     * Asserts that all 32 cards of the board are accounted for, each once: Chance cards in the Chance pile, Community
     * Chest cards in theirs, and only Get Out of Jail Free cards held by players.
     */
    static void assertCardsConserved(MonopolyGameState state) {
        List<MonopolyCard> all = new ArrayList<>();
        for (MonopolyCard c : state.getChanceDeck().getComponents()) {
            assertEquals(MonopolyCard.Pile.CHANCE, c.pile);
            all.add(c);
        }
        for (MonopolyCard c : state.getCommunityChestDeck().getComponents()) {
            assertEquals(MonopolyCard.Pile.COMMUNITY_CHEST, c.pile);
            all.add(c);
        }
        for (int p = 0; p < state.getNPlayers(); p++)
            for (MonopolyCard c : state.getJailCards(p).getComponents()) {
                assertEquals(MonopolyCard.Effect.GET_OUT_OF_JAIL, c.effect);
                all.add(c);
            }
        Set<MonopolyCard> expected = new HashSet<>(state.getBoard().chanceCards());
        expected.addAll(state.getBoard().communityChestCards());
        assertEquals(16 + 16, all.size());
        assertEquals(expected, new HashSet<>(all));
    }
}
