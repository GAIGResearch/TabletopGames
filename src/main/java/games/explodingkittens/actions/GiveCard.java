package games.explodingkittens.actions;

import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.explodingkittens.ExplodingKittenTurnOrder;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittenCard;

public class GiveCard extends DrawCard implements IPrintable {

    public GiveCard(int deckFrom, int deckTo, int index) {
        super(deckFrom, deckTo, index);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gs;
        ExplodingKittenTurnOrder ekto = ((ExplodingKittenTurnOrder) gs.getTurnOrder());
        Deck<ExplodingKittenCard> from = (Deck<ExplodingKittenCard>) ekgs.getComponentById(deckFrom);
        Deck<ExplodingKittenCard> to = (Deck<ExplodingKittenCard>) ekgs.getComponentById(deckTo);

        ExplodingKittenCard c = from.pick(fromIndex);
        to.add(c);
        gs.setMainGamePhase();
        ekto.endPlayerTurnStep(gs);
        ekto.addReactivePlayer(ekgs.getPlayerGettingAFavor());
        return true;
    }

    @Override
    public String toString(){
        return "Player gives card for a favor";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Player " + gameState.getCurrentPlayer() + " gives card " + getCard(gameState).getComponentName()
                + " for a favor";
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
