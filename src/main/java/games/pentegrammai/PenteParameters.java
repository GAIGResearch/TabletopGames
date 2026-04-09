package games.pentegrammai;

import core.components.Dice;
import evaluation.optimisation.TunableParameters;

import java.util.Arrays;
import java.util.List;

public class PenteParameters extends TunableParameters<PenteParameters> {

    public int boardSize = 10;
    public int dieSides = 6;
    public int[] sacredPoints = {2, 7};
    public boolean startOffBoard = false;
    public boolean mustMoveFromSacredLine = true;
    public boolean onePieceLimitOffSacredLine = true;
    public boolean blotRuleActive = false;
    public boolean slideToMiddleOnSacredLine = false;
    // Kidds variant is then to have startOffBoard=true, blotRuleActive = true, onePieceLimitOffSacredLine = false, mustMoveFromSacredLine = true, bearOffFromSacredLine = false
    // Schaedler's variant is startOffBoard=false, blotRuleActive = false, onePieceLimitOffSacredLine = true, mustMoveFromSacredLine = true, bearOffFromSacredLine = false
    public String diceJSON = "";
    public Dice customDie = null;

    public PenteParameters() {
        addTunableParameter("boardSize", 10);
        addTunableParameter("dieSides", 6, List.of(4, 6, 10));
        addTunableParameter("diceJSON", "");
        addTunableParameter("maxRounds", 500);
        addTunableParameter("startOffBoard", false, List.of(false, true));
        addTunableParameter("mustMoveFromSacredLine", true, List.of(false, true));
        addTunableParameter("onePieceLimitOffSacredLine", true, List.of(false, true));
        addTunableParameter("blotRuleActive", false, List.of(false, true));
        addTunableParameter("slideToMiddleOnSacredLine", false, List.of(false, true));
        // TODO: Another one to add is whether two players can share the Holy Line (this is assumed to be the case if blotRuleActive=false currently; in line with Schaedler
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
        blotRuleActive = (boolean) getParameterValue("blotRuleActive");
        startOffBoard = (boolean) getParameterValue("startOffBoard");
        mustMoveFromSacredLine = (boolean) getParameterValue("mustMoveFromSacredLine");
        slideToMiddleOnSacredLine = (boolean) getParameterValue("slideToMiddleOnSacredLine");
        onePieceLimitOffSacredLine = (boolean) getParameterValue("onePieceLimitOffSacredLine");
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
