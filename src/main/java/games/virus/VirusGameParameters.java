package games.virus;

import core.AbstractGameParameters;

public class VirusGameParameters extends AbstractGameParameters {
    public int nCardsPerOrgan = 5;
    public int nCardsPerVirus = 4;
    public int nCardsPerMedicine = 4;
    public int nCardsPerTreatment = 2;
    public int maxCardsDiscard = 3;

    public VirusGameParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractGameParameters _copy() {
        return new VirusGameParameters(System.currentTimeMillis());
    }
}
