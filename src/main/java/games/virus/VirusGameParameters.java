package games.virus;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.Arrays;
import java.util.Objects;

public class VirusGameParameters extends TunableParameters {
    public int nCardsPlayerHand = 3;
    public int nCardsDiscardLatexGlove = 3;

    public int nCardsPerOrgan = 5;
    public int nCardsPerVirus = 4;
    public int nCardsPerMedicine = 4;
    public int maxCardsDiscard = 3;
    public int nCardsPerWildOrgan = 1;
    public int nCardsPerWildVirus = 1;
    public int nCardsPerWildMedicine = 4;

    public int nCardsPerTreatmentSpreading = 2;
    public int nCardsPerTreatmentTransplant = 3;
    public int nCardsPerTreatmentOrganThief = 3;
    public int nCardsPerTreatmentLatexGlove = 1;
    public int nCardsPerTreatmentMedicalError = 1;

    public VirusGameParameters() {
        setTimeoutRounds(100);
        addTunableParameter("nCardsPlayerHand", 3, Arrays.asList(2, 3, 4, 5));
        addTunableParameter("nCardsDiscardLatexGlove", 3, Arrays.asList(2, 3, 4, 5));
        addTunableParameter("nCardsPerOrgan", 5, Arrays.asList(2, 3, 4, 5, 7, 8, 9, 10));
        addTunableParameter("nCardsPerVirus", 4, Arrays.asList(2, 3, 4, 5, 7, 8, 9, 10));
        addTunableParameter("nCardsPerMedicine", 4, Arrays.asList(2, 3, 4, 5, 7, 8, 9, 10));
        addTunableParameter("maxCardsDiscard", 3, Arrays.asList(2, 3, 4, 5, 7, 8, 9, 10));
        addTunableParameter("nCardsPerWildOrgan", 1, Arrays.asList(1, 2, 3, 4, 5, 7, 8, 9, 10));
        addTunableParameter("nCardsPerWildVirus", 1, Arrays.asList(1, 2, 3, 4, 5, 7, 8, 9, 10));
        addTunableParameter("nCardsPerWildMedicine", 4, Arrays.asList(1, 2, 3, 4, 5, 7, 8, 9, 10));
        addTunableParameter("nCardsPerTreatmentSpreading", 2, Arrays.asList(1, 2, 3, 4, 5, 7, 8, 9, 10));
        addTunableParameter("nCardsPerTreatmentTransplant", 3, Arrays.asList(1, 2, 3, 4, 5, 7, 8, 9, 10));
        addTunableParameter("nCardsPerTreatmentOrganThief", 3, Arrays.asList(1, 2, 3, 4, 5, 7, 8, 9, 10));
        addTunableParameter("nCardsPerTreatmentLatexGlove", 1, Arrays.asList(1, 2, 3, 4, 5, 7, 8, 9, 10));
        addTunableParameter("nCardsPerTreatmentMedicalError", 1, Arrays.asList(1, 2, 3, 4, 5, 7, 8, 9, 10));
        _reset();
    }

    @Override
    public void _reset() {
        nCardsPlayerHand = (int) getParameterValue("nCardsPlayerHand");
        nCardsDiscardLatexGlove = (int) getParameterValue("nCardsDiscardLatexGlove");
        nCardsPerOrgan = (int) getParameterValue("nCardsPerOrgan");
        nCardsPerVirus = (int) getParameterValue("nCardsPerVirus");
        nCardsPerMedicine = (int) getParameterValue("nCardsPerMedicine");
        maxCardsDiscard = (int) getParameterValue("maxCardsDiscard");
        nCardsPerWildOrgan = (int) getParameterValue("nCardsPerWildOrgan");
        nCardsPerWildVirus = (int) getParameterValue("nCardsPerWildVirus");
        nCardsPerWildMedicine = (int) getParameterValue("nCardsPerWildMedicine");
        nCardsPerTreatmentSpreading = (int) getParameterValue("nCardsPerTreatmentSpreading");
        nCardsPerTreatmentTransplant = (int) getParameterValue("nCardsPerTreatmentTransplant");
        nCardsPerTreatmentOrganThief = (int) getParameterValue("nCardsPerTreatmentOrganThief");
        nCardsPerTreatmentLatexGlove = (int) getParameterValue("nCardsPerTreatmentLatexGlove");
        nCardsPerTreatmentMedicalError = (int) getParameterValue("nCardsPerTreatmentMedicalError");
    }

    @Override
    protected AbstractParameters _copy() {
        return new VirusGameParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VirusGameParameters)) return false;
        VirusGameParameters that = (VirusGameParameters) o;
        return nCardsPerOrgan == that.nCardsPerOrgan &&
                nCardsDiscardLatexGlove == that.nCardsDiscardLatexGlove &&
                nCardsPlayerHand == that.nCardsPlayerHand &&
                nCardsPerVirus == that.nCardsPerVirus &&
                nCardsPerMedicine == that.nCardsPerMedicine &&
                maxCardsDiscard == that.maxCardsDiscard &&
                nCardsPerWildOrgan == that.nCardsPerWildOrgan &&
                nCardsPerWildVirus == that.nCardsPerWildVirus &&
                nCardsPerWildMedicine == that.nCardsPerWildMedicine &&
                nCardsPerTreatmentSpreading == that.nCardsPerTreatmentSpreading &&
                nCardsPerTreatmentTransplant == that.nCardsPerTreatmentTransplant &&
                nCardsPerTreatmentOrganThief == that.nCardsPerTreatmentOrganThief &&
                nCardsPerTreatmentLatexGlove == that.nCardsPerTreatmentLatexGlove &&
                nCardsPerTreatmentMedicalError == that.nCardsPerTreatmentMedicalError;
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.Virus, new VirusForwardModel(), new VirusGameState(this, GameType.Virus.getMinPlayers()));
    }
}


