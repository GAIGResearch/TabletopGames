package games.explodingkittens;

import core.AbstractParameters;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.HashMap;

public class ExplodingKittensParameters extends AbstractParameters {

    String dataPath = "data/explodingkittens/";

    HashMap<ExplodingKittensCard.CardType, Integer> cardCounts = new HashMap<ExplodingKittensCard.CardType, Integer>() {{
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

    public ExplodingKittensParameters(long seed) {
        super(seed);
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    protected AbstractParameters _copy() {
        ExplodingKittensParameters ekp = new ExplodingKittensParameters(System.currentTimeMillis());
        ekp.cardCounts = new HashMap<>(cardCounts);
        ekp.nCardsPerPlayer = nCardsPerPlayer;
        ekp.nDefuseCards = nDefuseCards;
        ekp.nSeeFutureCards = nSeeFutureCards;
        return ekp;
    }
}
