package games.descent2e.components.tokens;

import core.components.Token;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import utilities.Vector2D;

import java.util.Objects;

public class DToken extends Token {

    DescentTypes.DescentToken tokenType;
    Vector2D position;  // If null, not on map

    public DToken(DescentTypes.DescentToken tokenType, Vector2D pos) {
        super(tokenType.name());
        this.tokenType = tokenType;
        this.position = pos;
    }
    public DToken(DescentTypes.DescentToken tokenType, Vector2D pos, int componentID) {
        super(tokenType.name(), componentID);
        this.tokenType = tokenType;
        this.position = pos;
    }

    public void setOwnerId(int ownerId, DescentGameState dgs) {
        super.setOwnerId(ownerId);
    }

    @Override
    public DToken copy() {
        DToken copy = new DToken(tokenType, position != null? position.copy() : null, componentID);
        copyComponentTo(this);
        return copy;
    }

    public DescentTypes.DescentToken getDescentTokenType() {
        return tokenType;
    }

    public Vector2D getPosition() {
        return position;
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
        String rule;
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

        public String getRule() {
            return rule;
        }

        public void setRule(String rule) {
            this.rule = rule;
        }

        public String getAltName() {
            return altName;
        }

        public void setAltName(String altName) {
            this.altName = altName;
        }
    }
}
