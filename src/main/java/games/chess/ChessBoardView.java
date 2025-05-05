package games.chess;

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import games.chess.components.*;
import games.chess.components.ChessPiece.ChessPieceType;
import core.Game;

import static gui.GUI.defaultItemSize;

public class ChessBoardView extends JComponent {

    ChessGameState cgs;
    ChessForwardModel fm;
    int width, height;
    int offsetX = defaultItemSize;
    int offsetY = defaultItemSize;

    ChessParameters params = new ChessParameters();

    int dotSize = 6;
    Color[] colors = new Color[] {
            new Color(238,238,210), //white
            new Color(118,150,86), //black
            new Color(212,220,139), // white highlight
            new Color(152, 176, 77), // black highlight
    };


    Point mousePos;
    ChessPiece highlight;
    int[] selectedPos;
    int[] targetPos;
    ChessPiece selectedPiece;
    Game game;
    boolean dragging = false;
    int[] mouseOverPos;
    boolean promoting = false;
    ChessPieceType selectedPromotion;


    public ChessBoardView(ChessGameState cgs, Game game) {
        this.cgs = cgs;
        this.game = game;
        this.width = 8 * defaultItemSize;
        this.height = 8 * defaultItemSize;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && game.isHumanToMove() && !promoting) {
                    selectedPos = new int[]{(e.getX()-offsetX)/defaultItemSize, 7 - (e.getY()-offsetY)/defaultItemSize};
                    selectedPiece = cgs.getPiece(selectedPos[0], selectedPos[1]);
                    mousePos = new Point(e.getX(), e.getY());
                    mouseOverPos = new int[]{(mousePos.x-offsetX)/defaultItemSize, 7-(mousePos.y-offsetY)/defaultItemSize};
                    dragging = true;
                    if (selectedPiece != null && selectedPiece.getOwnerId() != cgs.getCurrentPlayer()) {
                        selectedPiece = null;
                        selectedPos = null;
                        targetPos = null;
                        mouseOverPos = null;
                        dragging = false;
                    }
                }
            }
        });
        addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON1 && selectedPiece != null && game.isHumanToMove() && !promoting) {
                            targetPos = new int[]{(e.getX()-offsetX)/defaultItemSize, 7 - (e.getY()-offsetY)/defaultItemSize};
                            dragging = false;
                            repaint((selectedPos[0]-1) * defaultItemSize, (7 - selectedPos[1]-1) * defaultItemSize, 2*defaultItemSize, 2*defaultItemSize);
                            repaint((targetPos[0]-1) * defaultItemSize, (7 - targetPos[1]-1) * defaultItemSize, 2*defaultItemSize, 2*defaultItemSize);
                            if (selectedPiece.getChessPieceType() == ChessPiece.ChessPieceType.PAWN && (targetPos[1] == 7 || targetPos[1] == 0)) {
                                //Show promotion options
                                promoting = true;
                            }
                        }
                    }
                }
        );
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (promoting) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        int direction = selectedPiece.getOwnerId() == 0 ? 1 : -2;
                        ChessPieceType[] pieceTypes = {ChessPieceType.QUEEN, ChessPieceType.ROOK, ChessPieceType.BISHOP, ChessPieceType.KNIGHT};
                        for (int i = 0; i < pieceTypes.length; i++) {
                            int xC = offsetX + targetPos[0] * defaultItemSize + (i - 1) * defaultItemSize/2;
                            int yC = offsetY + (7-targetPos[1]) * defaultItemSize- direction * defaultItemSize/2;
                            if (e.getX() >= xC && e.getX() <= xC + defaultItemSize/2 && e.getY() >= yC && e.getY() <= yC + defaultItemSize/2) {
                                selectedPromotion = pieceTypes[i];
                                promoting = false;
                                break;
                            }
                        }

                    }
                    promoting = false;
                    repaint();
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging) {
                    mousePos = new Point(e.getX(), e.getY());
                    mouseOverPos = new int[]{(mousePos.x-offsetX)/defaultItemSize, 7 - (mousePos.y-offsetY)/defaultItemSize};
                    repaint(mousePos.x - defaultItemSize, mousePos.y - defaultItemSize, 2*defaultItemSize, 2*defaultItemSize);
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D)g0;
        drawBoard(g);
        if (promoting) {
            showPromotionOptions(g, selectedPiece, targetPos);
        }

    }

    public void drawBoard(Graphics2D g) {
        // Draw cells
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                // Add number labels to the left of the board
                if (i == 0) {
                    g.setColor(Color.BLACK);
                    g.drawString(String.valueOf(j+1), offsetX - defaultItemSize/2, offsetY + (7-j) * defaultItemSize + defaultItemSize/2);
                }
                int colorIndex = (i + j + 1) % 2;
                if (selectedPos != null && selectedPiece != null) {
                    if ((selectedPos[0] == i && selectedPos[1] == j) || (mouseOverPos != null && mouseOverPos[0] == i && mouseOverPos[1] == j)) {
                        colorIndex += 2;
                    }
                }
                Color color= colors[colorIndex];
                int xC = offsetX + i * defaultItemSize;
                int yC = offsetY + (7-j) * defaultItemSize; // Invert y axis for GUI
                drawCell(g, cgs.getPiece(i, j), xC, yC, color);
            }
            //Add letter labels to the bottom of the board
            g.setColor(Color.BLACK);
            g.drawString(String.valueOf((char)('a' + i)), offsetX + i * defaultItemSize + defaultItemSize/2, offsetY + 8 * defaultItemSize + defaultItemSize/2);
        }
        // Draw pieces
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                ChessPiece piece = cgs.getPiece(i, j);
                if (piece != null) {
                    int xC = offsetX + i * defaultItemSize;
                    int yC = offsetY + (7-j) * defaultItemSize; // Invert y axis for GUI
                    drawPiece(g, piece, xC, yC);
                }
            }
        }
        //Draw selected piece
        if (selectedPiece != null) {
            int xC = offsetX + selectedPos[0] * defaultItemSize;
            int yC = offsetY + (7-selectedPos[1]) * defaultItemSize; // Invert y axis for GUI
            drawSelectedPiece(g, selectedPiece, xC, yC);
        }
    }

    private void drawCell(Graphics2D g, ChessPiece piece, int x, int y, Color color) {
        // Draw the cell background
        g.setColor(color);
        g.fillRect(x, y, defaultItemSize, defaultItemSize);
    }
    
    private void drawPiece(Graphics2D g, ChessPiece piece, int x, int y) {
        int[] pos = piece.getPosition();
        String pieceType = piece.toString();
        String path = params.dataPathString + pieceType + ".png";
        ImageIcon icon = new ImageIcon(path);
        Image image = icon.getImage();
        Image scaledImage = image.getScaledInstance(defaultItemSize, defaultItemSize, Image.SCALE_SMOOTH);
        icon = new ImageIcon(scaledImage);
        
        if (!dragging || pos[0] != selectedPos[0] || pos[1] != selectedPos[1]) {
            // Draw the piece at its board position
            g.drawImage(icon.getImage(), x, y, null);
        }
    }

    private void drawSelectedPiece(Graphics2D g, ChessPiece piece, int x, int y) {
        String pieceType = piece.toString();
        String path = params.dataPathString + pieceType + ".png";
        ImageIcon icon = new ImageIcon(path);
        Image image = icon.getImage();
        Image scaledImage = image.getScaledInstance(defaultItemSize, defaultItemSize, Image.SCALE_SMOOTH);
        icon = new ImageIcon(scaledImage);
        if (dragging) {
            g.drawImage(icon.getImage(), mousePos.x - defaultItemSize/2, mousePos.y - defaultItemSize/2, null);
        }
    }

    private void showPromotionOptions(Graphics2D g, ChessPiece piece, int[] targetPos) {
        // Show promotion options (images on the board)
        int direction = piece.getOwnerId() == 0 ? 1 : -2;
        String[] playerColor = {"w", "b"};
        String colorString = playerColor[piece.getOwnerId()];
        String[] pieceTypes = {"Q", "R", "B", "N"}; // Queen, Rook, Bishop, Knight
        for (int i = 0; i < pieceTypes.length; i++) {
            String path = params.dataPathString + colorString + pieceTypes[i] + ".png";
            ImageIcon icon = new ImageIcon(path);
            Image image = icon.getImage();
            Image scaledImage = image.getScaledInstance(defaultItemSize/2, defaultItemSize/2, Image.SCALE_SMOOTH);
            icon = new ImageIcon(scaledImage);
            int xC = offsetX + targetPos[0] * defaultItemSize + (i - 1) * defaultItemSize/2;
            int yC = offsetY + (7-targetPos[1]) * defaultItemSize - direction * defaultItemSize/2;
            g.drawImage(icon.getImage(), xC, yC, null);
        }
    }





    public void updateGameState(ChessGameState cgs) {
        this.cgs = cgs;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public ChessPiece getHighlight() {
        return highlight;
    }
    public int[] getSelectedPos() {
        return selectedPos;
    }
    public int[] getTargetPos() {
        return targetPos;
    }
    public ChessPiece getSelectedPiece() {
        return selectedPiece;
    }

}