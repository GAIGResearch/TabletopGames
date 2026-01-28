package games.thegame.components;

import core.CoreConstants;
import core.components.Component;
import core.components.Deck;

public class TheGameDeck<T extends Component> extends Deck<T> {

    private final boolean ascending;

    public TheGameDeck(String name, CoreConstants.VisibilityMode visibility, boolean ascending) {
        super(name, visibility);
        this.ascending = ascending;
    }

    public boolean isAscending() {
        return ascending;
    }

    public TheGameDeck<T> copy()
    {
        TheGameDeck<T> copy = new TheGameDeck<>(super.componentName, super.visibility, this.ascending);
        copyTo(copy);
        return copy;
    }
}
