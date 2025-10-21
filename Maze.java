import java.io.Serializable;
import java.util.Random;

/**
 * Represents the maze in the game.
 * This class handles maze generation and provides methods to check wall and exit locations.
 */
public class Maze implements Serializable {
    private static final long serialVersionUID = 1L; // Recommended for Serializable classes
    private final int width;
    private final int height;
    private final boolean[][] walls;
    private final Position exitPosition;
    private static final Random random = new Random();

    public Maze(int width, int height) {
        this.width = width;
        this.height = height;
        this.walls = new boolean[height][width];
        generateMaze();
        this.exitPosition = findExit();
    }

    /**
     * Generates a basic maze using a simple randomized algorithm.
     * Starts with a grid of walls and carves out paths.
     */
    public void generateMaze() {
        // Start with all walls
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                walls[y][x] = true;
            }
        }

        // Carve paths using a simple random walk from a starting point
        int startX = 1;
        int startY = 1;
        walls[startY][startX] = false; // Carve out the starting point
        carvePath(startX, startY);
    }

    /**
     * Recursive method to carve a path through the maze.
     * This is a simplified version of a depth-first search (DFS) algorithm.
     */
    private void carvePath(int x, int y) {
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        shuffle(directions); // Randomize the directions to create a less predictable maze

        for (int[] direction : directions) {
            int newX = x + direction[0] * 2;
            int newY = y + direction[1] * 2;
            if (newX >= 0 && newX < width && newY >= 0 && newY < height && walls[newY][newX]) {
                walls[y + direction[1]][x + direction[0]] = false; // Carve path
                walls[newY][newX] = false; // Carve next cell
                carvePath(newX, newY);
            }
        }
    }

    /**
     * Helper method to shuffle an array.
     */
    private void shuffle(int[][] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int[] temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

    /**
     * Finds a valid exit position for the maze.
     * It looks for the first empty cell on the maze's border.
     */
    private Position findExit() {
        // Find an empty space on the right-most wall
        for (int y = height - 2; y > 0; y--) {
            if (!walls[y][width - 2]) {
                return new Position(width - 1, y);
            }
        }
        return new Position(width - 1, height - 2);
    }

    /**
     * Checks if a given position is a wall.
     */
    public boolean isWall(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return true; // Treat out-of-bounds as a wall
        }
        return walls[y][x];
    }

    public Position getExitPosition() {
        return exitPosition;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Converts the maze grid to a string representation for printing.
     * This is crucial for the console-based display.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isWall(x, y)) {
                    sb.append("#");
                } else {
                    sb.append(" ");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
