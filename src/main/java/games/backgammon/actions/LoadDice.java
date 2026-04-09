package games.backgammon.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Dice;
import games.backgammon.BGGameEvents;
import games.backgammon.BGGameState;
import games.backgammon.BGParameters;

import java.util.Arrays;
import java.util.Random;

/**
 * This modifies the pdf of one of the dice in the game
 */
public class LoadDice extends AbstractAction {

    private static Random rnd = new Random(System.currentTimeMillis());

    protected final double[] newPDF;
    protected final int die;
    protected final boolean singleRoll;
    protected final double detectionChance;

    static LoadDice getPermanentShift(int die, double[] newPDF, double detectionChance) {
        return new LoadDice(die, newPDF, false, detectionChance);
    }

    static LoadDice getOneOffShift(int die, double[] newPDF, double detectionChance) {
        return new LoadDice(die, newPDF, true, detectionChance);
    }

    private LoadDice(int die, double[] newPDF, boolean singleRoll, double detectionChance) {
        this.newPDF = newPDF;
        this.die = die;
        this.singleRoll = singleRoll;
        this.detectionChance = detectionChance;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        BGGameState state = (BGGameState) gs;

        double[] originalPDF = state.getDicePdf(die);
        state.setDicePdf(die, newPDF);
        state.rollDice();
        if (singleRoll) {
            // reset pdf
            state.setDicePdf(die, originalPDF);
        }

        // then check for cheating detection
        if (rnd.nextDouble() < detectionChance) {
            // we do not use the state rnd, as this event is player-focused (and this way the game rnd just controls the die rolls)
            // cheating is detected and game ends
            state.logEvent(BGGameEvents.CheatingDetected,
                    () -> String.format("Cheating detected. Player %d rolled loaded dice. Player %d wins", state.getCurrentPlayer(), 1 - state.getCurrentPlayer()));
            state.moveAllPiecesToEnd(1 - state.getCurrentPlayer());
        }
        return true;
    }

    public double[] getPdf() {
        double[] copy = new double[newPDF.length];
        System.arraycopy(newPDF, 0, copy, 0, newPDF.length);
        return copy;
    }

    public double getDetectionChance() {
        return detectionChance;
    }
    public int getDieNumber() {
        return die;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LoadDice other = (LoadDice) obj;
        return die == other.die && Arrays.equals(newPDF, other.newPDF);
    }

    @Override
    public int hashCode() {
        return die * 31 + Arrays.hashCode(newPDF);
    }

    @Override
    public String toString() {
        return "Load Die " + die + ", pdf = " + Arrays.toString(newPDF);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
