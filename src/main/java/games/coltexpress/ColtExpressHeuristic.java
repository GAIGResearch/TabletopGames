package games.coltexpress;
import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Loot;
import utilities.Utils;

public class ColtExpressHeuristic implements IStateHeuristic {

    double FACTOR_BULLETS_PLAYER = -0.2;
    double FACTOR_BULLETS_ENEMY = 0.1;
    double FACTOR_BULLET_CARDS = -0.3;
    double FACTOR_LOOT = 0.5;
    int maxLoot = 5000;

    // use bullets, minimize bulletsLeft
    // dodge bullets, maximize others' bulletsLeft
    // minimize bullet cards in hand/discard
    // maximize loot value

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        ColtExpressGameState cegs = (ColtExpressGameState) gs;
        ColtExpressParameters cep = (ColtExpressParameters) gs.getGameParameters();
        Utils.GameResult playerResult = gs.getPlayerResults()[playerId];

        if (playerResult == Utils.GameResult.LOSE)
            return -1;
        if (playerResult == Utils.GameResult.WIN)
            return 1;

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
        for (ColtExpressCard c: cegs.playerHandCards.get(playerId).getComponents()) {
            if (c.cardType == ColtExpressCard.CardType.Bullet) {
                nBulletCards++;
            }
        }
        for (ColtExpressCard c: cegs.playerDecks.get(playerId).getComponents()) {
            if (c.cardType == ColtExpressCard.CardType.Bullet) {
                nBulletCards++;
            }
        }
        nBulletCards /= nMaxBulletCards;

        // Total value of loot collected by the player
        int lootValue = 0;
        for (Loot loot: cegs.playerLoot.get(playerId).getComponents()) {
            lootValue += loot.getValue();
        }
        lootValue /= maxLoot;

        // TODO: play shoot/punch cards when targets available, punch those with loot on them,
        //  punch those with bags if Cheyenne, always collect money, move marshall to compartments with other players

        return FACTOR_BULLET_CARDS * nBulletCards + FACTOR_BULLETS_ENEMY * nBulletsOthers + FACTOR_BULLETS_PLAYER * nBulletsPlayer +
                FACTOR_LOOT * lootValue;
    }
}