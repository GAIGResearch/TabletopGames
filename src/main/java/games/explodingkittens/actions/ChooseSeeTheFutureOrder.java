package games.explodingkittens.actions;

import core.actions.IAction;
import core.AbstractGameState;
import core.components.Card;
import core.components.PartialObservableDeck;
import core.observations.IPrintable;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittenCard;


public class ChooseSeeTheFutureOrder implements IAction, IPrintable {
    private ExplodingKittenCard card1;
    private ExplodingKittenCard card2;
    private ExplodingKittenCard card3;
    private PartialObservableDeck<ExplodingKittenCard> drawPile;
    private int playerID;

    public ChooseSeeTheFutureOrder(PartialObservableDeck<ExplodingKittenCard> drawPile,
                                   ExplodingKittenCard card1, ExplodingKittenCard card2, ExplodingKittenCard card3,
                                   int playerID){
        this.drawPile = drawPile;
        this.card1 = card1;
        this.card2 = card2;
        this.card3 = card3;
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (card1 != null)
            drawPile.remove(card1);
        if (card2 != null)
            drawPile.remove(card2);
        if (card3 != null)
            drawPile.remove(card3);

        if (card3 != null)
            drawPile.add(card3);
        if (card2 != null)
            drawPile.add(card2);
        if (card1 != null)
            drawPile.add(card1);

        for (int i = 0; i < 3; i++) {
            if (i == 0 && card1 != null)
                continue;
            if (i == 1 && card2 != null)
                continue;
            if (i == 2 && card3 != null)
                continue;

            for (int j = 0; j < gs.getNPlayers(); j++){
                drawPile.setVisibilityOfCard(i, j, false);        // other players don't know the order anymore
            }
            drawPile.setVisibilityOfCard(i, playerID, true);      // this player knows the first three cards
        }
        gs.setMainGamePhase();
        return false;
    }

    @Override
    public Card getCard() {
        return null;
    }

    public String toString(){
        String card1String = "no_card_left";
        String card2String = "no_card_left";
        String card3String = "no_card_left";

        if (card3 != null)
            card3String = card3.cardType.toString();
        if (card2 != null)
            card2String = card2.cardType.toString();
        if (card1 != null)
            card1String = card1.cardType.toString();

        return "Chosen card order: " + card1String + ", " + card2String + ", " + card3String;
    }

    @Override
    public void printToConsole() {
        System.out.println(toString());
    }

}
