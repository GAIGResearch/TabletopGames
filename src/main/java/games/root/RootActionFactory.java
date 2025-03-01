package games.root;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.root.actions.*;
import games.root.actions.choosers.ChooseRuler;
import games.root.actions.extended.*;
import games.root.components.cards.EyrieRulers;
import games.root.components.cards.RootCard;
import games.root.components.cards.VagabondCharacter;
import games.root.components.Item;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;

import static games.root.RootParameters.Relationship.Hostile;

public class RootActionFactory {
    static List<AbstractAction> getSetupActions(AbstractGameState gs) {
        int currentPlayer = gs.getCurrentPlayer();
        RootGameState gameState = (RootGameState) gs;
        RootParameters.Factions faction = gameState.getPlayerFaction(currentPlayer);
        if (faction.equals(RootParameters.Factions.MarquiseDeCat)) {
            return getCatSetupActions(gs);
        } else if (faction.equals(RootParameters.Factions.EyrieDynasties)) {
            return getBirdSetupActions(gs);
        } else if (faction.equals(RootParameters.Factions.WoodlandAlliance)) {
            return getWoodlandAllianceSetupActions(gs);
        } else if (faction.equals(RootParameters.Factions.Vagabond)) {
            return getVagabondSetupActions(gs);
        }

        return null;
    }

    static List<AbstractAction> getBirdsongActions(AbstractGameState gs) {
        int currentPlayer = gs.getCurrentPlayer();
        RootGameState gameState = (RootGameState) gs;
        RootParameters.Factions faction = gameState.getPlayerFaction(currentPlayer);

        if (faction.equals(RootParameters.Factions.MarquiseDeCat)) {
            return getCatBirdsongActions(gs);
        } else if (faction.equals(RootParameters.Factions.EyrieDynasties)) {
            return getBirdBirdsongActions(gs);
        } else if (faction.equals(RootParameters.Factions.WoodlandAlliance)) {
            return getWoodLandAllianceBirdsongActions(gs);
        } else if (faction.equals(RootParameters.Factions.Vagabond)) {
            return getVagabondBirdsongActions(gs);
        }
        return null;
    }

    static List<AbstractAction> getDaylightActions(AbstractGameState gs) {
        int currentPlayer = gs.getCurrentPlayer();
        RootGameState gameState = (RootGameState) gs;
        RootParameters.Factions faction = gameState.getPlayerFaction(currentPlayer);

        if (faction.equals(RootParameters.Factions.MarquiseDeCat)) {
            return getCatDaylightActions(gs);
        } else if (faction.equals(RootParameters.Factions.EyrieDynasties)) {
            return getBirdDaylightActions(gs);
        } else if (faction.equals(RootParameters.Factions.WoodlandAlliance)) {
            return getWoodLandAllianceDaylightActions(gs);
        } else if (faction.equals(RootParameters.Factions.Vagabond)) {
            return getVagabondDaylightActions(gs);
        }
        return null;
    }

    static List<AbstractAction> getEveningActions(AbstractGameState gs) {

        int currentPlayer = gs.getCurrentPlayer();
        RootGameState gameState = (RootGameState) gs;
        RootParameters.Factions faction = gameState.getPlayerFaction(currentPlayer);

        if (faction.equals(RootParameters.Factions.MarquiseDeCat)) {
            return getCatEveningActions(gs);
        } else if (faction.equals(RootParameters.Factions.EyrieDynasties)) {
            return getBirdEveningActions(gs);
        } else if (faction.equals(RootParameters.Factions.WoodlandAlliance)) {
            return getWoodLandAllianceEveningActions(gs);
        } else if (faction.equals(RootParameters.Factions.Vagabond)) {
            return getVagabondEveningActions(gs);
        }
        return null;
    }

    //region Cats actions
    static List<AbstractAction> getCatSetupActions(AbstractGameState gs) {
        //1st Choose Keep corner + populate clearings with warriors
        //2nd Place Workshop, Sawmill, and Recruiter
        RootGameState currentState = (RootGameState) gs;
        //System.out.println("Setting up Cats " + currentState.playerSubGamePhase);
        List<AbstractAction> actions = new ArrayList<>();
        if (currentState.playerSubGamePhase == 0) {
            //Place keep
            for (RootBoardNodeWithRootEdges node : currentState.getGameMap().getBoardNodes()) {
                if (!node.getKeep() && node.getCorner()) {
                    PlaceKeep action = new PlaceKeep(currentState.getCurrentPlayer(), node.getComponentID());
                    actions.add(action);
                }
            }
        } else if (currentState.playerSubGamePhase == 1) {
            //Place Workshop
            RootBoardNodeWithRootEdges keep = currentState.gameMap.getKeepNode();
            if(keep.hasBuildingRoom()) {
                Build action = new Build(keep.getComponentID(), currentState.getCurrentPlayer(), RootParameters.BuildingType.Workshop, true);
                actions.add(action);
            }
            for (RootBoardNodeWithRootEdges node : keep.getNeighbours()) {
                if (!node.getClearingType().equals(RootParameters.ClearingTypes.Forrest) && node.hasBuildingRoom()) {
                    Build actionWorkshop = new Build(node.getComponentID(), currentState.getCurrentPlayer(), RootParameters.BuildingType.Workshop, true);
                    actions.add(actionWorkshop);
                }
            }
        } else if (currentState.playerSubGamePhase == 2) {
            //Sawmill
            RootBoardNodeWithRootEdges keep = currentState.gameMap.getKeepNode();
            if (keep.hasBuildingRoom()) {
                Build action = new Build(keep.getComponentID(), currentState.getCurrentPlayer(), RootParameters.BuildingType.Sawmill, true);
                actions.add(action);
            }
            for (RootBoardNodeWithRootEdges node : keep.getNeighbours()) {
                if (!node.getClearingType().equals(RootParameters.ClearingTypes.Forrest) && node.hasBuildingRoom()) {
                    Build actionWorkshop = new Build(node.getComponentID(), currentState.getCurrentPlayer(), RootParameters.BuildingType.Sawmill, true);
                    actions.add(actionWorkshop);
                }
            }
        } else if (currentState.playerSubGamePhase == 3) {
            //Place Recruiter
            RootBoardNodeWithRootEdges keep = currentState.gameMap.getKeepNode();
            if(keep.hasBuildingRoom()) {
                Build action = new Build(keep.getComponentID(), currentState.getCurrentPlayer(), RootParameters.BuildingType.Recruiter, true);
                actions.add(action);
            }
            for (RootBoardNodeWithRootEdges node : keep.getNeighbours()) {
                if (!node.getClearingType().equals(RootParameters.ClearingTypes.Forrest)&& node.hasBuildingRoom()) {
                    Build actionWorkshop = new Build(node.getComponentID(), currentState.getCurrentPlayer(), RootParameters.BuildingType.Recruiter, true);
                    actions.add(actionWorkshop);
                }
            }
        } else if (currentState.playerSubGamePhase == 4) {
            EndTurn action = new EndTurn(currentState.getCurrentPlayer(), true);
            actions.add(action);

        }
        return actions;
    }

    static List<AbstractAction> getCatBirdsongActions(AbstractGameState gs) {
        //If less wood than sawmills: choose which sawmills to place
        RootGameState currentState = (RootGameState) gs;
        //System.out.println("Birdsong Cats " + currentState.playerSubGamePhase);
        List<AbstractAction> actions = new ArrayList<>();
        Deck<RootCard> crafted = currentState.getPlayerCraftedCards(currentState.getCurrentPlayer());
        for (int i = 0; i < crafted.getSize(); i++){
            if (crafted.get(i).cardType == RootCard.CardType.RoyalClaim){
                actions.add(new DiscardCraftedCard(currentState.getCurrentPlayer(), i, crafted.get(i).getComponentID()));
            }
        }
        if (currentState.actionsPlayed == 0 && currentState.getWood() > 0 && !currentState.getGameMap().getSawmills().isEmpty()){
            actions.add(new PlaceWoodSequence(currentState.getCurrentPlayer()));
        }else {
            PassGamePhase action = new PassGamePhase(gs.getCurrentPlayer());
            actions.add(action);
        }
        return actions;
    }

    static List<AbstractAction> getCatDaylightActions(AbstractGameState gs) {
        //1st Craft
        //2nd Any 3: March, Recruit, Battle, Build, Overwork + 1 per used blue card
        RootGameState currentState = (RootGameState) gs;
        RootParameters rp = (RootParameters) gs.getGameParameters();
        //System.out.println("Daylight Cats " + currentState.playerSubGamePhase);
        List<AbstractAction> actions = new ArrayList<>();
        PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(currentState.getCurrentPlayer());
        for (int i = 0; i < hand.getSize(); i++){
            if (hand.get(i).cardType == RootCard.CardType.Dominance && currentState.getGameScore(currentState.getCurrentPlayer())>=10){
                actions.add(new PlayDomination(currentState.getCurrentPlayer(), i, hand.get(i).getComponentID()));
            }
        }
        if (currentState.playerSubGamePhase == 0) {
            List<RootParameters.ClearingTypes> available = new ArrayList<>();
            for (RootBoardNodeWithRootEdges node: currentState.getGameMap().getNonForrestBoardNodes()){
                if (node.getWorkshops() > 0){
                    for (int i = 0; i < node.getWorkshops(); i++){
                        available.add(node.getClearingType());
                    }
                }
            }
            if (currentState.canCraft(currentState.getCurrentPlayer(), available)){
                actions.add(new CraftSequence(currentState.getCurrentPlayer(), available));
                actions.add(new PassSubGamePhase(currentState.getCurrentPlayer()));
            } else {
                actions.add(new PassSubGamePhase(currentState.getCurrentPlayer()));
            }
        } else if (currentState.playerSubGamePhase == 1) {
            if (currentState.actionsPlayed < 3) {
                if (currentState.canBuildCatBuilding(currentState.getCurrentPlayer())){
                    CatBuildSequence action = new CatBuildSequence(currentState.getCurrentPlayer());
                    actions.add(action);
                }
                //Recruit
                if (currentState.getCatWarriors() > 0 && !currentState.getGameMap().getRecruiters().isEmpty()){
                    actions.add(new CatRecruitSequence(currentState.getCurrentPlayer()));
                }
//                CatRecruit action = new CatRecruit(currentState.getCurrentPlayer());
//                actions.add(action);
                //Battle
                if (currentState.canAttack(currentState.getCurrentPlayer())){
                    actions.add(new BattleAction(currentState.getCurrentPlayer()));
                }
                //March
                if(currentState.getCatWarriors() < rp.maxWarriors.get(currentState.getPlayerFaction(currentState.getCurrentPlayer()))){
                    March march = new March(currentState.getCurrentPlayer());
                    actions.add(march);
                }
                if (currentState.canOverwork(currentState.getCurrentPlayer())){
                    actions.add(new OverworkSequence(currentState.getCurrentPlayer()));
                }
                PassGamePhase pass= new PassGamePhase(currentState.getCurrentPlayer());
                actions.add(pass);
                return actions;

            } else {
                //Spend blue card or enter evening
                for (int i = 0; i < hand.getSize(); i++) {
                    if(hand.get(i).suit == RootParameters.ClearingTypes.Bird){
                        DiscardBirdCard action = new DiscardBirdCard(currentState.getCurrentPlayer(), i, hand.get(i).getComponentID());
                        actions.add(action);
                    }
                }
                PassGamePhase action = new PassGamePhase(currentState.getCurrentPlayer());
                actions.add(action);
                return actions;
            }
        }
        return actions;
    }

    static List<AbstractAction> getCatEveningActions(AbstractGameState gs) {
        //Draw + Discard
        RootGameState currentState = (RootGameState) gs;
        //System.out.println("Evening Cats " + currentState.playerSubGamePhase);
        RootParameters rp = (RootParameters) currentState.getGameParameters();
        List<AbstractAction> actions = new ArrayList<>();
        if (currentState.playerSubGamePhase == 0) {
            Draw drawAction = new Draw(gs.getCurrentPlayer(), rp.cardsDrawnPerTurn+rp.catDrawingBonus.get(currentState.getBuildingCount(RootParameters.BuildingType.Recruiter)), true);
            actions.add(drawAction);
            return actions;
        } else if (currentState.playerSubGamePhase == 1) {
            Deck<RootCard> playerHand = currentState.getPlayerHand(gs.getCurrentPlayer());
            if (playerHand.getSize() > rp.maxCardsEndTurn) {
                for (int i = 0; i < playerHand.getSize(); i++) {
                    Discard discardAction = new Discard(currentState.getCurrentPlayer(), i, playerHand.get(i).getComponentID(), false);
                    actions.add(discardAction);
                }
                return actions;
            } else {
                EndTurn action = new EndTurn(currentState.getCurrentPlayer(), false);
                actions.add(action);
                return actions;
            }
        }
        return null;
    }
    //endregion

    //region Birds actions
    static List<AbstractAction> getBirdSetupActions(AbstractGameState gs) {
        //1st Choose Leader
        //2nd Place Viziers -> can be merged
        RootGameState currentState = (RootGameState) gs;
        //System.out.println("Setting up Eyrie " + currentState.playerSubGamePhase);
        List<AbstractAction> actions = new ArrayList<>();
        if (currentState.playerSubGamePhase == 0) {
            //Choose Viziers
            Deck<EyrieRulers> rulers = currentState.getAvailableRulers();
            for (int i = 0; i < rulers.getSize(); i++) {
                ChooseRuler action = new ChooseRuler(currentState.getCurrentPlayer(), i, rulers.get(i).getComponentID(), true);
                actions.add(action);
            }
        } else if (currentState.playerSubGamePhase == 1) {
            EndTurn action = new EndTurn(currentState.getCurrentPlayer(), true);
            actions.add(action);
        }
        return actions;
    }

    static List<AbstractAction> getBirdBirdsongActions(AbstractGameState gs) {
        //If no cards draw, add to decree, If no roosts add to the one with the fewest total warriors
        RootGameState currentState = (RootGameState) gs;
        //System.out.println("Birdsong Eyrie " + currentState.playerSubGamePhase);
        List<AbstractAction> actions = new ArrayList<>();
        Deck<RootCard> crafted = currentState.getPlayerCraftedCards(currentState.getCurrentPlayer());
        for (int i = 0; i < crafted.getSize(); i++){
            if (crafted.get(i).cardType == RootCard.CardType.RoyalClaim){
                actions.add(new DiscardCraftedCard(currentState.getCurrentPlayer(), i, crafted.get(i).getComponentID()));
            }
        }
        if (currentState.playerSubGamePhase == 0) {
            if (currentState.getPlayerHand(currentState.getCurrentPlayer()).getSize() == 0) {
                Draw action = new Draw(gs.getCurrentPlayer(), 1, true);
                actions.add(action);
                return actions;
            } else {
                PassSubGamePhase action = new PassSubGamePhase(currentState.getCurrentPlayer());
                actions.add(action);
                return actions;
            }
        } else if (currentState.playerSubGamePhase == 1) {
            actions.add(new AddToDecreeSequence(currentState.getCurrentPlayer()));
            return actions;

        } else if (currentState.playerSubGamePhase == 2) {
            if (currentState.getGameMap().getNumberRoosts() == 0) {
                if (currentState.getGameMap().getFewestWarriorsNode() != null) {
                    EyrieNoRoosts action = new EyrieNoRoosts(currentState.getCurrentPlayer(), currentState.getGameMap().getFewestWarriorsNode().getComponentID(), false);
                    actions.add(action);
                }else{
                    actions.add(new PassGamePhase(currentState.getCurrentPlayer()));
                }
                return actions;
            } else {
                PassGamePhase action = new PassGamePhase(currentState.getCurrentPlayer());
                actions.add(action);
                return actions;
            }
        }
        return null;
    }

    static List<AbstractAction> getBirdDaylightActions(AbstractGameState gs) {
        //1st Craft
        //2nd Recruit
        //3rd Move
        //4th Battle
        //5th Build
        RootGameState currentState = (RootGameState) gs;
        //System.out.println("Daylight Eyrie " + currentState.playerSubGamePhase);
        List<AbstractAction> actions = new ArrayList<>();
        PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(currentState.getCurrentPlayer());
        for (int i = 0; i < hand.getSize(); i++){
            if (hand.get(i).cardType == RootCard.CardType.Dominance && currentState.getGameScore(currentState.getCurrentPlayer())>=10){
                actions.add(new PlayDomination(currentState.getCurrentPlayer(), i, hand.get(i).getComponentID()));
            }
        }
        if (currentState.playerSubGamePhase == 0) {
            List<RootParameters.ClearingTypes> available = new ArrayList<>();
            for (RootBoardNodeWithRootEdges location : currentState.getGameMap().getNonForrestBoardNodes()){
                if (location.hasBuilding(RootParameters.BuildingType.Roost)){
                    available.add(location.getClearingType());
                }
            }
            if (currentState.canCraft(currentState.getCurrentPlayer(),available)){
                actions.add(new CraftSequence(currentState.getCurrentPlayer(),available));
                actions.add(new PassSubGamePhase(currentState.getCurrentPlayer(), "doesnt wish to craft"));
            }else {
                actions.add(new PassSubGamePhase(currentState.getCurrentPlayer(), "passes crafting"));

            }
            return actions;
        } else if (currentState.playerSubGamePhase == 1) {
            List<RootParameters.ClearingTypes> available = currentState.getDecreeSuits(0);
            for (RootParameters.ClearingTypes clearingType : available) {
                for (RootBoardNodeWithRootEdges node : currentState.getGameMap().getNodesOfType(clearingType)) {
                    if (currentState.getBirdWarriors() > 0 && node.getRoost() == 1 && currentState.getRuler().ruler != EyrieRulers.CardType.Charismatic) {
                        Recruit action = new Recruit(node.getComponentID(), currentState.getCurrentPlayer(), false,clearingType == RootParameters.ClearingTypes.Bird);
                        if (!actions.contains(action)) {
                            actions.add(action);
                        }
                    } else if(currentState.getBirdWarriors() > 1 && node.getRoost() == 1){
                        Recruit action = new Recruit(node.getComponentID(), currentState.getCurrentPlayer(),  2,false,clearingType == RootParameters.ClearingTypes.Bird);
                        if (!actions.contains(action)) {
                            actions.add(action);
                        }
                    }
                }
            }
            if (actions.isEmpty()) {
                if (currentState.actionsPlayed < currentState.getDecree().get(0).getSize()) {
                    for(int i = 0; i < currentState.getAvailableRulers().getSize(); i++){
                        Turmoil turmoil = new Turmoil(currentState.getCurrentPlayer(), currentState.getAvailableRulers().get(i).ruler, true);
                        actions.add(turmoil);
                    }
                } else {
                    PassSubGamePhase action = new PassSubGamePhase(currentState.getCurrentPlayer() , "finished recruiting");
                    actions.add(action);
                }
            }
            return actions;
        } else if (currentState.playerSubGamePhase == 2) {
            List<RootParameters.ClearingTypes> available = currentState.getDecreeSuits(1);
            for (RootParameters.ClearingTypes type : available) {
                for (RootBoardNodeWithRootEdges node : currentState.getGameMap().getNodesOfType(type)) {
                    if (node.getWarrior(currentState.getPlayerFaction(currentState.getCurrentPlayer())) > 0 && node.canMove(currentState.getCurrentPlayer())) {
                        MoveSequence action = new MoveSequence(currentState.getCurrentPlayer(),node.getComponentID(), type== RootParameters.ClearingTypes.Bird);
                        if (!actions.contains(action)) {
                            actions.add(action);
                        }
                    }
                }
            }
            if (actions.isEmpty()) {
                if (currentState.actionsPlayed < currentState.getDecree().get(1).getSize()) {
                    for(int i = 0; i < currentState.getAvailableRulers().getSize(); i++){
                        Turmoil turmoil = new Turmoil(currentState.getCurrentPlayer(), currentState.getAvailableRulers().get(i).ruler, true);
                        actions.add(turmoil);
                    }
                } else {
                    PassSubGamePhase action = new PassSubGamePhase(currentState.getCurrentPlayer(), "finished moving");
                    actions.add(action);
                }
            }
            return actions;
        } else if (currentState.playerSubGamePhase == 3) {

            if (currentState.canAttack(currentState.getCurrentPlayer())){
                actions.add(new BattleAction(currentState.getCurrentPlayer()));
            }
            if(actions.isEmpty()) {
                if (currentState.actionsPlayed < currentState.getDecree().get(2).getSize()) {
                    for (int i = 0; i < currentState.getAvailableRulers().getSize(); i++) {
                        Turmoil turmoil = new Turmoil(currentState.getCurrentPlayer(), currentState.getAvailableRulers().get(i).ruler, true);
                        actions.add(turmoil);
                    }
                } else {
                    PassSubGamePhase action = new PassSubGamePhase(currentState.getCurrentPlayer(), "finished battling");
                    actions.add(action);
                }
            }
            return actions;

        } else if (currentState.playerSubGamePhase == 4) {
            List<RootParameters.ClearingTypes> available = currentState.getDecreeSuits(3);
            //System.out.println(available.size());
            for (RootParameters.ClearingTypes clearingType : available) {
                for (RootBoardNodeWithRootEdges node : currentState.getGameMap().getNodesOfType(clearingType)) {
                    if (!node.getKeep() && node.rulerID == currentState.getCurrentPlayer() && node.getRoost() == 0 && node.hasBuildingRoom() && currentState.getBuildingCount(RootParameters.BuildingType.Roost) > 0) {
                        Build action = new Build(node.getComponentID(), currentState.getCurrentPlayer(), RootParameters.BuildingType.Roost, false, clearingType == RootParameters.ClearingTypes.Bird);
                        if (!actions.contains(action)){ actions.add(action);}

                    }
                }
            }
            if (actions.isEmpty()) {
                if (currentState.actionsPlayed < currentState.getDecree().get(3).getSize()) {
                    for (int i = 0; i < currentState.getAvailableRulers().getSize(); i++) {
                        Turmoil turmoil = new Turmoil(currentState.getCurrentPlayer(), currentState.getAvailableRulers().get(i).ruler, true);
                        actions.add(turmoil);
                    }
                } else {
                    PassGamePhase action = new PassGamePhase(currentState.getCurrentPlayer());
                    actions.add(action);
                }
            }
            return actions;
        }
        return null;
    }

    static List<AbstractAction> getBirdEveningActions(AbstractGameState gs) {
        //Score VP + Draw/Discard
        RootGameState currentState = (RootGameState) gs;
        //System.out.println("Evening Eyrie " + currentState.playerSubGamePhase);
        RootParameters rp = (RootParameters) currentState.getGameParameters();
        List<AbstractAction> actions = new ArrayList<>();
        if (currentState.playerSubGamePhase == 0) {
            actions.add(new EyrieScoreRoostVP(currentState.getCurrentPlayer(), rp.roostPoints.get(currentState.getBuildingCount(RootParameters.BuildingType.Roost))));
            return actions;
        } else if (currentState.playerSubGamePhase == 1) {
            Draw action = new Draw(gs.getCurrentPlayer(), rp.cardsDrawnPerTurn+rp.eyrieDrawingBonus.get(currentState.getBuildingCount(RootParameters.BuildingType.Roost)), true);
            actions.add(action);
            return actions;
        } else if (currentState.playerSubGamePhase == 2) {
            Deck<RootCard> playerHand = currentState.getPlayerHand(gs.getCurrentPlayer());
            if (playerHand.getSize() > rp.maxCardsEndTurn) {
                for (int i = 0; i < playerHand.getSize(); i++) {
                    Discard discardAction = new Discard(currentState.getCurrentPlayer(), i, playerHand.get(i).getComponentID(), false);
                    actions.add(discardAction);
                }
                return actions;
            } else {
                EndTurn action = new EndTurn(currentState.getCurrentPlayer(), false);
                actions.add(action);
                return actions;
            }
        }
        return null;
    }
    //endregion

    //region Woodland Alliance Action Factory
    static List<AbstractAction> getWoodlandAllianceSetupActions(AbstractGameState gs) {
        //Draw Supporters
        RootGameState currentState = (RootGameState) gs;
        //System.out.println("Setting up Woodland " + currentState.playerSubGamePhase);
        List<AbstractAction> actions = new ArrayList<>();
        if (currentState.playerSubGamePhase == 0) {
            WoodlandSetup action = new WoodlandSetup(currentState.getCurrentPlayer(), true);
            actions.add(action);
        } else if (currentState.playerSubGamePhase == 1) {
            actions.add(new EndTurn(currentState.getCurrentPlayer(), true));
        }
        return actions;
    }

    static List<AbstractAction> getWoodLandAllianceBirdsongActions(AbstractGameState gs) {
        //Discard Supporters
        //1st Revolt
        //2nd Spread Sympathy
        RootGameState currentState = (RootGameState) gs;
        RootParameters rp = (RootParameters) gs.getGameParameters();
        //System.out.println("Birdsong Woodland " + currentState.playerSubGamePhase);
        List<AbstractAction> actions = new ArrayList<>();
        Deck<RootCard> crafted = currentState.getPlayerCraftedCards(currentState.getCurrentPlayer());
        for (int i = 0; i < crafted.getSize(); i++){
            if (crafted.get(i).cardType == RootCard.CardType.RoyalClaim){
                actions.add(new DiscardCraftedCard(currentState.getCurrentPlayer(), i, crafted.get(i).getComponentID()));
            }
        }
        if (currentState.playerSubGamePhase == 0){
            if (currentState.getBuildingCount(RootParameters.BuildingType.FoxBase)==1 && currentState.getBuildingCount(RootParameters.BuildingType.MouseBase)==1 && currentState.getBuildingCount(RootParameters.BuildingType.RabbitBase)==1 && currentState.getSupporters().getSize() > 5){
                Deck<RootCard> supporters = currentState.getSupporters();
                for (int i = 0; i < supporters.getSize(); i++){
                    actions.add(new DiscardSupporter(currentState.getCurrentPlayer(), i, supporters.get(i).getComponentID(), false));
                }
            }else{
                actions.add(new PassSubGamePhase(currentState.getCurrentPlayer(), "does not discard supporters"));
            }
            return actions;

        } else if (currentState.playerSubGamePhase == 1) {
            PassSubGamePhase pass= new PassSubGamePhase(currentState.getCurrentPlayer(), "does not revolt");
            actions.add(pass);
            for(RootBoardNodeWithRootEdges node: currentState.getGameMap().getSympatheticClearings()){
                for(int i = 0; i < currentState.getSupporters().getSize(); i++){
                    for (int e = i+1; e < currentState.getSupporters().getSize();e++){
                        if(node.hasBuildingRoom() && (currentState.getSupporters().get(i).suit == node.getClearingType() || currentState.getSupporters().get(i).suit == RootParameters.ClearingTypes.Bird) && (currentState.getSupporters().get(e).suit == node.getClearingType() || currentState.getSupporters().get(e).suit == RootParameters.ClearingTypes.Bird)){
                            Revolt revoltAction = new Revolt(currentState.getCurrentPlayer(), node.getComponentID(), i, currentState.getSupporters().get(i).getComponentID(), e, currentState.getSupporters().get(e).getComponentID(), false);
                            actions.add(revoltAction);
                        }
                    }
                }
            }
            return actions;
        } else if (currentState.playerSubGamePhase == 2) {
            if(currentState.getSympathyTokens() == rp.sympathyTokens){
                for(RootBoardNodeWithRootEdges location: currentState.getGameMap().getNonForrestBoardNodes()){
                    if(!location.getKeep() && !location.getSympathy() && currentState.supportersContainClearingType(location.getClearingType())){
                        SpreadSympathy action = new SpreadSympathy(currentState.getCurrentPlayer(), location.getComponentID());
                        actions.add(action);
                    }
                }
            }else if (currentState.getSympathyTokens() > 0){
                for(RootBoardNodeWithRootEdges location: currentState.getGameMap().getNonSympathyNodesAdjacentToSympathy()){
                    if(!location.getKeep() && !location.getSympathy() && currentState.supportersContainClearingType(location.getClearingType())){
                        SpreadSympathy action = new SpreadSympathy(currentState.getCurrentPlayer(), location.getComponentID());
                        actions.add(action);
                    }
                }
            }
            PassGamePhase pass = new PassGamePhase(currentState.getCurrentPlayer());
            actions.add(pass);
            return actions;
        }
        return null;
    }

    static List<AbstractAction> getWoodLandAllianceDaylightActions(AbstractGameState gs) {
        //Craft
        //Mobilize (Add Card to Supporters)
        //Train (Recruit For Card)
        RootGameState currentState = (RootGameState) gs;
        //System.out.println("Daylight Woodland " + currentState.playerSubGamePhase);
        List<AbstractAction> actions = new ArrayList<>();
        //add all craft, Mobilize and Train actions + pass
        PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(currentState.getCurrentPlayer());
        for (int i = 0; i < hand.getSize(); i++){
            if (hand.get(i).cardType == RootCard.CardType.Dominance && currentState.getGameScore(currentState.getCurrentPlayer())>=10){
                actions.add(new PlayDomination(currentState.getCurrentPlayer(), i, hand.get(i).getComponentID()));
            }
        }
        if (currentState.playerSubGamePhase == 0) {
            List<RootParameters.ClearingTypes> available = new ArrayList<>();
            for (RootBoardNodeWithRootEdges location : currentState.getGameMap().getNonForrestBoardNodes()) {
                if (location.hasToken(RootParameters.TokenType.Sympathy)) {
                    available.add(location.getClearingType());
                }
            }
            if (currentState.canCraft(currentState.getCurrentPlayer(), available)) {
                actions.add(new CraftSequence(currentState.getCurrentPlayer(), available));
                actions.add(new PassSubGamePhase(currentState.getCurrentPlayer()));
            } else {
                actions.add(new PassSubGamePhase(currentState.getCurrentPlayer()));

            }
            return actions;

        }else {
            for (int i = 0; i < hand.getSize(); i++) {
                Mobilize action = new Mobilize(currentState.getCurrentPlayer(), i, hand.get(i).getComponentID());
                actions.add(action);
            }
            if (currentState.getWoodlandWarriors() > 0) {
                for (RootBoardNodeWithRootEdges node : currentState.getGameMap().getBaseNodes()) {
                    for (int i = 0; i < hand.getSize(); i++) {
                        if (node.getClearingType() == hand.get(i).suit || hand.get(i).suit == RootParameters.ClearingTypes.Bird) {
                            if (currentState.getWoodlandWarriors() > 0) {
                                Train action = new Train(currentState.getCurrentPlayer(), i, hand.get(i).getComponentID());
                                if (!actions.contains(action)) actions.add(action);
                            }
                        }
                    }
                }
            }
            PassGamePhase pass = new PassGamePhase(currentState.getCurrentPlayer());
            actions.add(pass);
            return actions;
        }
    }

    static List<AbstractAction> getWoodLandAllianceEveningActions(AbstractGameState gs) {
        //1st Move, Battle, Recruit, Organize
        //2nd Draw/Discard
        RootGameState currentState = (RootGameState) gs;
        List<AbstractAction> actions = new ArrayList<>();
        RootParameters rp = (RootParameters) gs.getGameParameters();
        //System.out.println("Evening Woodland " + currentState.playerSubGamePhase);
        if (currentState.playerSubGamePhase == 0) {
            if(currentState.actionsPlayed < currentState.officers) {
                if (currentState.canAttack(currentState.getCurrentPlayer())){
                    actions.add(new BattleAction(currentState.getCurrentPlayer()));
                }

                for (RootBoardNodeWithRootEdges node : currentState.getGameMap().getNonForrestBoardNodes()) {
                    if (node.getWarrior(currentState.getPlayerFaction(currentState.getCurrentPlayer())) > 0 && node.canMove(currentState.getCurrentPlayer())) {
                        MoveSequence action = new MoveSequence(currentState.getCurrentPlayer(),node.getComponentID(), false);
                        actions.add(action);
                        break;
                    }
                }
                if (currentState.getWoodlandWarriors() > 0 && !currentState.getGameMap().getBaseNodes().isEmpty()){
                    for (RootBoardNodeWithRootEdges node: currentState.getGameMap().getBaseNodes()){
                        actions.add(new Recruit(node.getComponentID(), currentState.getCurrentPlayer(), false));
                    }
                }
                for (RootBoardNodeWithRootEdges location: currentState.getGameMap().getNonForrestBoardNodes()){
                    if (!location.getSympathy() && location.getWarrior(RootParameters.Factions.WoodlandAlliance) > 0 && currentState.getSympathyTokens()>0){
                        actions.add(new Organize(currentState.getCurrentPlayer(), location.getComponentID()));
                    }
                }


            }
            PassSubGamePhase pass = new PassSubGamePhase(currentState.getCurrentPlayer(), "finishes with military operations");
            actions.add(pass);
            return actions;
        } else if (currentState.playerSubGamePhase == 1) {
            int additionalCards = currentState.getGameMap().getBaseNodes().size();
            Draw action = new Draw(gs.getCurrentPlayer(), rp.cardsDrawnPerTurn + additionalCards, true);
            actions.add(action);
            return actions;
        } else if (currentState.playerSubGamePhase == 2) {
            Deck<RootCard> playerHand = currentState.getPlayerHand(gs.getCurrentPlayer());
            if (playerHand.getSize() > rp.maxCardsEndTurn) {
                for (int i = 0; i < playerHand.getSize(); i++) {
                    Discard discardAction = new Discard(currentState.getCurrentPlayer(), i, playerHand.get(i).getComponentID(), false);
                    actions.add(discardAction);
                }
                return actions;
            } else {
                EndTurn action = new EndTurn(currentState.getCurrentPlayer(), false);
                actions.add(action);
                return actions;
            }
        }
        return null;
    }
    //endregion

    //region Vagabond Action Factory
    static List<AbstractAction> getVagabondSetupActions(AbstractGameState gs) {
        //1st Choose character + starting items + Quests
        RootGameState state = (RootGameState) gs;
        //System.out.println("Setting up Vagabond " + state.playerSubGamePhase);
        List<AbstractAction> actions = new ArrayList<>();
        if(state.getPlayerFaction(state.getCurrentPlayer()) == RootParameters.Factions.Vagabond){
            if(state.playerSubGamePhase == 0){
                for (VagabondCharacter.CardType character : VagabondCharacter.CardType.values()) {
                    //choose character, get starting items...
                    VagabondSetup action = new VagabondSetup(state.getCurrentPlayer(), character, true);
                    actions.add(action);
                }
            } else if (state.playerSubGamePhase == 1) {
                //choose starting forest
                for(RootBoardNodeWithRootEdges startingForrest : state.getGameMap().getBoardNodes()){
                    if(startingForrest.getClearingType() == RootParameters.ClearingTypes.Forrest){
                        actions.add(new Recruit(startingForrest.getComponentID(), state.getCurrentPlayer(), true));
                    }
                }
            } else{
                EndTurn action = new EndTurn(state.getCurrentPlayer(), true);
                actions.add(action);
            }
        }
        return actions;
    }

    static List<AbstractAction> getVagabondBirdsongActions(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        //System.out.println("Birdsong Vagabond " + currentState.playerSubGamePhase);
        //1st Refresh Items
        List<AbstractAction> actions = new ArrayList<>();
        Deck<RootCard> crafted = currentState.getPlayerCraftedCards(currentState.getCurrentPlayer());
        for (int i = 0; i < crafted.getSize(); i++){
            if (crafted.get(i).cardType == RootCard.CardType.RoyalClaim){
                actions.add(new DiscardCraftedCard(currentState.getCurrentPlayer(), i, crafted.get(i).getComponentID()));
            }
        }
        if(currentState.playerSubGamePhase == 0){
            RefreshItems refresh = new RefreshItems(currentState.getCurrentPlayer());
            actions.add(refresh);
        } else if (currentState.playerSubGamePhase == 1) {
            for (RootBoardNodeWithRootEdges neighbour: currentState.getGameMap().getVagabondClearing().getNeighbours()){
                Slip slip = new Slip(currentState.getCurrentPlayer(), currentState.getGameMap().getVagabondClearing().getComponentID(), neighbour.getComponentID(), true);
                actions.add(slip);
            }
            PassSubGamePhase pass = new PassSubGamePhase(currentState.getCurrentPlayer(), "does not slip");
            actions.add(pass);
        } else {
            PassGamePhase pass = new PassGamePhase(currentState.getCurrentPlayer());
            actions.add(pass);
        }
        //2nd Slip
        return actions;
    }

    static List<AbstractAction> getVagabondDaylightActions(AbstractGameState gs) {
        //Move
        //Battle
        //Explore
        //Aid
        //Quest
        //Strike
        //Repair/Craft
        RootGameState currentState = (RootGameState) gs;
        //System.out.println("Daylight Vagabond " + currentState.playerSubGamePhase);
        List<AbstractAction> actions = new ArrayList<>();
        boolean bootAdded = false;
        boolean swordAdded = false;
        boolean crossbowAdded = false;
        boolean hammerAdded = false;
        for (Item item: currentState.getSatchel()){
            if(!item.damaged && item.refreshed){
                switch (item.itemType){
                    case boot:
                        //move
                        if (currentState.getRefreshedItemCount(Item.ItemType.boot) > 0 && !bootAdded) {
                            for (RootBoardNodeWithRootEdges neighbour : currentState.getGameMap().getVagabondClearing().getNeighbours()) {
                                if (neighbour.getClearingType() != RootParameters.ClearingTypes.Forrest) {
                                    if ((currentState.getRelationship(RootParameters.Factions.MarquiseDeCat)!=Hostile || neighbour.getWarrior(RootParameters.Factions.MarquiseDeCat) == 0) && (currentState.getRelationship(RootParameters.Factions.EyrieDynasties)!=Hostile || neighbour.getWarrior(RootParameters.Factions.EyrieDynasties) == 0) && (currentState.getRelationship(RootParameters.Factions.WoodlandAlliance)!=Hostile || neighbour.getWarrior(RootParameters.Factions.WoodlandAlliance) == 0)) {
                                        actions.add(new VagabondMove(currentState.getCurrentPlayer(), neighbour.getComponentID()));
                                    }else if(currentState.getRefreshedItemCount(Item.ItemType.boot) > 1){
                                        //if the clearing is hostile
                                        actions.add(new VagabondMove(currentState.getCurrentPlayer(), neighbour.getComponentID()));
                                    }
                                }
                            }
                        }
                        bootAdded = true;
                        break;
                    case sword:
                        //battle
                        if (currentState.canAttack(currentState.getCurrentPlayer()) && !swordAdded) {
                            actions.add(new BattleAction(currentState.getCurrentPlayer()));
                            swordAdded = true;
                        }
                        break;
                    case torch:
                        //explore
                        if (currentState.getRefreshedItemCount(Item.ItemType.torch) > 0 && currentState.getGameMap().getVagabondClearing().hasBuilding(RootParameters.BuildingType.Ruins)){
                            actions.add(new VagabondExplore(currentState.getCurrentPlayer()));
                        }
                        //special
                        if (currentState.vagabondCharacter.characterType == VagabondCharacter.CardType.Ranger){
                            actions.add(new VagabondHideout(currentState.getCurrentPlayer()));
                        } else if (currentState.vagabondCharacter.characterType == VagabondCharacter.CardType.Tinker){
                            if (currentState.getDiscardPile().getSize() > 0){
                                actions.add(new VagabondDayLabour(currentState.getCurrentPlayer()));
                            }
                        } else if (currentState.vagabondCharacter.characterType == VagabondCharacter.CardType.Thief) {
                            if (currentState.canSteal(currentState.getCurrentPlayer())){
                                actions.add(new VagabondSteal(currentState.getCurrentPlayer()));
                            }
                        }
                        break;
                    case hammer:
                        //repair
                        if (!hammerAdded) {
                            for (Item itemToRepair : currentState.getSatchel()) {
                                if (itemToRepair.damaged) {
                                    VagabondRepair action = new VagabondRepair(currentState.getCurrentPlayer(), itemToRepair.itemType, true);
                                    if (!actions.contains(action)) actions.add(action);
                                }
                            }
                            for (Item itemToRepair : currentState.getCoins()) {
                                if (itemToRepair.damaged) {
                                    VagabondRepair action = new VagabondRepair(currentState.getCurrentPlayer(), itemToRepair.itemType, true);
                                    if (!actions.contains(action)) actions.add(action);
                                }
                            }
                            for (Item itemToRepair : currentState.getBags()) {
                                if (itemToRepair.damaged) {
                                    VagabondRepair action = new VagabondRepair(currentState.getCurrentPlayer(), itemToRepair.itemType, true);
                                    if (!actions.contains(action)) actions.add(action);
                                }
                            }
                            for (Item itemToRepair : currentState.getTeas()) {
                                if (itemToRepair.damaged) {
                                    VagabondRepair action = new VagabondRepair(currentState.getCurrentPlayer(), itemToRepair.itemType, true);
                                    if (!actions.contains(action)) actions.add(action);
                                }
                            }
                            hammerAdded = true;
                        }
                        break;
                    case crossbow:
                        //strike
                        if (currentState.canAttack(currentState.getCurrentPlayer()) && !crossbowAdded){
                            actions.add(new VagabondStrike(currentState.getCurrentPlayer()));
                            crossbowAdded = true;
                        }
                        break;
                }
            }
        }
        if (currentState.canAid(currentState.getCurrentPlayer())){
            actions.add(new VagabondAid(currentState.getCurrentPlayer()));
        }
        int n_hammers = currentState.getRefreshedItemCount(Item.ItemType.hammer);
        List<RootParameters.ClearingTypes> available = new ArrayList<>();
        if (currentState.getGameMap().getVagabondClearing().getClearingType() != RootParameters.ClearingTypes.Forrest) {
            for (int i = 0; i < n_hammers; i++) {
                available.add(currentState.getGameMap().getVagabondClearing().getClearingType());
            }
        }
        if (currentState.canCraft(currentState.getCurrentPlayer(), available)){
            actions.add(new VagabondCrafting(currentState.getCurrentPlayer(), available));
        }
        if (currentState.canCompleteQuest()){
            actions.add(new VagabondQuestSequence(currentState.getCurrentPlayer()));
        }
        PassGamePhase pass = new PassGamePhase(currentState.getCurrentPlayer());
        actions.add(pass);
        return actions;
    }

    static List<AbstractAction> getVagabondEveningActions(AbstractGameState gs) {
        //1st Repair ALL if in forest : Done
        //2nd Draw : Done
        //3rd Discard : Done
        //4th Discard items: Done
        RootGameState currentState = (RootGameState) gs;
        RootParameters rp = (RootParameters) gs.getGameParameters();
        List<AbstractAction> actions = new ArrayList<>();
        if (currentState.playerSubGamePhase == 0) {
            if(currentState.getGameMap().getVagabondClearing().getClearingType() == RootParameters.ClearingTypes.Forrest) {
                actions.add(new VagabondRepairAllItems(currentState.getCurrentPlayer()));
            }else {
                PassSubGamePhase pass = new PassSubGamePhase(currentState.getCurrentPlayer());
                actions.add(pass);
            }
            return actions;
        } else if (currentState.playerSubGamePhase == 1) {
            Draw action = new Draw(gs.getCurrentPlayer(), rp.cardsDrawnPerTurn + currentState.coins.size(), true);
            actions.add(action);
            return actions;
        } else if (currentState.playerSubGamePhase == 2) {
            Deck<RootCard> playerHand = currentState.getPlayerHand(gs.getCurrentPlayer());
            if (playerHand.getSize() > rp.maxCardsEndTurn) {
                for (int i = 0; i < playerHand.getSize(); i++) {
                    Discard discardAction = new Discard(currentState.getCurrentPlayer(), i, playerHand.get(i).getComponentID(), true);
                    actions.add(discardAction);
                }
                return actions;
            }else {
                actions.add(new PassSubGamePhase(currentState.getCurrentPlayer()));
                return actions;
            }
        }else if (currentState.playerSubGamePhase == 3) {
            if (currentState.getSatchel().size() + currentState.getDamagedAndExhaustedNonSatchelItems() > 6 + ((currentState.bags.size() * 2))) {
                for (Item satchel : currentState.getSatchel()){
                    VagabondDiscardItem action = new VagabondDiscardItem(currentState.getCurrentPlayer(), satchel.itemType, satchel.getComponentID());
                    if(!actions.contains(action)) actions.add(action);
                }
                for (Item bag : currentState.getBags()){
                    VagabondDiscardItem action = new VagabondDiscardItem(currentState.getCurrentPlayer(), bag.itemType, bag.getComponentID());
                    if(!actions.contains(action)) actions.add(action);
                }
                for (Item tea : currentState.getTeas()){
                    VagabondDiscardItem action = new VagabondDiscardItem(currentState.getCurrentPlayer(), tea.itemType, tea.getComponentID());
                    if(!actions.contains(action)) actions.add(action);
                }
                for (Item coin : currentState.getCoins()){
                    VagabondDiscardItem action = new VagabondDiscardItem(currentState.getCurrentPlayer(), coin.itemType, coin.getComponentID());
                    if(!actions.contains(action)) actions.add(action);
                }
                return actions;
            } else {
                EndTurn action = new EndTurn(currentState.getCurrentPlayer(), false);
                actions.add(action);
                return actions;

            }
        }

        return actions;
    }
    //endregion
}
