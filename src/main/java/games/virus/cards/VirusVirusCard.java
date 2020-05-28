package games.virus.cards;

import games.virus.VirusGameState;
import games.virus.VirusOrgan;

public class VirusVirusCard  extends VirusCard{
    public VirusVirusCard(VirusCardOrgan organ) {
        super(organ, VirusCardType.Virus);
    }

    public String toString() {
        String str = "Virus:";
        if      (organ == VirusCardOrgan.Hearth)  str += "Hearth";
        else if (organ == VirusCardOrgan.Brain)   str += "Brain";
        else if (organ == VirusCardOrgan.Stomach) str += "Stomach";
        else if (organ == VirusCardOrgan.Bone)    str += "Bone";
        else                                      str += "Wild";
        return str;
    }
}
