import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.List;

public class MazeRunnerSwingClient extends JFrame {
    private String playerName;
    private String myPlayerId;
    private Maze currentMaze;
    private Map<String, Player> players;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private static String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    // Difficulty settings
    public enum Difficulty { EASY, MEDIUM, HARD }
    private Difficulty difficulty = Difficulty.MEDIUM;

    private CardLayout cardLayout;
    private JPanel cardsPanel;

    private JPanel startScreenPanel;
    private JLabel timeLabel;
    private JLabel coinLabel;
    private JTextArea previousScoresArea;

    private JPanel gameScreenPanel;
    private MazePanel mazePanel;
    private JPanel statsPanel;
    private JLabel statusLabel;

    private int cellSize;

    private static final int DIR_UP = 0, DIR_RIGHT = 1, DIR_DOWN = 2, DIR_LEFT = 3;
    private int lastDir = DIR_DOWN;

    private final ImageIcon[] marioIcons = {
        new ImageIcon("mario0.gif"),
        new ImageIcon("mario1.gif"),
        new ImageIcon("mario2.gif"),
        new ImageIcon("mario3.gif")
    };

    private final ImageIcon playerIconDefault = new ImageIcon("mario2.gif");
    private final ImageIcon generalImage = new ImageIcon("mario.png");
    private final ImageIcon coinIcon = new ImageIcon("gold coin.gif");
    private final ImageIcon wallIcon = new ImageIcon("black square.png");
    private final ImageIcon pathIcon = new ImageIcon("white square.png");
    private final ImageIcon portalIcon = new ImageIcon("portal.jpg");

    private List<Position> coinPositions = new ArrayList<>();
    private int coinCount = 0;
    private int totalCoins = 30;
    private int timeLeft = 60;
    private int coinBagValue = 10;
    private int timeBonusValue = 10;
    private double powerUpSpawnRate = 0.1;
    private int powerUpSpawnInterval = 10; // seconds
    private int lastPowerUpSpawn = 0;

    private javax.swing.Timer gameTimer;
    private boolean gameStarted = false;
    private boolean coinsGenerated = false;

    private Position coinBagPos = null;
    private Position timeBonusPos = null;
    private ImageIcon coinBagIcon;
    private ImageIcon timeBonusIcon;
    private javax.swing.Timer coinBagTimer;
    private javax.swing.Timer timeBonusTimer;
    private long lastEarthquakeShown = 0;
    private boolean disconnectedIntentionally = false;

    public MazeRunnerSwingClient() {
        setTitle("Mario Maze Game");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);

        buildStartScreen();
        buildGameScreen();

        cardsPanel.add(startScreenPanel, "start");
        cardsPanel.add(gameScreenPanel, "game");

        setContentPane(cardsPanel);
        cardLayout.show(cardsPanel, "start");
    }

    private void buildStartScreen() {
        startScreenPanel = new JPanel(new BorderLayout());

        JPanel imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int panelWidth = getWidth();
                int panelHeight = getHeight();
                Image img = generalImage.getImage();

                float imgAspect = (float) img.getWidth(null) / img.getHeight(null);
                int drawWidth = panelWidth;
                int drawHeight = (int) (drawWidth / imgAspect);

                if (drawHeight > panelHeight) {
                    drawHeight = panelHeight;
                    drawWidth = (int) (drawHeight * imgAspect);
                }

                int x = (panelWidth - drawWidth) / 2;
                int y = (panelHeight - drawHeight) / 2;

                g.drawImage(img, x, y, drawWidth, drawHeight, this);
            }
        };

        startScreenPanel.add(imagePanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("MarioMazeGame", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
        bottomPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton easyButton = new JButton("Easy");
        easyButton.setPreferredSize(new Dimension(150, 60));
        easyButton.setBackground(Color.GREEN);
        easyButton.setForeground(Color.WHITE);
        easyButton.setFont(new Font("Arial", Font.BOLD, 24));
        easyButton.setOpaque(true);
        easyButton.setBorderPainted(false);

        JButton mediumButton = new JButton("Medium");
        mediumButton.setPreferredSize(new Dimension(150, 60));
        mediumButton.setBackground(Color.ORANGE);
        mediumButton.setForeground(Color.WHITE);
        mediumButton.setFont(new Font("Arial", Font.BOLD, 24));
        mediumButton.setOpaque(true);
        mediumButton.setBorderPainted(false);

        JButton hardButton = new JButton("Hard");
        hardButton.setPreferredSize(new Dimension(150, 60));
        hardButton.setBackground(Color.RED);
        hardButton.setForeground(Color.WHITE);
        hardButton.setFont(new Font("Arial", Font.BOLD, 24));
        hardButton.setOpaque(true);
        hardButton.setBorderPainted(false);

        buttonPanel.add(easyButton);
        buttonPanel.add(mediumButton);
        buttonPanel.add(hardButton);
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);

        previousScoresArea = new JTextArea(5, 20);
        previousScoresArea.setEditable(false);
        previousScoresArea.setBorder(BorderFactory.createTitledBorder("Previous Scores"));
        bottomPanel.add(new JScrollPane(previousScoresArea), BorderLayout.SOUTH);

        startScreenPanel.add(bottomPanel, BorderLayout.SOUTH);

        easyButton.addActionListener(e -> selectDifficulty(Difficulty.EASY));
        mediumButton.addActionListener(e -> selectDifficulty(Difficulty.MEDIUM));
        hardButton.addActionListener(e -> selectDifficulty(Difficulty.HARD));
    }

    private void selectDifficulty(Difficulty selectedDifficulty) {
        difficulty = selectedDifficulty;

        // Configure difficulty settings
        switch (difficulty) {
            case EASY:
                timeLeft = 90;
                totalCoins = 20;
                coinBagValue = 15;
                timeBonusValue = 15;
                powerUpSpawnInterval = 5; // Every 5 seconds
                break;
            case MEDIUM:
                timeLeft = 60;
                totalCoins = 30;
                coinBagValue = 10;
                timeBonusValue = 10;
                powerUpSpawnInterval = 10; // Every 10 seconds
                break;
            case HARD:
                timeLeft = 40;
                totalCoins = 40;
                coinBagValue = 7;
                timeBonusValue = 5;
                powerUpSpawnInterval = 15; // Every 15 seconds
                break;
        }

        String name = JOptionPane.showInputDialog(
            this,
            "Enter your player name:",
            "Player Name",
            JOptionPane.QUESTION_MESSAGE
        );

        if (name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Player name is required.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        playerName = name;
        startNewGame();
        cardLayout.show(cardsPanel, "game");
    }

    private void buildGameScreen() {
        gameScreenPanel = new JPanel(new BorderLayout());

        statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timeLabel = new JLabel("Time: 60s");
        timeLabel.setFont(new Font("Arial", Font.BOLD, 20));

        coinLabel = new JLabel("Coins: 0/" + totalCoins);
        coinLabel.setFont(new Font("Arial", Font.BOLD, 20));

        statsPanel.add(timeLabel);
        statsPanel.add(Box.createHorizontalStrut(20));
        statsPanel.add(coinLabel);

        statusLabel = new JLabel(" ");
        gameScreenPanel.add(statsPanel, BorderLayout.NORTH);
        gameScreenPanel.add(statusLabel, BorderLayout.SOUTH);

        mazePanel = new MazePanel();
        gameScreenPanel.add(mazePanel, BorderLayout.CENTER);
    }

    private void startNewGame() {
        coinCount = 0;
        coinsGenerated = false;
        gameStarted = true;
        coinBagPos = null;
        timeBonusPos = null;
        lastPowerUpSpawn = timeLeft;

        previousScoresArea.append(playerName + " started a " + difficulty + " game.\n");
        timeLabel.setText("Time: " + timeLeft + "s");
        coinLabel.setText("Coins: " + coinCount + "/" + totalCoins);

        clearNetwork();
        new Thread(this::setupNetwork).start();

        mazePanel.repaint();
        mazePanel.requestFocusInWindow();

        mazePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!gameStarted || out == null) return;

                String command = null;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> {
                        command = "up";
                        lastDir = DIR_UP;
                    }
                    case KeyEvent.VK_DOWN -> {
                        command = "down";
                        lastDir = DIR_DOWN;
                    }
                    case KeyEvent.VK_LEFT -> {
                        command = "left";
                        lastDir = DIR_LEFT;
                    }
                    case KeyEvent.VK_RIGHT -> {
                        command = "right";
                        lastDir = DIR_RIGHT;
                    }
                }

                if (command != null) {
                    try {
                        out.writeObject(command);
                        out.flush();
                    } catch (IOException ex) {
                        showError("Error sending command: " + ex.getMessage());
                    }
                    // Update local player's position and check collections after sending command
                    if (players != null && myPlayerId != null) {
                        Player me = players.get(myPlayerId);
                        if (me != null) {
                            checkCoinCollection(me.getPosition());
                        }
                    }
                    mazePanel.repaint();
                }
            }
        });

        startGameTimer();
    }

    private void clearNetwork() {
        out = null;
        in = null;
        players = null;
        currentMaze = null;
        myPlayerId = null;
    }

    private void startGameTimer() {
        if (gameTimer != null) {
            gameTimer.stop();
        }

        gameTimer = new javax.swing.Timer(1000, e -> {
            if (!gameStarted) return;

            timeLeft--;
            timeLabel.setText("Time: " + timeLeft + "s");

            // Spawn power-ups periodically based on interval
            if (lastPowerUpSpawn - timeLeft >= powerUpSpawnInterval) {
                // Spawn both power-ups if available
                if (coinBagPos == null) {
                    spawnCoinBag();
                }
                if (timeBonusPos == null) {
                    spawnTimeBonus();
                }
                lastPowerUpSpawn = timeLeft;
            }

            if (timeLeft <= 0) {
                ((javax.swing.Timer) e.getSource()).stop();
                showTimeExceeded();
            }

            mazePanel.repaint();
        });

        gameTimer.start();
    }

    private void showTimeExceeded() {
        JOptionPane.showMessageDialog(
            this,
            "Time exceeded! Game over.\nCoins collected: " + coinCount
        );

        previousScoresArea.append(playerName + ": " + coinCount + " coins in 60s\n");
        cardLayout.show(cardsPanel, "start");
        gameStarted = false;
    }

    private void setupNetwork() {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)) {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(playerName);
            out.flush();

            while (true) {
                Object message = in.readObject();
                handleServerMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!disconnectedIntentionally) {
                showError("Disconnected from server: " + e.getMessage());
                cardLayout.show(cardsPanel, "start");
                gameStarted = false;
            }
        }
    }

    private void handleServerMessage(Object message) {
        if (message instanceof Maze maze) {
            currentMaze = maze;
            if (!coinsGenerated) {
                generateCoins();
                coinsGenerated = true;
            }
            updateCellSize();
            // Handle earthquake: maze reshuffled - no popup, just update timestamp
            lastEarthquakeShown = System.currentTimeMillis();
        } else if (message instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Player> updatedPlayers = (Map<String, Player>) map;
            players = updatedPlayers;

            if (myPlayerId == null) {
                for (Player p : players.values()) {
                    if (p.getName().equals(playerName)) {
                        myPlayerId = p.getId();
                        break;
                    }
                }
            }

            if (myPlayerId != null) {
                Player me = players.get(myPlayerId);
                if (me != null) {
                    checkCoinCollection(me.getPosition());
                }
            }
        } else if (message instanceof String str && str.startsWith("WINNER")) {
            JOptionPane.showMessageDialog(this, str, "Game Over", JOptionPane.INFORMATION_MESSAGE);
            previousScoresArea.append(playerName + ": WIN\n");
            cardLayout.show(cardsPanel, "start");
            gameStarted = false;
            disconnectedIntentionally = true;
            // Disconnect gracefully
            try {
                if (out != null) out.close();
                if (in != null) in.close();
            } catch (IOException e) {
                // Ignore
            }
            return; // Exit the listening thread
        }

        SwingUtilities.invokeLater(() -> {
            mazePanel.repaint();
            updateStatusLabel();
        });
    }

    private void generateCoins() {
        coinPositions.clear();
        Random rand = new Random();
        int w = currentMaze.getWidth();
        int h = currentMaze.getHeight();

        while (coinPositions.size() < totalCoins) {
            int x = rand.nextInt(w);
            int y = rand.nextInt(h);
            Position pos = new Position(x, y);

            if (!currentMaze.isWall(x, y) &&
                !pos.equals(currentMaze.getExitPosition()) &&
                !coinPositions.contains(pos)) {
                coinPositions.add(pos);
            }
        }

        coinCount = 0;
        coinLabel.setText("Coins: 0/" + totalCoins);
    }

    private void checkCoinCollection(Position playerPos) {
        Iterator<Position> it = coinPositions.iterator();
        boolean collected = false;

        while (it.hasNext()) {
            Position pos = it.next();
            if (pos.equals(playerPos)) {
                coinCount++;
                it.remove();
                collected = true;
                break;
            }
        }

        // Check coin bag collection
        if (coinBagPos != null && coinBagPos.equals(playerPos)) {
            coinCount += coinBagValue;
            coinBagPos = null;
            if (coinBagTimer != null) coinBagTimer.stop();
            collected = true;
        }

        // Check time bonus collection
        if (timeBonusPos != null && timeBonusPos.equals(playerPos)) {
            timeLeft += timeBonusValue;
            timeBonusPos = null;
            collected = true;
        }

        if (collected) {
            coinLabel.setText("Coins: " + coinCount + "/" + totalCoins);
            timeLabel.setText("Time: " + timeLeft + "s");
            mazePanel.repaint();
        }
    }

    private void spawnCoinBag() {
        if (currentMaze == null) return;
        Random rand = new Random();
        int w = currentMaze.getWidth();
        int h = currentMaze.getHeight();
        Position pos;
        do {
            pos = new Position(rand.nextInt(w), rand.nextInt(h));
        } while (currentMaze.isWall(pos.x(), pos.y()) || pos.equals(currentMaze.getExitPosition()));
        coinBagPos = pos;
        mazePanel.repaint();

        // Auto-remove after 10 seconds
        if (coinBagTimer != null) coinBagTimer.stop();
        coinBagTimer = new javax.swing.Timer(10000, e -> {
            coinBagPos = null;
            System.out.println("Coin bag removed");
            mazePanel.repaint();
        });
        coinBagTimer.setRepeats(false);
        coinBagTimer.start();
    }

    private void spawnTimeBonus() {
        if (currentMaze == null) return;
        Random rand = new Random();
        int w = currentMaze.getWidth();
        int h = currentMaze.getHeight();
        Position pos;
        do {
            pos = new Position(rand.nextInt(w), rand.nextInt(h));
        } while (currentMaze.isWall(pos.x(), pos.y()) || pos.equals(currentMaze.getExitPosition()));
        timeBonusPos = pos;
        System.out.println("Time bonus spawned at: " + pos);
        mazePanel.repaint();

        // Auto-remove after 10 seconds
        if (timeBonusTimer != null) timeBonusTimer.stop();
        timeBonusTimer = new javax.swing.Timer(10000, e -> {
            timeBonusPos = null;
            System.out.println("Time bonus removed");
            mazePanel.repaint();
        });
        timeBonusTimer.setRepeats(false);
        timeBonusTimer.start();
    }

    private void updateStatusLabel() {
        if (players == null || players.isEmpty()) {
            statusLabel.setText("Waiting for players...");
        } else {
            StringBuilder sb = new StringBuilder("<html>Connected Players: ");
            for (Player p : players.values()) {
                if (p.getId().equals(myPlayerId)) {
                    sb.append("<b>").append(p.getName()).append("</b>, ");
                } else {
                    sb.append(p.getName()).append(", ");
                }
            }
            sb.setLength(sb.length() - 2);
            sb.append("<br>Use arrow keys to move.</html>");
            statusLabel.setText(sb.toString());
        }
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void updateCellSize() {
        if (currentMaze == null) return;

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width - 100;
        int screenHeight = screenSize.height - 150;

        int mazeWidth = currentMaze.getWidth();
        int mazeHeight = currentMaze.getHeight();

        cellSize = Math.min(screenWidth / mazeWidth, screenHeight / mazeHeight);
        if (cellSize < 10) cellSize = 10;
        if (cellSize > 50) cellSize = 50;

        // Scale icons to cell size
        ImageIcon originalCoinBag = new ImageIcon("coin_bag.png");
        Image scaledCoinBag = originalCoinBag.getImage().getScaledInstance(cellSize, cellSize, Image.SCALE_SMOOTH);
        coinBagIcon = new ImageIcon(scaledCoinBag);

        ImageIcon originalTimeBonus = new ImageIcon("time_icon.jpg");
        Image scaledTimeBonus = originalTimeBonus.getImage().getScaledInstance(cellSize, cellSize, Image.SCALE_SMOOTH);
        timeBonusIcon = new ImageIcon(scaledTimeBonus);

        if (mazePanel != null) {
            mazePanel.setPreferredSize(new Dimension(mazeWidth * cellSize, mazeHeight * cellSize));
            mazePanel.revalidate();
        }
    }

    private class MazePanel extends JPanel {
        public MazePanel() {
            setFocusable(true);
            requestFocusInWindow();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (!gameStarted) {
                int centerX = getWidth() / 2 - generalImage.getIconWidth() / 2;
                int centerY = getHeight() / 3;
                generalImage.paintIcon(this, g, centerX, centerY);

                g.setFont(new Font("Arial", Font.BOLD, 36));
                g.setColor(Color.BLUE);
                FontMetrics fm = g.getFontMetrics();
                String title = "MarioMazeGame";
                int tx = (getWidth() - fm.stringWidth(title)) / 2;
                g.drawString(title, tx, centerY - 40);
                return;
            }

            if (currentMaze == null || players == null) {
                g.drawString("Waiting for maze data...", 20, 20);
                return;
            }

            int mazeWidth = currentMaze.getWidth();
            int mazeHeight = currentMaze.getHeight();

            // Calculate cellSize to fill the entire panel
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            int dynamicCellSize = Math.min(panelWidth / mazeWidth, panelHeight / mazeHeight);
            if (dynamicCellSize < 1) dynamicCellSize = 1; // Minimum size

            Dimension dim = new Dimension(mazeWidth * dynamicCellSize, mazeHeight * dynamicCellSize);
            if (!dim.equals(getPreferredSize())) {
                setPreferredSize(dim);
                revalidate();
            }

            for (int y = 0; y < mazeHeight; y++) {
                for (int x = 0; x < mazeWidth; x++) {
                    if (currentMaze.isWall(x, y))
                        wallIcon.paintIcon(this, g, x * dynamicCellSize, y * dynamicCellSize);
                    else
                        pathIcon.paintIcon(this, g, x * dynamicCellSize, y * dynamicCellSize);
                }
            }

            Position exitPos = currentMaze.getExitPosition();
            portalIcon.paintIcon(this, g, exitPos.x() * dynamicCellSize, exitPos.y() * dynamicCellSize);

            for (Position coinPos : new ArrayList<>(coinPositions)) {
                coinIcon.paintIcon(this, g, coinPos.x() * dynamicCellSize, coinPos.y() * dynamicCellSize);
            }

            // Draw coin bag
            if (coinBagPos != null) {
                if (coinBagIcon != null && coinBagIcon.getIconWidth() > 0) {
                    coinBagIcon.paintIcon(this, g, coinBagPos.x() * dynamicCellSize, coinBagPos.y() * dynamicCellSize);
                } else {
                    // Fallback: draw a colored rectangle if image fails to load
                    g.setColor(Color.YELLOW);
                    g.fillRect(coinBagPos.x() * dynamicCellSize, coinBagPos.y() * dynamicCellSize, dynamicCellSize, dynamicCellSize);
                    g.setColor(Color.BLACK);
                    g.drawString("$", coinBagPos.x() * dynamicCellSize + dynamicCellSize/2 - 4, coinBagPos.y() * dynamicCellSize + dynamicCellSize/2 + 4);
                }
            }

            // Draw time bonus
            if (timeBonusPos != null) {
                // Always try to load the icon fresh each time
                ImageIcon tempIcon = new ImageIcon("time_icon.png");
                if (tempIcon != null && tempIcon.getIconWidth() > 0) {
                    // Scale it to cell size
                    Image scaled = tempIcon.getImage().getScaledInstance(dynamicCellSize, dynamicCellSize, Image.SCALE_SMOOTH);
                    ImageIcon scaledIcon = new ImageIcon(scaled);
                    scaledIcon.paintIcon(this, g, timeBonusPos.x() * dynamicCellSize, timeBonusPos.y() * dynamicCellSize);
                } else {
                    // Fallback: draw a colored rectangle if image fails to load
                    g.setColor(Color.BLUE);
                    g.fillRect(timeBonusPos.x() * dynamicCellSize, timeBonusPos.y() * dynamicCellSize, dynamicCellSize, dynamicCellSize);
                    g.setColor(Color.WHITE);
                    g.drawString("T", timeBonusPos.x() * dynamicCellSize + dynamicCellSize/2 - 4, timeBonusPos.y() * dynamicCellSize + dynamicCellSize/2 + 4);
                }
            }

            for (Player p : players.values()) {
                Position pos = p.getPosition();
                if (p.getId().equals(myPlayerId))
                    marioIcons[lastDir].paintIcon(this, g, pos.x() * dynamicCellSize, pos.y() * dynamicCellSize);
                else
                    playerIconDefault.paintIcon(this, g, pos.x() * dynamicCellSize, pos.y() * dynamicCellSize);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            SERVER_ADDRESS = args[0];
        } else {
            String ip = JOptionPane.showInputDialog(null, "Server IP (e.g. 192.168.31.191):", "Connect to Server", JOptionPane.QUESTION_MESSAGE);
            if (ip != null && !ip.trim().isEmpty()) {
                SERVER_ADDRESS = ip.trim();
            }
        }
        SwingUtilities.invokeLater(() -> {
            MazeRunnerSwingClient client = new MazeRunnerSwingClient();
            client.setVisible(true);
        });
    }
}
