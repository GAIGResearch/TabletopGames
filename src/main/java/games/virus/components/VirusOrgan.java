package games.virus.components;

import core.components.Component;
import core.components.Deck;
import core.interfaces.IComponentContainer;
import games.virus.cards.VirusCard;

import java.util.List;

import static core.CoreConstants.*;
import static core.CoreConstants.VisibilityMode.*;

public class VirusOrgan extends Component implements IComponentContainer<VirusCard> {

    public enum VirusOrganState {
        None,
        Neutral,
        Vaccinated,
        VaccinatedWild,
        Infected,
        InfectedWild,
        Immunised
    }

    public Deck<VirusCard> cards;
    public VirusOrganState state;

    public VirusOrgan()
    {
        super(ComponentType.TOKEN);
        state = VirusOrganState.None;
        cards = new Deck<>("DeckOnOrgan", VISIBLE_TO_ALL);
    }

    protected VirusOrgan(int ID)
    {
        super(ComponentType.TOKEN, ID);
        state = VirusOrganState.None;
        cards = new Deck<>("DeckOnOrgan", VISIBLE_TO_ALL);
    }

    public void initialiseOrgan()
    {
        state = VirusOrganState.Neutral;
    }

    public boolean isVaccinatedWild() {
        return state == VirusOrganState.VaccinatedWild;
    }

    public boolean isInfectedWild() {
        return state == VirusOrganState.InfectedWild;
    }


    public VirusOrganState applyMedicine(boolean isWild) {
        if (state == VirusOrganState.Neutral) {
            if (isWild)
                state = VirusOrganState.VaccinatedWild;
            else
                state = VirusOrganState.Vaccinated;
        }
        else if (state == VirusOrganState.Vaccinated || state==VirusOrganState.VaccinatedWild)
            state = VirusOrganState.Immunised;
        else if (state == VirusOrganState.Infected || state == VirusOrganState.InfectedWild)
            state = VirusOrganState.Neutral;
        return state;
    }

    public VirusOrganState applyVirus(boolean isWild) {
        if (state == VirusOrganState.Neutral) {
            if (isWild)
                state = VirusOrganState.InfectedWild;
            else
                state = VirusOrganState.Infected;
        }
        else if (state == VirusOrganState.Infected || state == VirusOrganState.InfectedWild)
            state = VirusOrganState.None;
        else if (state == VirusOrganState.Vaccinated || state==VirusOrganState.VaccinatedWild)
            state = VirusOrganState.Neutral;
        return state;
    }

    // Remove from deck a Virus Card.
    // Return the removed card
    public VirusCard removeAVirusCard() {
        List<VirusCard> cardsOnOrgan = cards.getComponents();

        for (VirusCard card: cardsOnOrgan) {
            if (card.type == VirusCard.VirusCardType.Virus) {
                cards.remove(card);
                return card;
            }
        }
        return null;
    }

    // Remove from deck a Medicine Card.
    // Return the removed card
    public VirusCard removeAMedicineCard() {
        List<VirusCard> cardsOnOrgan = cards.getComponents();

        for (VirusCard card: cardsOnOrgan) {
            if (card.type == VirusCard.VirusCardType.Medicine) {
                cards.remove(card);
                return card;
            }
        }
        return null;
    }

    // Remove from deck an Organ Card.
    // Return the removed card
    public VirusCard removeAnOrganCard() {
        List<VirusCard> cardsOnOrgan = cards.getComponents();

        for (VirusCard card: cardsOnOrgan) {
            if (card.type == VirusCard.VirusCardType.Organ) {
                cards.remove(card);
                return card;
            }
        }
        return null;
    }

    @Override
    public Component copy() {
        VirusOrgan organ = new VirusOrgan(componentID);
        organ.state = state;
        organ.cards = cards.copy();
        return organ;
    }

    @Override
    public String toString() {
        String s = "";
        if      (state == VirusOrganState.None)           s =              "None          ";
        else if (state == VirusOrganState.Neutral)        s = ANSI_BLUE  + "Neutral       " + ANSI_RESET;
        else if (state == VirusOrganState.Infected)       s = ANSI_RED   + "Infected      " + ANSI_RESET;
        else if (state == VirusOrganState.InfectedWild)   s = ANSI_RED   + "InfectedWild  " + ANSI_RESET;
        else if (state == VirusOrganState.Immunised)      s = ANSI_GREEN + "Immunised     " + ANSI_RESET;
        else if (state == VirusOrganState.Vaccinated)     s = ANSI_GREEN + "Vaccinated    " + ANSI_RESET;
        else if (state == VirusOrganState.VaccinatedWild) s = ANSI_GREEN + "VaccinatedWild" + ANSI_RESET;
        return s;
    }

    public boolean isHealthy() {
        return state == VirusOrganState.Neutral        || state == VirusOrganState.Vaccinated ||
               state == VirusOrganState.VaccinatedWild || state == VirusOrganState.Immunised;
    }

    public Deck<VirusCard> getCards() {
        return cards;
    }

    @Override
    public List<VirusCard> getComponents() {
        return cards.getComponents();
    }

    @Override
    public VisibilityMode getVisibilityMode() {
        return VISIBLE_TO_ALL;
    }

}
