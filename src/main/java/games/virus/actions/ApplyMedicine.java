package games.virus.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.virus.components.VirusBody;
import games.virus.VirusGameState;
import games.virus.components.VirusOrgan;
import games.virus.cards.VirusCard;

import java.util.Objects;

public class ApplyMedicine extends PlayVirusCard implements IPrintable {
    private VirusCard.OrganType organ;

    public ApplyMedicine(int deckFrom, int deckTo, int fromIndex, int bodyId, VirusCard.OrganType organ) {
        super(deckFrom, deckTo, fromIndex, bodyId);
        this.organ = organ;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        VirusCard card = (VirusCard)getCard(gs);
        VirusBody body = getBody(gs);
        VirusGameState vgs = (VirusGameState) gs;
        VirusOrgan.VirusOrganState newState = body.applyMedicine(card, organ);

        // discard cards?
        if (newState == VirusOrgan.VirusOrganState.Neutral)
        {
            vgs.getDiscardDeck().add(body.removeAMedicineCard(card, organ));
            vgs.getDiscardDeck().add(body.removeAVirusCard(card, organ));
        }
        return true;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Apply " + getCard(gameState).toString() + " on " + organ + " of player " + gameState.getCurrentPlayer();
    }

    @Override
    public void printToConsole() {
        System.out.println("Apply medicine");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ApplyMedicine that = (ApplyMedicine) o;
        return bodyId == that.bodyId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), bodyId);
    }

    @Override
    public AbstractAction copy() {
        return new ApplyMedicine(deckFrom, deckTo, fromIndex, bodyId, organ);
    }
}
