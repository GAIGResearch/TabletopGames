package games.descent2e.actions.tokens;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.cards.SearchCard;
import games.descent2e.components.tokens.DToken;
import utilities.Vector2D;

import static games.descent2e.DescentHelper.hasLineOfSight;
import static games.descent2e.DescentHelper.inRange;

public class GreedyAction extends SearchAction {
    public GreedyAction() {
        super();
    }

    @Override
    public boolean execute(DescentGameState gs) {
        Hero f = (Hero) gs.getActingFigure();
        f.getAttribute(Figure.Attribute.Fatigue).increment();
        return super.execute(gs);
    }

    @Override
    public GreedyAction _copy() {
        return new GreedyAction();
    }

    @Override
    public boolean canExecute(DescentGameState gs) {
        if (gs.getActingFigure().getNActionsExecuted().isMaximum()) return false;

        // Can only execute if player has the Greedy Skill (Thief Archetype)
        // has enough Fatigue to use Greedy (1 Fatigue)
        // and is up to 3 spaces away from the token
        Hero hero = (Hero) gs.getActingFigure();

        if (hero.getAttribute(Figure.Attribute.Fatigue).isMaximum()) return false;

        Deck<DescentCard> skills = hero.getSkills();
        if (skills == null || skills.getSize() == 0) return false;

        for (DescentCard skill : (hero.getSkills().getComponents())) {
            if (skill.getProperty("name").toString().equals("Greedy")) {
                Vector2D loc = hero.getPosition();
                for (DToken token : gs.getTokens()) {
                    if (token.getDescentTokenType() == DescentTypes.DescentToken.Search) {
                        Vector2D tokenLoc = token.getPosition();
                        if (tokenLoc == null) continue;
                        // hasLineOfSight is to stop the player from searching through walls
                        if (inRange(loc, tokenLoc, 3) && hasLineOfSight(gs, loc, tokenLoc)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof GreedyAction;
    }

    @Override
    public String toString() {
        return "Greedy: Search a search token within 3 spaces of you.";
    }
}
