package games.descent2e.actions.tokens;

import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;

import java.util.Objects;

public abstract class TokenAction extends DescentAction {
    int tokenID;  // TODO copy this in subclasses

    public TokenAction(int tokenID, Triggers triggerPoint) {
        super(triggerPoint);
        this.tokenID = tokenID;
    }

    public void setTokenID(int tokenID) {
        this.tokenID = tokenID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenAction that = (TokenAction) o;
        return tokenID == that.tokenID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenID);
    }
}
