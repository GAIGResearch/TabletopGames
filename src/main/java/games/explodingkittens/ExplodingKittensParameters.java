package games.explodingkittens;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.*;

public class ExplodingKittensParameters extends TunableParameters {

    String dataPath = "data/explodingkittens/";

    public Map<ExplodingKittensCard.CardType, Integer> cardCounts = new HashMap<>() {{
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
    public boolean nopeOwnCards = true;

    public ExplodingKittensParameters() {
        addTunableParameter("nCardsPerPlayer", 7, Arrays.asList(3,5,7,10,15));
        addTunableParameter("nopeOwnCards", true, Arrays.asList(false, true));
        for (ExplodingKittensCard.CardType c: cardCounts.keySet()) {
            if (c == ExplodingKittensCard.CardType.EXPLODING_KITTEN) addTunableParameter(c.name() + "_count", -1);
            else addTunableParameter(c.name() + "_count", cardCounts.get(c), Arrays.asList(1,2,3,4,5,6,7,8,9,10));
        }
        addTunableParameter("dataPath", "data/explodingkittens/");
        _reset();
    }

    @Override
    public void _reset() {
        nCardsPerPlayer = (int) getParameterValue("nCardsPerPlayer");
        nopeOwnCards = (boolean) getParameterValue("nopeOwnCards");
        cardCounts.replaceAll((c, v) -> (Integer) getParameterValue(c.name() + "_count"));
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
