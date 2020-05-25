package games.virus.cards;

import games.virus.VirusOrgan;

public abstract class VirusCard {
    public enum VirusCardOrgan {
        Hearth,
        Brain,
        Stomach,
        Bone,
        None
    }

    public enum VirusCardType {
        Organ,
        Virus,
        Medicine,
        Treatment
    }

    public final VirusCardOrgan organ;
    public final VirusCardType type;

    public VirusCard(VirusCardOrgan organ, VirusCardType type) {
        this.organ = organ;
        this.type = type;
    }
}
