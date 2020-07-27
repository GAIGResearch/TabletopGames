package games.virus.cards;

import core.components.Card;


public class VirusCard extends Card {
    public enum OrganType {
        Heart,
        Brain,
        Stomach,
        Bone,
        Wild,
        Treatment
    }

    public enum VirusCardType {
        Organ,
        Virus,
        Medicine,
        Treatment
    }

    public final OrganType organ;
    public final VirusCardType type;

    public VirusCard(OrganType organ, VirusCardType type) {
        super(type.toString());
        this.organ = organ;
        this.type = type;
    }
    
    public VirusCard(OrganType organ, VirusCardType type, int ID) {
        super(type.toString(), ID);
        this.organ = organ;
        this.type = type;
    }

    @Override
    public String toString() {
        return "VirusCard{" + type + ": " + organ + '}';
    }

    @Override
    public Card copy() {
        return new VirusCard(organ, type, componentID);
    }
}
