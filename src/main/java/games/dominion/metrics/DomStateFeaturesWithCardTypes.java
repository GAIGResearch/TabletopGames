package games.dominion.metrics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import games.dominion.DominionConstants;
import games.dominion.DominionForwardModel;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class DomStateFeaturesWithCardTypes implements IStateFeatureVector {

    DomStateFeaturesReduced domStateFeaturesReduced = new DomStateFeaturesReduced();

    String[] names;
    {
        // then add on once feature for each CardType
        String[] cardTypeNames = Arrays.stream(CardType.values())
                .map(CardType::name)
                .toArray(String[]::new);
        names = new String[domStateFeaturesReduced.names.length + cardTypeNames.length];
        System.arraycopy(domStateFeaturesReduced.names, 0, names, 0, domStateFeaturesReduced.names.length);
        System.arraycopy(cardTypeNames, 0, names, domStateFeaturesReduced.names.length, cardTypeNames.length);
    }
    DominionForwardModel fm = new DominionForwardModel();

    @Override
    public double[] doubleVector(AbstractGameState gs, int playerId) {
        DominionGameState state = (DominionGameState) gs;

        double[] retValue = new double[names.length];

        double[] baseFeatures = domStateFeaturesReduced.doubleVector(gs, playerId);
        System.arraycopy(baseFeatures, 0, retValue, 0, domStateFeaturesReduced.names.length);

        Map<CardType, Long> playerDeck = state.getDeck(DominionConstants.DeckType.HAND, playerId).stream()
                .map(DominionCard::cardType)
                .collect(groupingBy(c -> c, Collectors.counting()));

        // we rely on the fact that the order of CardType.values() is consistent
        for (int i = 0; i < CardType.values().length; i++) {
            CardType cardType = CardType.values()[i];
            long count = playerDeck.getOrDefault(cardType, 0L);
            retValue[baseFeatures.length + i] = count;
        }

        return retValue;
    }

    @Override
    public String[] names() {
        return names;
    }

}
