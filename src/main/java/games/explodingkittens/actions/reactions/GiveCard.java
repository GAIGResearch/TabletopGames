package games.explodingkittens.actions.reactions;

import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.explodingkittens.ExplodingKittensTurnOrder;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

public class GiveCard extends DrawCard implements IPrintable {

    public GiveCard(int deckFrom, int deckTo, int index) {
        super(deckFrom, deckTo, index);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gs;
        ExplodingKittensTurnOrder ekto = ((ExplodingKittensTurnOrder) gs.getTurnOrder());
        if (fromIndex > -1) { // to allow for GiveCard to occur when the target's deck is empty
            Deck<ExplodingKittensCard> from = (Deck<ExplodingKittensCard>) ekgs.getComponentById(deckFrom);
            Deck<ExplodingKittensCard> to = (Deck<ExplodingKittensCard>) ekgs.getComponentById(deckTo);

            ExplodingKittensCard c = from.pick(fromIndex);
            to.add(c);
        }
        gs.setMainGamePhase();
        ekto.endPlayerTurnStep(gs);
        ekto.addReactivePlayer(ekgs.getPlayerGettingAFavor());
        return true;
    }

    @Override
    public String toString() {
        return "Player gives card for a favor";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Card card = getCard(gameState);
        return card == null ? "No Card to give" : "Give " + card.getComponentName() + " for a favor";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this.toString());
    }

    @Override
    public AbstractAction copy() {
        return new GiveCard(deckFrom, deckTo, fromIndex);
    }
}
