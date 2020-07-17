package games.explodingkittens;

import core.AbstractParameters;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.HashMap;
import java.util.Objects;

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
    public boolean nopeOwnCards = true;

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

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExplodingKittensParameters)) return false;
        if (!super.equals(o)) return false;
        ExplodingKittensParameters that = (ExplodingKittensParameters) o;
        return nCardsPerPlayer == that.nCardsPerPlayer &&
                nDefuseCards == that.nDefuseCards &&
                nSeeFutureCards == that.nSeeFutureCards &&
                Objects.equals(dataPath, that.dataPath) &&
                Objects.equals(cardCounts, that.cardCounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dataPath, cardCounts, nCardsPerPlayer, nDefuseCards, nSeeFutureCards);
    }
}
