import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.List;
import java.util.Arrays;

//was used for diff polling paradigm, kept for ref
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


//for polling updates for game chganges
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ClientMicroservice acts as the client-side application that interacts with remote RMI services,
 * including AccountManager, WordServer, and CrosswordInterface. It also listens for game events
 * like win/loss notifications.
 */
public class ClientMicroservice { //extends UnicastRemoteObject implements GameEventListener 

    private String name = null; // Stores the player's username after login
    private String gameID = null; // Stores the active game ID if the player is in a game
    private GameState state = GameState.LOGIN; // Tracks the client's current state

    private AccountManager accountManager;
    private WordServer wordServer;
    private CrosswordGameState CrosswordGameState;
    private CrissCrossPuzzleServer server;

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
                        //registerEventListener(); // Register user-specific listener after login
                        state = GameState.READY;
                        System.out.println("Login successful! Welcome, " + name);
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
                System.err.println("Invalid input. Please enter a number.");
                scanner.nextLine(); // Consume invalid input
            }
        }
    }


	/**
     * Handles user interactions in the READY state.
     */
	private void handleReady() {
		System.out.println("Welcome, " + name + "! Choose an option:");
		System.out.println("1. View History");
		System.out.println("2. Lookup Word");
		System.out.println("3. Remove Word");
		System.out.println("4. Add Word");
		System.out.println("5. Start Singleplayer");
		System.out.println("6. Start Multiplayer");
		System.out.println("7. Join Multiplayer");
		System.out.println("8. Logout");
	
		int choice = scanner.nextInt();
		scanner.nextLine();
	
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
                    int numWords = scanner.nextInt();
                    System.out.print("Enter fail factor: ");
                    int failFactor = scanner.nextInt();
                    scanner.nextLine(); // consume newline

                    // Now call startGame on the instance
                    gameID = server.startGame(name, numWords, failFactor, null);

					if (gameID != null) {
						state = GameState.INGAME;
					}
					break;
				case 6:
					//gameID = server.startMultiplayer(name);/////////////////////////// TODO interface doesnt exist add # of players maybefixed
					//if (gameID != null) {
					//	state = GameState.INGAME;
					//}
					break;
                    case 7:
                    try {
                        // Retrieve available lobbies from the remote server
                        List<GameLobbyInfo> lobbies = server.listLobbies();
                        
                        if (lobbies.isEmpty()) {
                            System.out.println("No available lobbies at the moment. Please try again later.");
                        } else {
                            System.out.println("Available Lobbies:");
                            for (int i = 0; i < lobbies.size(); i++) {
                                System.out.println((i + 1) + ": " + lobbies.get(i).toString());
                            }
                            System.out.print("Enter the number of the lobby you wish to join: ");
                            choice = scanner.nextInt();
                            scanner.nextLine(); // consume the newline
                            
                            if (choice < 1 || choice > lobbies.size()) {
                                System.out.println("Invalid selection. Please try again.");
                            } else {
                                // Get the selected lobby's game ID
                                String selectedGameID = lobbies.get(choice - 1).getGameID();
                                // Attempt to join the lobby using the game ID
                                gameID = server.joinMultiplayer(name, selectedGameID);
                                if (gameID != null) {
                                    state = GameState.INGAME;
                                }
                            }
                        }
                    } catch (RemoteException e) {
                        System.err.println("Error listing or joining lobby: " + e.getMessage());
                    }
                    break;
				case 8:
					handleLogout();
					break;
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
        System.out.println("You are now in-game.");
        System.out.println("Type 'exit' to leave the game.");
        
        // Scheduled executor to poll the game state periodically.
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        // AtomicReference to store the last known game state for comparison.
        AtomicReference<CrosswordGameState> lastState = new AtomicReference<>();
    
        // Start polling immediately, then every 3 seconds.
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Retrieve the current game state from the remote service.
                CrosswordGameState currentState = server.getGameState(gameID); // This might throw RemoteException
                
                if (lastState.get() == null || !isStateEqual(currentState, lastState.get())) {
                    lastState.set(currentState);
                    printUserGame(gameID);
                }

            } catch (RemoteException e) {
                System.err.println("Error polling game state: " + e.getMessage());
            }
        }, 0, 3, TimeUnit.SECONDS);
    
        // Main in-game loop for user commands
        boolean inGameLoop = true;
        while (inGameLoop && state == GameState.INGAME) {
            System.out.println("\nEnter letter or word for guess"); 
            System.out.println("exit (Leave)");    
            System.out.println("? (Lookup)");   
            System.out.println("! (Restart)");

            String input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "exit":
                    System.out.print("This will count as a loss. Are you sure you would like to quit? (y/n): ");
                    input = scanner.nextLine().trim().toLowerCase();
                    if ("y".equals(input)) {
                        System.out.println("Exiting game...");
                        inGameLoop = false;
                        state = GameState.READY;
                        gameID = null;
                    }
                    break;

                case "!":
                    System.out.print("This will count as a loss. Are you sure you want to restart? (y/n): ");
                    input = scanner.nextLine().trim().toLowerCase();
                    if ("y".equals(input)) {
                        gameID = server.restartGame(gameID);
                    }
                    printUserGame(gameID);
                    break;

                case "?":
                    System.out.print("Enter word to look up: ");
                    String word = scanner.nextLine().trim(); // Store input separately
                    System.out.println(wordServer.checkWord(word));
                    break;

                case "":
                    System.out.println("\nInvalid Input\n"); // Use println for better formatting
                    break;

                default:
                    System.out.println("\nChecking word: " + input); // Use println instead of print
                    gameID = server.checkGuess(server.getGameState(gameID), input);

                    CrosswordGameState gameState = server.getGameState(gameID);

                    if ("WIN".equals(gameState.getGameStatus())) {  // Use constant on the left to avoid NullPointerException
                        System.out.println("GAME WON\n");
                        inGameLoop = false;
                        state = GameState.READY;
                        gameID = null;
                        } else if ("LOSE".equals(gameState.getGameStatus())) {
                        System.out.println("GAME LOST\n");
                        inGameLoop = false;
                    state = GameState.READY;
                        gameID = null;
                    } else {
                        printUserGame(gameID); // Consider removing if unnecessary
                    }
                    break;
            }
        }
        // Stop the polling once the game is exited.
        scheduler.shutdownNow();
    }

    // Helper method to compare game states
    private boolean isStateEqual(CrosswordGameState currentState, CrosswordGameState lastState) {
        // Null check to avoid NullPointerException
        if (currentState == null || lastState == null) {
            return false;
        }

        try {
            // Compare game states field by field
            boolean isEqual = currentState.getGameID().equals(lastState.getGameID()) &&
                              currentState.getLives() == lastState.getLives() &&
                              Arrays.deepEquals(currentState.getPlayerGrid(), lastState.getPlayerGrid()) &&
                              Arrays.equals(currentState.getLettersGuessed(), lastState.getLettersGuessed()) &&
                            Arrays.equals(currentState.getWordsGuessed(), lastState.getWordsGuessed());


            return isEqual;
        } catch (RemoteException e) {
            System.err.println("Error comparing game states: " + e.getMessage());
            return false;
        }
    }

    public void printUserGame(String gameID) throws RemoteException {
        CrosswordGameState gameServer = server.getGameState(gameID);
        System.out.print("\n");
        for (char[] row : gameServer.getPlayerGrid()) {
    		    System.out.println(new String(row) + "+");  // Append '+' at the end of each row
    		}
        System.out.print("\nLives: " + gameServer.getLives() + "\n");
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











