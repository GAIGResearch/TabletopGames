package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.actions.TMAction;

import java.awt.*;

public class PlayableActionRequirement implements Requirement<TMGameState> {

    TMAction action;

    public PlayableActionRequirement(TMAction action) {
        this.action = action;
    }

    @Override
    public boolean testCondition(TMGameState gs) {
        return action.canBePlayed(gs);
    }

    @Override
    public boolean isMax() {
        return false;
    }

    @Override
    public boolean appliesWhenAnyPlayer() {
        return false;
    }

    @Override
    public String getDisplayText(TMGameState gs) {
        return "Playable action";
    }

    @Override
    public String getReasonForFailure(TMGameState gs) {
        String reasons = "";
        for (Requirement<TMGameState> req: action.requirements) {
            if (req.testCondition(gs)) reasons += "OK: " + req.toString() + "\n";
            else reasons += "FAIL: " + req.toString() + " \\\\ " + req.getReasonForFailure(gs) + "\n";
        }
        return reasons;
    }

    @Override
    public Image[] getDisplayImages() {
        return null;
    }

    @Override
    public String toString() {
        return "Playable Action";
    }
}
