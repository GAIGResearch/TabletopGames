package games.virus.actions;

import core.AbstractGameState;
import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import core.observations.IPrintable;
import games.virus.VirusBody;
import games.virus.VirusGameState;
import games.virus.cards.VirusCard;


public class AddOrgan implements IAction, IPrintable {
    private VirusCard       card;
    private VirusBody       body;
    private Deck<VirusCard> playerHand;
    private Deck<VirusCard> drawDeck;
    private int             playerId;

    public AddOrgan(VirusCard card, VirusBody body, Deck<VirusCard> playerHand, int playerId,  Deck<VirusCard> drawDeck) {
        this.card       = card;
        this.body       = body;
        this.playerHand = playerHand;
        this.drawDeck   = drawDeck;
        this.playerId   = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        playerHand.remove(card);
        body.addOrgan(card);
        if (drawDeck.getSize() == 0)
            ((VirusGameState) gs).discardToDraw();

        VirusCard newCard = drawDeck.draw();
        playerHand.add(newCard);
        return true;
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public void printToConsole() {
        System.out.println("Add " + card.toString() + " on body of player " + playerId);
    }
}
