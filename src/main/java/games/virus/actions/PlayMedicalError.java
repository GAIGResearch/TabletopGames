package games.virus.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.virus.VirusGameState;
import games.virus.cards.VirusCard;
import games.virus.components.VirusBody;

import java.util.Objects;

public class PlayMedicalError extends PlayVirusCard implements IPrintable {
    private int otherBodyId;
    private int playerId;
    private int otherPlayerId;

    public PlayMedicalError(int deckFrom, int deckTo, int fromIndex, int bodyId, int otherBodyId, int playerId, int otherPlayerId) {
        super(deckFrom, deckTo, fromIndex, bodyId);

        this.otherBodyId    = otherBodyId;
        this.playerId       = playerId;
        this.otherPlayerId  = otherPlayerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        VirusGameState vgs = (VirusGameState) gs;
        super.execute(gs);

        VirusBody myBody    = (VirusBody) vgs.getComponentById(bodyId);
        VirusBody otherBody = (VirusBody) vgs.getComponentById(otherBodyId);

        VirusBody temp = new VirusBody();

        for (VirusCard.OrganType organType : VirusCard.OrganType.values()) {
            if (myBody.hasOrgan(organType)) {
                Deck<VirusCard> cards = myBody.removeOrgan(organType);
                temp.addCardsToOrgan(cards, organType);
            }
        }

        for (VirusCard.OrganType organType : VirusCard.OrganType.values()) {
            if (otherBody.hasOrgan(organType)) {
                Deck<VirusCard> cards = otherBody.removeOrgan(organType);
                myBody.addCardsToOrgan(cards, organType);
            }
        }

        for (VirusCard.OrganType organType : VirusCard.OrganType.values()) {
            if (temp.hasOrgan(organType)) {
                Deck<VirusCard> cards = temp.removeOrgan(organType);
                otherBody.addCardsToOrgan(cards, organType);
            }
        }

        return true;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Play " + getCard(gameState).toString() + " replacing body of player " + playerId + " with " +
                " body of player " + otherPlayerId;
    }

    @Override
    public void printToConsole() {
        System.out.println("Play Treatment Medical Error");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayMedicalError)) return false;
        if (!super.equals(o)) return false;
        PlayMedicalError that = (PlayMedicalError) o;
        return playerId == that.playerId && otherPlayerId == that.otherPlayerId && otherBodyId == that.otherBodyId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), otherBodyId, playerId, otherPlayerId);
    }

    @Override
    public AbstractAction copy() {
        return new PlayMedicalError(deckFrom, deckTo, fromIndex, bodyId, otherBodyId, playerId, otherPlayerId);
    }

}
