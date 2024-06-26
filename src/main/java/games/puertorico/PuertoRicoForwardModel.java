package games.puertorico;

import core.*;
import core.actions.AbstractAction;
import core.components.Deck;
import games.puertorico.actions.*;
import games.puertorico.components.*;

import java.util.*;

import static games.puertorico.PuertoRicoConstants.BuildingType.*;
import static games.puertorico.PuertoRicoConstants.Crop.*;
import static java.util.stream.Collectors.*;

public class PuertoRicoForwardModel extends StandardForwardModel {
    @Override
    protected void _setup(AbstractGameState firstState) {
        PuertoRicoGameState pgs = (PuertoRicoGameState) firstState;
        PuertoRicoParameters params = (PuertoRicoParameters) firstState.getGameParameters();

        // first we give each player one plantation on their player board
        // the first two player get Indigo, the others Corn
        // TODO: parameterise this and make it dependent on the number of players
        int indigoOnPlayerBoard = 0;
        int cornOnPlayerBoard = 0;
        pgs.playerBoards = new ArrayList<>();
        for (int i = 0; i < pgs.getNPlayers(); i++) {
            PRPlayerBoard pb = new PRPlayerBoard(i);
            pgs.playerBoards.add(pb);
            Plantation p;
            if (i < 2) {
                p = new Plantation(INDIGO);
                indigoOnPlayerBoard++;
            } else {
                p = new Plantation(CORN);
                cornOnPlayerBoard++;
            }
            pb.plantations.add(p);
            pb.changeDoubloons(params.startingDoubloons[firstState.getNPlayers()][i]);
        }

        pgs.currentRole = null;

        pgs.plantationDeck = new Deck<>("Plantation Deck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        pgs.visiblePlantations = new Deck<>("Available Plantations", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        pgs.plantationDiscards = new Deck<>("Discarded Plantations", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        for (int i = 0; i < params.numCornPlantations - cornOnPlayerBoard; i++) {
            pgs.plantationDeck.add(new Plantation(CORN));
        }
        for (int i = 0; i < params.numIndigoPlantations - indigoOnPlayerBoard; i++) {
            pgs.plantationDeck.add(new Plantation(INDIGO));
        }
        for (int i = 0; i < params.numSugarPlantations; i++) {
            pgs.plantationDeck.add(new Plantation(SUGAR));
        }
        for (int i = 0; i < params.numTobaccoPlantations; i++) {
            pgs.plantationDeck.add(new Plantation(TOBACCO));
        }
        for (int i = 0; i < params.numCoffeePlantations; i++) {
            pgs.plantationDeck.add(new Plantation(COFFEE));
        }

        pgs.plantationDeck.shuffle(pgs.getRnd());

        for (int i = 0; i < params.extraVisiblePlantations + pgs.getNPlayers(); i++)
            pgs.visiblePlantations.add(pgs.plantationDeck.draw());

        pgs.cropSupply = new EnumMap<>(PuertoRicoConstants.Crop.class);
        pgs.cropSupply.put(CORN, params.numCorn);
        pgs.cropSupply.put(INDIGO, params.numIndigo);
        pgs.cropSupply.put(SUGAR, params.numSugar);
        pgs.cropSupply.put(TOBACCO, params.numTobacco);
        pgs.cropSupply.put(COFFEE, params.numCoffee);

        pgs.soldInMarket = new ArrayList<>();

        pgs.buildingsAvailable = new EnumMap<>(PuertoRicoConstants.BuildingType.class);
        pgs.buildingsAvailable.put(SMALL_INDIGO_PLANT, params.numSmallProductionBuildings);
        pgs.buildingsAvailable.put(SMALL_SUGAR_MILL, params.numSmallProductionBuildings);

        pgs.buildingsAvailable.put(INDIGO_PLANT, params.numLargeProductionBuildings);
        pgs.buildingsAvailable.put(SUGAR_MILL, params.numLargeProductionBuildings);
        pgs.buildingsAvailable.put(COFFEE_ROASTER, params.numLargeProductionBuildings);
        pgs.buildingsAvailable.put(TOBACCO_STORAGE, params.numLargeProductionBuildings);

        pgs.buildingsAvailable.put(SMALL_MARKET, params.numOtherBuildings);
        pgs.buildingsAvailable.put(HACIENDA, params.numOtherBuildings);
        pgs.buildingsAvailable.put(SMALL_WAREHOUSE, params.numOtherBuildings);
        pgs.buildingsAvailable.put(CONSTRUCTION_HUT, params.numOtherBuildings);
        pgs.buildingsAvailable.put(LARGE_MARKET, params.numOtherBuildings);
        pgs.buildingsAvailable.put(LARGE_WAREHOUSE, params.numOtherBuildings);
        pgs.buildingsAvailable.put(HOSPICE, params.numOtherBuildings);
        pgs.buildingsAvailable.put(OFFICE, params.numOtherBuildings);
        pgs.buildingsAvailable.put(FACTORY, params.numOtherBuildings);
        pgs.buildingsAvailable.put(UNIVERSITY, params.numOtherBuildings);
        pgs.buildingsAvailable.put(HARBOUR, params.numOtherBuildings);
        pgs.buildingsAvailable.put(WHARF, params.numOtherBuildings);

        pgs.buildingsAvailable.put(CITY_HALL, params.numVictoryBuildings);
        pgs.buildingsAvailable.put(CUSTOMS_HOUSE, params.numVictoryBuildings);
        pgs.buildingsAvailable.put(RESIDENCE, params.numVictoryBuildings);
        pgs.buildingsAvailable.put(GUILD_HALL, params.numVictoryBuildings);
        pgs.buildingsAvailable.put(FORTRESS, params.numVictoryBuildings);


        pgs.quarries = new ArrayList<>();
        for (int i = 0; i < params.quarries; i++) {
            pgs.quarries.add(new Plantation(QUARRY));
        }

        pgs.rolesAvailable = new EnumMap<>(PuertoRicoConstants.Role.class);
        for (PuertoRicoConstants.Role r : PuertoRicoConstants.Role.values()) {
            if (r.hidden)
                continue;
            pgs.rolesAvailable.put(r, true);
        }
        pgs.moneyOnRoles = pgs.rolesAvailable.keySet().stream().collect(toMap(r -> r, r -> 0));
        pgs.colonistsOnShip = pgs.getNPlayers();
        pgs.colonistsInSupply = params.totalColonists[pgs.getNPlayers()] - pgs.colonistsOnShip;
        pgs.vpSupply = params.totalVP[pgs.getNPlayers()];

        // Now set up Ships on game state, taking the number from the parameters
        pgs.ships = Arrays.stream(params.shipCapacities[pgs.getNPlayers()])
                .mapToObj(Ship::new).collect(toList());
        pgs.gameEndTriggered = false;
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        PuertoRicoGameState state = (PuertoRicoGameState) gameState;
        List<AbstractAction> retValue = new ArrayList<>();
        // We calculate the available actions from the ones still available on the state
        retValue = state.rolesAvailable.entrySet().stream()
                .filter(Map.Entry::getValue)
                .filter(r -> !r.getKey().hidden)
                .map(e -> new SelectRole(e.getKey()))
                .collect(toList());

        return retValue;
    }

    @Override
    public void _afterAction(AbstractGameState gameState, AbstractAction action) {
        PuertoRicoGameState state = (PuertoRicoGameState) gameState;

        if (state.isActionInProgress())
            return;  // we always wait for any EAS to finish

        int nextPlayer = (state.getCurrentPlayer() + 1) % state.getNPlayers(); // we increment one more

        endPlayerTurn(state, nextPlayer);
        // if it is the end of the round (i.e. the roleSelectionPlayer has moved back to firstPlayer)
        if (state.getTurnOwner() == state.getFirstPlayer()) {
            if (state.gameEndTriggered) {
                endGame(state);
            } else {
                endRound(state, (state.getTurnOwner() + 1) % state.getNPlayers());
                endRoundProcessing(state);
            }
        }
    }

    public void endRoundProcessing(PuertoRicoGameState state) {
        // We refresh all the roles to be available, adding one money to each one that was not taken
        for (PuertoRicoConstants.Role r : state.rolesAvailable.keySet()) {
            if (state.rolesAvailable.get(r)) {
                state.moneyOnRoles.put(r, state.moneyOnRoles.get(r) + 1);
            } else {
                state.moneyOnRoles.put(r, 0); // was selected last turn, so no money
            }
            state.rolesAvailable.put(r, true);
        }
    }
}
