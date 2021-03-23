package games.sushigo;

import core.AbstractParameters;

public class SGParameters extends AbstractParameters {
    public String dataPath = "data/sushigo/";

    public int nMakiCards = 1;
    public int nChopstickCards = 1;
    public int nTempuraCards = 1;
    public int nSashimiCards = 1;
    public int nDumplingCards = 1;
    public int nSquidNigiriCards = 1;
    public int nSalmonNigiriCards = 1;
    public int nEggNigiriCards = 1;
    public int nWasabiCards = 1;
    public int nPuddingCards = 1;

    public SGParameters(long seed) {
        super(seed);
    }

    public String getDataPath() { return dataPath; }

    @Override
    protected AbstractParameters _copy() {
        SGParameters sgp = new SGParameters(System.currentTimeMillis());
        sgp.dataPath = dataPath;
        sgp.nMakiCards = nMakiCards;
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
        return nMakiCards == that.nMakiCards &&
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
