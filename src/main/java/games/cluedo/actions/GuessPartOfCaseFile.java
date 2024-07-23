package games.cluedo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.cluedo.CluedoConstants;
import games.cluedo.CluedoGameState;
import games.cluedo.cards.CharacterCard;
import games.cluedo.cards.CluedoCard;
import games.cluedo.cards.RoomCard;
import games.cluedo.cards.WeaponCard;

import java.util.Objects;

public class GuessPartOfCaseFile extends AbstractAction  {
    CluedoCard guessCard;

    public GuessPartOfCaseFile(AbstractGameState gameState, CluedoConstants.Character guess) {
        CluedoGameState cgs = (CluedoGameState) gameState;
        for (CluedoCard card : cgs.getAllCards()) {
            if (Objects.equals(card.getComponentName(), guess.name())) {
                guessCard = card;
                break;
            }
        }
    }

    public GuessPartOfCaseFile(AbstractGameState gameState, CluedoConstants.Weapon guess) {
        CluedoGameState cgs = (CluedoGameState) gameState;
        for (CluedoCard card : cgs.getAllCards()) {
            if (Objects.equals(card.getComponentName(), guess.name())) {
                guessCard = card;
                break;
            }
        }    }

    public GuessPartOfCaseFile(AbstractGameState gameState, CluedoConstants.Room guess) {
        CluedoGameState cgs = (CluedoGameState) gameState;
        for (CluedoCard card : cgs.getAllCards()) {
            if (Objects.equals(card.getComponentName(), guess.name())) {
                guessCard = card;
                break;
            }
        }    }

    private GuessPartOfCaseFile(CluedoCard card) {
        guessCard = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CluedoGameState cgs = (CluedoGameState) gs;
        if (guessCard != null && cgs.currentGuess.getSize() != 3) {
            cgs.currentGuess.add(guessCard);
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new GuessPartOfCaseFile(guessCard);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GuessPartOfCaseFile)) return false;
        GuessPartOfCaseFile that = (GuessPartOfCaseFile) o;
        return guessCard == that.guessCard;
    }

    @Override
    public int hashCode() {
        return Objects.hash(guessCard);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "GuessPartOfCaseFile{" +
                "guessCard=" + (guessCard != null? guessCard : "guess-card-not-found");
    }
}
