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
                nPuddingCards == that.nPuddingCards;
    }
}
