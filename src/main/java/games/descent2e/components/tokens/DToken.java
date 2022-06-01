package games.descent2e.components.tokens;

import core.components.Token;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.tokens.TokenAction;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

// Important: ownerID is idx of hero in list of heroes in game state, NOT player IDX
public class DToken extends Token {
    ArrayList<TokenAction> effects;
    HashMap<Figure.Attribute, Integer> attributeModifiers;
    DescentTypes.DescentToken tokenType;
    Vector2D position;  // If null, not on map

    public DToken(DescentTypes.DescentToken tokenType, Vector2D pos) {
        super(tokenType.name());
        this.tokenType = tokenType;
        this.position = pos;
        effects = new ArrayList<>();
        attributeModifiers = new HashMap<>();
    }
    public DToken(DescentTypes.DescentToken tokenType, Vector2D pos, int componentID) {
        super(tokenType.name(), componentID);
        this.tokenType = tokenType;
        this.position = pos;
        effects = new ArrayList<>();
        attributeModifiers = new HashMap<>();
    }

    @Override
    public void setOwnerId(int ownerId) {
        super.setOwnerId(ownerId);
        int a = 0;
    }

    public void setOwnerId(int ownerId, DescentGameState dgs) {
        if (this.ownerId != -1) {
            Hero hero = dgs.getHeroes().get(ownerId);
            // Revert attribute modifiers
            for (Figure.Attribute a: attributeModifiers.keySet()) {
                int curMax = hero.getAttribute(a).getMaximum();
                hero.getAttribute(a).setMaximum(curMax + attributeModifiers.get(a)*-1);
            }
            // Remove abilities given by owning this token
            for (DescentAction ef: effects) {
                hero.removeAbility(ef);
            }
        }
        super.setOwnerId(ownerId);
        if (this.ownerId != -1) {
            Hero hero = dgs.getHeroes().get(ownerId);
            // Add attribute modifiers
            for (Figure.Attribute a: attributeModifiers.keySet()) {
                int curMax = hero.getAttribute(a).getMaximum();
                hero.getAttribute(a).setMaximum(curMax + attributeModifiers.get(a));
            }
            // Add abilities given by owning this token
            for (DescentAction ef: effects) {
                hero.addAbility(ef);
            }
        }
    }

    public void setEffects(ArrayList<TokenAction> effects) {
        this.effects = effects;
    }

    public void setAttributeModifiers(HashMap<Figure.Attribute, Integer> attributeModifiers) {
        this.attributeModifiers = attributeModifiers;
    }

    public ArrayList<TokenAction> getEffects() {
        return effects;
    }

    public HashMap<Figure.Attribute, Integer> getAttributeModifiers() {
        return attributeModifiers;
    }

    @Override
    public DToken copy() {
        DToken copy = new DToken(tokenType, position != null? position.copy() : null, componentID);
        for (TokenAction act: effects) {
            copy.effects.add(act.copy());
        }
        copy.attributeModifiers = new HashMap<>(attributeModifiers);
        copyComponentTo(copy);
        return copy;
    }

    public DescentTypes.DescentToken getDescentTokenType() {
        return tokenType;
    }

    public Vector2D getPosition() {
        return position;
    }

    public void setPosition(Vector2D position) {
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DToken)) return false;
        if (!super.equals(o)) return false;
        DToken dToken = (DToken) o;
        return tokenType == dToken.tokenType && Objects.equals(position, dToken.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tokenType, position);
    }

    public static class DTokenDef {
        DescentTypes.DescentToken tokenType;
        String setupHowMany;
        String[] locations;
        ArrayList<TokenAction> effects;
        HashMap<Figure.Attribute, Integer> attributeModifiers;
        String altName;

        public DescentTypes.DescentToken getTokenType() {
            return tokenType;
        }

        public void setTokenType(DescentTypes.DescentToken tokenType) {
            this.tokenType = tokenType;
        }

        public String getSetupHowMany() {
            return setupHowMany;
        }

        public void setSetupHowMany(String setupHowMany) {
            this.setupHowMany = setupHowMany;
        }

        public String[] getLocations() {
            return locations;
        }

        public void setLocations(String[] locations) {
            this.locations = locations;
        }

        public void setAttributeModifiers(HashMap<Figure.Attribute, Integer> attributeModifiers) {
            this.attributeModifiers = attributeModifiers;
        }

        public ArrayList<TokenAction> getEffects() {
            return effects;
        }

        public HashMap<Figure.Attribute, Integer> getAttributeModifiers() {
            return attributeModifiers;
        }

        public void setEffects(ArrayList<TokenAction> effects) {
            this.effects = effects;
        }

        public String getAltName() {
            return altName;
        }

        public void setAltName(String altName) {
            this.altName = altName;
        }
    }
}
