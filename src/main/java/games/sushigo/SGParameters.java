package games.sushigo;

import core.AbstractParameters;

public class SGParameters extends AbstractParameters {
    public String dataPath = "data/sushigo/";

    public int nMaki_3Cards = 12;
    public int nMaki_2Cards = 8;
    public int nMaki_1Cards = 6;
    public int nChopstickCards = 4;
    public int nTempuraCards = 14;
    public int nSashimiCards = 14;
    public int nDumplingCards = 14;
    public int nSquidNigiriCards = 5;
    public int nSalmonNigiriCards = 10;
    public int nEggNigiriCards = 5;
    public int nWasabiCards = 6;
    public int nPuddingCards = 10;

    public int valueMakiMost = 6;
    public int valueMakiSecond = 3;
    public int valueTempuraPair = 5;
    public int valueSashimiTriss = 10;
    public int valueDumpling = 1;
    public int valueDumplingPair = 3;
    public int valueDumplingTriss = 6;
    public int valueDumplingQuad = 10;
    public int valueDumplingPent = 15;
    public int valueSquidNigiri = 3;
    public int valueSalmonNigiri = 2;
    public int valueEggNigiri = 1;
    public int multiplierWasabi = 3;
    public int valuePuddingMost = 6;
    public int valuePuddingLeast = -6;

    public int cardAmountTwoPlayers = 10;
    public int cardAmountThreePlayers = 9;
    public int cardAmountFourPlayers = 8;
    public int cardAmountFivePlayers = 7;

    public SGParameters(long seed) {
        super(seed);
    }

    public String getDataPath() { return dataPath; }

    @Override
    protected AbstractParameters _copy() {
        SGParameters sgp = new SGParameters(System.currentTimeMillis());
        sgp.dataPath = dataPath;
        sgp.nMaki_3Cards = nMaki_3Cards;
        sgp.nMaki_2Cards = nMaki_2Cards;
        sgp.nMaki_1Cards = nMaki_1Cards;
        sgp.nChopstickCards = nChopstickCards;
        sgp.nTempuraCards = nTempuraCards;
        sgp.nSashimiCards = nSashimiCards;
        sgp.nDumplingCards = nDumplingCards;
        sgp.nSquidNigiriCards = nSquidNigiriCards;
        sgp.nSalmonNigiriCards = nSalmonNigiriCards;
        sgp.nEggNigiriCards = nEggNigiriCards;
        sgp.nWasabiCards = nWasabiCards;
        sgp.nPuddingCards = nPuddingCards;

        sgp.valueMakiMost = valueMakiMost;
        sgp.valueMakiSecond = valueMakiSecond;
        sgp.valueTempuraPair = valueTempuraPair;
        sgp.valueSashimiTriss = valueSashimiTriss;
        sgp.valueDumpling = valueDumpling;
        sgp.valueDumplingPair = valueDumplingPair;
        sgp.valueDumplingTriss = valueDumplingTriss;
        sgp.valueDumplingQuad = valueDumplingQuad;
        sgp.valueDumplingPent = valueDumplingPent;
        sgp.valueSquidNigiri = valueSquidNigiri;
        sgp.valueSalmonNigiri = valueSalmonNigiri;
        sgp.valueEggNigiri = valueEggNigiri;
        sgp.multiplierWasabi = multiplierWasabi;
        sgp.valuePuddingMost = valuePuddingMost;
        sgp.valuePuddingLeast = valuePuddingLeast;

        sgp.cardAmountTwoPlayers = cardAmountTwoPlayers;
        sgp.cardAmountThreePlayers = cardAmountThreePlayers;
        sgp.cardAmountFourPlayers = cardAmountFourPlayers;
        sgp.cardAmountFivePlayers = cardAmountFivePlayers;
        return sgp;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SGParameters)) return false;
        if (!super.equals(o)) return false;
        SGParameters that = (SGParameters) o;
        return nMaki_3Cards == that.nMaki_3Cards &&
                nMaki_2Cards == that.nMaki_2Cards &&
                nMaki_1Cards == that.nMaki_1Cards &&
                nChopstickCards == that.nChopstickCards &&
                nTempuraCards == that.nTempuraCards &&
                nSashimiCards == that.nSashimiCards &&
                nDumplingCards == that.nDumplingCards &&
                nSquidNigiriCards == that.nSquidNigiriCards &&
                nSalmonNigiriCards == that.nSalmonNigiriCards &&
                nEggNigiriCards == that.nEggNigiriCards &&
                nWasabiCards == that.nWasabiCards &&
                nPuddingCards == that.nPuddingCards &&
                valueMakiMost == that.valueMakiMost &&
                valueMakiSecond == that.valueMakiSecond &&
                valueTempuraPair == that.valueTempuraPair &&
                valueSashimiTriss == that.valueSashimiTriss &&
                valueDumpling == that.valueDumpling &&
                valueDumplingPair == that.valueDumplingPair &&
                valueDumplingTriss == that.valueDumplingTriss &&
                valueDumplingQuad == that.valueDumplingQuad &&
                valueDumplingPent == that.valueDumplingPent &&
                valueSquidNigiri == that.valueSquidNigiri &&
                valueSalmonNigiri == that.valueSalmonNigiri &&
                valueEggNigiri == that.valueEggNigiri &&
                multiplierWasabi == that.multiplierWasabi &&
                valuePuddingMost == that.valuePuddingMost &&
                valuePuddingLeast == that.valuePuddingLeast &&
                cardAmountTwoPlayers == that.cardAmountTwoPlayers &&
                cardAmountThreePlayers == that.cardAmountThreePlayers &&
                cardAmountFourPlayers == that.cardAmountFourPlayers &&
                cardAmountFivePlayers == that.cardAmountFivePlayers;
    }
}
