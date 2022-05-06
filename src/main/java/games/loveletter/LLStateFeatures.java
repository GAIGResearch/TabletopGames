package games.loveletter;

import core.AbstractGameState;
import core.components.PartialObservableDeck;
import core.interfaces.IComponentContainer;
import core.interfaces.IStateFeatureVector;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static games.loveletter.cards.LoveLetterCard.CardType.getMaxCardValue;
import static java.util.stream.Collectors.toList;

public class LLStateFeatures implements IStateFeatureVector {

    String[] names = new String[]{"AFFECTION", "ADVANTAGE", "OUR_TURN", "HAS_WON", "FINAL_POSITION", "BIAS",
            "GUARD", "PRIEST", "BARON", "HANDMAID", "PRINCE", "COUNTESS", "PRINCESS",
            "GUARD_KNOWN", "PRIEST_KNOWN", "BARON_KNOWN", "HANDMAID_KNOWN", "PRINCE_KNOWN", "COUNTESS_KNOWN", "PRINCESS_KNOWN",
            "GUARD_DISCARD", "PRIEST_DISCARD", "BARON_DISCARD", "HANDMAID_DISCARD", "PRINCE_DISCARD", "COUNTESS_DISCARD",
            "GUARD_OTHER", "PRIEST_OTHER", "BARON_OTHER", "HANDMAID_OTHER", "PRINCE_OTHER", "COUNTESS_OTHER", "PRINCESS_OTHER",
            "HIDDEN", "CARDS", "DRAW_DECK"
    };

    @Override
    public double[] featureVector(AbstractGameState gs, int playerID) {
        LoveLetterGameState state = (LoveLetterGameState) gs;
        LoveLetterParameters params = (LoveLetterParameters) gs.getGameParameters();
        Utils.GameResult playerResult = gs.getPlayerResults()[playerID];

        double[] retValue = new double[names.length];
        int cardsOwnedOffset = 5;
        int cardsKnownOffset = 12;
        int discardOffset = 19;
        int otherOffset = 25;

        double cardValues = 0;
        Set<LoveLetterCard.CardType> cardTypes = new HashSet<>();
        PartialObservableDeck<LoveLetterCard> hand = state.getPlayerHandCards().get(playerID);
        for (int i = 0; i < hand.getSize(); i++) {
            boolean[] visibility = hand.getVisibilityOfComponent(i);
            LoveLetterCard card = hand.get(i);
            cardValues += card.cardType.getValue();
            cardTypes.add(card.cardType);
            int value = card.cardType.getValue();
            retValue[cardsOwnedOffset + value] = 1.0;
            for (int j = 0; j < visibility.length; j++) {
                if (j == playerID)
                    continue;
                if (visibility[j]) {
                    retValue[cardsKnownOffset + value] = 1.0;
                    break;
                }
            }
        }

        double maxCardValue = 1 + state.getPlayerHandCards().get(playerID).getSize() * getMaxCardValue();
        double nRequiredTokens = (state.getNPlayers() == 2 ? params.nTokensWin2 : state.getNPlayers() == 3 ? params.nTokensWin3 : params.nTokensWin4);
        if (nRequiredTokens < state.affectionTokens[playerID]) nRequiredTokens = state.affectionTokens[playerID];

        int visibleCards = 0;
        for (int player = 0; player < state.getNPlayers(); player++) {
            if (player != playerID) {
                PartialObservableDeck<LoveLetterCard> deck = state.getPlayerHandCards().get(player);
                for (int i = 0; i < deck.getSize(); i++) {
                    if (deck.getVisibilityOfComponent(i)[playerID]) {
                        visibleCards++;
                        retValue[otherOffset + deck.getComponents().get(i).cardType.getValue()] = 1.0;
                    }
                }
                visibleCards += (int) IntStream.range(0, deck.getSize()).filter(i -> deck.getVisibilityForPlayer(i, playerID)).count();
            }
        }
        List<LoveLetterCard> discardDecks = state.getPlayerDiscardCards().stream()
                .flatMap(IComponentContainer::stream)
                .collect(toList());
        for (LoveLetterCard discard : discardDecks) {
            retValue[discardOffset + discard.cardType.getValue()] += 1.0;
        }
        // divide by total cards
        retValue[1 + discardOffset] /= 5.0;
        for (int i = 2; i <= 5; i++)
            retValue[i + discardOffset] /= 2.0;

        int maxOtherScore = IntStream.range(0, state.getNPlayers())
                .filter(p -> p != playerID)
                .map(p -> (int) state.getGameScore(p)).max().orElseThrow(() -> new AssertionError("??"));

        retValue[0] = state.affectionTokens[playerID] / nRequiredTokens;
        retValue[1] = ((state.affectionTokens[playerID] - maxOtherScore) / nRequiredTokens);
        retValue[2] = state.getCurrentPlayer() == playerID ? 1.0 : 0.0;
        retValue[3] = playerResult == Utils.GameResult.WIN ? 1.0 : 0.0;
        retValue[4] = state.isNotTerminal() ? 0.0 : state.getOrdinalPosition(playerID);
        retValue[5] = 1.0;
        retValue[names.length - 2] = cardValues / maxCardValue;
        retValue[names.length - 3] = visibleCards / (state.getNPlayers() - 1.0);
        retValue[names.length - 1] = state.getDrawPile().getSize() / 16.0;

        return retValue;
    }

    @Override
    public String[] names() {
        return names;
    }
}
