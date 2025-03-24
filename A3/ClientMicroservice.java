
/**
 * ClientMicroservice acts as the client-side application that interacts with remote RMI services,
 * including AccountManager, WordServer, and CrosswordInterface. It also listens for game events
 * like win/loss notifications.
 */

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.List;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

//was used for diff polling paradigm, kept for ref
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


//for polling updates for game chganges
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


public class ClientMicroservice { //extends UnicastRemoteObject implements GameEventListener 

    private String name = null; // Stores the player's username after login
    private String gameID = null; // Stores the active game ID if the player is in a game
    private GameState state = GameState.LOGIN; // Tracks the client's current state

    private AccountManager accountManager;
    private WordServer wordServer;
    private CrosswordGameState CrosswordGameState;
    private CrissCrossPuzzleServer server;
    private Scoreboard scoreboard;
    private static char[][] currentGrid;
    private long sequenceNumber;  // Client's local sequence tracker

    private Scanner scanner = new Scanner(System.in); // Scanner instance for user input
    private BlockingQueue<String> eventQueue = new LinkedBlockingQueue<>(); // Event queue for handling game events


    /**
     * Enum representing different states of the client.
     */
    private enum GameState {
        LOGIN, READY, INGAME
    }
    
    /**
     * Constructor initializes the client
     */
    protected ClientMicroservice() throws RemoteException {
        super(); //esentially UnicastRemoteObject.exportObject(this,0). UnicastRemoteObject does not have a 0 argument constructer so super must be called to accept rmi game updates
        try {
            accountManager = (AccountManager) Naming.lookup("rmi://localhost/AccountManager");
            wordServer = (WordServer) Naming.lookup("rmi://localhost/WordServer");
            CrosswordGameState = (CrosswordGameState) Naming.lookup("rmi://localhost/CrosswordGameState");
            server = (CrissCrossPuzzleServer) Naming.lookup("rmi://localhost/CrissCrossPuzzleServer");
        } catch (Exception e) {
            System.err.println("Error connecting to RMI services: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Main client loop, continuously running and switching between different game states.
     */
    public void run() {
        while (true) {
            try {
                switch (state) {
                    case LOGIN:
                        handleLogin();
                        break;
                    case READY:
                        handleReady();
                        break;
                    case INGAME:
                        handleInGame();
                        break;
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }


    /**
     * Handles logout by unregistering the event listener and resetting the client state.
     */
    private void handleLogout() {
        //unregisterEventListener();
        name = null;
        gameID = null;
        state = GameState.LOGIN;
        System.out.println("Logged out successfully.");
    }

    /**
     * Handles the login process.
     */
    private void handleLogin() {
        while (state == GameState.LOGIN) {
            System.out.println("Welcome! Choose an option:");
            System.out.println("1. Create Account");
            System.out.println("2. Login");
            System.out.println("3. Exit");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        System.out.print("Enter username: ");
                        String newUser = scanner.nextLine();
                        System.out.print("Enter password: ");
                        String newPassword = scanner.nextLine();
                        accountManager.createUser(newUser, newPassword);
                        System.out.println("Account created! You may now login.");
                        break;
                    case 2:
                        System.out.print("Enter username: ");
                        String username = scanner.nextLine();
                        System.out.print("Enter password: ");
                        String password = scanner.nextLine();
                        accountManager.loginUser(username, password);
                        name = username;
                        
                        this.sequenceNumber = server.registerClient(username); //REGISTER CLIENT
                        sequenceNumber++;

                        state = GameState.READY;
                        System.out.println("\nLogin successful! Welcome, " + name);
                        break;
                    case 3:
                        System.out.println("Exiting...");
                        scanner.close();
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (Exception e) {
                System.err.println("Error during client registration: " + e.getMessage());
                e.printStackTrace(); // This will show the stack trace for better debugging
                scanner.nextLine(); // Consume invalid input
            }
        }
    }


	/**
     * Handles user interactions in the READY state.
     */
	private void handleReady() {
		System.out.println("\nWelcome! Choose an option:");
		System.out.println("1. View History");
		System.out.println("2. Lookup Word");
		System.out.println("3. Remove Word");
		System.out.println("4. Add Word");
		System.out.println("5. Start Singleplayer");
		System.out.println("6. Start Multiplayer");
        System.out.println("7. Logout");
    
        int choice = -1; // Default value if no valid input is provided
        
        // Loop until a valid integer is entered
        while (choice == -1) {
            System.out.print("Enter your choice (an integer): ");
            try {
                choice = scanner.nextInt(); // Try to read an integer
            } catch (InputMismatchException e) {
                // Handle the case where input is not an integer
                System.out.println("Invalid input! Please enter a valid integer.");
                scanner.nextLine(); // Consume the invalid input (string or other)
            }
        }

        int numWords;
        int failFactor;
        int players;
		scanner.nextLine();
        System.out.print("\n");

		try {
			switch (choice) {
				case 1:
					System.out.println(accountManager.getHistory(name));
					break;
				case 2:
					System.out.print("Enter word to look up: ");
					System.out.println(wordServer.checkWord(scanner.nextLine()));
					break;
				case 3:
					System.out.print("Enter word to remove: ");
					System.out.println(wordServer.removeWord(scanner.nextLine()));
					break;
				case 4:
					System.out.print("Enter word to add: ");
					System.out.println(wordServer.createWord(scanner.nextLine()));
					break;
				case 5:
                    System.out.print("Enter number of words: ");
                    numWords = scanner.nextInt();
                    System.out.print("Enter fail factor: ");
                    failFactor = scanner.nextInt();
                    scanner.nextLine(); // consume newline

                    // Now call startGame on the instance
                    /////////////DUPLICATE CHECK
                    gameID = null;
                    gameID = server.startGame(name, numWords, failFactor, gameID, sequenceNumber);
                    System.out.println("Start Game Sequence Number: " + sequenceNumber);

                    if (Math.random() < 0.5) {
                        gameID = server.startGame(name, numWords, failFactor, gameID, sequenceNumber);
                        System.out.println("Start game Sequence Number: " + sequenceNumber);
                    }

                    sequenceNumber++;

					if (gameID != null) {
						state = GameState.INGAME;
					}
					break;
				case 6:
                    System.out.print("Enter number of players: ");
                    players = scanner.nextInt();
                    System.out.print("Enter number of words: ");
                    numWords = scanner.nextInt();
                    System.out.print("Enter fail factor: ");
                    failFactor = scanner.nextInt();

                    System.out.print("Waiting for game");
                    gameID = null;
					gameID = server.startMultiplayer(name, players, failFactor, gameID, sequenceNumber);
                    System.out.println("\nStart Multiplayer Game Sequence Number: " + sequenceNumber);
                    if (Math.random() < 0.5) {
                        gameID = server.startMultiplayer(name, players, failFactor, gameID, sequenceNumber);
                        System.out.println("\nStart Multiplayer Game Sequence Number: " + sequenceNumber);
                    }
                    sequenceNumber++;

					if (gameID != null) {
					    state = GameState.INGAME;
					}
					break;
				case 7:
                    handleLogout();
                    break;
                
                default:
                    System.out.println("Invalid Input");
			}
		} catch (RemoteException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	

    /**
     * Handles user interactions while in an active game.
     * TODO: Implement full functionality for INGAME state operations.
     */
    private void handleInGame() throws RemoteException {
        Thread beat = new Thread(new ClientHeartbeat(server, name, gameID));
		beat.start();
        
        System.out.println("\nYou are now in-game.");
        System.out.println("Type 'exit' to leave the game.");
        System.out.println("\nSetting up Game... Please Wait\n");
        System.out.println("\n-----------------------");
        printUserGame(gameID);
        
        CrosswordGameState gameState = server.getGameState(gameID);
        boolean multiplayer = gameState.checkMultiplayer();

        System.out.println("\nStarting Game\n");

        String gameScores;
        gameScores = server.displayAllScores(gameID, name, sequenceNumber);
        if (Math.random() < 0.5) {
            gameScores = server.displayAllScores(gameID, name, sequenceNumber);
        }
        System.out.println("Display Scores Sequence Number: " + sequenceNumber);

        sequenceNumber++;

        System.out.print("\nCurrent Scores:\n");
        System.out.print(gameScores);

        Scanner scanner = new Scanner(System.in);
        startMissingNamesCheck(gameState);



        

        // Main in-game loop for user commands
        boolean inGameLoop = true;
        
        while (inGameLoop && state == GameState.INGAME && gameID != null) {
            gameState = server.getGameState(gameID);
            String activePlayer = server.getActivePlayer(gameID);
            

            if ("WIN".equals(gameState.getGameStatus()) || "LOSE".equals(gameState.getGameStatus())) {
                inGameLoop = false;
                state = GameState.READY;

                if ("LOSE".equals(gameState.getGameStatus())) {
                    System.out.println("GAME LOST\n");
                    accountManager.updateScore(name, false, multiplayer);
                }
                else {
                    System.out.println("GAME WON\n");
                    accountManager.updateScore(name, true, multiplayer);
                }
                
                gameState.removePlayer(name);
                if(gameState.getLobbySize() == 0) {
                    server.exitGame(gameID, name, sequenceNumber);
                    if (Math.random() < 0.5) {
                        server.exitGame(gameID, name, sequenceNumber);
                    }
                    System.out.println("Exit Game Sequence Number: " + sequenceNumber);
       
                    sequenceNumber++; 
                    gameID = null;
                }
            }
            else if(activePlayer.equals(name))
            {   if(!server.checkPlayerInGame(gameID, name)) {
                    inGameLoop = false;
                    state = GameState.READY;
                    gameID = leaveGame(gameID, name);
                }
                System.out.println("\nYou Are The Active Player :" + activePlayer);
                System.out.println("\nEnter letter or word for guess"); 
                System.out.println("exit (Leave)");    
                System.out.println("? (Lookup)");   
                System.out.println("! (Restart)");
                System.out.print("\nCurrent Score:\n");
                System.out.print(gameScores);
                printUserGame(gameID);
                
                System.out.print("Enter guess: ");
                String input = scanner.nextLine().trim().toLowerCase();

                switch (input) {
                    case "exit":
                        System.out.print("This will count as a loss. Are you sure you would like to quit? (y/n): ");
                        input = scanner.nextLine().trim().toLowerCase();
                        if ("y".equals(input)) {
                            inGameLoop = false;
                            state = GameState.READY;
                            gameID = leaveGame(gameID, name);
                        }
                        break;

                    case "!":
                        if(gameState.getLobbySize() > 1) {
                            System.out.print("Restarting Multiplayer Games is not allowed");
                        }
                        else{
                            System.out.print("This will count as a loss. Are you sure you want to restart? (y/n): ");
                            input = scanner.nextLine().trim().toLowerCase();
                            if ("y".equals(input)) {
                                gameState = server.getGameState(gameID);
                                accountManager.updateScore(name, false, gameState.checkMultiplayer());
                                gameID = server.restartGame(gameID, name, sequenceNumber);
                                if (Math.random() < 0.5) {
                                    gameID = server.restartGame(gameID, name, sequenceNumber);
                                }
                                System.out.println("Restart Game Sequence Number: " + sequenceNumber);

                                sequenceNumber++;
                            }
                            printUserGame(gameID);
                        }
                        break;

                    case "?":
                        System.out.print("Enter word to look up: ");
                        String word = scanner.nextLine().trim(); // Store input separately
                        System.out.println(wordServer.checkWord(word));
                        break;

                    case "":
                        break;

                    default:
                        System.out.println("\nChecking word: " + input); // Use println instead of print
                        currentGrid = server.getCurrentGrid(gameID);
                        gameID = server.checkGuess(server.getGameState(gameID), input, name, sequenceNumber);
                        System.out.println("Check Guess Sequence Number: " + sequenceNumber);
                        if (Math.random() < 0.5) {
                            gameID = server.checkGuess(server.getGameState(gameID), input, name, sequenceNumber);
                            System.out.println("Check Guess Sequence Number: " + sequenceNumber);
                        }

                        sequenceNumber++;

                        int addPoints = calculateScore(currentGrid, server.getCurrentGrid(gameID));
                        //System.out.println("\nADDING SCORE TEST: " + addPoints);
                        server.updateActivePlayer(gameID, name, sequenceNumber);
                        if (Math.random() < 0.5) {
                            server.updateActivePlayer(gameID, name, sequenceNumber);

                        }
                        System.out.println("Update Active Player Sequence Number: " + sequenceNumber);

                        sequenceNumber++;

                        break;
                }
            } else{
                String currentPlayer = server.getActivePlayer(gameID);
                boolean exitGame = false;
                System.out.println("\nPlease Wait your turn, current player: " + server.getActivePlayer(gameID));
                while(!currentPlayer.equals(name) && !exitGame) {
                    if(!server.checkPlayerInGame(gameID, name)) {
                        inGameLoop = false;
                        state = GameState.READY;
                        exitGame = true;
                        gameID = leaveGame(gameID, name);
                    }
                    else if(!currentPlayer.equals(server.getActivePlayer(gameID)))
                    {      
                        gameScores = server.displayAllScores(gameID, name, sequenceNumber);
                        if (Math.random() < 0.5) {
                            gameScores = server.displayAllScores(gameID, name, sequenceNumber);
                        }
                        System.out.println("Display Scores Sequence Number: " + sequenceNumber);

                        sequenceNumber++;
                        System.out.print("\nCurrent Score:\n");
                        System.out.print(gameScores);
                        printUserGame(gameID);
                        currentPlayer = server.getActivePlayer(gameID);
                        System.out.println("\nPlease Wait your turn, current player: " + server.getActivePlayer(gameID));
                    }
                    else {
                        try {
                            Thread.sleep(1000);  // Sleep for 1 second
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    
    public int calculateScore(char[][] currentGrid, char[][] comparisonGrid) {
        int score = 0;
    
        // Ensure both grids have the same dimensions
        if (currentGrid.length != comparisonGrid.length || currentGrid[0].length != comparisonGrid[0].length) {
            throw new IllegalArgumentException("Grids must have the same dimensions!");
        }

        // Iterate through each cell in the grids
        for (int i = 0; i < currentGrid.length; i++) {
            for (int j = 0; j < currentGrid[i].length; j++) {
                char currentChar = currentGrid[i][j];
                char compareChar = comparisonGrid[i][j];

                // Add 50 points for each differing character
                if (currentChar != compareChar && compareChar == '-') {
                    score += 50;
                }
            }
        }

        return score;
    }

    // Helper method to compare game states
    private boolean isStateEqual(CrosswordGameState currentState, CrosswordGameState lastState) {
        // Null check to avoid NullPointerException
        if (currentState == null || lastState == null) {
            return false;
        }

        try {
            // Compare game states field by field
            boolean isEqual = Objects.equals(currentState.getLives(), lastState.getLives()) &&
                  Objects.equals(currentState.getGameID(), lastState.getGameID());
                  System.out.println("\nLAST STATE LIVES: " + lastState.getLives());
                  System.out.println("\nCURRENT STATE LIVES: " + currentState.getLives());
            return isEqual;
        } catch (RemoteException e) {
            System.err.println("Error comparing game states: " + e.getMessage());
            return false;
        }
    }

    public void printUserGame(String gameID) throws RemoteException {
        if(gameID != null) {
            CrosswordGameState gameServer = server.getGameState(gameID);
        
            for (int i = 0; gameServer.getPlayerGrid() == null; i++) {
                try {
                    Thread.sleep(1000);  // Sleep for 1 second
                } catch (InterruptedException e) {
                    e.printStackTrace();  // Handle the exception (you can log it or re-throw it depending on your needs)
                }

                // Back to main menu in case of error
                if (i == 6) {
                    System.out.println("Game Loading error");
                    handleReady();
                }
            }

            System.out.print("\n");
            for (char[] row : gameServer.getPlayerGrid()) {
    	    	    System.out.println(new String(row) + "+");  // Append '+' at the end of each row
    	    	}
            System.out.print("\nLives: " + gameServer.getLives() + "\n");
        }
    }

    public String leaveGame(String gameID, String name) throws RemoteException {
        System.out.println("Exiting game...");
        CrosswordGameState gameState = server.getGameState(gameID);

        accountManager.updateScore(name, false, gameState.checkMultiplayer());
        
        server.updateActivePlayer(gameID, name, sequenceNumber);
        if (Math.random() < 0.5) {
            server.updateActivePlayer(gameID, name, sequenceNumber);
        }
        server.updateActivePlayer(gameID, name, sequenceNumber);
        System.out.println("Update Active Player Sequence Number: " + sequenceNumber);

        sequenceNumber++;

        gameState.removePlayer(name);
        if(gameState.getLobbySize() == 0) {
            server.exitGame(gameID, name, sequenceNumber);
            if (Math.random() < 0.5) {
                server.exitGame(gameID, name, sequenceNumber);
            }
            System.out.println("Exit Game Sequence Number: " + sequenceNumber);

            sequenceNumber++;
            gameID = null;
        }

        return gameID;
    }







   // Helper method to start a thread that periodically checks for missing players
private void startMissingNamesCheck(final CrosswordGameState gameState) {
    // Use an AtomicReference to hold the existing players so we can update it
    final AtomicReference<String[]> existingPlayersRef = new AtomicReference<>();
    try {
        existingPlayersRef.set(gameState.getPlayerNames());
    } catch (RemoteException e) {
        e.printStackTrace();
        return;
    }

    // Create and start a thread that calls checkMissingNames 
    Thread missingNamesThread = new Thread(() -> {
        while (true) {
            try {
                // Get the current active players
                String[] currentPlayers = gameState.getPlayerNames();

                if (currentPlayers == null || currentPlayers.length == 0) {
                    //exit thread on player list empty
                    break;
                }

                // Compare with the expected list stored in existingPlayersRef
                checkMissingNames(existingPlayersRef.get(), currentPlayers);
                // Update the existing players to the current players for the next iteration
                existingPlayersRef.set(currentPlayers);
                Thread.sleep(1000); // sleep for 1/2 second
            } catch (InterruptedException e) {
                System.out.println("Missing names thread interrupted.");
                break;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    });
    // Optional: Mark this thread as daemon so it doesn't block the JVM from exiting
    missingNamesThread.setDaemon(true);
    missingNamesThread.start();
}

// Function that checks for names in fullList missing from checkList
public static void checkMissingNames(String[] allPlayers, String[] activePlayers) {
    Set<String> checkSet = new HashSet<>(Arrays.asList(activePlayers));
    for (String name : allPlayers) {
        if (!checkSet.contains(name)) {
            System.out.println("\n" + name + " has been disconnected from the game.\n");
        }
    }
}





    /**
     * Main method to start the client microservice.
     */
    public static void main(String[] args) {
        try {
            ClientMicroservice client = new ClientMicroservice();
            client.run();
        } catch (Exception e) {
            System.err.println("Error initializing client: " + e.getMessage());
        }
    }
}

