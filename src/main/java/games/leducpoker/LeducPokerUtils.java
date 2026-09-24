package games.leducpoker;

import core.components.FrenchCard;

public class LeducPokerUtils {

    private LeducPokerUtils() {
    }

    /**
     * The rank of a card, from 0 for the lowest.
     */
    public static int rankValue(FrenchCard card) {
        return LeducPokerParameters.RANKS.indexOf(card.type);
    }

    /**
     * The winner of a showdown: 0 or 1, or -1 for a tie.
     *
     * @param highCardUsesBoard LeducPokerParameters.highCardUsesBoard
     */
    public static int showdownWinner(FrenchCard card0, FrenchCard card1, FrenchCard board, boolean highCardUsesBoard) {
        // a card that pairs the board wins
        boolean pair0 = card0.type == board.type;
        boolean pair1 = card1.type == board.type;
        if (pair0 != pair1)
            return pair0 ? 0 : 1;
        // otherwise the higher card wins: the private card, or with highCardUsesBoard (from the Valet RECYCLE code)
        // the higher of the private card and the board card
        int rank0 = rankValue(card0);
        int rank1 = rankValue(card1);
        if (highCardUsesBoard) {
            rank0 = Math.max(rank0, rankValue(board));
            rank1 = Math.max(rank1, rankValue(board));
        }
        if (rank0 == rank1)
            return -1;
        return rank0 > rank1 ? 0 : 1;
    }
}
