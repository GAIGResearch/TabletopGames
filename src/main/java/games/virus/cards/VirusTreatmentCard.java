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

    private TreatmentType treatmentType;

    public VirusTreatmentCard(TreatmentType treatmentType) {
        super(OrganType.None, VirusCardType.Treatment);
        this.treatmentType = treatmentType;
    }

    public VirusTreatmentCard(TreatmentType treatmentType, int ID) {
        super(OrganType.None, VirusCardType.Treatment, ID);
        this.treatmentType = treatmentType;
    }

    @Override
    public String toString() {
        return "Treatment: " + treatmentType.toString();
    }

    @Override
    public Card copy() {
        return new VirusTreatmentCard(treatmentType, componentID);
    }
}
