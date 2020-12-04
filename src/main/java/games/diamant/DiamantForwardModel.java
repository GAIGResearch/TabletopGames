package games.diamant;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.diamant.actions.ContinueInCave;
import games.diamant.actions.ExitFromCave;
import games.diamant.actions.OutOfCave;
import games.diamant.cards.DiamantCard;
import games.diamant.components.DiamantHand;
import games.diamant.components.DiamantTreasureChest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DiamantForwardModel extends AbstractForwardModel {

    private List<AbstractAction> actionsPlayed;

    @Override
    protected void _setup(AbstractGameState firstState) {
        DiamantGameState dgs = (DiamantGameState) firstState;
        Random r = new Random(dgs.getGameParameters().getRandomSeed());

        dgs.hands          = new ArrayList<>();
        dgs.treasureChests = new ArrayList<>();

        for (int i = 0; i < dgs.getNPlayers(); i++)
        {
            dgs.hands.add(new DiamantHand());
            dgs.treasureChests.add(new DiamantTreasureChest());
            dgs.playerOnCave.add(true);
        }

        dgs.mainDeck    = new Deck("MainDeck");
        dgs.discardDeck = new Deck("DiscardDeck");
        dgs.path        = new Deck("Path");

        createCards(dgs);
        dgs.mainDeck.shuffle(r);

        // Draw first card
        DiamantCard card = (DiamantCard) dgs.mainDeck.draw();
        dgs.path.add(card);

        PlayCard(card, dgs);

        dgs.getTurnOrder().setStartingPlayer(0);
        actionsPlayed = new ArrayList<>();
    }

    /**
     * Create all the cards and include them into the main deck.
     * @param dgs - current game state.
     */
    private void createCards(DiamantGameState dgs) {
        DiamantParameters dp = (DiamantParameters) dgs.getGameParameters();

        // 3 of each hazard
        // 15 treasures :1,2,3,4,5,7,9,10,11,12,13,14,15,16,17

        // Add artifacts
        //for (int i=0; i< dp.nArtifactCards; i++)
        //    dgs.mainDeck.add(new DiamantCard(DiamantCard.DiamantCardType.Artifact, DiamantCard.HazardType.None, 0));

        // Add hazards
        for (int i=0; i< dp.nHazardCardsPerType; i++)
        {
            for (DiamantCard.HazardType h : DiamantCard.HazardType.values())
                if (h != DiamantCard.HazardType.None)
                    dgs.mainDeck.add(new DiamantCard(DiamantCard.DiamantCardType.Hazard, h, 0));
        }

        // Add treasures
        for (int t : dp.treasures)
            dgs.mainDeck.add(new DiamantCard(DiamantCard.DiamantCardType.Treasure, DiamantCard.HazardType.None, t));
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        DiamantGameState dgs = (DiamantGameState) currentState;

        if (dgs.getCurrentPlayer() == dgs.getNPlayers() - 1)
        {
            int nPlayersExit = 0;
            for (AbstractAction a : actionsPlayed)
                if (a instanceof ExitFromCave)
                    nPlayersExit += 1;

            if (nPlayersExit == 0)
            {
                // All active players continue
                // Draw new card
                DiamantCard card = (DiamantCard) dgs.mainDeck.draw();
                dgs.path.add(card);
                PlayCard(card, dgs);
            }
            else if (nPlayersExit == dgs.getNPlayers())
            {
                // All active players left the cave
                DistributeGemsAmongPlayers(dgs);
                PrepareNewCave(dgs);
            }
            else
            {
                // Only some players left the cave
                DistributeGemsAmongPlayers(dgs);
            }
            actionsPlayed.clear();
        }
        else {
            actionsPlayed.add(action);
            dgs.getTurnOrder().endPlayerTurn(dgs);
        }
    }

    private void DistributeGemsAmongPlayers(DiamantGameState dgs)
    {
        int gems_to_players = (int) Math.floor(dgs.nGemsOnPath / dgs.GetNPlayersOnCave());
        dgs.nGemsOnPath = dgs.nGemsOnPath % dgs.GetNPlayersOnCave();


        for (int p = 0; p < dgs.getNPlayers(); p++)
        {
            if (actionsPlayed.get(p) instanceof ExitFromCave)
            {
                dgs.hands.get(p).AddGems(gems_to_players);
                dgs.treasureChests.get(p).AddGems(dgs.hands.get(p).GetNumberGems());
                dgs.hands.get(p).SetNumberGems(0);

                dgs.playerOnCave.get(p) = Boolean.FALSE;
            }
        }
    }

    private void PrepareNewCave(DiamantGameState dgs)
    {
        DiamantParameters dp = (DiamantParameters) dgs.getGameParameters();

        dgs.nCave ++;

        // No more caves ?
        if (dgs.nCave == dp.nCaves)
            EndGame();

        else {
            Random r = new Random(dgs.getGameParameters().getRandomSeed());

            // Move path cards to maindeck and shuffle
            dgs.mainDeck.add(dgs.path);
            dgs.path.clear();
            dgs.mainDeck.shuffle(r);

            // All the player will participate in next cave
            for (Boolean b : dgs.playerOnCave)
                b = Boolean.TRUE;

        }
    }


    // There are always two actions: Continue or Exit
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState)
    {
        DiamantGameState dgs = (DiamantGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();

        // If the player is still in the cave
        if (dgs.playerOnCave.get(gameState.getCurrentPlayer()))
        {
            actions.add(new ContinueInCave());
            actions.add(new ExitFromCave());
        }
        else
            actions.add(new OutOfCave());

        return actions;
    }

    @Override
    protected AbstractForwardModel _copy()
    {
        DiamantForwardModel dfm = new DiamantForwardModel();
        dfm.actionsPlayed = new ArrayList<>();

        for (AbstractAction a : actionsPlayed)
            dfm.actionsPlayed.add(a.copy());

        return dfm;
    }

    private void PlayCard(DiamantCard card, DiamantGameState dgs)
    {
        if (card.getCardType() == DiamantCard.DiamantCardType.Treasure) {
            int gems_to_players = (int) Math.floor(card.getNumberOfGems() / dgs.getNPlayers());
            int gems_to_path    = card.getNumberOfGems() % dgs.getNPlayers();

            for (int p=0; p<dgs.getNPlayers(); p++)
                dgs.hands.get(p).AddGems(gems_to_players);

            dgs.nGemsOnPath = gems_to_path;
        }
        else if (card.getCardType() == DiamantCard.DiamantCardType.Hazard)
        {
            if (card.getHazardType() == DiamantCard.HazardType.Explosions)
                dgs.nHazardExplosionsOnPath += 1;
            else if (card.getHazardType() == DiamantCard.HazardType.PoissonGas)
                dgs.nHazardPoissonGasOnPath += 1;
            else if (card.getHazardType() == DiamantCard.HazardType.Rockfalls)
                dgs.nHazardRockfallsOnPath += 1;
            else if (card.getHazardType() == DiamantCard.HazardType.Scorpions)
                dgs.nHazardScorpionsOnPath += 1;
            else if (card.getHazardType() == DiamantCard.HazardType.Snakes)
                dgs.nHazardSnakesOnPath += 1;
        }
    }
}
