package games.skitgubbe;

import core.AbstractPlayer;
import core.Game;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static core.components.FrenchCard.FrenchCardType.*;
import static org.junit.Assert.assertEquals;

/**
 * Helpers to arrange Skitgubbe states. Every helper MOVES cards from wherever they are, so the 52 cards are always
 * conserved (each exactly once). A card taken from another player's hand is replaced there by the card at the
 * BOTTOM of the draw deck, so hand sizes (and any cards stacked on top of the draw deck) are kept.
 * Arrange in this order: trick / held / collected / trump card first, then hands (giveHand), then the draw deck
 * (stackDrawDeck or leaveDrawDeck) last - an emptied draw deck cannot supply replacements.
 */
final class SkitgubbeTestUtils {

    static final List<FrenchCard> FULL_DECK = FrenchCard.generateDeck("FullDeck", VISIBLE_TO_ALL).getComponents();

    private SkitgubbeTestUtils() {
    }

    /** A real game from the factory, reset with random players, for integration tests driven by fm.next. */
    static Game newGame(int nPlayers, long seed) {
        return newGame(nPlayers, seed, new SkitgubbeParameters());
    }

    /** As newGame(nPlayers, seed), with the given parameters. The seed overrides any seed on params. */
    static Game newGame(int nPlayers, long seed, SkitgubbeParameters params) {
        Game game = GameType.Skitgubbe.createGameInstance(nPlayers, seed, params);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            players.add(new RandomPlayer(new Random(seed + p)));
        game.reset(players);
        return game;
    }

    /** A state set up directly (no Game), for unit tests. */
    static SkitgubbeGameState newState(int nPlayers, long seed, SkitgubbeParameters params) {
        params.setRandomSeed(seed);
        SkitgubbeGameState state = new SkitgubbeGameState(params, nPlayers);
        new SkitgubbeForwardModel().setup(state);
        return state;
    }

    /** A card from a short code: rank (2-10, J, Q, K, A) then suit (H, D, C, S). e.g. "8H", "10D", "KS". */
    static FrenchCard card(String code) {
        FrenchCard.Suite suit = switch (code.charAt(code.length() - 1)) {
            case 'H' -> FrenchCard.Suite.Hearts;
            case 'D' -> FrenchCard.Suite.Diamonds;
            case 'C' -> FrenchCard.Suite.Clubs;
            case 'S' -> FrenchCard.Suite.Spades;
            default -> throw new IllegalArgumentException("Unknown suit in " + code);
        };
        return switch (code.substring(0, code.length() - 1)) {
            case "J" -> new FrenchCard(Jack, suit);
            case "Q" -> new FrenchCard(Queen, suit);
            case "K" -> new FrenchCard(King, suit);
            case "A" -> new FrenchCard(Ace, suit);
            default -> new FrenchCard(Number, suit, Integer.parseInt(code.substring(0, code.length() - 1)));
        };
    }

    static List<FrenchCard> cards(String... codes) {
        List<FrenchCard> list = new ArrayList<>();
        for (String c : codes) list.add(card(c));
        return list;
    }

    /** Every deck that can hold a card: hands, draw deck, trump card, trick, held, collected, discard pile. */
    static List<Deck<FrenchCard>> allDecks(SkitgubbeGameState state) {
        List<Deck<FrenchCard>> decks = new ArrayList<>(state.playerHands);
        decks.add(state.drawDeck);
        decks.add(state.trumpCard);
        decks.add(state.trick);
        decks.addAll(state.heldCards);
        decks.addAll(state.collectedCards);
        decks.add(state.discardPile);
        return decks;
    }

    /** Remove the card from wherever it is; a hand it leaves gets the draw deck's bottom card instead. */
    private static void takeFromWherever(SkitgubbeGameState state, FrenchCard card) {
        for (Deck<FrenchCard> deck : allDecks(state)) {
            if (deck.contains(card)) {
                deck.remove(card);
                if (state.playerHands.contains(deck)) {
                    if (state.drawDeck.getSize() == 0)
                        throw new IllegalStateException("no draw deck card to replace " + card + " in a hand - arrange the draw deck last");
                    deck.add(state.drawDeck.pickLast());
                }
                return;
            }
        }
        throw new IllegalArgumentException(card + " is not in any deck");
    }

    /** Move these cards, in order, onto the top of the target deck (so the last listed ends on top). */
    static void moveTo(SkitgubbeGameState state, Deck<FrenchCard> target, String... codes) {
        for (FrenchCard c : cards(codes)) {
            takeFromWherever(state, c);
            target.add(c);
        }
    }

    /**
     * Replace the player's hand with exactly these cards (any number). The old hand goes to the bottom of the draw
     * deck.
     */
    static void giveHand(SkitgubbeGameState state, int player, String... codes) {
        Deck<FrenchCard> hand = state.playerHands.get(player);
        for (FrenchCard c : new ArrayList<>(hand.getComponents())) {
            hand.remove(c);
            state.drawDeck.addToBottom(c);
        }
        for (FrenchCard c : cards(codes)) {
            takeFromWherever(state, c);
            hand.add(c);
        }
    }

    /** Put these cards on top of the draw deck, the first listed on top. Call after giveHand. */
    static void stackDrawDeck(SkitgubbeGameState state, String... codes) {
        List<FrenchCard> list = cards(codes);
        for (int i = list.size() - 1; i >= 0; i--) {
            takeFromWherever(state, list.get(i));
            state.drawDeck.add(list.get(i));
        }
    }

    /**
     * Make the draw deck exactly these cards (first listed on top; none for an empty draw deck). Every other draw
     * deck card goes to dumpPlayer's collectedCards. Call last.
     */
    static void leaveDrawDeck(SkitgubbeGameState state, int dumpPlayer, String... codes) {
        List<FrenchCard> keep = cards(codes);
        for (FrenchCard c : keep) takeFromWherever(state, c);
        while (state.drawDeck.getSize() > 0)
            state.collectedCards.get(dumpPlayer).add(state.drawDeck.draw());
        for (int i = keep.size() - 1; i >= 0; i--)
            state.drawDeck.add(keep.get(i));
    }

    /** The player has already drawn this card as the trump card (owner and trumpPlayer set). */
    static void giveTrumpCard(SkitgubbeGameState state, int player, String code) {
        state.trumpCard.setOwnerId(player);
        moveTo(state, state.trumpCard, code);
        state.trumpPlayer = player;
    }

    /**
     * Put the state straight into phase two: every card leaves its deck for the discard pile, then each player's
     * collectedCards (their phase-two hand) becomes exactly the cards listed for them, as a space-separated string
     * ("" for none; one string per player). trumpSuit is set from its initial ("H", "D", "C", "S"), trickSize = the
     * number of players given cards, trumpPlayer = toAct, and toAct is to act. The rest of the pack stays in the
     * discard pile (so assert on the discard pile's change, not its size). exitScores and phaseTwoActions are not
     * touched (0 from setup) - set them in the test where they matter.
     */
    static void arrangePhaseTwo(SkitgubbeGameState state, String trump, int toAct, String... perPlayer) {
        if (perPlayer.length != state.getNPlayers())
            throw new IllegalArgumentException("one string of cards per player");
        for (Deck<FrenchCard> deck : allDecks(state)) {
            if (deck == state.discardPile) continue;
            while (deck.getSize() > 0)
                state.discardPile.add(deck.draw());
        }
        state.trumpCard.setOwnerId(-1);
        int holders = 0;
        for (int p = 0; p < perPlayer.length; p++) {
            String list = perPlayer[p].trim();
            if (list.isEmpty()) continue;
            holders++;
            moveTo(state, state.collectedCards.get(p), list.split(" +"));
        }
        state.trumpSuit = card("2" + trump).suite;
        state.trumpPlayer = toAct;
        state.trickSize = holders;
        state.setGamePhase(SkitgubbeGameState.Phase.PHASE_TWO);
        state.setTurnOwner(toAct);
    }

    static int totalCards(SkitgubbeGameState state) {
        return allDecks(state).stream().mapToInt(Deck::getSize).sum();
    }

    /** All 52 cards are present, each exactly once. */
    static void assertAllCardsPresent(SkitgubbeGameState state) {
        List<FrenchCard> all = new ArrayList<>();
        allDecks(state).forEach(d -> all.addAll(d.getComponents()));
        assertEquals("number of cards", 52, all.size());
        assertEquals(new HashSet<>(FULL_DECK), new HashSet<>(all));
    }

    /** The deck's cards as a set (order ignored). */
    static HashSet<FrenchCard> asSet(Deck<FrenchCard> deck) {
        return new HashSet<>(deck.getComponents());
    }

    static HashSet<FrenchCard> setOf(String... codes) {
        return new HashSet<>(cards(codes));
    }
}
