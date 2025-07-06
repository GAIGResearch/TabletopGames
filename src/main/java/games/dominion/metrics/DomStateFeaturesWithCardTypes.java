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
    List<CardType> exclusionList = Arrays.asList(CardType.ESTATE, CardType.DUCHY, CardType.PROVINCE);
    List<CardType> inclusionList = Arrays.stream(CardType.values())
            .filter(c -> !exclusionList.contains(c))
            .collect(Collectors.toList());

    String[] names;
    {
        // then add on once feature for each CardType
        String[] cardTypeNames = inclusionList.stream()
                .map(CardType::name)
                .toArray(String[]::new);
        names = new String[domStateFeaturesReduced.names.length + cardTypeNames.length];
        System.arraycopy(domStateFeaturesReduced.names, 0, names, 0, domStateFeaturesReduced.names.length);
        System.arraycopy(cardTypeNames, 0, names, domStateFeaturesReduced.names.length, cardTypeNames.length);
    }

    @Override
    public double[] doubleVector(AbstractGameState gs, int playerId) {
        DominionGameState state = (DominionGameState) gs;

        double[] retValue = new double[names.length];

        double[] baseFeatures = domStateFeaturesReduced.doubleVector(gs, playerId);
        System.arraycopy(baseFeatures, 0, retValue, 0, domStateFeaturesReduced.names.length);

        // we rely on the fact that the order of CardType.values() is consistent
        for (int i = 0; i < inclusionList.size(); i++) {
            CardType cardType = inclusionList.get(i);
            long count = state.getTotal(playerId, c -> c.cardType() == cardType ? 1 : 0);
            retValue[baseFeatures.length + i] = count;
        }

        return retValue;
    }

    @Override
    public String[] names() {
        return names;
    }

}
