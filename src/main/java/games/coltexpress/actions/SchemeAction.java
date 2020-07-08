package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.coltexpress.cards.ColtExpressCard;

import java.util.Arrays;
import java.util.Objects;


public class SchemeAction extends DrawCard implements IPrintable {

    private final boolean hidden;

    public SchemeAction(int handCards, int actionList, int cardIdx, boolean hidden){
        super(handCards, actionList, cardIdx);

        this.hidden = hidden;
    }

    @Override
    public boolean execute(AbstractGameState gs){
        super.execute(gs);

        PartialObservableDeck<ColtExpressCard> actionList = (PartialObservableDeck<ColtExpressCard>) gs.getComponentById(deckTo);
        ColtExpressCard card = (ColtExpressCard) getCard(gs);

        if (hidden){
            actionList.setVisibilityOfComponent(0, card.playerID, true);
        } else {
            boolean[] allVisible = new boolean[gs.getNPlayers()];
            Arrays.fill(allVisible, true);
            actionList.setVisibilityOfComponent(0, allVisible);
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SchemeAction)) return false;
        if (!super.equals(o)) return false;
        SchemeAction that = (SchemeAction) o;
        return hidden == that.hidden;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), hidden);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return ((ColtExpressCard)getCard(gameState)).cardType.name();
    }

    @Override
    public String toString(){
        if (hidden)
            return "PlayCard(hidden)";
        return "PlayCard";
    }

    @Override
    public AbstractAction copy() {
        return new SchemeAction(deckFrom, deckTo, fromIndex, hidden);
    }
}
