import java.io.Serializable;

/**
 * A simple record to represent a coordinate (x, y) on the maze grid.
 * Records are immutable data carriers, perfect for this use case.
 * It implements Serializable to be sent over the network.
 */
public record Position(int x, int y) implements Serializable {
    private static final long serialVersionUID = 1L;
}
