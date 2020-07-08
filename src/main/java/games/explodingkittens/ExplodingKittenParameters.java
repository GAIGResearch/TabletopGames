package games.explodingkittens;

import core.AbstractParameters;
import games.explodingkittens.cards.ExplodingKittenCard;

import java.util.HashMap;

public class ExplodingKittenParameters extends AbstractParameters {

    HashMap<ExplodingKittenCard.CardType, Integer> cardCounts = new HashMap<ExplodingKittenCard.CardType, Integer>() {{
        put(ExplodingKittenCard.CardType.ATTACK, 4);
        put(ExplodingKittenCard.CardType.SKIP, 4);
        put(ExplodingKittenCard.CardType.FAVOR, 4);
        put(ExplodingKittenCard.CardType.SHUFFLE, 4);
        put(ExplodingKittenCard.CardType.SEETHEFUTURE, 5);
        put(ExplodingKittenCard.CardType.TACOCAT, 4);
        put(ExplodingKittenCard.CardType.MELONCAT, 4);
        put(ExplodingKittenCard.CardType.BEARDCAT, 4);
        put(ExplodingKittenCard.CardType.RAINBOWCAT, 4);
        put(ExplodingKittenCard.CardType.FURRYCAT, 4);
        put(ExplodingKittenCard.CardType.NOPE, 5);
        put(ExplodingKittenCard.CardType.DEFUSE, 6);
        put(ExplodingKittenCard.CardType.EXPLODING_KITTEN, -1);
    }};
    public int nCardsPerPlayer = 7;
    public int nDefuseCards = 6;
    public int nSeeFutureCards = 3;

    public ExplodingKittenParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        ExplodingKittenParameters ekp = new ExplodingKittenParameters(System.currentTimeMillis());
        ekp.cardCounts = new HashMap<>(cardCounts);
        ekp.nCardsPerPlayer = nCardsPerPlayer;
        ekp.nDefuseCards = nDefuseCards;
        ekp.nSeeFutureCards = nSeeFutureCards;
        return ekp;
    }
}
