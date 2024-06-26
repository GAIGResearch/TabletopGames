package games.virus.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.virus.VirusGameState;
import games.virus.cards.VirusCard;
import games.virus.components.VirusBody;

import java.util.Objects;

import static games.virus.components.VirusOrgan.VirusOrganState.Neutral;

public class PlaySpreading extends PlayVirusCard implements IPrintable {

    private int                 otherPlayerId;
    private int                 otherPlayerBodyId;
    private VirusCard.OrganType myOrganType;
    private VirusCard.OrganType otherOrganType;

    public PlaySpreading(int deckFrom, int deckTo, int fromIndex, int bodyId, int otherPlayerId, int otherPlayerBodyId,
                         VirusCard.OrganType myOrganType, VirusCard.OrganType otherOrganType) {
        super(deckFrom, deckTo, fromIndex, bodyId);

        this.otherPlayerId     = otherPlayerId;
        this.otherPlayerBodyId = otherPlayerBodyId;
        this.myOrganType       = myOrganType;
        this.otherOrganType    = otherOrganType;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        VirusGameState vgs = (VirusGameState) gs;
        super.execute(gs);

        VirusBody myBody    = (VirusBody) vgs.getComponentById(bodyId);
        VirusBody otherBody = (VirusBody) vgs.getComponentById(otherPlayerBodyId);

        VirusCard card = myBody.removeAVirusCard(myOrganType);
        myBody.organs.get(myOrganType).state= Neutral;
        otherBody.applyVirus(card, otherOrganType);

        return true;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Play " + getCard(gameState).toString() + " Move virus " + myOrganType + " to organ " + otherOrganType +" of player " + otherPlayerId;
    }

    @Override
    public void printToConsole() {
        System.out.println("Play Treatment Spreading");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaySpreading)) return false;
        PlaySpreading that = (PlaySpreading) o;
        return super.equals(o) &&
                otherPlayerId == that.otherPlayerId &&
                otherPlayerBodyId == that.otherPlayerBodyId &&
                myOrganType == that.myOrganType &&
                otherOrganType == that.otherOrganType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), otherPlayerId, otherPlayerBodyId, myOrganType, otherOrganType);
    }

    @Override
    public AbstractAction copy() {
        return new PlaySpreading(deckFrom, deckTo, fromIndex, bodyId, otherPlayerId, otherPlayerBodyId, myOrganType, otherOrganType);
    }
}