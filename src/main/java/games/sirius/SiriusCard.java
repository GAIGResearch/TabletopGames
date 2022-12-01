package games.sirius;

import core.components.Card;
import core.properties.Property;

import java.util.Objects;

public class SiriusCard extends Card {

    public final int value;

    public SiriusCard(String name, int value) {
        super(name);
        this.value = value;
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
        return Objects.hash(value, componentName); // deliberately excludes componentID
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SiriusCard) {
            SiriusCard other = (SiriusCard) o;
            return other.componentID == componentID && other.value == value && other.componentName == componentName;
        }
        return false;
    }

}
