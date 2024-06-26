package games.virus.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.virus.components.VirusBody;
import games.virus.VirusGameState;
import games.virus.components.VirusOrgan;
import games.virus.cards.VirusCard;

import java.util.Objects;

public class ApplyVirus extends PlayVirusCard implements IPrintable {
    private VirusCard.OrganType organ;
    private int otherPlayerId;

    public ApplyVirus(int deckFrom, int deckTo, int fromIndex, int bodyId, VirusCard.OrganType organ, int otherPlayerId) {
        super(deckFrom, deckTo, fromIndex, bodyId);
        this.organ         = organ;
        this.otherPlayerId = otherPlayerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        VirusGameState vgs = (VirusGameState) gs;
        super.execute(gs);
        VirusCard card = (VirusCard) getCard(gs);
        VirusBody body = getBody(gs);

        VirusOrgan.VirusOrganState newState = body.applyVirus(card, organ);

        // discard cards?
        if (newState == VirusOrgan.VirusOrganState.Neutral)
        {
            vgs.getDiscardDeck().add(body.removeAVirusCard(organ));
            vgs.getDiscardDeck().add(body.removeAMedicineCard(organ));
        }
        else if (newState == VirusOrgan.VirusOrganState.None)
        {
            vgs.getDiscardDeck().add(body.removeAVirusCard(organ));
            vgs.getDiscardDeck().add(body.removeAVirusCard(organ));
            vgs.getDiscardDeck().add(body.removeAnOrganCard(organ));
        }
        return true;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Apply " + getCard(gameState).toString() + " on " + organ + " of player " + otherPlayerId;
    }
    @Override
    public void printToConsole() {
        System.out.println("Apply virus");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApplyVirus)) return false;
        if (!super.equals(o)) return false;
        ApplyVirus that = (ApplyVirus) o;
        return organ == that.organ && otherPlayerId == that.otherPlayerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), organ, otherPlayerId);
    }

    @Override
    public AbstractAction copy() {
        return new ApplyVirus(deckFrom, deckTo, fromIndex, bodyId, organ, otherPlayerId);
    }


}
