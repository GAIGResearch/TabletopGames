package games.loveletter;

import core.AbstractGameState;
import core.components.PartialObservableDeck;
import core.interfaces.IComponentContainer;
import games.loveletter.cards.LoveLetterCard;
import players.heuristics.AbstractStateFeature;

import java.util.List;
import java.util.stream.IntStream;

import static games.loveletter.cards.LoveLetterCard.CardType.getMaxCardValue;
import static java.util.stream.Collectors.toList;

public class LLStateFeatures extends AbstractStateFeature {

    String[] localNames = new String[]{
            "PROTECTED", "HIDDEN", "CARDS", "DRAW_DECK",
            "GUARD", "PRIEST", "BARON", "HANDMAID", "PRINCE", "KING", "COUNTESS", "PRINCESS",
            "GUARD_KNOWN", "PRIEST_KNOWN", "BARON_KNOWN", "HANDMAID_KNOWN", "PRINCE_KNOWN", "KING_KNOWN", "COUNTESS_KNOWN", "PRINCESS_KNOWN",
            "GUARD_DISCARD", "PRIEST_DISCARD", "BARON_DISCARD", "HANDMAID_DISCARD", "PRINCE_DISCARD", "KING_DISCARD", "COUNTESS_DISCARD", "PRINCESS_DISCARD",
            "GUARD_OTHER", "PRIEST_OTHER", "BARON_OTHER", "HANDMAID_OTHER", "PRINCE_OTHER", "KING_OTHER", "COUNTESS_OTHER", "PRINCESS_OTHER"
    };

    @Override
    protected double maxScore() {
        return 5.0;
    }

    @Override
    protected double maxRounds() {
        return 10.0;
    }

    @Override
    protected String[] localNames() {
        return localNames;
    }

    @Override
    protected double[] localFeatureVector(AbstractGameState gs, int playerID) {
        LoveLetterGameState state = (LoveLetterGameState) gs;

        double[] retValue = new double[localNames.length];
        int cardsOwnedOffset = 3;
        int cardsKnownOffset = 11;
        int discardOffset = 19;
        int otherOffset = 27;

        double cardValues = 0;
        PartialObservableDeck<LoveLetterCard> hand = state.getPlayerHandCards().get(playerID);
        for (int i = 0; i < hand.getSize(); i++) {
            boolean[] visibility = hand.getVisibilityOfComponent(i);
            LoveLetterCard card = hand.get(i);
            cardValues += card.cardType.getValue();
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

        retValue[0] = state.isNotProtected(playerID) ? 0.0 : 1.0;
        retValue[1] = visibleCards / (state.getNPlayers() - 1.0);
        retValue[2] = cardValues / maxCardValue;
        retValue[3] = state.getDrawPile().getSize() / 16.0;

        return retValue;
    }

}
