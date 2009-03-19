package Examples;
import java.awt.*;
import java.awt.event.MouseEvent;

class LifeCanvas extends Canvas {
    int           cellRows, cellColumns;
    int           cellSizeX = 3, cellSizeY = 3;
    int           currentX  = -1, currentY = -1;
    byte          cells[];
    Image         imageBuffer;
    Graphics      imageGC;
    
    public String toString() {return "LifeCanvas";}

    //////////////////////////////////////////////
    // constructors
    //////////////////////////////////////////////
    public LifeCanvas ()             {this (5);}
    public LifeCanvas (int cellSize) {this (cellSize, 100,100);}
    public LifeCanvas (int cellSize, int initialColumns, int initialRows) {
        super ();
        cellSizeX      = cellSize;
        cellSizeY      = cellSize;
        cellColumns    = initialColumns;
        cellRows       = initialRows;
        doClear();
    }

    //////////////////////////////////////////////
    // functions for mouse input
    //////////////////////////////////////////////

    void mouseDownToggle (MouseEvent e) {currentX=-1; toggleMousePoint (e.getX(),e.getY());}
    void mouseDragToggle (MouseEvent e) {             toggleMousePoint (e.getX(),e.getY());}

    private void toggleMousePoint (int x, int y) {
        if (imageBuffer==null) createCells ();
        if (imageBuffer==null) return;
        invertAt (x,y);
    }

    //////////////////////////////////////////////
    // functions for painting and the like
    //////////////////////////////////////////////
    public void validate () {

        super.validate ();
        int newWidth = getSize().width, newHeight = getSize().height;

        if (newWidth==0
        || newHeight==0)  return;

        if (imageBuffer == null) {
            createCells (newWidth, newHeight);
            repaint ();
        } else {
            if (   imageBuffer.getWidth(null)  != newWidth 
                || imageBuffer.getHeight(null) != newHeight) {
                resizeCells (newWidth, newHeight);
                repaint ();
            }
        }
    }

    public void cleanUp () {
        if (imageBuffer == null) return;
        imageGC.dispose();
        imageBuffer.flush();
        imageBuffer = null;
    }

    /** randomize the canvas with the given density */
    public synchronized void doRandomize (double density) {
        if (imageBuffer==null) createCells ();
        if (imageBuffer==null) return;
        imageGC.clearRect (0,0,imageBuffer.getWidth (null)-1,
                               imageBuffer.getHeight(null)-1);
        for (int i=0; i<cells.length; i++) {
            if (density >  0 
            &&  density >= Math.random()) {
                cells[i] = 1;
                paintCellInImage(i%cellColumns, i/cellColumns, true);
            } else {
                cells[i] = 0;
            }
        }
        repaint();
    }
    public synchronized void doRandomize() {doRandomize(0.3);}
    public synchronized void doClear    () {doRandomize(0.0);}

    public Dimension getPreferredSize () {
        return new Dimension (cellSizeX*cellColumns, cellSizeY*cellRows);
    }

    public void update(Graphics g) {paint (g);}
    public void paint (Graphics g) {
        if (imageBuffer == null) createCells ();
        if (imageBuffer == null) {
            g.setColor(Color.red);
            g.fillRect(0,0,getSize().width-1,getSize().height-1);
            return;
        }
        g.drawImage (imageBuffer, 0, 0, null);
    }

    void paintCellInImage(int cellX, int cellY, boolean isBlack) {
        if (isBlack)
             imageGC. fillRect(cellX*cellSizeX, cellY*cellSizeY, cellSizeX, cellSizeY);
        else imageGC.clearRect(cellX*cellSizeX, cellY*cellSizeY, cellSizeX, cellSizeY);
    }

    //////////////////////////////////////////////
    // functions implementing the life algorithm
    //////////////////////////////////////////////

    private synchronized void createCells () {
        createCells (getSize().width, getSize().height);
    }

    private synchronized void createCells (int width, int height) {
        if (width==0
        || height==0)
        {
            return; // this always happens in the beginning on Win95 
        }
        cellColumns = width  / cellSizeX;
        cellRows    = height / cellSizeY;

        cells       = new byte[cellColumns * cellRows];

        imageBuffer = createImage (width, height);
        if (imageBuffer == null) return;

        imageGC     = imageBuffer.getGraphics();
    }


    private synchronized void resizeCells (int newWidth, int newHeight) {
        if (newWidth==0
        || newHeight==0)
        {
            System.out.println("resizeCells fails: newWidth = "+newWidth+", newHeight= "+newHeight);
            return;
        }

        Image newImageBuffer = createImage (newWidth, newHeight);

        if (newImageBuffer == null) return;

        Graphics g = newImageBuffer.getGraphics();

        int newCellColumns      = newWidth  / cellSizeX;
        int newCellRows         = newHeight / cellSizeY;
        byte newCells []        = new byte[newCellColumns * newCellRows];

        int newCellColumnOffset = (newCellColumns - cellColumns) / 2;
        int newCellRowOffset    = (newCellRows    - cellRows)    / 2;

        int cellIndex = 0;
        for (int cellY = 0; cellY < cellRows; cellY++) {
            int newCellY = cellY + newCellRowOffset;
            if ((newCellY >= 0) && (newCellY < newCellRows)) {

              int newCellIndex = newCellY * newCellColumns + newCellColumnOffset;
              for (int cellX = 0; cellX < cellColumns; cellX++) {
                int newCellX = cellX + newCellColumnOffset;
                if (   (newCellX >= 0) && (newCellX < newCellColumns)
                    && (cells [cellIndex] > 0)) {
                    newCells[newCellIndex] = 1;
                    g.fillRect (newCellX*cellSizeX, newCellY*cellSizeY, 
                                cellSizeX, cellSizeY);
                }
                cellIndex++;
                newCellIndex++;
              }
            } else {
                cellIndex += cellColumns;
            }
        }

        cells       = newCells;
        cellColumns = newCellColumns;
        cellRows    = newCellRows;

        if (newImageBuffer != null) {
            imageGC.dispose();
            imageBuffer.flush();
            imageBuffer = newImageBuffer;
            imageGC     = g;
        }
    }

    private void invertAt (int x,int y) {invertCell (x/cellSizeX, y/cellSizeY);}

    private synchronized void invertCell (int cellX, int cellY) {
        if (     cellX < 0 || cellX >= cellColumns 
              || cellY < 0 || cellY >= cellRows)
            return;

        if (cellX == currentX 
        &&  cellY == currentY) return;

        int cellIndex     = cellColumns * cellY + cellX;
        byte oldCellState = cells [cellIndex];
        byte newCellState = (byte) (1 - oldCellState);
        cells [cellIndex] = newCellState;
        currentX = cellX;
        currentY = cellY;
        if (imageBuffer == null) return;

        paintCellInImage (cellX, cellY, newCellState>0);
        repaint(cellX*cellSizeX, cellY*cellSizeY, cellSizeX, cellSizeY);
    }

    synchronized void calculateGeneration () {
        if (cellRows <= 2 || cellColumns <= 2) return;
    
        byte neighbors [] = new byte [ cellColumns * cellRows ];

        int prevLineIndex = 0;
        int thisLineIndex = cellColumns;
        int nextLineIndex = 2 * cellColumns;

        int cellIndex = cellColumns + 1;
        for (int cellY = 1; cellY < cellRows - 1; cellY++) {
            for (int cellX = 1; cellX < cellColumns - 1; cellX++) {
                if (cells [cellIndex] > 0) {
                    ++neighbors[prevLineIndex];
                    ++neighbors[prevLineIndex + 1];
                    ++neighbors[prevLineIndex + 2];
                    ++neighbors[thisLineIndex];
                    ++neighbors[thisLineIndex + 2];
                    ++neighbors[nextLineIndex];
                    ++neighbors[nextLineIndex + 1];
                    ++neighbors[nextLineIndex + 2];
                }
                cellIndex++;
                prevLineIndex++;
                thisLineIndex++;
                nextLineIndex++;
            }
            cellIndex     += 2;
            prevLineIndex += 2;
            thisLineIndex += 2;
            nextLineIndex += 2;
        }

        cellIndex = cellColumns + 1;
        for (int cellY = 1; cellY < cellRows - 1; cellY++) {
            for (int cellX = 1; cellX < cellColumns - 1; cellX++) {

                byte cellNeighbors = neighbors[cellIndex];
                if (cells [cellIndex] > 0) {
                    if (cellNeighbors < 2 
                    ||  cellNeighbors > 3) {
                       cells[cellIndex]=0;
                       paintCellInImage (cellX, cellY, false);
                    }
                } else {
                    if (cellNeighbors == 3) {
                       cells[cellIndex] = 1;
                       paintCellInImage (cellX, cellY, true);
                    }
                }
                cellIndex++;
            }
            cellIndex += 2;
        }
    }
}
