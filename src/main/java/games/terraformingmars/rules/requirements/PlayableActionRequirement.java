package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.actions.TMAction;

import java.awt.*;
import java.util.Objects;

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
    public PlayableActionRequirement copy() {
        return new PlayableActionRequirement((action != null? action.copy() : null));
    }

    @Override
    public Requirement<TMGameState> copySerializable() {
        return new PlayableActionRequirement((action != null? action.copySerializable() : null));
    }

    @Override
    public String toString() {
        return "Playable Action";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayableActionRequirement)) return false;
        PlayableActionRequirement that = (PlayableActionRequirement) o;
        return Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action);
    }
}
