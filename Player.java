import java.io.Serializable;

/**
 * Represents a player in the game.
 * This class is a simple data structure to hold a player's state.
 */
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String name;
    private Position position;

    public Player(String id, String name, Position startPosition) {
        this.id = id;
        this.name = name;
        this.position = startPosition;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position newPosition) {
        this.position = newPosition;
    }
}
