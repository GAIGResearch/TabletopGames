package games.sirius;


import games.sirius.SiriusConstants.MoonType;

/**
 * Metropolis overrides the card drawing behaviour of a normal Moon, as there is
 * an infinite supply of Favour cards
 */
public class Metropolis extends Moon {

    public Metropolis(String name, int nPlayers) {
        super(name, MoonType.METROPOLIS, nPlayers);
    }

    protected Metropolis(String name, MoonType type, int componentID, int nPlayers) {
        super(name, type, componentID, nPlayers);
    }

    @Override
    public SiriusCard drawCard() {
        // A Metropolis is different in that there is an infinite supply of FAVOUR cards
        if (policePresent)
            return null;
        return new SiriusCard("Favour", SiriusConstants.SiriusCardType.FAVOUR, 1);
    }

    @Override
    public Metropolis copy() {
        Metropolis retValue = new Metropolis(this.componentName, this.moonType, componentID, deck.getDeckVisibility().length);
        retValue.cartelPlayer = cartelPlayer;
        retValue.policePresent = policePresent;
        retValue.deck = deck; // this is immutable for a Metropolis
        copyComponentTo(retValue);
        return retValue;
    }

    @Override
    public int getDeckSize() {
        return 0;
    }

    @Override
    public void addCard(SiriusCard card) {
        throw new IllegalArgumentException("Not implemented");
    }

}

