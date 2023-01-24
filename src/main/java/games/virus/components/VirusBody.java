package games.virus.components;

import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.interfaces.IComponentContainer;
import games.virus.cards.VirusCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirusBody extends Component implements IComponentContainer<VirusOrgan> {
    public HashMap<VirusCard.OrganType, VirusOrgan> organs;

    public VirusBody()
    {
        super(CoreConstants.ComponentType.TOKEN);
        organs = new HashMap<>();
        for (VirusCard.OrganType oType : VirusCard.OrganType.values())
            if (oType != VirusCard.OrganType.Treatment)
                organs.put(oType, new VirusOrgan());
    }

    protected VirusBody(int ID)
    {
        super(CoreConstants.ComponentType.TOKEN, ID);
        organs = new HashMap<>();
        for (VirusCard.OrganType oType : VirusCard.OrganType.values())
            organs.put(oType, new VirusOrgan());
    }

    @Override
    public Component copy() {
        VirusBody vb = new VirusBody(componentID);
        vb.organs = new HashMap<>();
        for (Map.Entry<VirusCard.OrganType, VirusOrgan> e: organs.entrySet()) {
            vb.organs.put(e.getKey(), (VirusOrgan) e.getValue().copy());
        }
        return vb;
    }

    @Override
    public String toString() {
        String s = "";
        for (Map.Entry<VirusCard.OrganType, VirusOrgan> e: organs.entrySet()) {
            s += e.getKey() + ": " + e.getValue().toString() + " ";
        }
        return s;
    }

    public HashMap<VirusCard.OrganType, VirusOrgan> getOrgans() {
        return organs;
    }

    public boolean hasOrgan(VirusCard.OrganType organ) {
        return organ != VirusCard.OrganType.Treatment && organs.get(organ).state != VirusOrgan.VirusOrganState.None;
    }

    /**
     * Returns true is the body has an organ of type organ vaccinated with a wild card
     * @param organ: organ type
     * @return true or false
     */
    public boolean hasOrganVaccinatedWild(VirusCard.OrganType organ) {
        return organ != VirusCard.OrganType.Treatment && organs.get(organ).state == VirusOrgan.VirusOrganState.VaccinatedWild;
    }

    /**
     * Returns true is the body has an organ of type organ in neutral state (i.e. no virus, no medicines)
     * @param organ: organ type
     * @return true or false
     */
    public boolean hasOrganNeutral(VirusCard.OrganType organ) {
        return organ != VirusCard.OrganType.Treatment && organs.get(organ).state == VirusOrgan.VirusOrganState.Neutral;
    }

    /**
     * Returns true is the body has an organ of type organ infected with a wild card
     * @param organ: organ type
     * @return true or false
     */
    public boolean hasOrganInfectedWild(VirusCard.OrganType organ) {
        return organ != VirusCard.OrganType.Treatment && organs.get(organ).state == VirusOrgan.VirusOrganState.InfectedWild;
    }

    /**
     * Returns true is the body has an organ of type organ infected with a virus (including the wild virus)
     * @param organ: organ type
     * @return true or false
     */
    public boolean hasOrganInfected(VirusCard.OrganType organ) {
        return organs.get(organ).state == VirusOrgan.VirusOrganState.Infected ||
                organs.get(organ).state == VirusOrgan.VirusOrganState.InfectedWild;
    }

    public boolean organNotYetImmunised(VirusCard.OrganType organ) {
        return organs.get(organ).state != VirusOrgan.VirusOrganState.Immunised;
    }

    public void addOrgan(VirusCard card) {
        organs.get(card.organ).initialiseOrgan();
        organs.get(card.organ).cards.add(card);
    }

    /**
     * Add cards to an organ (at state None) and actualize the state
     * @param organType: organ type
     */
    public void addCardsToOrgan(Deck<VirusCard> cards, VirusCard.OrganType organType) {
        // add cards in the correct order
        for (int i=cards.getSize()-1; i>=0; i--) {
            organs.get(organType).cards.add(cards.get(i));
        }

        // card 0th is the virus or medicine
        // card 1st is the organ
        if (cards.getSize() == 1)
            organs.get(organType).state = VirusOrgan.VirusOrganState.Neutral;
        else if (cards.getSize() == 2)
        {
            if (organs.get(organType).cards.get(0).type == VirusCard.VirusCardType.Medicine) {
                if (organs.get(organType).cards.get(0).organ == VirusCard.OrganType.Wild)
                    organs.get(organType).state = VirusOrgan.VirusOrganState.VaccinatedWild;
                else
                    organs.get(organType).state = VirusOrgan.VirusOrganState.Vaccinated;
            }
            else if (organs.get(organType).cards.get(0).type == VirusCard.VirusCardType.Virus)
            {
                if (organs.get(organType).cards.get(0).organ == VirusCard.OrganType.Wild)
                    organs.get(organType).state = VirusOrgan.VirusOrganState.InfectedWild;
                else
                    organs.get(organType).state = VirusOrgan.VirusOrganState.Infected;
            }
        }
    }

    /**
     * Returns the cards in the organ organType and set the state of the organ to None
     * @param organType: organ type
     * @return {@code Deck<VirusCard>} included in the organ
     */
    public Deck<VirusCard> removeOrgan(VirusCard.OrganType organType)
    {
        Deck<VirusCard> cards = organs.get(organType).cards.copy();
        organs.get(organType).cards.clear();
        organs.get(organType).state = VirusOrgan.VirusOrganState.None;
        return cards;
    }

    public VirusOrgan.VirusOrganState applyMedicine(VirusCard card, VirusCard.OrganType organ) {
        VirusOrgan.VirusOrganState newState = organs.get(organ).applyMedicine(card.organ == VirusCard.OrganType.Wild);
        organs.get(organ).cards.add(card);
        return newState;
    }

    public VirusOrgan.VirusOrganState applyVirus(VirusCard card, VirusCard.OrganType organ) {
        VirusOrgan.VirusOrganState newState = organs.get(organ).applyVirus(card.organ == VirusCard.OrganType.Wild);
        organs.get(organ).cards.add(card);
        return newState;
    }

    public VirusCard removeAVirusCard(VirusCard.OrganType organ)
    {
        return organs.get(organ).removeAVirusCard();
    }

    public VirusCard removeAMedicineCard(VirusCard.OrganType organ)
    {
        return organs.get(organ).removeAMedicineCard();
    }

    public VirusCard removeAnOrganCard(VirusCard.OrganType organ)
    {
        return organs.get(organ).removeAnOrganCard();
    }

    /**
     * Return the number of health organs
       * @return - the number of health organs
     */
    public int getNOrganHealthy()
    {
        int nHealthy = 0;
        for (VirusOrgan organ: organs.values()) {
            if (organ.isHealthy())
                nHealthy ++;
        }
        return nHealthy;
    }

    @Override
    public List<VirusOrgan> getComponents() {
        return new ArrayList<>(organs.values());
    }

    @Override
    public CoreConstants.VisibilityMode getVisibilityMode() {
        return CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
    }
}
