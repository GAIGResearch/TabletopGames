package games.dominion.metrics;

import core.AbstractGameState;
import core.components.Deck;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;
import players.heuristics.AbstractStateFeature;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static games.dominion.DominionConstants.DeckType.*;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

public class DomStateFeatures extends AbstractStateFeature {

    String[] localNames;
    List<CardType> cardTypes = Arrays.stream(CardType.values()).collect(Collectors.toList());
    String[] cardNames = Arrays.stream(CardType.values()).map(CardType::name).toArray(String[]::new);
    int baseFeatureCount = 7;

    public DomStateFeatures() {
        String[] baseFeatureNames = new String[]{"TREASURE", "ACTION", "TR_H", "AC_H", "AC_LEFT", "BUY_LEFT", "TOT_CRDS"};
        if (baseFeatureNames.length != baseFeatureCount)
            throw new AssertionError("Inconsistent Data in DomStateFeatures");
        localNames = new String[baseFeatureCount + cardNames.length * 3];
        // In owned, in hand, left to buy
        System.arraycopy(baseFeatureNames, 0, localNames, 0, baseFeatureNames.length);
        for (int i = 0; i < cardNames.length; i++) {
            localNames[baseFeatureCount + i * 3 + 0] = cardNames[i] + "_IN_DECK";
            localNames[baseFeatureCount + i * 3 + 1] = cardNames[i] + "_IN_HAND";
            localNames[baseFeatureCount + i * 3 + 2] = cardNames[i] + "_IN_SUPPLY";
        }
    }

    @Override
    public double[] localFeatureVector(AbstractGameState gs, int playerId) {
        DominionGameState state = (DominionGameState) gs;
        double[] retValue = new double[localNames.length];

        // treasureValue - total treasure divided by 50
        retValue[0] = state.getTotal(playerId, DominionCard::treasureValue) / 50.0;

        // actionCards - percentage of deck made of action cards
        retValue[1] = state.getTotal(playerId, c -> c.isActionCard() ? 1 : 0) /
                (double) state.getTotalCards(playerId);

        // treasureInHand - total treasure in hand divided by 20
        retValue[2] = state.getTotal(playerId, HAND, DominionCard::treasureValue) / 20.0;

        // actionCardsInHand - number / 5 of actionCards In Hand
        retValue[3] = state.getTotal(playerId, HAND, c -> c.isActionCard() ? 1 : 0) / 5.0;

        // actionsLeft / 5.
        if (state.getCurrentPlayer() == playerId)
            retValue[4] = state.actionsLeft() / 5.0;

        // buysLeft / 5
        if (state.getCurrentPlayer() == playerId)
            retValue[5] = state.buysLeft() / 5.0;

        // Total cards / 40
        retValue[6] = state.getTotalCards(playerId) / 40.0;

        // The next set are probably most efficiently done by going through the supply, player deck and hand
        // This avoids computational effort on all the cards that are not in the game
        for (CardType card : state.cardsIncludedInGame()) {
            int index = cardTypes.indexOf(card);
     //       retValue[baseFeatureCount + index * 4] = 1.0;
            retValue[baseFeatureCount + index * 3 + 2] = state.cardsOfType(card, -1, DominionConstants.DeckType.SUPPLY) / 10.0;
        }
        List<CardType> hand = state.getDeck(HAND, playerId).stream().map(DominionCard::cardType).collect(Collectors.toList());
        Deck<DominionCard> deck = state.getDeck(DRAW, playerId).copy();
        deck.add(state.getDeck(DISCARD, playerId));
        Map<CardType, Long> allCards = deck.stream().collect(groupingBy(DominionCard::cardType, counting()));
        for (CardType card : allCards.keySet()) {
            int index = cardTypes.indexOf(card);
            retValue[baseFeatureCount + index * 3 + 0] = allCards.get(card) / 5.0;
            if (hand.contains(card))
                retValue[baseFeatureCount + index * 3 + 1] = 1.0;
        }

        return retValue;
    }

    @Override
    protected double maxScore() {
        return 50.0;
    }

    @Override
    protected double maxRounds() {
        return 50.0;
    }

    @Override
    public String[] localNames() {
        return localNames;
    }
}
