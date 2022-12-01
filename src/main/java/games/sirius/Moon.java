package games.sirius;


import core.CoreConstants;
import core.components.*;
import games.sirius.SiriusConstants.MoonType;
import utilities.Utils;

import java.util.*;
import java.util.function.Predicate;


public class Moon extends Component {

    Deck<SiriusCard> deck;
    Random rnd;
    MoonType moonType;

    public Moon(String name, MoonType type, Random rnd) {
        super(Utils.ComponentType.AREA, name);
        init(type, rnd);
    }
    private Moon(String name, MoonType type, Random rnd, int componentID)  {
        super(Utils.ComponentType.AREA, name, componentID);
        init(type, rnd);
    }
    private void init(MoonType type, Random rnd) {
        deck = new Deck<>("Cards on " + componentName, -1, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
        this.rnd = rnd;
        this.moonType = type;
    }

    public SiriusCard drawCard() {
        return deck.draw();
    }
    public Optional<SiriusCard> drawCard( Predicate<SiriusCard> predicate) {
        Optional<SiriusCard> retValue = deck.stream().filter(predicate).findFirst();
        retValue.ifPresent(c -> deck.remove(c));
        return retValue;
    }

    public MoonType getMoonType() {return moonType;}
    public int getDeckSize() {return deck.getSize();}
    public Deck<SiriusCard> getDeck() {return deck.copy();}
    public void addCard(SiriusCard card) {
        deck.add(card);
    }

    public void shuffle() {
        deck.shuffle(rnd);
    }

    @Override
    public Moon copy() {
        Moon retValue = new Moon(this.componentName, this.moonType, new Random(rnd.nextInt()), componentID);
        copyComponentTo(retValue);
        return retValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deck, moonType);
    }

    @Override
    public String toString() {
        return componentName + "(" + moonType + ")";
    }
}

