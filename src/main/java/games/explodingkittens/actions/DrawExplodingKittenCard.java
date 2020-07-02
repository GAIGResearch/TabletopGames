package games.explodingkittens.actions;

import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.explodingkittens.ExplodingKittenTurnOrder;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittenCard;

import static core.CoreConstants.VERBOSE;
import static games.explodingkittens.ExplodingKittensGameState.ExplodingKittensGamePhase.Defuse;

public class DrawExplodingKittenCard extends DrawCard implements IPrintable {

    public DrawExplodingKittenCard(int deckFrom, int deckTo) {
        super(deckFrom, deckTo);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gs;
        int playerID = gs.getCurrentPlayer();
        Deck<ExplodingKittenCard> from = ((ExplodingKittensGameState) gs).getDrawPile();
        Deck<ExplodingKittenCard> to = ((ExplodingKittensGameState) gs).getPlayerHandCards().get(playerID);
        Deck<ExplodingKittenCard> discardDeck = ((ExplodingKittensGameState)gs).getDiscardPile();

        // Draw the card for the player
        super.execute(gs);

        // Execute exploding kitten effect
        ExplodingKittenCard c = (ExplodingKittenCard) getCard(gs);
        ExplodingKittenCard.CardType type = c.cardType;
        if (type == ExplodingKittenCard.CardType.EXPLODING_KITTEN) {
            // An exploding kitten was drawn, check if player has defuse card
            int defuseCard = -1;
            for (int card = 0; card < to.getSize(); card++){
                if (to.getComponents().get(card).cardType == ExplodingKittenCard.CardType.DEFUSE) {
                    defuseCard = card;
                    break;
                }
            }
            if (defuseCard != -1){
                // Player does have defuse card. Set game phase and put defuse card in discard deck
                new DrawCard(deckTo, discardDeck.getComponentID(), defuseCard).execute(gs);
                gs.setGamePhase(Defuse);
            } else {
                if (VERBOSE) {
                    System.out.println("Player " + playerID + " died");
                }
                discardDeck.add(to);
                to.clear();
                ((ExplodingKittensGameState) gs).killPlayer(playerID);
            }
        } else {
            ((ExplodingKittenTurnOrder)gs.getTurnOrder()).endPlayerTurnStep(gs);
        }
        return true;
    }

    @Override
    public String toString(){
        return "Player draws a card";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Player " + gameState.getCurrentPlayer() + " draws a card";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this.toString());
    }

    @Override
    public AbstractAction copy() {
        return new DrawExplodingKittenCard(deckFrom, deckTo);
    }
}
