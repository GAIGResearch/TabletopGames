package games.wonders7;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import games.wonders7.actions.*;
import games.wonders7.cards.Wonder7Board;
import games.wonders7.cards.Wonder7Card;

import java.util.*;
import java.util.stream.Collectors;

import static games.wonders7.Wonders7Constants.Resource.*;
import static games.wonders7.cards.Wonder7Board.Wonder.TheMausoleumOfHalicarnassus;
import static games.wonders7.cards.Wonder7Card.CardType.*;



public class Wonders7ForwardModel extends StandardForwardModel {
// The rationale of the ForwardModel is that it contains the core game logic, while the GameState contains the underlying game data. 
// Usually this means that ForwardModel is stateless, and this is a good principle to adopt, but as ever there will always be exceptions.

    public void _setup(AbstractGameState state) {
        Wonders7GameState wgs = (Wonders7GameState) state;
        Wonders7GameParameters params = (Wonders7GameParameters) wgs.getGameParameters();

        // Sets game in Age 1
        wgs.currentAge = 1;
        wgs.direction = 1;

        // Then fills every player's hashmaps, so each player has 0 of each resource
        for (int i = 0; i < wgs.getNPlayers(); i++) { // For each
            for (Wonders7Constants.Resource type : Wonders7Constants.Resource.values()) {
                wgs.playerResources.get(i).put(type, 0);
            }
        }

        //System.out.println("THE GAME HAS STARTED");
        wgs.playerHands = new ArrayList<>();
        wgs.playedCards = new ArrayList<>();
        wgs.turnActions = new AbstractAction[wgs.getNPlayers()];
        wgs.playerWonderBoard = new Wonder7Board[wgs.getNPlayers()];
        wgs.ageDeck = new Deck<>("Age Deck", CoreConstants.VisibilityMode.MIXED_VISIBILITY);
        wgs.wonderBoardDeck = new Deck<>("Wonder Board Deck", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        for (int i = 0; i < wgs.getNPlayers(); i++) {
            wgs.playerHands.add(new Deck<>("Player hand" + i, i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            wgs.playedCards.add(new Deck<>("Played Cards", CoreConstants.VisibilityMode.VISIBLE_TO_ALL));
        }

        // Cards that have been discarded by all players
        wgs.discardPile = new Deck<>("Discarded Cards", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);

        // Shuffles wonder-boards
        createWonderDeck(wgs, params.wonderShuffleSeed == -1 ? state.getRnd() : new Random(params.wonderShuffleSeed)); // Adds Wonders into game

        // Gives each player wonder board and manufactured goods from the wonder
        for (int player = 0; player < wgs.getNPlayers(); player++) {
            wgs.setPlayerWonderBoard(player, wgs.wonderBoardDeck.draw());// Each player has one designated Wonder board

            // Players get their wonder board manufacturedGoods added to their resources
            Wonders7Constants.Resource wonderResource = wgs.getPlayerWonderBoard(player).wonderType().resourcesProduced;
            wgs.getPlayerResources(player).put(wonderResource, 1);
            // add coins
            wgs.getPlayerResources(player).put(Coin, params.startingCoins);
        }

        ageSetup(wgs); // Shuffles deck and fills player hands, sets the turn owner
    }

    public void ageSetup(AbstractGameState state) {
        Wonders7GameState wgs = (Wonders7GameState) state;

        // Sets up the age
        createAgeDeck(wgs); // Fills Age1 deck with cards
        wgs.ageDeck.shuffle(wgs.cardRnd);
        //System.out.println("ALL THE CARDS IN THE GAME: "+wgs.AgeDeck.getSize());
        // Give each player their 7 cards, wonderBoard and the manufactured goods from the wonder-board
        for (int player = 0; player < wgs.getNPlayers(); player++) {
            for (int card = 0; card < ((Wonders7GameParameters) wgs.getGameParameters()).nWonderCardsPerPlayer; card++) {
                wgs.getPlayerHand(player).add(wgs.ageDeck.draw());
            }
        }

        // Player 0 starts
        wgs.setTurnOwner(0);
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction actionTaken) {
        Wonders7GameState wgs = (Wonders7GameState) state;
        // EVERYBODY NOW PLAYS THEIR CARDS (ACTION ROUND)
        if (checkActionRound(wgs)) {
            executeAllActions(wgs);

            endRound(wgs); // Ends the round, increments the round counter
            // this ensures that one round is one turn per player

            // then if Halicarnassus triggers, put that functionality on the stack
            for (int p = 0; p < wgs.getNPlayers(); p++) {
                Wonder7Board wonderBoard = wgs.getPlayerWonderBoard(p);
                if (wonderBoard.wonderType() == TheMausoleumOfHalicarnassus &&
                        !wonderBoard.effectUsed &&
                        ((wonderBoard.getSide() == 0 && wonderBoard.nextStageToBuild() == 3 ) ||
                        (wonderBoard.getSide() == 1 && wonderBoard.nextStageToBuild() > 1))) {
                    wgs.setActionInProgress(new BuildFromDiscard(p));
                    wgs.getPlayerWonderBoard(p).effectUsed = true;
                    return;
                }
                // the _afterAction method of BuildFromDiscard will rotate hands and check for end of age
            }

            rotateHands(wgs);

            checkAgeEnd(wgs); // Check for Age end;
        }
        // We now check that all players have the same number of cards in hand
        int cardsExpected = wgs.getPlayerHand(0).getSize();
        for (int p = 1; p < wgs.getNPlayers(); p++) {
            if (wgs.getPlayerHand(p).getSize() != cardsExpected) {
                throw new AssertionError("Player " + p + " has " + wgs.getPlayerHand(p).getSize() + " cards in hand, but player 0 has " + cardsExpected);
            }
        }

        // the next player is whoever still has something to choose
        if (wgs.isNotTerminal()) {
            boolean playerStillToChoose = true;
            for (int p = 0; p < wgs.getNPlayers(); p++) {
                if (wgs.getTurnAction(p) == null) {
                    endPlayerTurn(wgs, p);
                    playerStillToChoose = false;
                    break;
                }
            }
            if (playerStillToChoose) {
                throw new AssertionError("All players have chosen, so we should not be here");
            }
        }

    }

    private void executeAllActions(Wonders7GameState wgs) {
        for (int i = 0; i < wgs.getNPlayers(); i++) {
            wgs.setTurnOwner(i); // PLAYER i DOES THE ACTION THEY SELECTED, NOT ANOTHER PLAYERS ACTION
            wgs.getTurnAction(i).execute(wgs); // EXECUTE THE ACTION
            wgs.setTurnAction(i, null); // REMOVE EXECUTED ACTION
        }
        wgs.setTurnOwner(0);
    }

    public void rotateHands(Wonders7GameState wgs) {
        Deck<Wonder7Card> temp = wgs.getPlayerHands().get(0);
        if (wgs.direction == 1) {
            for (int i = 0; i < wgs.getNPlayers(); i++) {
                if (i == wgs.getNPlayers() - 1) {
                    wgs.getPlayerHands().set(i, temp);
                } // makes sure the last player receives first players original hand
                else {
                    wgs.getPlayerHands().set(i, wgs.getPlayerHands().get(i + 1));
                } // Rotates hands clockwise
            }
        } else {
            temp = wgs.getPlayerHand((wgs.getNPlayers() - 1) % wgs.getNPlayers());
            for (int i = (wgs.getNPlayers() - 1) % wgs.getNPlayers(); i > -1; i--) {
                if (i % wgs.getNPlayers() == 0) {
                    wgs.getPlayerHands().set(i, temp);
                } // makes sure the last player receives first players original hand
                else {
                    wgs.getPlayerHands().set(i, wgs.getPlayerHands().get(i - 1));
                } // Rotates hands anticlockwise
            }
        }
    }

    protected boolean checkActionRound(AbstractGameState gameState) {
        Wonders7GameState wgs = (Wonders7GameState) gameState;
        for (int i = 0; i < wgs.getNPlayers(); i++) {
            if (wgs.turnActions[i] == null) return false;
        }
        return true;
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        Wonders7GameState wgs = (Wonders7GameState) gameState;
        int player = wgs.getCurrentPlayer();
        Deck<Wonder7Card> playerHand = wgs.getPlayerHand(player);
        Set<AbstractAction> actions = new LinkedHashSet<>();

        // If player has the prerequisite card/enough resources/the card is free/the player can pay for the resources to play the card
        for (Wonder7Card card : playerHand.getComponents()) { // Goes through each card in hand
            if (card.isAlreadyPlayed(player, wgs)) continue;

            if (card.isPlayable(player, wgs).a) {  // Is free / Meets the costs / can pay neighbours for resources
                actions.add(new PlayCard(player, card.cardType, card.isFree(player, wgs)));
            }
        }

        // If next stage is playable or not
        if (wgs.getPlayerWonderBoard(player).isPlayable(wgs)) {
            for (int i = 0; i < playerHand.getSize(); i++) { // Goes through each card in hand
                actions.add(new BuildStage(player, playerHand.get(i).cardType));
            }
        }

        // All discard-able cards in player hand
        for (int i = 0; i < playerHand.getSize(); i++) {
            actions.add(new DiscardCard(playerHand.get(i).cardType, player));
        }

        return actions.stream().map(ChooseCard::new).collect(Collectors.toList());
    }

    protected void createWonderDeck(Wonders7GameState wgs, Random rnd) {
        // Create all the possible wonders a player could be assigned
        // this takes all seven boards and pre-shuffles them to one side or the other
        for (Wonder7Board.Wonder wonder : wgs.getParams().wonders) {
            wgs.wonderBoardDeck.add(new Wonder7Board(wonder, rnd.nextInt(2)));
        }
        wgs.wonderBoardDeck.shuffle(rnd);
    }

    public void checkAgeEnd(AbstractGameState gameState) {
        Wonders7GameState wgs = (Wonders7GameState) gameState;


        if (wgs.getPlayerHand(wgs.getCurrentPlayer()).getSize() == 1) {  // If all players hands are empty

            // if the relevant stage of Babylon is built, then auto-build the last card in hand
            for (int p = 0; p < wgs.getNPlayers(); p++) {
                Wonder7Board wonderBoard = wgs.getPlayerWonderBoard(p);
                if (wonderBoard.wonderType() == Wonder7Board.Wonder.TheHangingGardensOfBabylon &&
                        wonderBoard.getSide() == 1 &&
                        wonderBoard.nextStageToBuild() > 1) {
                    if (wgs.getPlayerHand(p).getSize() > 1) {
                        throw new AssertionError("Babylon should have only one card in hand at this point");
                    }
                    Wonder7Card cardToBuildForFree = wgs.getPlayerHand(p).get(0);
                    PlayCard action = new PlayCard(p, cardToBuildForFree.cardType, true);
                    action.execute(wgs);
                }
            }

            for (int i = 0; i < wgs.getNPlayers(); i++) {
                if (wgs.getPlayerHand(i).getSize() > 0) {
                    wgs.getDiscardPile().add(wgs.getPlayerHand(i).get(0));
                    wgs.getPlayerHand(i).remove(0);
                }
            }

            wgs.reverse(); // Turn Order reverses at end of Age

            // Resolves military conflicts
            wgs.updateEndOfAgeMilitaryVPs();

            wgs.getAgeDeck().clear();
            wgs.currentAge += 1; // Next age starts
            if (wgs.currentAge == 4) {
                // game is over
                endGame(wgs);
            } else {
                ageSetup(wgs);
            }
        }
    }

    protected void createAgeDeck(Wonders7GameState wgs) {
        // This method will create the deck for the current Era and
        // All the hashmaps containing different number of resources
        switch (wgs.currentAge) {
            // ALL THE CARDS IN DECK 1
            case 1:
                switch (wgs.getNPlayers()) {
                    case 7:
                        wgs.ageDeck.add(Wonder7Card.factory(Well, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Baths, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Tavern, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(EastTradingPost, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(WestTradingPost, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Stockade, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Workshop, wgs.getParams()));
                    case 6:
                        wgs.ageDeck.add(Wonder7Card.factory(TreeFarm, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Mine, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Press, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Loom, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Glassworks, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Theatre, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Marketplace, wgs.getParams()));
                    case 5:
                        wgs.ageDeck.add(Wonder7Card.factory(StonePit, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(ClayPool, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(ForestCave, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Altar, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Tavern, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Barracks, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Apothecary, wgs.getParams()));
                    case 4:
                        wgs.ageDeck.add(Wonder7Card.factory(LumberYard, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(OreVein, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Excavation, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Well, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Tavern, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(GuardTower, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Scriptorium, wgs.getParams()));
                    case 3:
                        wgs.ageDeck.add(Wonder7Card.factory(LumberYard, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(StonePit, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(ClayPool, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(OreVein, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(ClayPit, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(TimberYard, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Press, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Loom, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Glassworks, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Baths, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Altar, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Theatre, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(EastTradingPost, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(WestTradingPost, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Marketplace, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Stockade, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Barracks, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(GuardTower, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Apothecary, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Workshop, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Scriptorium, wgs.getParams()));
                        break;
                    default:
                        throw new AssertionError("Number of players not supported: " + wgs.getNPlayers());
                }
                break;
            case 2:
                switch (wgs.getNPlayers()) {
                    case 7:
                        wgs.ageDeck.add(Wonder7Card.factory(Statue, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Aqueduct, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Walls, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(TrainingGround, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(School, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Forum, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Bazaar, wgs.getParams()));
                    case 6:
                        wgs.ageDeck.add(Wonder7Card.factory(Temple, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(ArcheryRange, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(TrainingGround, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Library, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Caravansery, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Forum, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Vineyard, wgs.getParams()));
                    case 5:
                        wgs.ageDeck.add(Wonder7Card.factory(LoomAge2, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(GlassworksAge2, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(PressAge2, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Courthouse, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Stables, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Laboratory, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Caravansery, wgs.getParams()));
                    case 4:
                        wgs.ageDeck.add(Wonder7Card.factory(Sawmill, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Quarry, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Foundry, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Brickyard, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(TrainingGround, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Dispensary, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Bazaar, wgs.getParams()));
                    case 3:
                        wgs.ageDeck.add(Wonder7Card.factory(Sawmill, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Quarry, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Foundry, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Brickyard, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(LoomAge2, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(GlassworksAge2, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(PressAge2, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Aqueduct, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Temple, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Courthouse, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Statue, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Stables, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(ArcheryRange, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Walls, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Library, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Laboratory, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Dispensary, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(School, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Caravansery, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Forum, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Vineyard, wgs.getParams()));
                        break;
                    default:
                        throw new AssertionError("Number of players not supported: " + wgs.getNPlayers());
                }
                break;
            case 3:
                switch (wgs.getNPlayers()) {
                    case 7:
                        wgs.ageDeck.add(Wonder7Card.factory(Palace, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Castrum, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Fortifications, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(University, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Observatory, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Ludus, wgs.getParams()));
                    case 6:
                        wgs.ageDeck.add(Wonder7Card.factory(TownHall, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Pantheon, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Lighthouse, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(ChamberOfCommerce, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Circus, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Lodge, wgs.getParams()));
                    case 5:
                        wgs.ageDeck.add(Wonder7Card.factory(Senate, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Ludus, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Arena, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(SiegeWorkshop, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Arsenal, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Study, wgs.getParams()));
                    case 4:
                        wgs.ageDeck.add(Wonder7Card.factory(Gardens, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(ChamberOfCommerce, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Haven, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Circus, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Castrum, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(University, wgs.getParams()));
                    case 3:
                        wgs.ageDeck.add(Wonder7Card.factory(Gardens, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Senate, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(TownHall, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Pantheon, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Palace, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(University, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Lodge, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Study, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Academy, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Observatory, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(SiegeWorkshop, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Arsenal, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Fortifications, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Arena, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Lighthouse, wgs.getParams()));
                        wgs.ageDeck.add(Wonder7Card.factory(Haven, wgs.getParams()));

                        // We add two Guild cards per player
                        List<Wonder7Card> allGuilds = new ArrayList<>();
                        allGuilds.add(Wonder7Card.factory(WorkersGuild, wgs.getParams()));
                        allGuilds.add(Wonder7Card.factory(CraftsmenGuild, wgs.getParams()));
                        allGuilds.add(Wonder7Card.factory(TradersGuild, wgs.getParams()));
                        allGuilds.add(Wonder7Card.factory(PhilosophersGuild, wgs.getParams()));
                        allGuilds.add(Wonder7Card.factory(SpiesGuild, wgs.getParams()));
                        allGuilds.add(Wonder7Card.factory(DecoratorsGuild, wgs.getParams()));
                        allGuilds.add(Wonder7Card.factory(ShipownersGuild, wgs.getParams()));
                        allGuilds.add(Wonder7Card.factory(BuildersGuild, wgs.getParams()));
                        allGuilds.add(Wonder7Card.factory(MagistratesGuild, wgs.getParams()));
                        allGuilds.add(Wonder7Card.factory(ScientistsGuild, wgs.getParams()));
                        Collections.shuffle(allGuilds, wgs.getRnd());
                        for (int i = 0; i < wgs.getNPlayers() + 2; i++) {
                            wgs.ageDeck.add(allGuilds.get(i));
                        }
                        break;
                    default:
                        throw new AssertionError("Number of players not supported: " + wgs.getNPlayers());
                }
                break;
            default:
                throw new AssertionError("Age not supported: " + wgs.currentAge);
        }
    }
}

