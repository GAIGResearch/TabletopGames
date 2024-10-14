import core.AbstractGameState;
import core.components.Component;
import core.components.GridBoard;
import games.tictactoe.TicTacToeGameState;
import utilities.Vector2D;

import java.util.List;

public class TicTacToeEvaluator002 {

public double evaluateState(AbstractGameState gameState, int playerId) {
TicTacToeGameState ticTacToeGameState = (TicTacToeGameState) gameState;
GridBoard gridBoard = ticTacToeGameState.getGridBoard();
int boardSize = gridBoard.getWidth();
Component playerToken = ticTacToeGameState.getPlayerToken(playerId);
if (checkWin(gridBoard, playerToken)) {
return 1.0;
}
int opponentId = (playerId + 1) % 2;
Component opponentToken = ticTacToeGameState.getPlayerToken(opponentId);
if (checkWin(gridBoard, opponentToken)) {
return 0.0;
}
double threatScore = 0.0;
threatScore += checkPotentialWin(gridBoard, playerToken);
threatScore -= checkPotentialWin(gridBoard, opponentToken);
threatScore = Math.max(0.0, Math.min(1.0, threatScore));
return threatScore;
}

private boolean checkWin(GridBoard gridBoard, Component token) {
for (int row = 0; row < gridBoard.getHeight(); row++) {
if (gridBoard.getElement(row, 0).equals(token) &&
gridBoard.getElement(row, 1).equals(token) &&
gridBoard.getElement(row, 2).equals(token)) {
return true;
}
}
for (int col = 0; col < gridBoard.getWidth(); col++) {
if (gridBoard.getElement(0, col).equals(token) &&
gridBoard.getElement(1, col).equals(token) &&
gridBoard.getElement(2, col).equals(token)) {
return true;
}
}
if (gridBoard.getElement(0, 0).equals(token) &&
gridBoard.getElement(1, 1).equals(token) &&
gridBoard.getElement(2, 2).equals(token)) {
return true;
}
if (gridBoard.getElement(0, 2).equals(token) &&
gridBoard.getElement(1, 1).equals(token) &&
gridBoard.getElement(2, 0).equals(token)) {
return true;
}
return false;
}

private double checkPotentialWin(GridBoard gridBoard, Component token) {
double score = 0.0;
int occupiedSpaces = 0;
for (int row = 0; row < gridBoard.getHeight(); row++) {
occupiedSpaces = 0;
for (int col = 0; col < gridBoard.getWidth(); col++) {
if (gridBoard.getElement(row, col).equals(token)) {
occupiedSpaces++;
}
}
if (occupiedSpaces == 2) {
score += 0.5;
}
if (occupiedSpaces == 3) {
score += 1.0;
}
}
for (int col = 0; col < gridBoard.getWidth(); col++) {
occupiedSpaces = 0;
for (int row = 0; row < gridBoard.getHeight(); row++) {
if (gridBoard.getElement(row, col).equals(token)) {
occupiedSpaces++;
}
}
if (occupiedSpaces == 2) {
score += 0.5;
}
if (occupiedSpaces == 3) {
score += 1.0;
}
}
occupiedSpaces = 0;
for (int i = 0; i < gridBoard.getWidth(); i++) {
if (gridBoard.getElement(i, i).equals(token)) {
occupiedSpaces++;
}
}
if (occupiedSpaces == 2) {
score += 0.5;
}
if (occupiedSpaces == 3) {
score += 1.0;
}
occupiedSpaces = 0;
for (int i = 0; i < gridBoard.getWidth(); i++) {
if (gridBoard.getElement(i, gridBoard.getWidth() - i - 1).equals(token)) {
occupiedSpaces++;
}
}
if (occupiedSpaces == 2) {
score += 0.5;
}
if (occupiedSpaces == 3) {
score += 1.0;
}
return score;
}
}
