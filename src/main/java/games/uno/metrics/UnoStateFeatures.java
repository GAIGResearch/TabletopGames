package games.uno.metrics;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Deck;
import core.interfaces.IStateFeatureVector;
import games.uno.UnoGameParameters;
import games.uno.UnoGameState;
import games.uno.cards.UnoCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UnoStateFeatures implements IStateFeatureVector {

    static String[] cardTypeNames = Arrays.stream(UnoCard.UnoCardType.values()).map(Enum::name).toArray(String[]::new);
    static String[] otherNames = new String[]{"playerScore", "leaderMargin", "cardsInHand",
            "cardsInDeck", "playersKnockedOut", "diffColorsNumber", "diffNumbersNumber", "minOpponentCards"};
    static String[] allNames;
    static {
        allNames = new String[otherNames.length + cardTypeNames.length];
        System.arraycopy(otherNames, 0, allNames, 0, otherNames.length);
        System.arraycopy(cardTypeNames, 0, allNames, otherNames.length, cardTypeNames.length);
    }

    @Override
    public String[] names() {
        return allNames;
    }

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        UnoGameState ugs = (UnoGameState) state;
        UnoGameParameters ugp = (UnoGameParameters) state.getGameParameters();
        double[] data = new double[allNames.length];

        // playerScorePct: player score (as percentage of the required winning score)
        data[0] = (double) ugs.getPlayerScore()[playerID] / ugp.nWinPoints;

        // leaderMargin: leader margin
        double playerScore = ugs.getPlayerScore()[playerID];
        double bestOtherScore = -1e9;
        for (int i = 0; i < ugs.getNPlayers(); i++) {
            if (i != playerID) {
                if (ugs.getPlayerScore()[i] > bestOtherScore) {
                    bestOtherScore = ugs.getPlayerScore()[i];
                }
            }
        }
        data[1] = (playerScore - bestOtherScore) / ugp.nWinPoints;

        // cardsInHand: cards in hand
        Deck<UnoCard> playerHand = ugs.getPlayerDecks().get(playerID);
        data[2] = playerHand.getSize();

        // cardsInDeck: cards in deck
        data[3] = ugs.getDrawDeck().getSize();

        // playersKnockedOut: players knocked out
        int knockedOut = 0;
        for (int i = 0; i < ugs.getNPlayers(); i++) {
            if (ugs.getPlayerResults()[i] != CoreConstants.GameResult.GAME_ONGOING) {
                knockedOut++;
            }
        }
        data[4] = knockedOut;

        // count of each type of UnoCard in hand
        Set<String> numberColors = new HashSet<>();
        Set<Integer> numberValues = new HashSet<>();
        for (UnoCard card : playerHand.getComponents()) {
            int typeIndex = card.unoCardType.ordinal();
            data[otherNames.length + typeIndex] += 1.0;
            if (card.unoCardType == UnoCard.UnoCardType.Number) {
                numberColors.add(card.color);
                numberValues.add(card.number);
            }
        }

        // diffColorsNumber: number of different colors for Number cards
        data[5] = numberColors.size();

        // diffNumbersNumber: number of different numbers for Number cards
        data[6] = numberValues.size();

        // minOpponentCards: lowest number of cards any other player has in hand
        int minOpponentCards = Integer.MAX_VALUE;
        for (int i = 0; i < ugs.getNPlayers(); i++) {
            if (i != playerID) {
                int opponentCards = ugs.getPlayerDecks().get(i).getSize();
                if (opponentCards < minOpponentCards) {
                    minOpponentCards = opponentCards;
                }
            }
        }
        data[7] = minOpponentCards == Integer.MAX_VALUE ? 0 : minOpponentCards;

        return data;
    }
}
