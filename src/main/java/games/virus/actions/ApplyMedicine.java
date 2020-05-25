package games.virus.actions;

import core.AbstractGameState;
import core.actions.IAction;
import core.components.Deck;
import core.observations.IPrintable;
import games.virus.VirusBody;
import games.virus.VirusGameState;
import games.virus.VirusOrgan;
import games.virus.cards.VirusCard;

public class ApplyMedicine implements IAction, IPrintable {

    private VirusCard       card;
    private VirusBody       body;
    private Deck<VirusCard> playerHand;
    private Deck<VirusCard> drawDeck;
    private Deck<VirusCard> discardDeck;
    private int             playerId;

    public ApplyMedicine(VirusCard card, VirusBody body, Deck<VirusCard> playerHand, int playerId,
                         Deck<VirusCard> drawDeck, Deck<VirusCard> discardDeck) {
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
        VirusOrgan.VirusOrganState newState = body.applyMedicine(card);

        if (drawDeck.getSize() == 0)
            ((VirusGameState) gs).discardToDraw();

        VirusCard newCard = drawDeck.draw();
        playerHand.add(newCard);

        // discard cards?
        if (newState == VirusOrgan.VirusOrganState.Neutral)
        {
            discardDeck.add(body.removeAMedicineCard(card));
            discardDeck.add(body.removeAVirusCard(card));
        }
        return true;
    }

    @Override
    public void printToConsole() {
        System.out.println("Apply " + card.toString() + " on body of player " + playerId);

    }
}
