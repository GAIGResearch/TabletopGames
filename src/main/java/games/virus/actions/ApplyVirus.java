package games.virus.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.virus.components.VirusBody;
import games.virus.VirusGameState;
import games.virus.components.VirusOrgan;
import games.virus.cards.VirusCard;

public class ApplyVirus extends PlayVirusCard implements IPrintable {
    public ApplyVirus(int deckFrom, int deckTo, int fromIndex, int bodyId) {
        super(deckFrom, deckTo, fromIndex, bodyId);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        VirusGameState vgs = (VirusGameState) gs;
        super.execute(gs);
        VirusCard card = (VirusCard) getCard(gs);
        VirusBody body = getBody(gs);

        VirusOrgan.VirusOrganState newState = body.applyVirus(card);

        // discard cards?
        if (newState == VirusOrgan.VirusOrganState.Neutral)
        {
            vgs.getDiscardDeck().add(body.removeAVirusCard(card));
            vgs.getDiscardDeck().add(body.removeAMedicineCard(card));
        }
        else if (newState == VirusOrgan.VirusOrganState.None)
        {
            vgs.getDiscardDeck().add(body.removeAVirusCard(card));
            vgs.getDiscardDeck().add(body.removeAVirusCard(card));
            vgs.getDiscardDeck().add(body.removeAnOrganCard(card));
        }
        return true;
    }

    @Override
    public void printToConsole() {
        System.out.println("Apply virus");
    }

    @Override
    public AbstractAction copy() {
        return new ApplyVirus(deckFrom, deckTo, fromIndex, bodyId);
    }
}
