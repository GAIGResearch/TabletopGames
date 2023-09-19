package games.puertorico.roles;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.puertorico.*;
import games.puertorico.actions.OccupyBuilding;
import games.puertorico.actions.OccupyPlantation;
import games.puertorico.components.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Mayor extends PuertoRicoRole<Mayor> {

    public Mayor(PuertoRicoGameState state) {
        super(state, PuertoRicoConstants.Role.MAYOR);
    }

    protected Mayor(Mayor toCopy) {
        super(toCopy);
    }

    @Override
    protected void prePhaseProcessing(PuertoRicoGameState state) {
        // Here we add calculate how many colonists each player gets, and add them to their colonist pool
        int colonists = state.getColonistsOnShip();
        state.changeColonistsOnShip(-colonists);
        int nPlayers = state.getNPlayers();
        int[] colonistsPerPlayer = new int[nPlayers];
        for (int i = 0; i < colonists; i++) {
            colonistsPerPlayer[(i + roleOwner) % nPlayers]++;
        }
        // and we give one from the supply to the role owner
        if (state.getColonistsInSupply() > 0) {
            colonistsPerPlayer[roleOwner]++;
            state.changeColonistsInSupply(-1);
        }
        for (int i = 0; i < nPlayers; i++) {
            PRPlayerBoard playerBoard = state.getPlayerBoard(i);
            playerBoard.addColonists(colonistsPerPlayer[i]);

            // We then remove all colonists from occupying plantations (and buildings) (and add them to the player board supply)
            int colonistsOnPlantations = playerBoard.getPlantations().stream().mapToInt(p -> p.isOccupied() ? 1 : 0).sum();
            playerBoard.getPlantations().forEach(Plantation::unsetOccupied);
            playerBoard.addColonists(colonistsOnPlantations);
            // ditto for buildings
            int colonistsOnBuildings = playerBoard.getBuildings().stream().mapToInt(Building::getOccupation).sum();
            playerBoard.getBuildings().forEach(b -> b.setOccupation(0));
            playerBoard.addColonists(colonistsOnBuildings);

            // We can now decide which players have to do something; if they have more colonists than vacancies then we just fill everything
            allocateAllColonistsIfDeterministic(state, i);
            // else we need to make decisions about which buildings/plantations to occupy/leave empty
        }
    }

    private boolean allocateAllColonistsIfDeterministic(PuertoRicoGameState state, int player) {
        PRPlayerBoard playerBoard = state.getPlayerBoard(player);
        if (playerBoard.getUnassignedColonists() >= playerBoard.getPlantationVacancies() + playerBoard.getTownVacancies()) {
            hasFinished[player] = true;
            int total = playerBoard.getPlantationVacancies() + playerBoard.getTownVacancies();
            playerBoard.getPlantations().forEach(Plantation::setOccupied);
            playerBoard.getBuildings().forEach(b -> b.setOccupation(b.buildingType.capacity));
            playerBoard.addColonists(-total);
        }
        if (playerBoard.getUnassignedColonists() == 0) {
            hasFinished[player] = true;
        }
        if (playerBoard.getTownVacancies() == 0 && playerBoard.getPlantations().stream().filter(p -> !p.isOccupied()).map(p -> p.crop).distinct().count() == 1) {
            // in this case just occupy as many as possible of the only remaining type
            List<Plantation> plantations = playerBoard.getPlantations().stream().filter(p -> !p.isOccupied()).collect(Collectors.toList());
            for (int i = 0; i < playerBoard.getUnassignedColonists(); i++) {
                plantations.get(i).setOccupied();
            }
            playerBoard.addColonists(-playerBoard.getUnassignedColonists());
            hasFinished[player] = true;
        }
        return hasFinished[player];
    }

    @Override
    protected void postPhaseProcessing(PuertoRicoGameState state) {
        // and we refill the ship with colonists (if able)
        if (state.getColonistsOnShip() > 0) {
            throw new AssertionError("We should not have any colonists on the ship at this point! ");
        }
        int totalVacancies = IntStream.range(0, state.getNPlayers()).map(i -> state.getPlayerBoard(i).getTownVacancies()).sum();
        int colonists = Math.max(state.getNPlayers(), totalVacancies);
        if (colonists > state.getColonistsInSupply()) {
            colonists = state.getColonistsInSupply();
            state.setGameEndTriggered();
        }
        state.changeColonistsOnShip(colonists);
        state.changeColonistsInSupply(-colonists);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        // We get all the plantations the player has which are not occupied
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        PRPlayerBoard playerBoard = state.getPlayerBoard(currentPlayer);
        // we first to check here to see if the remaining colonists can be used to occupy all remaining buildings and plantations
        // or if we have any colonists left to allocate
        if (allocateAllColonistsIfDeterministic(state, currentPlayer)) {
            List<AbstractAction> retValue = new ArrayList<>();
            retValue.add(new DoNothing());
            return retValue;
        }

        List<AbstractAction> retValue = playerBoard.getPlantations().stream().filter(p -> !p.isOccupied())
                .map(p -> new OccupyPlantation(p.crop))
                .distinct()
                .collect(Collectors.toList());
        retValue.addAll(playerBoard.getBuildings().stream().filter(b -> b.getOccupation() < b.buildingType.capacity)
                .map(b -> new OccupyBuilding(b.buildingType))
                .distinct()
                .collect(Collectors.toList()));
        if (retValue.isEmpty())
            retValue.add(new DoNothing());

        return retValue;
    }

    @Override
    public Mayor copy() {
        return new Mayor(this);
    }
}
