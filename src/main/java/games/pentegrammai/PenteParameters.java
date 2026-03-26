package games.pentegrammai;

import core.components.Dice;
import evaluation.optimisation.TunableParameters;

import java.util.Arrays;
import java.util.List;

public class PenteParameters extends TunableParameters {

    public int boardSize = 10;
    public int dieSides = 6;
    public int[] sacredPoints = {2, 7};
    public boolean startOffBoard;
    public boolean mustMoveFromSacredLine;
    public boolean onePieceLimitOffSacredLine;
    public boolean blotRuleActive;
    public boolean bearOffFromSacredLine;
    // Kidds variant is then to have startOffBoard=true, blotRuleActive = true, onePieceLimitOffSacredLine = false, mustMoveFromSacredLine = true, bearOffFromSacredLine = false
    // Schaedler's variant is startOffBoard=false, blotRuleActive = false, onePieceLimitOffSacredLine = true, mustMoveFromSacredLine = false, bearOffFromSacredLine = false
    public String diceJSON = "";
    public Dice customDie = null;

    public PenteParameters() {
        addTunableParameter("boardSize", 10);
        addTunableParameter("dieSides", 6, List.of(4, 6, 10));
        addTunableParameter("kiddsVariant", false, List.of(false, true));
        addTunableParameter("diceJSON", "");
        addTunableParameter("maxRounds", 500);
        maxRounds = 500;
    }

    @Override
    public PenteParameters instantiate() {
        return this;
    }

    @Override
    public void _reset() {
        boardSize = (int) getParameterValue("boardSize");
        dieSides = (int) getParameterValue("dieSides");
        sacredPoints = new int[]{boardSize / 4, 3 * boardSize / 4}; // default sacred points
        diceJSON = (String) getParameterValue("diceJSON");
        if (!diceJSON.isBlank()) {
            customDie = new Dice(diceJSON);
        }
        maxRounds = (int) getParameterValue("maxRounds");
    }

    @Override
    protected PenteParameters _copy() {
        PenteParameters copy = new PenteParameters();
        copy.sacredPoints = Arrays.copyOf(this.sacredPoints, this.sacredPoints.length);
        copy.customDie = this.customDie == null ? null : this.customDie.copy();
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof PenteParameters other)) return false;
        return Arrays.equals(sacredPoints, other.sacredPoints);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 31 * Arrays.hashCode(sacredPoints) +
                (customDie == null ? 0 : 31 * 31 * customDie.hashCode());
    }
}
