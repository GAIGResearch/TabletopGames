package games.virus.components;

import core.components.Component;
import games.virus.cards.VirusCard;
import utilities.Utils;

import java.util.HashMap;
import java.util.Map;

public class VirusBody extends Component {
    public HashMap<VirusCard.VirusCardOrgan, VirusOrgan> organs;

    public VirusBody()
    {
        super(Utils.ComponentType.TOKEN);
        organs = new HashMap<>();
        for (VirusCard.VirusCardOrgan oType : VirusCard.VirusCardOrgan.values()) {
            if (oType != VirusCard.VirusCardOrgan.None && oType != VirusCard.VirusCardOrgan.Wild) {
                organs.put(oType, new VirusOrgan());
            }
        }
    }

    protected VirusBody(int ID)
    {
        super(Utils.ComponentType.TOKEN, ID);
        organs = new HashMap<>();
        for (VirusCard.VirusCardOrgan oType : VirusCard.VirusCardOrgan.values()) {
            if (oType != VirusCard.VirusCardOrgan.None && oType != VirusCard.VirusCardOrgan.Wild) {
                organs.put(oType, new VirusOrgan());
            }
        }
    }

    @Override
    public Component copy() {
        VirusBody vb = new VirusBody(componentID);
        vb.organs = new HashMap<>();
        for (Map.Entry<VirusCard.VirusCardOrgan, VirusOrgan> e: organs.entrySet()) {
            vb.organs.put(e.getKey(), (VirusOrgan) e.getValue().copy());
        }
        return vb;
    }

    @Override
    public String toString() {
        String s = "";
        for (Map.Entry<VirusCard.VirusCardOrgan, VirusOrgan> e: organs.entrySet()) {
            s += e.getKey() + ": " + e.getValue().toString() + " ";
        }
        return s;
    }

    public HashMap<VirusCard.VirusCardOrgan, VirusOrgan> getOrgans() {
        return organs;
    }

    public boolean hasOrgan(VirusCard.VirusCardOrgan organ) {
        return organs.get(organ).state != VirusOrgan.VirusOrganState.None;
    }

    public boolean hasOrganImmunised(VirusCard.VirusCardOrgan organ) {
        return organs.get(organ).state != VirusOrgan.VirusOrganState.Immunised;
    }

    public void addOrgan(VirusCard card) {
        organs.get(card.organ).initialiseOrgan();
        organs.get(card.organ).cards.add(card);
    }

    public VirusOrgan.VirusOrganState applyMedicine(VirusCard card) {
        VirusOrgan.VirusOrganState newState = organs.get(card.organ).applyMedicine();
        organs.get(card.organ).cards.add(card);
        return newState;
    }

    public VirusOrgan.VirusOrganState applyVirus(VirusCard card) {
        VirusOrgan.VirusOrganState newState = organs.get(card.organ).applyVirus();
        organs.get(card.organ).cards.add(card);
        return newState;
    }

    public VirusCard removeAVirusCard(VirusCard card)
    {
        return organs.get(card.organ).removeAVirusCard();
    }

    public VirusCard removeAMedicineCard(VirusCard card)
    {
        return organs.get(card.organ).removeAMedicineCard();
    }

    public VirusCard removeAnOrganCard(VirusCard card)
    {
        return organs.get(card.organ).removeAnOrganCard();
    }

    // Return true if the body has all organs and they are healthy
    public boolean areAllOrganHealthy()
    {
        boolean allHealthy = true;
        for (VirusOrgan organ: organs.values()) {
            if (!organ.isHealthy()) {
                allHealthy = false;
            }
        }
        return allHealthy;
    }

}
