package games.virus.cards;

import games.virus.VirusOrgan;

public class VirusOrganCard extends VirusCard {

    public VirusOrganCard(VirusCardOrgan organ) {
        super(organ, VirusCardType.Organ);
    }

    @Override
    public String toString() {
        String str = "Organ:";
        if      (organ == VirusCardOrgan.Hearth)  str += "Hearth";
        else if (organ == VirusCardOrgan.Brain)   str += "Brain";
        else if (organ == VirusCardOrgan.Stomach) str += "Stomach";
        else if (organ == VirusCardOrgan.Bone)    str += "Bone";
        else                                      str += "Wild";
        return str;
    }
}
