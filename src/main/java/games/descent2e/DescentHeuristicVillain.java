package games.descent2e;

import core.AbstractGameState;
import core.CoreConstants;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;

public class DescentHeuristicVillain extends DescentHeuristic {

    // --- MAXIMISING FLOW ---

    // The difficulty should scale based on player skill level
    double FACTOR_SKILL = 0.0; // The Heroes' success rates should be considered - a weaker team should be more lenient, a stronger team more challenged

    // The game should resolve in a rewarding way for players
    // double FACTOR_REWARDING = 0.0; // Not relevant for Descent, the Heuristics already cover this to good extent

    // Players should feel as if they are in control of their actions, and their choices have impact
    double FACTOR_CONTROL = 0.0; // May be more important for movement than attacks? Most actions already have obvious impacts

    // Players should quickly and easily see their actions' results
    double FACTOR_FEEDBACK = 0.0; // Could be the variants in the rewards that the heroes are seeing?

    // --- MAXIMISING SELF-DETERMINATION ---

    // Players should feel that they can and are playing their best
    double FACTOR_COMPETENCE = 0.0; // Relevant more for human players than AI players, maybe if their scores are on a negative gradient?

    // Players should feel free to choose their own actions and not be forced into one
    double FACTOR_AUTONOMY = 0.0; // Size of the action space? If they have more actions available, they will have higher autonomy

    // --- MAXIMISING SUSPENSE ---

    // Players should have diverse ranges of situations without repeats
    double FACTOR_DIVERSITY = 0.0; // Compare similarities of game states, e.g. window of past 5-10 states and how different the new next state will be

    // Players should not be able to predict the game's progression
    double FACTOR_UNCERTAINTY_GAME = 0.0; // An approximate/surprise search, how different the end result is to the expected result

    // Players should not be able to predict the game's ending
    double FACTOR_UNCERTAINTY_SUCCESS = 0.0; // Win prediction research will fit well into this section

    public DescentHeuristicVillain() {
        super();
        addTunableParameter("FACTOR_SKILL", FACTOR_SKILL);
        //addTunableParameter("FACTOR_REWARDING", FACTOR_REWARDING);
        addTunableParameter("FACTOR_CONTROL", FACTOR_CONTROL);
        addTunableParameter("FACTOR_FEEDBACK", FACTOR_FEEDBACK);
        addTunableParameter("FACTOR_COMPETENCE", FACTOR_COMPETENCE);
        addTunableParameter("FACTOR_AUTONOMY", FACTOR_AUTONOMY);
        addTunableParameter("FACTOR_DIVERSITY", FACTOR_DIVERSITY);
        addTunableParameter("FACTOR_UNCERTAINTY_GAME", FACTOR_UNCERTAINTY_GAME);
        addTunableParameter("FACTOR_UNCERTAINTY_SUCCESS", FACTOR_UNCERTAINTY_SUCCESS);
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        DescentGameState dgs = (DescentGameState) gs;
        DescentParameters dp = (DescentParameters) gs.getGameParameters();
        CoreConstants.GameResult playerResult = gs.getPlayerResults()[playerId];

        if (!gs.isNotTerminal()) {
            return playerResult.value;
        }

        double retValue = 0.0;

        List<Double> heuristics = getHeuristics(dgs, playerId);
        List<Double> villainHeuristics = getVillainHeuristics(dgs, playerId);

        for(double h : heuristics)
        {
            retValue += h;
        }

        for(double h : villainHeuristics)
        {
            retValue += h;
        }

        // As playerResult.value is 1.0 for a win or -1.0 for a loss
        // we should clamp the return value to just below to ensure that a win or loss is more noticeable
        return Utils.clamp(retValue, -0.99, 0.99);
    }

    public List<Double> getVillainHeuristics(DescentGameState dgs, int playerId) {

        List<Double> heuristics = new ArrayList<>();

        heuristics.add(FACTOR_SKILL * getPlayerSkill() / dgs.heroes.size());
        //heuristics.add(FACTOR_REWARDING);
        heuristics.add(FACTOR_CONTROL * getControl() / dgs.heroes.size());
        heuristics.add(FACTOR_FEEDBACK * getFeedback() / dgs.heroes.size());
        heuristics.add(FACTOR_COMPETENCE * getCompetence() / dgs.heroes.size());
        heuristics.add(FACTOR_AUTONOMY * getAutonomy() / dgs.heroes.size());
        heuristics.add(FACTOR_DIVERSITY * getDiversity() / dgs.heroes.size());
        heuristics.add(FACTOR_UNCERTAINTY_GAME * getUncertaintyGame() / dgs.heroes.size());
        heuristics.add(FACTOR_UNCERTAINTY_SUCCESS * getUncertaintySuccess() / dgs.heroes.size());

        return heuristics;
    }

    public List<Double> getHeuristicParameters() {
        List<Double> ret = super.getHeuristicParameters();
        ret.add(FACTOR_SKILL);
        //ret.add(FACTOR_REWARDING);
        ret.add(FACTOR_CONTROL);
        ret.add(FACTOR_FEEDBACK);
        ret.add(FACTOR_COMPETENCE);
        ret.add(FACTOR_AUTONOMY);
        ret.add(FACTOR_DIVERSITY);
        ret.add(FACTOR_UNCERTAINTY_GAME);
        ret.add(FACTOR_UNCERTAINTY_SUCCESS);
        return ret;
    }

    public double getPlayerSkill() {
        // Need to assess the combined skill levels of the Heroes players
        double skill = 0.0;

        // Get the Hero's HP, how close/far away they are from the nearest monster/objective
        // And the Heroes' current Heuristic value

        return skill;
    }

    public double getRewarding() {
        // Not relevant for Descent, the Heuristics already cover this to good extent
        double rewarding = 0.0;

        // Might do later if needed

        return rewarding;
    }

    public double getControl() {
        // May be more important for movement than attacks? Most actions already have obvious impacts
        double control = 0.0;

        // Assess the Hero's current position and the possible actions they can take
        // If they are in a position to react better to the Overlord's minions, then they have more control

        return control;
    }

    public double getFeedback() {
        // Could be the variants in the rewards that the heroes are seeing?
        double feedback = 0.0;

        // Maybe take the average of the Heroes' Heuristic values over the past 5-10 states?

        return feedback;
    }

    public double getCompetence() {
        // Relevant more for human players than AI players, maybe if their scores are on a negative gradient?
        double competence = 0.0;

        // Go through each Hero player and assess their current Heuristic value
        // If it is on a negative gradient for the past 10 turns, then they are losing competence

        return competence;
    }

    public double getAutonomy() {
        // Size of the action space? If they have more actions available, they will have higher autonomy
        double autonomy = 0.0;

        // Compare the size of each Hero's action space at the start of their turns over the past 5-10 turns

        return autonomy;
    }

    public double getDiversity() {
        // Compare similarities of game states, e.g. window of past 5-10 states and how different the new next state will be
        double diversity = 0.0;

        // Compare the past 5-10 game states and see what actions are common between them and which are not

        return diversity;
    }

    public double getUncertaintyGame() {
        // An approximate/surprise search, how different the end result is to the expected result

        double uncertaintyGame = 0.0;



        return uncertaintyGame;
    }

    public double getUncertaintySuccess() {
        // Win prediction research will fit well into this section
        double uncertaintySuccess = 0.0;

        // Insert win prediction algorithm here

        return uncertaintySuccess;
    }

}
