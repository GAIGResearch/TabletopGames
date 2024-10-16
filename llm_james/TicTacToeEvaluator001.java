import core.AbstractGameState;
import core.components.Component;
import core.components.GridBoard;
import games.tictactoe.TicTacToeGameState;
import utilities.Vector2D;

import java.util.List;

public class TicTacToeEvaluator001 {

public double evaluateState(AbstractGameState gameState, int playerId) {
TicTacToeGameState ticTacToeGameState = (TicTacToeGameState) gameState;
GridBoard gridBoard = ticTacToeGameState.getGridBoard();
if (checkWin(gridBoard, ticTacToeGameState.getPlayerToken(playerId))) {
return 1.0;
}
if (checkWin(gridBoard, ticTacToeGameState.getPlayerToken(1 - playerId))) {
return 0.0;
}
int playerWinOpportunities = countWinningLines(gridBoard, ticTacToeGameState.getPlayerToken(playerId));
int opponentWinOpportunities = countWinningLines(gridBoard, ticTacToeGameState.getPlayerToken(1 - playerId));
if (playerWinOpportunities > 0 && opponentWinOpportunities == 0) {
return 0.75;
} else if (playerWinOpportunities == 0 && opponentWinOpportunities > 0) {
return 0.25;
} else if (playerWinOpportunities > opponentWinOpportunities) {
return 0.6;
} else if (opponentWinOpportunities > playerWinOpportunities) {
return 0.4;
} else {
return 0.5;
}
}

private boolean checkWin(GridBoard gridBoard, Component playerToken) {
for (int i = 0; i < gridBoard.getHeight(); i++) {
if (gridBoard.getElement(0, i) == playerToken &&
gridBoard.getElement(1, i) == playerToken &&
gridBoard.getElement(2, i) == playerToken) {
return true;
}
}
for (int j = 0; j < gridBoard.getWidth(); j++) {
if (gridBoard.getElement(j, 0) == playerToken &&
gridBoard.getElement(j, 1) == playerToken &&
gridBoard.getElement(j, 2) == playerToken) {
return true;
}
}
if (gridBoard.getElement(0, 0) == playerToken &&
gridBoard.getElement(1, 1) == playerToken &&
gridBoard.getElement(2, 2) == playerToken) {
return true;
}
if (gridBoard.getElement(0, 2) == playerToken &&
gridBoard.getElement(1, 1) == playerToken &&
gridBoard.getElement(2, 0) == playerToken) {
return true;
}
return false;
}

private int countWinningLines(GridBoard gridBoard, Component playerToken) {
int count = 0;
for (int i = 0; i < gridBoard.getHeight(); i++) {
if (countWinningLine(gridBoard, playerToken, 0, i, 1, i, 2, i)) {
count++;
}
}
for (int j = 0; j < gridBoard.getWidth(); j++) {
if (countWinningLine(gridBoard, playerToken, j, 0, j, 1, j, 2)) {
count++;
}
}
if (countWinningLine(gridBoard, playerToken, 0, 0, 1, 1, 2, 2)) {
count++;
}
if (countWinningLine(gridBoard, playerToken, 0, 2, 1, 1, 2, 0)) {
count++;
}
return count;
}

private boolean countWinningLine(GridBoard gridBoard, Component playerToken, int x1, int y1, int x2, int y2, int x3, int y3) {
if (gridBoard.getElement(x1, y1) == playerToken &&
gridBoard.getElement(x2, y2) == playerToken &&
gridBoard.getElement(x3, y3) == playerToken) {
return true;
}
return false;
}
}
