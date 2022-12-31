package games.sirius;

import core.components.Card;
import core.properties.Property;
import games.sirius.SiriusConstants.SiriusCardType;

import java.util.Objects;

import static games.sirius.SiriusConstants.SiriusCardType.AMMONIA;

public class SiriusCard extends Card {

    public final int value;
    public final SiriusCardType cardType;

    public SiriusCard(String name, SiriusCardType type, int value) {
        super(name);
        this.value = value;
        this.cardType = type;
    }

    @Override
    public Card copy(){
        return this;
        // immutable by declaration
    }

    @Override
    public void setProperty(Property prop) {
        throw new UnsupportedOperationException("No Properties allowed for immutability");
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardType, value, componentName); // deliberately excludes componentID
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SiriusCard) {
            SiriusCard other = (SiriusCard) o;
            return other.value == value && other.componentID == componentID
                    && other.cardType == cardType
                    && Objects.equals(other.componentName, componentName);
        }
        return false;
    }

}
