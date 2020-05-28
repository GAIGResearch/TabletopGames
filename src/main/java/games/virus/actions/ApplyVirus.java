package games.virus.actions;

import core.AbstractGameState;
import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import core.observations.IPrintable;
import games.virus.VirusBody;
import games.virus.VirusGameState;
import games.virus.VirusOrgan;
import games.virus.cards.VirusCard;

public class ApplyVirus implements IAction, IPrintable {
    private VirusCard       card;
    private VirusBody       body;
    private Deck<VirusCard> playerHand;
    private Deck<VirusCard> drawDeck;
    private Deck<VirusCard> discardDeck;
    private int             playerId;

    public ApplyVirus(VirusCard card, VirusBody body, Deck<VirusCard> playerHand, int playerId,
                      Deck<VirusCard> drawDeck,  Deck<VirusCard> discardDeck) {
        this.card        = card;
        this.body        = body;
        this.playerHand  = playerHand;
        this.playerId    = playerId;
        this.drawDeck    = drawDeck;
        this.discardDeck = discardDeck;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        playerHand.remove(card);
        VirusOrgan.VirusOrganState newState = body.applyVirus(card);

        if (drawDeck.getSize() == 0)
            ((VirusGameState) gs).discardToDraw();

        VirusCard newCard = drawDeck.draw();
        playerHand.add(newCard);

        // discard cards?
        if (newState == VirusOrgan.VirusOrganState.Neutral)
        {
            discardDeck.add(body.removeAVirusCard(card));
            discardDeck.add(body.removeAMedicineCard(card));
        }
        else if (newState == VirusOrgan.VirusOrganState.None)
        {
            discardDeck.add(body.removeAVirusCard(card));
            discardDeck.add(body.removeAVirusCard(card));
            discardDeck.add(body.removeAnOrganCard(card));
        }
        return true;
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public void printToConsole() {
        System.out.println("Apply " + card.toString() + " on body of player " + playerId);
    }
}
