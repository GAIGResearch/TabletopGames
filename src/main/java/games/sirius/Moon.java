package games.sirius;


import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.sirius.SiriusConstants.MoonType;
import utilities.Utils;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;


public class Moon extends Component {

    PartialObservableDeck<SiriusCard> deck;
    Random rnd;
    MoonType moonType;

    public Moon(String name, MoonType type, Random rnd, int nPlayers) {
        super(Utils.ComponentType.AREA, name);
        init(type, rnd, nPlayers);
    }

    private Moon(String name, MoonType type, Random rnd, int componentID, int nPlayers) {
        super(Utils.ComponentType.AREA, name, componentID);
        init(type, rnd, nPlayers);
    }

    private void init(MoonType type, Random rnd, int nPlayers) {
        deck = new PartialObservableDeck<>("Cards on " + componentName, nPlayers);
        this.rnd = rnd;
        this.moonType = type;
    }

    public SiriusCard drawCard() {
        return deck.draw();
    }

    public Optional<SiriusCard> drawCard(Predicate<SiriusCard> predicate) {
        Optional<SiriusCard> retValue = deck.stream().filter(predicate).findFirst();
        retValue.ifPresent(c -> deck.remove(c));
        return retValue;
    }

    public MoonType getMoonType() {
        return moonType;
    }

    public int getDeckSize() {
        return deck.getSize();
    }

    public void lookAtDeck(int player) {
        for (int i = 0; i < deck.getSize(); i++) {
            deck.setVisibilityOfComponent(i, player, true);
        }
    }
    public PartialObservableDeck<SiriusCard> getDeck() {
        return deck.copy();
    }

    public void addCard(SiriusCard card) {
        deck.add(card);
    }

    public void shuffle() {
        deck.shuffle(rnd);
    }

    @Override
    public Moon copy() {
        Moon retValue = new Moon(this.componentName, this.moonType, new Random(rnd.nextInt()), componentID);
        retValue.deck = deck.copy();
        copyComponentTo(retValue);
        return retValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deck, moonType);
    }

    @Override
    public String toString() {
        return componentName + " (" + moonType + ")";
    }
}

