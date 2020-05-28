package games.virus;

import core.components.Deck;
import games.virus.cards.VirusCard;
import games.virus.cards.VirusMedicineCard;
import games.virus.cards.VirusOrganCard;
import games.virus.cards.VirusVirusCard;

import java.util.List;

public class VirusOrgan {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_BLUE = "\u001B[34m";

    public enum VirusOrganState {
        None,
        Neutral,
        Vaccinated,
        Infected,
        Immunised
    }

    public Deck<VirusCard> cards;

    public VirusOrganState state;

    public VirusOrgan()
    {
        state = VirusOrganState.None;
        cards = new Deck<VirusCard>("DeckOnOrgan");
    }

    public void initialiseOrgan()
    {
        state = VirusOrganState.Neutral;
    }

    public VirusOrganState applyMedicine() {
        if (state == VirusOrganState.Neutral)
            state = VirusOrganState.Vaccinated;
        else if (state == VirusOrganState.Vaccinated)
            state = VirusOrganState.Immunised;
        else if (state == VirusOrganState.Infected)
            state = VirusOrganState.Neutral;

        return state;
    }

    public VirusOrganState applyVirus() {
        if (state == VirusOrganState.Neutral)
            state = VirusOrganState.Infected;
        else if (state == VirusOrganState.Infected)
            state = VirusOrganState.None;
        else if (state == VirusOrganState.Immunised)
            state = VirusOrganState.Neutral;

        return state;
    }

    public void applyCard(VirusCard card)
    {
        if (card instanceof VirusOrganCard) {
            initialiseOrgan();
        }
        else if (card instanceof VirusMedicineCard) {
            applyMedicine();
        }
        else if (card instanceof VirusVirusCard) {
            applyVirus();
        }
    }

    // Remove from deck a Virus Card.
    // Return the removed card
    public VirusCard removeAVirusCard() {
        List<VirusCard> cardsOnOrgan = cards.getCards();

        for (VirusCard card: cardsOnOrgan) {
            if (card instanceof  VirusVirusCard) {
                cards.remove(card);
                return card;
            }
        }
        return null;
    }

    // Remove from deck a Medicine Card.
    // Return the removed card
    public VirusCard removeAMedicineCard() {
        List<VirusCard> cardsOnOrgan = cards.getCards();

        for (VirusCard card: cardsOnOrgan) {
            if (card instanceof  VirusMedicineCard) {
                cards.remove(card);
                return card;
            }
        }
        return null;
    }

    // Remove from deck an Organ Card.
    // Return the removed card
    public VirusCard removeAnOrganCard() {
        List<VirusCard> cardsOnOrgan = cards.getCards();

        for (VirusCard card: cardsOnOrgan) {
            if (card instanceof  VirusOrganCard) {
                cards.remove(card);
                return card;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        String s = "";
        if      (state == VirusOrganState.None)       s = "None      ";
        else if (state == VirusOrganState.Neutral)    s = ANSI_BLUE  + "Neutral   " + ANSI_RESET;
        else if (state == VirusOrganState.Infected)   s = ANSI_RED   + "Infected  " + ANSI_RESET;
        else if (state == VirusOrganState.Immunised)  s = ANSI_GREEN + "Immunised " + ANSI_RESET;
        else if (state == VirusOrganState.Vaccinated) s = ANSI_GREEN + "Vaccinated" + ANSI_RESET;
        return s;
    }

    public boolean isHealthy() {
        return state == VirusOrganState.Neutral || state == VirusOrganState.Vaccinated || state == VirusOrganState.Immunised;
    }
}
