package games.klaverjassen;

import core.components.FrenchCard;
import games.tricktaking.CardOrder;
import games.tricktaking.Trick;

import java.util.ArrayList;
import java.util.List;

public final class KlaverjassenUtils {

    private KlaverjassenUtils() {
    }

    /**
     * The cards in the hand that may be played to the trick under the Amsterdam rules, in hand order.
     *
     * @param rule what may be played when unable to follow suit while partner is winning with a trump
     */
    public static List<FrenchCard> legalPlays(List<FrenchCard> hand, Trick trick, FrenchCard.Suite trumps,
                                              KlaverjassenParameters.PartnerTrumpRule rule) {
        // any card may be led
        if (trick.getSize() == 0)
            return new ArrayList<>(hand);
        CardOrder order = trick.getOrder();
        FrenchCard.Suite lead = trick.getLeadSuit();
        // the hand belongs to the next player to play; players p and p + 2 are partners
        int player = trick.playerOf(trick.getSize());
        int winner = trick.winner(trumps);
        FrenchCard winningCard = trick.get(indexOf(trick, winner));
        boolean winningTrump = winningCard.suite == trumps;
        // the trumps that beat the winning card (every trump beats a card that is not a trump)
        List<FrenchCard> higherTrumps = hand.stream()
                .filter(c -> c.suite == trumps && (!winningTrump || order.rank(c) > order.rank(winningCard)))
                .toList();
        List<FrenchCard> trumpsHeld = hand.stream().filter(c -> c.suite == trumps).toList();
        List<FrenchCard> nonTrumps = hand.stream().filter(c -> c.suite != trumps).toList();

        if (lead == trumps) {
            // when trumps are led, the highest trump is winning: overtrump it if able, else play any trump
            if (!higherTrumps.isEmpty())
                return higherTrumps;
            if (!trumpsHeld.isEmpty())
                return trumpsHeld;
        } else if (hand.stream().anyMatch(c -> c.suite == lead)) {
            // any card of the suit led, with no need to beat the winning card
            return hand.stream().filter(c -> c.suite == lead).toList();
        }

        // the player cannot follow suit
        boolean partnerWinning = winner % 2 == player % 2;
        if (!partnerWinning) {
            // trump the opponent if able, and never undertrump unless holding nothing else
            if (!higherTrumps.isEmpty())
                return higherTrumps;
            return nonTrumps.isEmpty() ? new ArrayList<>(hand) : nonTrumps;
        }
        // partner is winning: anything may be played, unless partner's card is a trump
        if (!winningTrump)
            return new ArrayList<>(hand);
        List<FrenchCard> allowed = switch (rule) {
            case DISCARD -> nonTrumps;
            case NO_UNDERTRUMP -> hand.stream().filter(c -> c.suite != trumps || higherTrumps.contains(c)).toList();
        };
        // a hand holding only lower trumps may play any of them
        return allowed.isEmpty() ? new ArrayList<>(hand) : allowed;
    }

    /**
     * The roem (bonus points) scored by the cards of one trick.
     */
    public static int roem(List<FrenchCard> cards, FrenchCard.Suite trumps, KlaverjassenParameters params) {
        int roem = 0;
        // the longest run of cards of one suit in the order A K Q J 10 9 8 7 scores. FrenchCard numbers follow that
        // order: Seven to Ten, then Jack 11, Queen 12, King 13 and Ace 14
        int longestRun = 0;
        for (FrenchCard.Suite suit : FrenchCard.Suite.values()) {
            boolean[] held = new boolean[15];
            for (FrenchCard c : cards)
                if (c.suite == suit)
                    held[c.number] = true;
            int run = 0;
            for (int n = 7; n <= 14; n++) {
                run = held[n] ? run + 1 : 0;
                longestRun = Math.max(longestRun, run);
            }
        }
        if (longestRun >= 4)
            roem += params.runOfFourBonus;
        else if (longestRun == 3)
            roem += params.runOfThreeBonus;

        // stuk: the King and Queen of trumps, scored as well as any run
        boolean trumpKing = cards.stream().anyMatch(c -> c.suite == trumps && c.number == 13);
        boolean trumpQueen = cards.stream().anyMatch(c -> c.suite == trumps && c.number == 12);
        if (trumpKing && trumpQueen)
            roem += params.stukBonus;

        // four Jacks, or four Kings, Queens, Aces or Tens
        if (cards.size() == 4 && cards.stream().allMatch(c -> c.number == cards.get(0).number)) {
            int number = cards.get(0).number;
            if (number == 11)
                roem += params.fourJacksBonus;
            else if (number >= 10)
                roem += params.fourOfAKindBonus;
        }
        return roem;
    }

    private static int indexOf(Trick trick, int player) {
        for (int i = 0; i < trick.getSize(); i++) {
            if (trick.playerOf(i) == player)
                return i;
        }
        throw new IllegalArgumentException("Player " + player + " has not played to the trick");
    }
}
