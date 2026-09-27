package games.sueca;

import core.AbstractPlayer;
import core.Game;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import games.tricktaking.PlayCard;
import games.tricktaking.Trick;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static games.tricktaking.TrickTakingTestUtils.card;
import static games.tricktaking.TrickTakingTestUtils.cards;
import static org.junit.Assert.assertEquals;

/**
 * Helpers to arrange Sueca states. Every arrange helper moves cards from wherever they currently are (hands, trick,
 * team piles), so the 40 cards are always conserved. Card codes are as TrickTakingTestUtils.card ("7H", "JS", "AC").
 * Team 0 is players 0 and 2, team 1 players 1 and 3.
 */
final class SuecaTestUtils {

    /**
     * The 40-card pack: a 52-card pack without the 8s, 9s and 10s (FrenchCard numbers Aces as 14).
     */
    static final List<FrenchCard> FULL_PACK = FrenchCard.generateDeck("FullPack", VISIBLE_TO_ALL).getComponents()
            .stream().filter(c -> c.number < 8 || c.number > 10).toList();

    /**
     * Rank order from the rules, lowest first: 2 3 4 5 6 Q J K 7 A (FrenchCard numbers J 11, Q 12, K 13, A 14).
     */
    static final List<Integer> RANK_ORDER = List.of(2, 3, 4, 5, 6, 12, 11, 13, 7, 14);

    /**
     * A complete arranged deal of ten cards each, every suit A 7 K J Q 6 5 4 3 2:
     * spades A 2 Q to player 0, 7 3 J to 1, K 4 to 2, 5 6 to 3; hearts 7 3 / A 4 / K 5 6 / J Q 2;
     * diamonds K 4 6 / Q 5 / J 2 / A 7 3; clubs J 5 / K 6 2 / A 3 Q / 7 4. Use with trump card 2H (player 3, the dealer).
     */
    static final String[][] DEAL = {
            {"AS", "2S", "7H", "3H", "KD", "4D", "JC", "5C", "QS", "6D"},
            {"7S", "3S", "AH", "4H", "QD", "5D", "KC", "6C", "2C", "JS"},
            {"KS", "4S", "KH", "5H", "JD", "2D", "AC", "3C", "6H", "QC"},
            {"5S", "6S", "JH", "QH", "2H", "AD", "7D", "3D", "7C", "4C"}
    };

    private SuecaTestUtils() {
    }

    /**
     * A real 4-player game from the factory, reset with random players, for integration tests driven by fm.next.
     */
    static Game newGame(long seed) {
        return newGame(seed, null);
    }

    /**
     * As newGame(seed), with the given parameters (null for the defaults). The seed overrides any seed on params.
     */
    static Game newGame(long seed, SuecaParameters params) {
        // never pass null: GameType.createParameters(seed) ignores the seed for a Parameters class without a
        // Long constructor, and the game would not be reproducible
        Game game = GameType.Sueca.createGameInstance(4, seed, params == null ? new SuecaParameters() : params);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < 4; p++)
            players.add(new RandomPlayer(new Random(seed + p)));
        game.reset(players);
        return game;
    }

    /**
     * A state from direct setup with default parameters and the given seed, for unit tests.
     */
    static SuecaGameState newState(long seed) {
        return newState(seed, new SuecaParameters());
    }

    /**
     * A state from direct setup with the given parameters and seed, for unit tests.
     */
    static SuecaGameState newState(long seed, SuecaParameters params) {
        params.setRandomSeed(seed);
        SuecaGameState state = new SuecaGameState(params, 4);
        new SuecaForwardModel().setup(state);
        return state;
    }

    static List<Deck<FrenchCard>> allDecks(SuecaGameState state) {
        List<Deck<FrenchCard>> decks = new ArrayList<>(state.playerHands);
        decks.add(state.currentTrick);
        decks.addAll(state.teamPiles);
        return decks;
    }

    private static void takeFromWherever(SuecaGameState state, FrenchCard card) {
        for (Deck<FrenchCard> deck : allDecks(state)) {
            if (deck.contains(card)) {
                deck.remove(card);
                return;
            }
        }
        throw new IllegalArgumentException(card + " is not in any deck");
    }

    /**
     * Make this card the trump card, so its suit is trumps. Does not move the card.
     */
    static void setTrumpCard(SuecaGameState state, String code) {
        state.trumpCard = card(code);
    }

    /**
     * Replace every hand: hands[p] becomes player p's hand (in the order given), taking the cards from wherever they
     * are. The current trick is emptied (keeping its leader). Every card in no listed hand ends on team 0's pile, as
     * if played in earlier tricks - call setTeamPile afterwards to split them between the teams.
     */
    @SafeVarargs
    static void arrangeHands(SuecaGameState state, List<FrenchCard>... hands) {
        for (Deck<FrenchCard> hand : state.playerHands) {
            state.teamPiles.get(0).add(hand);
            hand.clear();
        }
        state.teamPiles.get(0).add(state.currentTrick);
        state.currentTrick.clear();
        for (int p = 0; p < hands.length; p++) {
            for (FrenchCard c : hands[p]) {
                takeFromWherever(state, c);
                state.playerHands.get(p).addToBottom(c);
            }
        }
    }

    /**
     * Arrange the complete ten-card hands of DEAL, with trump card 2H (Hearts trumps) in player 3's (the dealer's)
     * hand, both team piles and the trick empty, player 0 to lead.
     */
    static void arrangeDeal(SuecaGameState state) {
        arrangeHands(state, cards(DEAL[0]), cards(DEAL[1]), cards(DEAL[2]), cards(DEAL[3]));
        setTrumpCard(state, "2H");
        arrangeTrick(state, 0);
    }

    /**
     * Replace the current trick (whose cards go to the bottom of team 0's pile) by one led by the given player, with
     * SuecaUtils.CARD_ORDER, holding these cards in play order (taken from wherever they are); make the next player
     * to play to it the current player. Does not use Trick.play, so records no voids.
     */
    static void arrangeTrick(SuecaGameState state, int leader, String... codes) {
        state.teamPiles.get(0).add(state.currentTrick);
        state.currentTrick.clear();
        state.currentTrick = new Trick<>("CurrentTrick", 4, leader, SuecaUtils.CARD_ORDER);
        for (String code : codes) {
            FrenchCard c = card(code);
            takeFromWherever(state, c);
            state.currentTrick.addToBottom(c);
        }
        state.setTurnOwner((leader + codes.length) % 4);
    }

    /**
     * Make the team's pile exactly these cards, taken from wherever they are; the pile's other cards go to the other
     * team's pile. Call it after arrangeHands / arrangeTrick, which put cards on team 0's pile.
     */
    static void setTeamPile(SuecaGameState state, int team, List<FrenchCard> pileCards) {
        Deck<FrenchCard> pile = state.teamPiles.get(team);
        Deck<FrenchCard> other = state.teamPiles.get(1 - team);
        for (FrenchCard c : new ArrayList<>(pile.getComponents())) {
            pile.remove(c);
            other.addToBottom(c);
        }
        for (FrenchCard c : pileCards) {
            takeFromWherever(state, c);
            pile.addToBottom(c);
        }
    }

    /**
     * Arrange the 10th (last) trick of the deal: player p holds just lastCards[p], an empty trick led by the leader
     * (who is to play), team 0's pile exactly team0Pile, and team 1's pile every other card.
     */
    static void arrangeLastTrick(SuecaGameState state, int leader, List<FrenchCard> team0Pile, String... lastCards) {
        arrangeHands(state, cards(lastCards[0]), cards(lastCards[1]), cards(lastCards[2]), cards(lastCards[3]));
        arrangeTrick(state, leader);
        setTeamPile(state, 0, team0Pile);
    }

    /**
     * Parameters for pagat's rubber (playRubber) with the given number of games to win.
     */
    static SuecaParameters rubberParams(int targetGames) {
        SuecaParameters params = new SuecaParameters();
        params.setParameterValue("playRubber", true);
        params.setParameterValue("targetGames", targetGames);
        return params;
    }

    /**
     * A rubber state (playRubber, targetGames) from direct setup, for unit tests.
     */
    static SuecaGameState newRubberState(long seed, int targetGames) {
        return newState(seed, rubberParams(targetGames));
    }

    /**
     * Arrange the last trick of a deal and play it through fm.next: team 0's pile is exactly team0Pile, team 1's pile
     * every other card, Diamonds trumps (the 2D is on a pile, already played), and the last trick 5S 2S 3S 4S, led
     * by player 0 with the 5S, which wins it for team 0 - a trick worth 0 card points. So after it team 0 has the
     * card points of team0Pile and team0Pile.size() / 4 + 1 tricks. team0Pile must not hold 2S 3S 4S 5S.
     */
    static void playZeroLastTrickWonByTeam0(SuecaGameState state, SuecaForwardModel fm, List<FrenchCard> team0Pile) {
        arrangeLastTrick(state, 0, team0Pile, "5S", "2S", "3S", "4S");
        setTrumpCard(state, "2D");
        assertEquals(0, state.getCurrentPlayer());
        playCards(state, fm, "5S", "2S", "3S", "4S");
    }

    static void playCards(SuecaGameState state, SuecaForwardModel fm, String... codes) {
        for (String code : codes)
            fm.next(state, new PlayCard<>(card(code)));
    }

    /**
     * All 40 cards of the pack, each exactly once, across the hands, the current trick and the team piles.
     */
    static void assertAllCardsPresent(SuecaGameState state) {
        List<FrenchCard> all = new ArrayList<>();
        allDecks(state).forEach(d -> all.addAll(d.getComponents()));
        assertEquals("number of cards", 40, all.size());
        assertEquals(new HashSet<>(FULL_PACK), new HashSet<>(all));
    }

    /**
     * Oracle from the rules: the card points of the card.
     */
    static int expectedCardPoints(FrenchCard c) {
        return switch (c.number) {
            case 14 -> 11;      // Ace
            case 7 -> 10;
            case 13 -> 4;       // King
            case 11 -> 3;       // Jack
            case 12 -> 2;       // Queen
            default -> 0;
        };
    }

    static int expectedCardPoints(List<FrenchCard> cards) {
        return cards.stream().mapToInt(SuecaTestUtils::expectedCardPoints).sum();
    }

    /**
     * Oracle from the rules: the index in the (partial or complete) trick of the card winning it.
     */
    static int expectedWinningIndex(List<FrenchCard> trick, FrenchCard.Suite trumps) {
        int best = 0;
        for (int i = 1; i < trick.size(); i++) {
            FrenchCard c = trick.get(i), b = trick.get(best);
            if (c.suite == b.suite) {
                // a higher card of the suit led, or a higher trump
                if (RANK_ORDER.indexOf(c.number) > RANK_ORDER.indexOf(b.number)) best = i;
            } else if (c.suite == trumps) {
                // the first trump beats any card of the suit led; any other suit is a discard
                best = i;
            }
        }
        return best;
    }

    /**
     * Oracle from the rules: the cards of the hand (in hand order) that may be played to the (partial) trick.
     */
    static List<FrenchCard> expectedLegalPlays(List<FrenchCard> hand, List<FrenchCard> trick) {
        if (trick.isEmpty())
            return new ArrayList<>(hand);       // the leader may lead any card
        FrenchCard.Suite led = trick.get(0).suite;
        // follow the suit led if possible, otherwise play any card
        List<FrenchCard> follow = hand.stream().filter(c -> c.suite == led).toList();
        return new ArrayList<>(follow.isEmpty() ? hand : follow);
    }
}
