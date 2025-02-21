package games.descent2e.actions.tokens;

import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;

import java.util.Objects;

public abstract class TokenAction<A extends TokenAction<A>> extends DescentAction {
    int tokenID;

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
        if (!(o instanceof TokenAction that)) return false;
        return tokenID == that.tokenID;
    }

    @Override
    public int hashCode() {
        return tokenID + 292;
    }

    public final A copy() {
        A retValue =  _copy();
        retValue.tokenID = tokenID;
        return retValue;
    }

    public abstract A _copy();
}
