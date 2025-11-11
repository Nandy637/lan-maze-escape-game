# LAN Maze Escape Game

A multiplayer LAN maze escape game built in Java with both console and GUI versions. Players connect to a server, navigate a procedurally generated maze, and compete to reach the exit first.

## Features

### Core Gameplay
- **Client-Server Architecture**: Server manages game state and client connections
- **Multiplayer Support**: Multiple players can join the same game
- **Procedural Maze Generation**: Random mazes using depth-first search algorithm
- **Real-time Updates**: Live synchronization of player positions and game state

### Game Modes
- **Console Client**: Text-based interface with ASCII maze display
- **GUI Client**: Mario-themed graphical interface with animated sprites

### Enhanced Features
- **Earthquake Events**: Maze reshuffles every 30 seconds, affecting all players
- **Coin Collection**: Collect 30 coins scattered throughout the maze
- **Power-ups**:
  - **Coin Bag**: Grants +10 coins (spawns randomly, disappears after 10 seconds)
  - **Time Bonus**: Adds +10 seconds to timer (spawns randomly, disappears after 10 seconds)
- **Time Limit**: 60-second timer for added challenge
- **Win Condition**: First player to reach the exit wins

## Prerequisites

- Java 8 or higher
- Image files for GUI version: `mario.png`, `mario0.gif` to `mario3.gif`, `gold coin.gif`, `black square.png`, `white square.png`, `portal.jpg`, `coin_bag.png`, `time_icon.png`

## How to Run

### Compilation
```bash
javac *.java
```

### Finding Your IP Address
To play across multiple machines on the same LAN, you need the server's IP address.

On Windows, open Command Prompt and run:
```bash
ipconfig
```
Look for the "IPv4 Address" under your active network adapter (usually Wi-Fi or Ethernet).

### Start the Server
Run on one machine (the host):
```bash
java Server
```
The server will display its IP address and listen on port 12345 for connections.

### Start Clients
Run on other machines or the same machine for testing. Open separate terminals for each player.

**Console Version:**
```bash
java Client <server_ip> <player_name>
```
Or run `java Client` and enter the server IP and player name when prompted.

**GUI Version:**
```bash
java MazeRunnerSwingClient <server_ip>
```
Or run `java MazeRunnerSwingClient` and enter the server IP when prompted. Enter your player name in the game.

Use arrow keys to navigate the maze.

## Game Controls

- **Arrow Keys**: Move up, down, left, right
- **Objective**: Navigate to the exit (E) while avoiding walls (#)
- **Collectibles**: Coins (gold), Coin Bags (+10 coins), Time Bonuses (+10 seconds)

## Game Mechanics

- **Maze**: 21x11 grid with walls and open paths
- **Players**: Represented as 'P' (yourself) or 'O' (others) in console, Mario sprites in GUI
- **Exit**: Marked as 'E' in console, portal image in GUI
- **Earthquake**: Every 30 seconds, maze regenerates and all players are notified
- **Power-ups**: Spawn randomly during gameplay, auto-disappear after 10 seconds
- **Timer**: 60 seconds to complete the maze, extendable with time bonuses

## Architecture

- **Server.java**: Handles client connections, game logic, broadcasting
- **Client.java**: Console client for text-based gameplay
- **MazeRunnerSwingClient.java**: GUI client with enhanced features
- **Maze.java**: Maze generation and wall/exit checking
- **Player.java**: Player data structure
- **Position.java**: Coordinate system

## Network Protocol

- Uses Java Sockets with ObjectInputStream/ObjectOutputStream
- Server broadcasts maze and player updates to all connected clients
- Clients send movement commands to server

## Troubleshooting

- **Connection Refused**: Ensure server is running before starting clients
- **Port Issues**: Check if port 12345 is available
- **Image Loading**: Place image files in the same directory as JAR/class files
- **Firewall**: Allow Java applications through firewall for LAN play

## Development Notes

- Built with OOP principles (encapsulation, modularity)
- Thread-safe using ConcurrentHashMap for player management
- Serializable objects for network transmission
- Swing-based GUI with custom painting for smooth animations

Enjoy the game!