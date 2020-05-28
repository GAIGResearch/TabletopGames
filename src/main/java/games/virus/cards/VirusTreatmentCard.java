package games.virus.cards;

import games.virus.VirusOrgan;

public class VirusTreatmentCard extends VirusCard {
    public enum TreatmentType {
        Transplant,
        OrganThief,
        Spreading,
        LatexGlove,
        MedicalError
    }

    private TreatmentType treatmentType;

    public VirusTreatmentCard(TreatmentType treatmentType) {
        super(VirusCardOrgan.None, VirusCardType.Treatment);
        this.treatmentType = treatmentType;
    }

    public String toString() {
        String str = "Treatment:";
        if      (treatmentType == TreatmentType.Transplant)   str += "Transplant";
        else if (treatmentType == TreatmentType.OrganThief)   str += "OrganThief";
        else if (treatmentType == TreatmentType.Spreading)    str += "Spreading";
        else if (treatmentType == TreatmentType.LatexGlove)   str += "LatexGlove";
        else if (treatmentType == TreatmentType.MedicalError) str += "MedicalError";
        return str;
    }
}
