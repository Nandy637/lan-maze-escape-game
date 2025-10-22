import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Scanner;

/**
 * The server for the LAN Maze Escape Game.
 * This class handles client connections, game logic, and broadcasting the game state.
 */
public class Server {
    private static final int PORT = 12345;
    private static final int MAZE_WIDTH = 21;
    private static final int MAZE_HEIGHT = 11;
    private static volatile Maze maze = new Maze(MAZE_WIDTH, MAZE_HEIGHT);
    private static final Map<String, Player> players = new ConcurrentHashMap<>();
    private static final Map<String, ObjectOutputStream> clientOutputs = new ConcurrentHashMap<>();
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) throws IOException {
        System.out.println("Server started. Waiting for clients on port " + PORT + "...");

        // Start earthquake timer (every 30 seconds)
        Timer earthquakeTimer = new Timer();
        earthquakeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                triggerEarthquake();
            }
        }, 30000, 30000);

        try (ServerSocket listener = new ServerSocket(PORT)) {
            while (true) {
                pool.execute(new ClientHandler(listener.accept()));
            }
        }
    }

    /**
     * Broadcasts the current game state to all connected clients.
     * This method is synchronized to prevent concurrent modification of the clientOutputs map.
     */
    public static synchronized void broadcastGameState() {
        // Send fresh objects to all clients (fixes Java serialization caching)
        for (ObjectOutputStream out : clientOutputs.values()) {
            try {
                out.reset(); // Force fresh object state
                out.writeObject(maze);
                out.writeObject(players);
                out.flush();
            } catch (IOException e) {
                System.err.println("Error broadcasting game state: " + e.getMessage());
            }
        }
    }

    /**
     * Triggers an earthquake that regenerates the maze.
     */
    private static void triggerEarthquake() {
        System.out.println("Earthquake! The maze is shifting...");
        maze = new Maze(MAZE_WIDTH, MAZE_HEIGHT);
        broadcastGameState();
    }
    
    /**
     * Inner class to handle individual client connections and game commands.
     */
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private String playerId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
              // Deadlock fix: Create ObjectOutputStream BEFORE ObjectInputStream
              try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                  ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                
                // Read and set player name
                String playerName = null;
                try {
                    Object obj = in.readObject();
                    if (obj instanceof String) {
                        playerName = (String) obj;
                    } else {
                        System.err.println("Received an invalid object instead of player name. Terminating connection.");
                        return;
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Invalid object received for player name: " + e.getMessage());
                    return; // Terminate this handler
                }
                
                this.playerId = "player_" + System.nanoTime(); // Generate unique ID
                
                Position startPos = findStartLocation();
                players.put(playerId, new Player(playerId, playerName, startPos));
                clientOutputs.put(playerId, out);

                System.out.println("Player " + playerName + " connected.");
                broadcastGameState();

                // Keep connection alive and listen for commands
                while (!socket.isClosed()) {
                    try {
                        String command = (String) in.readObject();
                        processCommand(command);
                        broadcastGameState();
                    } catch (ClassNotFoundException e) {
                        System.err.println("Invalid object received: " + e.getMessage());
                        break; // Exit loop on invalid data
                    } catch (IOException e) {
                        // Connection closed by client
                        break;
                    }
                }

            } catch (IOException e) {
                System.err.println("Player " + playerId + " disconnected: " + e.getMessage());
            } finally {
                // Clean up resources
                players.remove(playerId);
                clientOutputs.remove(playerId);
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }
                broadcastGameState();
                System.out.println("Player " + playerId + " cleanup completed.");
            }
        }

        /**
         * Processes a command received from a client.
         */
        private void processCommand(String command) {
            Player player = players.get(playerId);
            if (player == null) return;

            Position currentPos = player.getPosition();
            Position newPos = currentPos;
            Position exitPos = maze.getExitPosition();

            // Debug: print received command and positions
            System.out.println(">>> Command from " + player.getName() +
                " = " + command +
                " current=" + currentPos);

            // Game logic: Move player if command is valid and not a wall, or if it's the exit cell
            switch (command.toLowerCase().trim()) {
                case "up":
                    newPos = new Position(currentPos.x(), currentPos.y() - 1);
                    break;
                case "down":
                    newPos = new Position(currentPos.x(), currentPos.y() + 1);
                    break;
                case "left":
                    newPos = new Position(currentPos.x() - 1, currentPos.y());
                    break;
                case "right":
                    newPos = new Position(currentPos.x() + 1, currentPos.y());
                    break;
            }

            // Debug: print attempted move and wall check
            System.out.println(">>> Attempting move to " + newPos +
                " isWall=" + maze.isWall(newPos.x(), newPos.y()));

            // Allow move if not a wall OR it's the exit cell
            if (!newPos.equals(currentPos) &&
                (!maze.isWall(newPos.x(), newPos.y()) || newPos.equals(exitPos))) {
                player.setPosition(newPos);
                System.out.println(">>> " + player.getName() + " moved to " + newPos);

                // Check for win condition
                if (newPos.equals(exitPos)) {
                    System.out.println("Player " + player.getName() + " reached the exit!");
                    broadcastWinner(player.getName());
                    // Don't shutdown pool immediately, allow graceful disconnect
                }
            } else {
                System.out.println(">>> " + player.getName() + " move blocked");
            }
        }
        
        /**
         * Finds a random empty space for a new player to spawn.
         */
        private Position findStartLocation() {
            int x, y;
            do {
                x = (int) (Math.random() * maze.getWidth());
                y = (int) (Math.random() * maze.getHeight());
            } while (maze.isWall(x, y) || maze.getExitPosition().equals(new Position(x, y)));
            return new Position(x, y);
        }
        
        /**
         * Broadcasts a winner message to all clients.
         */
        private void broadcastWinner(String winnerName) {
            String winMessage = "WINNER: " + winnerName;
            for (ObjectOutputStream out : clientOutputs.values()) {
                try {
                    out.writeObject(winMessage);
                    out.flush(); // Ensure message is sent immediately
                } catch (IOException e) {
                    System.err.println("Error broadcasting winner: " + e.getMessage());
                }
            }
        }
    }
}
