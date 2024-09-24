package games.saboteur.components;

import core.components.Component;
import core.components.GridBoard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PartialObservableGridBoard<T extends Component> extends GridBoard<T>
{
    //visibility of Board for each player
    private boolean[] gridBoardVisibility;

    //visibility of cell in Grid for each player, order corresponds to playerID
    private List<boolean[][]> elementVisibility = new ArrayList<>();
//--------------------------------------------------------------------------------------------------//
//region Constructor
    public PartialObservableGridBoard(int width, int height, int nPlayers, boolean defaultValue)
    {
        //WHY IS THIS HEIGHT THEN WIDTH IN THE SUPER CALL?
        //this.grid = new Component[height][width];
        super(width, height);

        //filling Visibility for each cell in grid
        boolean[][] visibilityGrid = new boolean[height][width];
        for(int i = 0; i < height; i++)
        {
            Arrays.fill(visibilityGrid[i], defaultValue);
        }
        for(int i = 0; i < nPlayers; i++)
        {
            elementVisibility.add(visibilityGrid);
        }

        //filling Visibility of overall grid for each player
        gridBoardVisibility = new boolean[nPlayers];
        Arrays.fill(gridBoardVisibility, defaultValue);
    }

    public PartialObservableGridBoard(Component[][] grid)
    {
        super(grid);
    }
//endregion
//--------------------------------------------------------------------------------------------------//
//region isVisible Functions
    public boolean isBoardVisible(int playerID)
    {
        CheckBoardVisibilityArgument(playerID);

        if(!gridBoardVisibility[playerID])
        {
            return false;
        }

        return gridBoardVisibility[playerID];
    }

    public boolean isCellVisible(int x, int y, int playerID)
    {
        CheckBoardVisibilityArgument(playerID);
        CheckGridRangeArgument(x,y);

        return elementVisibility.get(playerID)[y][x];
    }
//endregion
//--------------------------------------------------------------------------------------------------//
//region setters
    public void setElementVisibility(int x, int y, int playerID, boolean value)
    {
        CheckBoardVisibilityArgument(playerID);

        elementVisibility.get(playerID)[y][x] = value;
    }

    public void setGridBoardVisibility(int playerID, boolean value)
    {
        CheckBoardVisibilityArgument(playerID);
        gridBoardVisibility[playerID] = value;
    }
//endregion
//--------------------------------------------------------------------------------------------------//
//region Argument Checks
    public void CheckBoardVisibilityArgument(int playerID)
    {
        if (playerID < 0 || playerID >= gridBoardVisibility.length)
            throw new IllegalArgumentException("playerID " + playerID + " needs to be in range [0," + (gridBoardVisibility.length - 1) + "]");
    }

    public void CheckGridRangeArgument(int x, int y)
    {
        if(getElement(x, y) == null)
        {
            throw new IllegalArgumentException("Cell Coordinates need to be between [0," + getWidth() + "] [0," + getHeight() + "]");
        }
    }
//endregion
//--------------------------------------------------------------------------------------------------//
    @Override
    public PartialObservableGridBoard<T> copy()
    {
        PartialObservableGridBoard<T> copy = new PartialObservableGridBoard<>(getGridValues());

        copy.gridBoardVisibility = gridBoardVisibility.clone();
        return copy;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
/*        for(int i = 0; i < gridBoardVisibility.length; i++)
        {
            sb.append("Player ").append(i).append(" visibility: ").append(gridBoardVisibility[i]).append("\n");
        }*/
        for(int j = 0; j < getHeight(); j++)
        {
            for(int k = 0; k < getWidth(); k++)
            {
                PathCard pathCard = (PathCard) getElement(k, j);
                if(pathCard != null)
                {
                    sb.append(pathCard.getString());
                }
                else
                {
                    sb.append("░");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public String toString(int x, int y)
    {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < gridBoardVisibility.length; i++)
        {
            sb.append("Player ").append(i).append(" visibility: ").append(gridBoardVisibility[i]).append("\n");
        }
        for(int j = 0; j < getHeight(); j++)
        {
            for(int k = 0; k < getWidth(); k++)
            {
                if(j == y && x == k)
                {
                    sb.append("X");
                    continue;
                }
                PathCard pathCard = (PathCard) getElement(k, j);
                if(pathCard != null)
                {
                    sb.append(pathCard.getString());
                }
                else
                {
                    sb.append("░");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

}
