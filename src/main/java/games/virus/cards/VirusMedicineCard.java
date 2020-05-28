package games.virus.cards;

import games.virus.VirusOrgan;

public class VirusMedicineCard extends VirusCard {

    public VirusMedicineCard(VirusCardOrgan organ) {
        super(organ, VirusCardType.Medicine);
    }

    public String toString() {
        String str = "Medicine:";
        if      (organ == VirusCardOrgan.Hearth)  str += "Hearth";
        else if (organ == VirusCardOrgan.Brain)   str += "Brain";
        else if (organ == VirusCardOrgan.Stomach) str += "Stomach";
        else if (organ == VirusCardOrgan.Bone)    str += "Bone";
        else                                      str += "Wild";
        return str;
    }
}
