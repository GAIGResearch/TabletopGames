package core.actions;

import core.AbstractGameState;

public class LogEvent extends AbstractAction {

    public final String text;

    public LogEvent(String message) {
        text = message;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof LogEvent) && ((LogEvent) obj).text.equals(text);
    }

    @Override
    public int hashCode() {
        return text.hashCode() - 31;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}
