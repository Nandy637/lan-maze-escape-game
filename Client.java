import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;

/**
 * The client for the LAN Maze Escape Game.
 * This class handles user input, communicates with the server, and displays the game state.
 */
public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static String myPlayerId;
    private static final Scanner consoleScanner = new Scanner(System.in);
    private static Maze currentMaze;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String serverIP = args.length > 0 ? args[0] : "";
        final String playerName;

        if (serverIP.isEmpty()) {
            System.out.print("Enter server IP (e.g. 192.168.31.191): ");
            serverIP = sc.nextLine().trim();
        }

        if (args.length > 1) {
            playerName = args[1];
        } else {
            System.out.print("Enter your player name: ");
            playerName = sc.nextLine().trim();
        }

        try (Socket socket = new Socket(serverIP, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            System.out.println("Connected to " + serverIP + ":" + SERVER_PORT);

            // Send player name to the server and ensure it's flushed immediately
            out.writeObject(playerName);
            out.flush(); // Flush after every writeObject to avoid network delays

            // Listen for server updates in a separate thread
            new Thread(() -> {
                try {
                    while (true) {
                        Object serverMessage = in.readObject();
                        handleServerMessage(serverMessage, playerName);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("Disconnected from server: " + e.getMessage());
                }
            }).start();

            // Main loop to handle user commands
            while (true) {
                String command = consoleScanner.nextLine();
                out.writeObject(command);
                out.flush(); // Flush after every command for real-time updates
            }
        } catch (ConnectException ce) {
            System.err.println("Could not connect to " + serverIP + ":" + SERVER_PORT + "  -> " + ce.getMessage());
            System.err.println("Check server IP, server running, same Wi-Fi, and firewall.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes messages received from the server.
     */
    private static void handleServerMessage(Object message, String localPlayerName) {
        if (message instanceof Maze maze) {
            currentMaze = maze;
        } else if (message instanceof Map<?, ?> playersMap) {
            @SuppressWarnings("unchecked")
            Map<String, Player> players = (Map<String, Player>) playersMap;
            
            // Find our own player ID from the received map
            if (myPlayerId == null) {
                for (Player player : players.values()) {
                    if (player.getName().equals(localPlayerName)) {
                        myPlayerId = player.getId();
                        break;
                    }
                }
            }
            
            clearConsole();
            printGameState(players);
        } else if (message instanceof String winMessage && winMessage.startsWith("WINNER")) {
            System.out.println("\n*** " + winMessage + " ***");
            System.exit(0);
        }
    }

    /**
     * Prints the current game state to the console, including the maze and players.
     */
    private static void printGameState(Map<String, Player> players) {
        if (currentMaze == null) {
            System.out.println("Waiting for maze data...");
            return;
        }
        if (players.isEmpty()) {
            System.out.println("Waiting for players...");
            return;
        }

        char[][] displayGrid = new char[currentMaze.getHeight()][currentMaze.getWidth()];

        // Initialize display grid with maze walls
        for (int y = 0; y < currentMaze.getHeight(); y++) {
            for (int x = 0; x < currentMaze.getWidth(); x++) {
                displayGrid[y][x] = currentMaze.isWall(x, y) ? '#' : ' ';
            }
        }
        
        // Add exit and players to the grid
        Position exitPos = currentMaze.getExitPosition();
        displayGrid[exitPos.y()][exitPos.x()] = 'E';

        for (Player p : players.values()) {
            Position pos = p.getPosition();
            char playerChar = (p.getId().equals(myPlayerId)) ? 'P' : 'O';
            displayGrid[pos.y()][pos.x()] = playerChar;
        }

        // Print the grid
        for (char[] row : displayGrid) {
            System.out.println(new String(row));
        }
        
        // Print player information
        System.out.println("\n--- Connected Players ---");
        players.forEach((id, player) -> System.out.println("- " + player.getName()));
        System.out.print("\nYour command (up/down/left/right): ");
    }
    
    /**
     * Simple hack to clear the console screen for a better display.
     */
    private static void clearConsole() {
        try {
            final String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (final Exception e) {
            System.err.println("Could not clear console.");
        }
    }
}
