package games.descent2e.actions.attack;

import core.AbstractGameState;
import core.components.Card;
import games.descent2e.DescentGameState;
import games.descent2e.abilities.Cowardly;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.Objects;

public class SurgeAttackAction extends DescentAction {

    public final Surge surge;
    public final int figureSource;
    Card card = null;

    public SurgeAttackAction(Surge surge, int figure) {
        super(Triggers.SURGE_DECISION);
        this.surge = surge;
        this.figureSource = figure;
    }

    public SurgeAttackAction(Surge surge, int figure, Card card) {
        super(Triggers.SURGE_DECISION);
        this.surge = surge;
        this.figureSource = figure;
        this.card = card;
    }

    @Override
    public String toString() {
        return surge.name() + " : " + figureSource;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        String figureName = gameState.getComponentById(figureSource).getComponentName();
        figureName = figureName.replace("Hero: ", "");

        String surgeName = surge.name().replace("_", " ").replace("PLUS ", "+");
        return String.format("Surge: "+ surgeName + " by " + figureName);
        //return toString();
    }

    @Override
    public boolean execute(DescentGameState gs) {
        MeleeAttack attack = (MeleeAttack) gs.currentActionInProgress();
        attack.registerSurge(surge);
        surge.apply(attack, gs);
        System.out.println("SurgeAttackAction: " + surge.name() + " by " + gs.getComponentById(figureSource).getComponentName());
        if (card != null) {
            if (gs.getActingPlayer() == gs.getOverlordPlayer()) {
                gs.playOverlordCard(card, attack.hashCode());
            } else {
                gs.getActingFigure().exhaustCard(card);
            }
        }
        return true;
    }

    @Override
    public DescentAction copy() {
        return this; // immutable
    }

    @Override
    public boolean canExecute(DescentGameState dgs)
    {
        Figure f = dgs.getActingFigure();
        if (f.getComponentID() != figureSource)
        {
            return false;
        }

        if (f instanceof Monster)
        {
            // If the figure has the Cowardly passive, they can only surge if they are near a master or lieutenant monster
            if (((Monster) f).hasPassive(MonsterAbilities.MonsterPassive.COWARDLY))
                return Cowardly.isNearMasterOrLieutenant(dgs, f);
        }

        if (card != null)
        {
            if (dgs.getActingPlayer() == dgs.getOverlordPlayer())
            {
                if (!dgs.getOverlordHand().contains(card))
                {
                    return false;
                }
            }
            else
            {
                if (!f.isExhausted(card))
                {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SurgeAttackAction) {
            SurgeAttackAction o = (SurgeAttackAction) other;
            return o.figureSource == figureSource && o.surge == surge;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(figureSource, surge.ordinal()) - 492209;
    }
}
