package games.coltexpress;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Loot;

public class ColtExpressHeuristic extends TunableParameters implements IStateHeuristic {

    double FACTOR_BULLETS_PLAYER = -0.2;
    double FACTOR_BULLETS_ENEMY = 0.1;
    double FACTOR_BULLET_CARDS = -0.3;
    double FACTOR_LOOT = 0.5;
    int maxLoot = 5000;

    // use bullets, minimize bulletsLeft
    // dodge bullets, maximize others' bulletsLeft
    // minimize bullet cards in hand/discard
    // maximize loot value

    public ColtExpressHeuristic() {
        addTunableParameter("maxLoot", 5000);
        addTunableParameter("BULLET_CARDS", -0.3);
        addTunableParameter("BULLETS_PLAYER", -0.2);
        addTunableParameter("BULLETS_ENEMY", 0.1);
        addTunableParameter("LOOT", 0.5);
    }

    @Override
    public void _reset() {
        maxLoot = (int) getParameterValue("maxLoot");
        FACTOR_BULLET_CARDS = (double) getParameterValue("BULLET_CARDS");
        FACTOR_BULLETS_PLAYER = (double) getParameterValue("BULLETS_PLAYER");
        FACTOR_BULLETS_ENEMY = (double) getParameterValue("BULLETS_ENEMY");
        FACTOR_LOOT = (double) getParameterValue("LOOT");
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        ColtExpressGameState cegs = (ColtExpressGameState) gs;
        ColtExpressParameters cep = (ColtExpressParameters) gs.getGameParameters();

        if (!cegs.isNotTerminal())
            return cegs.getPlayerResults()[playerId].value;

        // Number of bullets left for the player
        int nBulletsPlayer = cegs.bulletsLeft[playerId] / cep.nBulletsPerPlayer;

        // Number of bullets left for all other players
        int nBulletsOthers = 0;
        for (int i = 0; i < cegs.getNPlayers(); i++) {
            if (i != playerId) {
                nBulletsOthers += cegs.bulletsLeft[i] / cep.nBulletsPerPlayer;
            }
        }

        // Number of bullet cards in the player's deck or hand
        int nMaxBulletCards = cep.nBulletsPerPlayer * (cegs.getNPlayers() - 1);
        int nBulletCards = 0;
        for (ColtExpressCard c : cegs.playerHandCards.get(playerId).getComponents()) {
            if (c.cardType == ColtExpressCard.CardType.Bullet) {
                nBulletCards++;
            }
        }
        for (ColtExpressCard c : cegs.playerDecks.get(playerId).getComponents()) {
            if (c.cardType == ColtExpressCard.CardType.Bullet) {
                nBulletCards++;
            }
        }
        nBulletCards /= nMaxBulletCards;

        // Total value of loot collected by the player
        int lootValue = 0;
        for (Loot loot : cegs.playerLoot.get(playerId).getComponents()) {
            lootValue += loot.getValue();
        }
        lootValue /= maxLoot;

        // TODO: play shoot/punch cards when targets available, punch those with loot on them,
        //  punch those with bags if Cheyenne, always collect money, move marshall to compartments with other players

        return FACTOR_BULLET_CARDS * nBulletCards + FACTOR_BULLETS_ENEMY * nBulletsOthers + FACTOR_BULLETS_PLAYER * nBulletsPlayer +
                FACTOR_LOOT * lootValue;
    }

    @Override
    protected ColtExpressHeuristic _copy() {
        ColtExpressHeuristic retValue = new ColtExpressHeuristic();
        retValue.maxLoot = maxLoot;
        retValue.FACTOR_BULLETS_ENEMY = FACTOR_BULLETS_ENEMY;
        retValue.FACTOR_LOOT = FACTOR_LOOT;
        retValue.FACTOR_BULLETS_PLAYER = FACTOR_BULLETS_PLAYER;
        retValue.FACTOR_BULLET_CARDS = FACTOR_BULLET_CARDS;
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof ColtExpressHeuristic) {
            ColtExpressHeuristic other = (ColtExpressHeuristic) o;
            return other.FACTOR_BULLET_CARDS == FACTOR_BULLET_CARDS &&
                    other.FACTOR_BULLETS_ENEMY == FACTOR_BULLETS_ENEMY &&
                    other.FACTOR_BULLETS_PLAYER == FACTOR_BULLETS_PLAYER &&
                    other.FACTOR_LOOT == FACTOR_LOOT &&
                    other.maxLoot == maxLoot;
        }
        return false;
    }

    public ColtExpressHeuristic instantiate() {
        return this._copy();
    }

}