package games.explodingkittens;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.*;

public class ExplodingKittensParameters extends TunableParameters {

    String dataPath = "data/explodingkittens/";

    Map<ExplodingKittensCard.CardType, Integer> cardCounts = new HashMap<>() {{
        put(ExplodingKittensCard.CardType.ATTACK, 4);
        put(ExplodingKittensCard.CardType.SKIP, 4);
        put(ExplodingKittensCard.CardType.FAVOR, 4);
        put(ExplodingKittensCard.CardType.SHUFFLE, 4);
        put(ExplodingKittensCard.CardType.SEETHEFUTURE, 5);
        put(ExplodingKittensCard.CardType.TACOCAT, 4);
        put(ExplodingKittensCard.CardType.MELONCAT, 4);
        put(ExplodingKittensCard.CardType.BEARDCAT, 4);
        put(ExplodingKittensCard.CardType.RAINBOWCAT, 4);
        put(ExplodingKittensCard.CardType.FURRYCAT, 4);
        put(ExplodingKittensCard.CardType.NOPE, 5);
        put(ExplodingKittensCard.CardType.DEFUSE, 6);
        put(ExplodingKittensCard.CardType.EXPLODING_KITTEN, -1);
    }};
    public int nCardsPerPlayer = 7;
    public int nDefuseCards = 6;
    public int nSeeFutureCards = 3;
    public boolean nopeOwnCards = true;

    public ExplodingKittensParameters() {
        addTunableParameter("nCardsPerPlayer", 7, Arrays.asList(3,5,7,10,15));
        addTunableParameter("nDefuseCards", 6, Arrays.asList(1,2,3,6,9));
        addTunableParameter("nSeeFutureCards", 3, Arrays.asList(1,3,5,7));
        addTunableParameter("nopeOwnCards", true, Arrays.asList(false, true));
        for (ExplodingKittensCard.CardType c: cardCounts.keySet()) {
            if (c == ExplodingKittensCard.CardType.EXPLODING_KITTEN) addTunableParameter(c.name() + " count", -1);
            else addTunableParameter(c.name() + " count", cardCounts.get(c), Arrays.asList(1,2,3,4,5));
        }
        addTunableParameter("dataPath", "data/explodingkittens/");
        _reset();
    }

    @Override
    public void _reset() {
        nCardsPerPlayer = (int) getParameterValue("nCardsPerPlayer");
        nDefuseCards = (int) getParameterValue("nDefuseCards");
        nSeeFutureCards = (int) getParameterValue("nSeeFutureCards");
        nopeOwnCards = (boolean) getParameterValue("nopeOwnCards");
        cardCounts.replaceAll((c, v) -> (Integer) getParameterValue(c.name() + " count"));
        dataPath = (String) getParameterValue("dataPath");
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    protected AbstractParameters _copy() {
        ExplodingKittensParameters ekp = new ExplodingKittensParameters();
        ekp.cardCounts = new HashMap<>(cardCounts);
        return ekp;
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof ExplodingKittensParameters;
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.ExplodingKittens, new ExplodingKittensForwardModel(), new ExplodingKittensGameState(this, GameType.ExplodingKittens.getMinPlayers()));
    }
}
