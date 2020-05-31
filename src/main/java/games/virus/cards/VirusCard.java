package games.virus.cards;

import core.components.Card;


public class VirusCard extends Card {
    public enum VirusCardOrgan {
        Hearth,
        Brain,
        Stomach,
        Bone,
        Wild,
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
        super(type.toString());
        this.organ = organ;
        this.type = type;
    }

    @Override
    public String toString() {
        return "VirusCard{" + type + ": " + organ + '}';
    }
}
