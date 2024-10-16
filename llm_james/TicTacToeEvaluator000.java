import core.AbstractGameState;
import core.components.Component;
import core.components.GridBoard;
import games.tictactoe.TicTacToeGameState;
import utilities.Vector2D;

import java.util.List;

public class TicTacToeEvaluator000 {

public double evaluateState(AbstractGameState gameState, int playerId) {
TicTacToeGameState ticTacToeGameState = (TicTacToeGameState) gameState;
GridBoard gridBoard = ticTacToeGameState.getGridBoard();
Component playerToken = ticTacToeGameState.getPlayerToken(playerId);
Component opponentToken = ticTacToeGameState.getPlayerToken(1 - playerId);

if (hasWon(gridBoard, playerToken)) {
return 1.0;
} else if (hasWon(gridBoard, opponentToken)) {
return 0.0;
}

if (hasWon(gridBoard, opponentToken)) {
return 0.0;
}

List<Vector2D> opponentWinningMoves = getWinningMoves(gridBoard, opponentToken);
if (!opponentWinningMoves.isEmpty()) {
return 0.1;
}

List<Vector2D> playerWinningMoves = getWinningMoves(gridBoard, playerToken);
if (!playerWinningMoves.isEmpty()) {
return 0.9;
}

double playerScore = countPotentialWins(gridBoard, playerToken);
double opponentScore = countPotentialWins(gridBoard, opponentToken);
return (playerScore / (playerScore + opponentScore));
}

private boolean hasWon(GridBoard gridBoard, Component token) {
for (int row = 0; row < gridBoard.getHeight(); row++) {
if (gridBoard.getElement(0, row) == token && gridBoard.getElement(1, row) == token && gridBoard.getElement(2, row) == token) {
return true;
}
}
for (int col = 0; col < gridBoard.getWidth(); col++) {
if (gridBoard.getElement(col, 0) == token && gridBoard.getElement(col, 1) == token && gridBoard.getElement(col, 2) == token) {
return true;
}
}
if (gridBoard.getElement(0, 0) == token && gridBoard.getElement(1, 1) == token && gridBoard.getElement(2, 2) == token) {
return true;
}
if (gridBoard.getElement(0, 2) == token && gridBoard.getElement(1, 1) == token && gridBoard.getElement(2, 0) == token) {
return true;
}
return false;
}

private List<Vector2D> getWinningMoves(GridBoard gridBoard, Component token) {
List<Vector2D> winningMoves = gridBoard.getEmptyCells(token);
return winningMoves;
}

private double countPotentialWins(GridBoard gridBoard, Component token) {
double score = 0.0;
return score;
}
}
