package games.virus.components;

import core.components.Component;
import core.components.Deck;
import games.virus.cards.VirusCard;
import utilities.Utils;

import java.util.List;

public class VirusOrgan extends Component {
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
        super(Utils.ComponentType.TOKEN);
        state = VirusOrganState.None;
        cards = new Deck<>("DeckOnOrgan");
    }

    protected VirusOrgan(int ID)
    {
        super(Utils.ComponentType.TOKEN, ID);
        state = VirusOrganState.None;
        cards = new Deck<>("DeckOnOrgan");
    }

    public void applyCard(VirusCard card)
    {
        switch (card.type) {
            case Organ:
                initialiseOrgan();
                break;
            case Medicine:
                applyMedicine();
                break;
            case Virus:
                applyVirus();
                break;
        }
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

    public Deck<VirusCard> getCards() {
        return cards;
    }
}
