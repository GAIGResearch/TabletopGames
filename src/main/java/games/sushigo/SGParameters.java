package games.sushigo;

import core.AbstractParameters;
import games.sushigo.cards.SGCard;
import utilities.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class SGParameters extends AbstractParameters {
    public String dataPath = "data/sushigo/";

    public int nRounds = 3;

    public HashMap<Pair<SGCard.SGCardType, Integer>, Integer> nCardsPerType = new HashMap<Pair<SGCard.SGCardType, Integer>, Integer>() {{
        put(new Pair<>(SGCard.SGCardType.Maki, 3), 12);
        put(new Pair<>(SGCard.SGCardType.Maki, 2), 8);
        put(new Pair<>(SGCard.SGCardType.Maki, 1), 6);
        put(new Pair<>(SGCard.SGCardType.Chopsticks, 1), 4);
        put(new Pair<>(SGCard.SGCardType.Tempura, 1), 14);
        put(new Pair<>(SGCard.SGCardType.Sashimi, 1), 14);
        put(new Pair<>(SGCard.SGCardType.Dumpling, 1), 14);
        put(new Pair<>(SGCard.SGCardType.SquidNigiri, 1), 5);
        put(new Pair<>(SGCard.SGCardType.SalmonNigiri, 1), 10);
        put(new Pair<>(SGCard.SGCardType.EggNigiri, 1), 5);
        put(new Pair<>(SGCard.SGCardType.Wasabi, 1), 6);
        put(new Pair<>(SGCard.SGCardType.Pudding, 1), 10);
    }};

    public int valueMakiMost = 6;
    public int valueMakiSecond = 3;
    public int valueTempuraPair = 5;
    public int valueSashimiTriple = 10;
    public int[] valueDumpling = new int[] {1, 3, 6, 10, 15};
    public int valueSquidNigiri = 3;
    public int valueSalmonNigiri = 2;
    public int valueEggNigiri = 1;
    public int multiplierWasabi = 3;
    public int valuePuddingMost = 6;
    public int valuePuddingLeast = -6;

    public int nCards = 10;  // for 2 players

    public String getDataPath() { return dataPath; }

    @Override
    protected AbstractParameters _copy() {
        SGParameters sgp = new SGParameters();
        sgp.dataPath = dataPath;
        sgp.nCardsPerType = new HashMap<>(nCardsPerType);

        sgp.valueMakiMost = valueMakiMost;
        sgp.valueMakiSecond = valueMakiSecond;
        sgp.valueTempuraPair = valueTempuraPair;
        sgp.valueSashimiTriple = valueSashimiTriple;
        sgp.valueDumpling = valueDumpling.clone();
        sgp.valueSquidNigiri = valueSquidNigiri;
        sgp.valueSalmonNigiri = valueSalmonNigiri;
        sgp.valueEggNigiri = valueEggNigiri;
        sgp.multiplierWasabi = multiplierWasabi;
        sgp.valuePuddingMost = valuePuddingMost;
        sgp.valuePuddingLeast = valuePuddingLeast;

        sgp.nCards = nCards;
        sgp.nRounds = nRounds;
        return sgp;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SGParameters)) return false;
        if (!super.equals(o)) return false;
        SGParameters that = (SGParameters) o;
        return nRounds == that.nRounds && valueMakiMost == that.valueMakiMost && valueMakiSecond == that.valueMakiSecond && valueTempuraPair == that.valueTempuraPair && valueSashimiTriple == that.valueSashimiTriple && valueSquidNigiri == that.valueSquidNigiri && valueSalmonNigiri == that.valueSalmonNigiri && valueEggNigiri == that.valueEggNigiri && multiplierWasabi == that.multiplierWasabi && valuePuddingMost == that.valuePuddingMost && valuePuddingLeast == that.valuePuddingLeast && nCards == that.nCards && Objects.equals(dataPath, that.dataPath) && Objects.equals(nCardsPerType, that.nCardsPerType) && Arrays.equals(valueDumpling, that.valueDumpling);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), dataPath, nRounds, nCardsPerType, valueMakiMost, valueMakiSecond, valueTempuraPair, valueSashimiTriple, valueSquidNigiri, valueSalmonNigiri, valueEggNigiri, multiplierWasabi, valuePuddingMost, valuePuddingLeast, nCards);
        result = 31 * result + Arrays.hashCode(valueDumpling);
        return result;
    }
}
