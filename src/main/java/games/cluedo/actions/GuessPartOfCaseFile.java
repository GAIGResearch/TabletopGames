package games.cluedo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.cluedo.CluedoConstants;
import games.cluedo.CluedoGameState;
import games.cluedo.cards.CluedoCard;

import java.util.Objects;

public class GuessPartOfCaseFile extends AbstractAction  {
    CluedoCard guessCard;
    String guessName;

    public GuessPartOfCaseFile(AbstractGameState gameState, CluedoConstants.Character guess) {
        guessPartOfCaseFile(gameState, guess);
    }

    public GuessPartOfCaseFile(AbstractGameState gameState, CluedoConstants.Weapon guess) {
        guessPartOfCaseFile(gameState, guess);
    }

    public GuessPartOfCaseFile(AbstractGameState gameState, CluedoConstants.Room guess) {
        guessPartOfCaseFile(gameState, guess);
    }

    private void guessPartOfCaseFile(AbstractGameState gameState, Object guess) {
        CluedoGameState cgs = (CluedoGameState) gameState;
        guessName = guess.toString();
        for (CluedoCard card : cgs.getAllCards()) {
            if (Objects.equals(card.getComponentName(), guess.toString())) {
                guessCard = card;
                break;
            }
        }
    }

    private GuessPartOfCaseFile(CluedoCard card, String cardName) {
        guessCard = card;
        guessName = cardName;
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
        return new GuessPartOfCaseFile(guessCard, guessName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GuessPartOfCaseFile)) return false;
        GuessPartOfCaseFile that = (GuessPartOfCaseFile) o;
        return Objects.equals(guessCard, that.guessCard)
                && Objects.equals(guessName, that.guessName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(guessCard, guessName);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "GuessPartOfCaseFile{" +
                "guessCard=" + (guessCard != null? guessCard : "guess-card-not-found") +
                "guessName=" + (guessName != null? guessName : "guess-name-not-found");
    }

    public String getGuessName() {
        return guessName;
    }
}
