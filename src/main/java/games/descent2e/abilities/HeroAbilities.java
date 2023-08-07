package games.descent2e.abilities;

import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.attack.Surge;
import games.descent2e.actions.attack.SurgeAttackAction;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import utilities.Vector2D;

public class HeroAbilities {

    // Self-Contained Class for all Hero Abilities

    // ----- HEALER -----

    // Ashrian's Hero Ability
    // If we are a Monster, and we start our turn adjacent to Ashrian, we are forced to take the Stunned condition
    public static void ashrian(DescentGameState dgs, Figure actingFigure) {
        Hero ashrian = dgs.getHeroByName("Ashrian");
        if (ashrian != null) {
            Vector2D position = actingFigure.getPosition();
            Vector2D other = ashrian.getPosition();
            if (Math.abs(position.getX() - other.getX()) <= 1 && Math.abs(position.getY() - other.getY()) <= 1) {
                actingFigure.addCondition(DescentTypes.DescentCondition.Stun);
            }
        }
    }

    // Avric Albright's Hero Ability
    // If we are a Hero (including Avric himself) within 3 spaces of Avric, we gain a Surge action of Recover 1 Heart
    public static void avric(DescentGameState dgs, Figure actingFigure) {
        Hero avric = dgs.getHeroByName("Avric Albright");
        if (avric != null) {
            Vector2D position = actingFigure.getPosition();
            Vector2D other = avric.getPosition();
            SurgeAttackAction surge = new SurgeAttackAction(Surge.RECOVER_1_HEART, actingFigure.getComponentID());
            if (actingFigure.equals(avric) ||
                    (Math.abs(position.getX() - other.getX()) <= 3 && Math.abs(position.getY() - other.getY()) <= 3)) {
                if (!actingFigure.getAbilities().contains(surge)) {
                    actingFigure.addAbility(surge);
                }
            }
            else
            {
                if (actingFigure.getAbilities().contains(surge)) {
                    actingFigure.removeAbility(surge);
                }
            }
        }
    }

    // ----- MAGE -----

    // Leoric of the Book's Hero Ability
    // If a Monster is within 3 spaces of Leoric, its attacks deal -1 Heart (to a minimum of 1)
    public static int leoric(DescentGameState dgs, Figure actingFigure, int damage) {
        Hero leoric = dgs.getHeroByName("Leoric");
        if (leoric != null) {
            Vector2D position = actingFigure.getPosition();
            Vector2D other = leoric.getPosition();
            if (Math.abs(position.getX() - other.getX()) <= 3 && Math.abs(position.getY() - other.getY()) <= 3) {
                // Leoric can only reduce damage to a minimum of 1
                if (damage > 1)
                    return damage - 1;
            }
        }
        return damage;
    }

    // Widow Tarha's Hero Ability

    // ----- SCOUT -----

    // Jain Fairwood's Hero Ability

    // Tomble Burrowell's Hero Ability

    // ----- WARRIOR -----

    // Grisban the Thirsty's Hero Ability

    // Syndrael's Hero Ability
}
