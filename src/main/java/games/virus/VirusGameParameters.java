package games.virus;

import core.AbstractParameters;

public class VirusGameParameters extends AbstractParameters {
    public int nCardsPerOrgan        = 5;
    public int nCardsPerVirus        = 4;
    public int nCardsPerMedicine     = 4;
    public int maxCardsDiscard       = 3;
    public int nCardsPerWildOrgan    = 1;
    public int nCardsPerWildVirus    = 1;
    public int nCardsPerWildMedicine = 4;

    public int nCardsPerTreatmentSpreading    = 2;
    public int nCardsPerTreatmentTransplant   = 3;
    public int nCardsPerTreatmentOrganThief   = 3;
    public int nCardsPerTreatmentLatexGlove   = 1;
    public int nCardsPerTreatmentMedicalError = 1;


    public VirusGameParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        VirusGameParameters vgp = new VirusGameParameters(System.currentTimeMillis());
        vgp.nCardsPerOrgan = nCardsPerOrgan;
        vgp.nCardsPerVirus = nCardsPerVirus;
        vgp.nCardsPerMedicine = nCardsPerMedicine;
        vgp.maxCardsDiscard = maxCardsDiscard;

        vgp.nCardsPerWildOrgan =nCardsPerWildOrgan ;
        vgp.nCardsPerWildVirus = nCardsPerWildVirus;
        vgp.nCardsPerWildMedicine = nCardsPerWildMedicine;

        vgp.nCardsPerTreatmentSpreading = nCardsPerTreatmentSpreading;
        vgp.nCardsPerTreatmentTransplant = nCardsPerTreatmentTransplant;
        vgp.nCardsPerTreatmentOrganThief = nCardsPerTreatmentOrganThief;
        vgp.nCardsPerTreatmentLatexGlove = nCardsPerTreatmentLatexGlove;
        vgp.nCardsPerTreatmentMedicalError = nCardsPerTreatmentMedicalError;


        return vgp;
    }
}
