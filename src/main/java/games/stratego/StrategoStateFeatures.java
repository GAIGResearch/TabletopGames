package games.stratego;

import core.AbstractGameState;
import core.components.PartialObservableDeck;
import core.interfaces.IComponentContainer;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;
import players.heuristics.AbstractStateFeature;

import java.util.List;
import java.util.stream.IntStream;

import static games.loveletter.cards.LoveLetterCard.CardType.getMaxCardValue;
import static java.util.stream.Collectors.toList;

public class StrategoStateFeatures extends AbstractStateFeature {

    String[] localNames = new String[0];

    @Override
    protected double maxScore() {
        return 1.0;
    }

    @Override
    protected double maxRounds() {
        return 500.0;
    }

    @Override
    protected String[] localNames() {
        return localNames;
    }

    @Override
    protected double[] localFeatureVector(AbstractGameState gs, int playerID) {
        return new double[0];
    }

}
