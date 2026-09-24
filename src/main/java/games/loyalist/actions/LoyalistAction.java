package games.loyalist.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

import games.loyalist.LoyalistForwardModel;
import games.loyalist.LoyalistGameState;

import java.util.Objects;

/** Values describe the current player's choice, never references to hidden state. */
public final class LoyalistAction extends AbstractAction {
    public enum Kind {
        ASSIGN,
        PASS,
        CHOOSE,
        BUY,
        SELL,
        SUPPLY,
        CONVERT_SUPPLY,
        REPORT,
        NOMINATE,
        STEAL,
        CONTRIBUTE,
        PLAY_CARD,
        FINISH_CARDS,
        INSPECT,
        TESTIFY,
        REWARD_TOKEN,
        REWARD_CARDS,
        RETURN,
        EXILE,
        DISCARD_BLIND,
        CONVERT,
        KEEP,
        LEVY,
        INFORMANT,
        AUDIT,
        APPOINT
    }

    public final Kind kind;
    public final int value;

    public LoyalistAction(Kind kind) {
        this(kind, 0);
    }

    public LoyalistAction(Kind kind, int value) {
        this.kind = kind;
        this.value = value;
    }

    @Override
    public boolean execute(AbstractGameState state) {
        LoyalistForwardModel.apply((LoyalistGameState) state, this);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LoyalistAction a && kind == a.kind && value == a.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, value);
    }

    @Override
    public String getString(AbstractGameState state) {
        return kind + " " + value;
    }

    @Override
    public String getString(AbstractGameState state, int perspective) {
        return perspective == state.getCurrentPlayer() || isPublic()
                ? getString(state)
                : "Private decision";
    }

    private boolean isPublic() {
        return switch (kind) {
            case ASSIGN,
                            PASS,
                            BUY,
                            SELL,
                            REPORT,
                            NOMINATE,
                            TESTIFY,
                            REWARD_TOKEN,
                            REWARD_CARDS,
                            RETURN,
                            EXILE,
                            CONVERT,
                            LEVY,
                            APPOINT ->
                    true;
            default -> false;
        };
    }

    @Override
    public String toString() {
        return kind + " " + value;
    }
}
