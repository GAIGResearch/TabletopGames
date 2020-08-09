package games.virus.cards;


import core.components.Card;

public class VirusTreatmentCard extends VirusCard {
    public enum TreatmentType {
        Transplant,
        OrganThief,
        Spreading,
        LatexGlove,
        MedicalError
    }

    public TreatmentType treatmentType;

    public VirusTreatmentCard(TreatmentType treatmentType) {
        super(OrganType.Treatment, VirusCardType.Treatment);
        this.treatmentType = treatmentType;
    }

    public VirusTreatmentCard(TreatmentType treatmentType, int ID) {
        super(OrganType.Treatment, VirusCardType.Treatment, ID);
        this.treatmentType = treatmentType;
    }

    @Override
    public String toString() {
        return "VirusCard{Treatment: " + treatmentType + "}";
    }

    @Override
    public Card copy() {
        return new VirusTreatmentCard(treatmentType, componentID);
    }
}
