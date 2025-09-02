package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.actions.monsterfeats.Subdue;
import games.descent2e.components.Figure;

import java.util.Objects;

public class RunicSorceryStatus extends Subdue {

    public RunicSorceryStatus(int figureID, DescentTypes.DescentCondition condition) {
        super(figureID, condition);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(figureID);
        return String.format("Runic Sorcery: Inflict %s with %s", f.getComponentName().replace("Hero: ", ""), condition.name());
    }

    public String toString() {
        return String.format("Runic Sorcery: Inflict %s with %s", figureID, condition.name());
    }

    @Override
    public RunicSorceryStatus copy() {
        return new RunicSorceryStatus(figureID, condition);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RunicSorceryStatus subdue) {
            return this.figureID == subdue.figureID && this.condition.equals(subdue.condition) && super.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }
}
