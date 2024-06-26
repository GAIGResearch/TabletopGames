package games.virus.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.virus.VirusGameState;
import games.virus.cards.VirusCard;
import games.virus.components.VirusBody;

import java.util.Objects;

public class PlayOrganThief extends PlayVirusCard implements IPrintable {
    private int                 otherPlayerId;
    private int                 otherBodyId;
    private VirusCard.OrganType organ;

    public PlayOrganThief(int deckFrom, int deckTo, int fromIndex, int bodyId, int otherBodyId, int otherPlayerId, VirusCard.OrganType organ) {
        super(deckFrom, deckTo, fromIndex, bodyId);
        this.otherPlayerId = otherPlayerId;
        this.otherBodyId   = otherBodyId;
        this.organ         = organ;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        VirusGameState vgs = (VirusGameState) gs;
        super.execute(gs);

        VirusBody myBody    = (VirusBody) vgs.getComponentById(bodyId);
        VirusBody otherBody = (VirusBody) vgs.getComponentById(otherBodyId);

        Deck<VirusCard> cards = otherBody.removeOrgan(organ);
        myBody.addCardsToOrgan(cards, organ);

        return true;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Play " + getCard(gameState).toString() + " on " + organ + " of player " + otherPlayerId;
    }

    @Override
    public void printToConsole() {
        System.out.println("Play Treatment OrganThief");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayOrganThief)) return false;
        if (!super.equals(o)) return false;
        PlayOrganThief that = (PlayOrganThief) o;
        return otherPlayerId == that.otherPlayerId && otherBodyId == that.otherBodyId && organ == that.organ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), otherBodyId, otherPlayerId, organ);
    }

    @Override
    public AbstractAction copy() {
        return new PlayOrganThief(deckFrom, deckTo, fromIndex, bodyId, otherBodyId, otherPlayerId, organ);
    }
}
