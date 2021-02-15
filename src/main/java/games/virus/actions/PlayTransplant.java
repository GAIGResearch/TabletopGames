package games.virus.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.virus.VirusGameState;
import games.virus.cards.VirusCard;
import games.virus.components.VirusBody;

import java.util.Objects;

public class PlayTransplant extends PlayVirusCard implements IPrintable {
    private int                 playerId;
    private int                 otherPlayerId;
    private int                 otherBodyId;
    private VirusCard.OrganType myOrganType;
    private VirusCard.OrganType otherOrganType;

    public PlayTransplant(int deckFrom, int deckTo, int fromIndex, int bodyId, int otherBodyId, int playerId, int otherPlayerId,
                          VirusCard.OrganType myOrganType, VirusCard.OrganType otherOrganType) {
        super(deckFrom, deckTo, fromIndex, bodyId);
        this.playerId       = playerId;
        this.otherPlayerId  = otherPlayerId;
        this.otherBodyId    = otherBodyId;
        this.myOrganType    = myOrganType;
        this.otherOrganType = otherOrganType;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        VirusGameState vgs = (VirusGameState) gs;
        super.execute(gs);

        VirusBody myBody    = (VirusBody) vgs.getComponentById(bodyId);
        VirusBody otherBody = (VirusBody) vgs.getComponentById(otherBodyId);

        Deck<VirusCard> myCards    = myBody.removeOrgan(myOrganType);
        Deck<VirusCard> otherCards = otherBody.removeOrgan(otherOrganType);

        myBody.addCardsToOrgan(otherCards, otherOrganType);
        otherBody.addCardsToOrgan(myCards, myOrganType);

        return true;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Play " + getCard(gameState).toString() + " replacing " + myOrganType + " of player " + playerId + " with " +
                otherOrganType + " of player " + otherPlayerId;
    }

    @Override
    public void printToConsole() {
        System.out.println("Play Treatment OrganThief");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayTransplant)) return false;
        if (!super.equals(o)) return false;
        PlayTransplant that = (PlayTransplant) o;
        return playerId == that.playerId && otherPlayerId == that.otherPlayerId && otherBodyId == that.otherBodyId
                && myOrganType == that.myOrganType && otherOrganType == that.otherOrganType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), otherBodyId, playerId, otherPlayerId, myOrganType, otherOrganType);
    }

    @Override
    public AbstractAction copy() {
        return new PlayTransplant(deckFrom, deckTo, fromIndex, bodyId, otherBodyId, playerId, otherPlayerId, myOrganType, otherOrganType);
    }
}
