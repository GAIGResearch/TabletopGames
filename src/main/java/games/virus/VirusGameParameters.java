package games.virus;

import core.AbstractParameters;

import java.util.Objects;

public class VirusGameParameters extends AbstractParameters {
    public int nCardsPerOrgan = 5;
    public int nCardsPerVirus = 4;
    public int nCardsPerMedicine = 4;
    public int nCardsPerTreatment = 2;
    public int maxCardsDiscard = 3;

    public VirusGameParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        VirusGameParameters vgp = new VirusGameParameters(System.currentTimeMillis());
        vgp.nCardsPerOrgan = nCardsPerOrgan;
        vgp.nCardsPerVirus = nCardsPerVirus;
        vgp.nCardsPerMedicine = nCardsPerMedicine;
        vgp.nCardsPerTreatment = nCardsPerTreatment;
        vgp.maxCardsDiscard = maxCardsDiscard;
        return vgp;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VirusGameParameters)) return false;
        if (!super.equals(o)) return false;
        VirusGameParameters that = (VirusGameParameters) o;
        return nCardsPerOrgan == that.nCardsPerOrgan &&
                nCardsPerVirus == that.nCardsPerVirus &&
                nCardsPerMedicine == that.nCardsPerMedicine &&
                nCardsPerTreatment == that.nCardsPerTreatment &&
                maxCardsDiscard == that.maxCardsDiscard;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nCardsPerOrgan, nCardsPerVirus, nCardsPerMedicine, nCardsPerTreatment, maxCardsDiscard);
    }
}
