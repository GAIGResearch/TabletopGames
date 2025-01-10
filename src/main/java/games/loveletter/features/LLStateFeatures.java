package games.loveletter.features;

import core.AbstractGameState;
import core.components.PartialObservableDeck;
import core.interfaces.IComponentContainer;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.CardType;
import games.loveletter.cards.LoveLetterCard;
import players.heuristics.AbstractStateFeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static games.loveletter.cards.CardType.Guard;
import static games.loveletter.cards.CardType.getMaxCardValue;
import static java.util.stream.Collectors.toList;
import static utilities.Utils.enumNames;

public class LLStateFeatures extends AbstractStateFeature {

    final String[] localNames;
    final int baseFeatures = 4;

    public LLStateFeatures() {
        List<String> allNames = new ArrayList<>(Arrays.asList("PROTECTED", "HIDDEN", "CARDS", "DRAW_DECK"));
        // For each card type, do we have one in hand
        allNames.addAll(enumNames(CardType.class).stream().map(s -> s + "_HAND").toList());
        // Card is in Hand and is also Known to at least one other player
        allNames.addAll(enumNames(CardType.class).stream().map(s -> s + "_KNOWN").toList());
        // Percentage of card type in Discard
        allNames.addAll(enumNames(CardType.class).stream().map(s -> s + "_DISCARD").toList());
        // Card is known to be in hand of another player
        allNames.addAll(enumNames(CardType.class).stream().map(s -> s + "_OTHER").toList());
        localNames = allNames.toArray(new String[0]);
    }

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
        int featuresPerGroup = CardType.values().length;

        double cardValues = 0;
        PartialObservableDeck<LoveLetterCard> hand = state.getPlayerHandCards().get(playerID);
        for (int i = 0; i < hand.getSize(); i++) {
            boolean[] visibility = hand.getVisibilityOfComponent(i);
            LoveLetterCard card = hand.get(i);
            cardValues += card.cardType.getValue();
            int value = card.cardType.ordinal();
            retValue[baseFeatures + value] = 1.0;
            for (int j = 0; j < visibility.length; j++) {
                if (j == playerID)
                    continue;
                if (visibility[j]) {
                    retValue[baseFeatures + featuresPerGroup + value] = 1.0;
                    break;
                }
            }
        }

        int visibleCards = 0;
        for (int player = 0; player < state.getNPlayers(); player++) {
            if (player != playerID) {
                PartialObservableDeck<LoveLetterCard> deck = state.getPlayerHandCards().get(player);
                for (int i = 0; i < deck.getSize(); i++) {
                    if (deck.getVisibilityOfComponent(i)[playerID]) {
                        visibleCards++;
                        retValue[baseFeatures + 3 * featuresPerGroup + deck.getComponents().get(i).cardType.ordinal()] = 1.0;
                    }
                }
                visibleCards += (int) IntStream.range(0, deck.getSize()).filter(i -> deck.getVisibilityForPlayer(i, playerID)).count();
            }
        }
        List<LoveLetterCard> discardDecks = state.getPlayerDiscardCards().stream()
                .flatMap(IComponentContainer::stream)
                .toList();
        for (LoveLetterCard discard : discardDecks) {
            retValue[baseFeatures + 2 * featuresPerGroup + discard.cardType.ordinal()] += 1.0;
        }

        retValue[0] = state.isProtected(playerID) ? 1.0 : 0.0;
        retValue[1] = visibleCards / (state.getNPlayers() - 1.0);
        retValue[2] = cardValues;
        retValue[3] = state.getDrawPile().getSize() ;

        return retValue;
    }

}
