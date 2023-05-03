package games.explodingkittens.actions;

import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.explodingkittens.ExplodingKittensTurnOrder;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

import static games.explodingkittens.ExplodingKittensGameState.ExplodingKittensGamePhase.Defuse;

public class DrawExplodingKittenCard extends DrawCard implements IPrintable {

    public DrawExplodingKittenCard(int deckFrom, int deckTo) {
        super(deckFrom, deckTo);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gs;
        int playerID = gs.getCurrentPlayer();
        Deck<ExplodingKittensCard> from = ((ExplodingKittensGameState) gs).getDrawPile();
        Deck<ExplodingKittensCard> to = ((ExplodingKittensGameState) gs).getPlayerHandCards().get(playerID);
        Deck<ExplodingKittensCard> discardDeck = ((ExplodingKittensGameState)gs).getDiscardPile();

        // Draw the card for the player
        super.execute(gs);

        // Execute exploding kitten effect
        ExplodingKittensCard c = (ExplodingKittensCard) getCard(gs);
        ExplodingKittensCard.CardType type = c.cardType;
        if (type == ExplodingKittensCard.CardType.EXPLODING_KITTEN) {
            // An exploding kitten was drawn, check if player has defuse card
            int defuseCard = -1;
            for (int card = 0; card < to.getSize(); card++){
                if (to.getComponents().get(card).cardType == ExplodingKittensCard.CardType.DEFUSE) {
                    defuseCard = card;
                    break;
                }
            }
            if (defuseCard != -1){
                // Player does have defuse card. Set game phase and put defuse card in discard deck
                new DrawCard(deckTo, discardDeck.getComponentID(), defuseCard).execute(gs);
                gs.setGamePhase(Defuse);
            } else {
                discardDeck.add(to);
                to.clear();
                ekgs.killPlayer(playerID);
            }
        } else {
            ((ExplodingKittensTurnOrder)ekgs.getTurnOrder()).endPlayerTurnStep(gs);
        }
        return true;
    }

    @Override
    public String toString(){
        return "Player draws a card";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Draw card";
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
