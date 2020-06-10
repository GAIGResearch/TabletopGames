package games.virus.cards;


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
        return "Treatment: " + treatmentType.toString();
    }
}
