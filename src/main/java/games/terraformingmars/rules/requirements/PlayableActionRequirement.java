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
        return null;
    }

    @Override
    public Image[] getDisplayImages() {
        return null;
    }
}
