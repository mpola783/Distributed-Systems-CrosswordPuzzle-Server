
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
import java.net.MalformedURLException;
import game.Game;


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
    //private CrosswordGameState CrosswordGameState;
    private CrissCrossPuzzleServer server;
    private ReceiverInterface client; 
    private ReceiverInterface receiver;
    private volatile Game game;
    private static char[][] currentGrid;

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
            //CrosswordGameState = (CrosswordGameState) Naming.lookup("rmi://localhost/CrosswordGameState");
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
                        client = new ReceiverImpl(name); // Create the Receiver object
                        Naming.rebind("rmi://localhost/ReceiverInterface/" + name, client);
                        
                        
                        ReceiverImpl.registerGameHandler(updatedGame -> {
                            this.game = updatedGame;
                            if (state != GameState.INGAME) {
                                state = GameState.INGAME;
                            }
                            System.out.println("\n[SYNC] Game updated by another player:");
                            game.displayGrid("player");
                        });
                        
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
                System.err.println("Invalid input. Please enter a number.");
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
		System.out.println("5. Start Singleplayer // REMOVED FROM VERSION 4"); //TODO remove
		System.out.println("6. Join Multiplayer");
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
                     System.out.print("Singleplayer no longer supported");
                    // numWords = scanner.nextInt();
                    // System.out.print("Enter fail factor: ");
                    // failFactor = scanner.nextInt();
                    // scanner.nextLine(); // consume newline

                    // // Now call startGame on the instance
                    // gameID = server.startGame(name, numWords, failFactor, null);

					// if (gameID != null) {
					// 	state = GameState.INGAME;
					// }
					break;
				case 6:
                    System.out.print("Enter number of players: ");
                    players = scanner.nextInt();
                    System.out.print("Enter number of words: ");
                    numWords = scanner.nextInt();
                    System.out.print("Enter fail factor: ");
                    failFactor = scanner.nextInt();

                    System.out.print("Waiting for game");
					game = server.startMultiplayer(name, players, failFactor);
					
					state = GameState.INGAME;
					
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
        

        //////////////////// Dirty loop to wait for game ///////////////////
        for (int i = 0; i < 200; i++) {
            if (game != null) {
                break; // Game has loaded, exit loop
            }
        
            try {
                Thread.sleep(100); // Wait a bit before retrying (200 * 100ms = ~20s max wait)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupt flag
                break;
            }
        }
        
        // If game never loaded after waiting
        if (game == null) {
            System.out.println("Game failed to load. Returning to menu...");
            state = GameState.READY;
        }
        System.out.println("\n===================================");
        System.out.println("       Welcome! You are now in-game");
        System.out.println("===================================\n");
        
        System.out.println("Setting up the game... Please wait...");
        System.out.println("Starting game now!\n");
        
        System.out.println("===================================");
        System.out.println(" Player: " + name);
        System.out.println("===================================\n");
        
        System.out.println("  How to Play:\n");
        System.out.println("  - Enter a letter or word as your guess\n");
        System.out.println("  - Type '?'      Get a hint or lookup\n");
        System.out.println("  - Type '!'      Restart the game\n");
        System.out.println("  - Type 'exit'   Leave the game\n");
        
        
        System.out.println("\nYour Game Board:");
        game.displayGrid("player");
        Scanner scanner = new Scanner(System.in);
    
        // Main in-game loop for user commands
        boolean inGameLoop = true;
        while (inGameLoop && state == GameState.INGAME) {
            //gameState = server.getGameState(gameID); //TODO not needed
            
            if ("WIN".equals(game.getGameStatus()) || "LOSE".equals(game.getGameStatus())) {
                inGameLoop = false;
                state = GameState.READY;

                if ("LOSE".equals(game.getGameStatus())) {
                    System.out.println("GAME LOST\n");
                    accountManager.updateScore(name, false, true);
                }
                else {
                    System.out.println("GAME WON\n");
                    accountManager.updateScore(name, true, true);
                }
                
                //gameState.removePlayer(name);
                //if(gameState.getLobbySize() == 0) { from heartbeat quick exit, not needed
                //    server.exitGame(gameID);
                //    game = null;
                //}
                game = null; //exit game
            }

            else
            {   
                System.out.print("\nEnter guess: ");
                String input = scanner.nextLine().trim().toLowerCase();


                

                switch (input) {
                    case "exit":
                        System.out.print("This will count as a loss. Are you sure you would like to quit? (y/n): ");
                        input = scanner.nextLine().trim().toLowerCase();
                        if ("y".equals(input)) {
                            System.out.println("Exiting game...");
                            //gameState = server.getGameState(gameID);
                            accountManager.updateScore(name, false, true);
                            inGameLoop = false;
                            state = GameState.READY;
                            //gameState.removePlayer(name);
                            //if(gameState.getLobbySize() == 0) {
                            //    server.exitGame(gameID); //TODO idk?
                            //    game = null;
                            //}
                            game = null; //TODO new logic?
                        }
                        break;

                    case "!":
                        if(true) {
                            System.out.print("Restarting Multiplayer Games is not allowed");
                        }
                        // else{
                        //     System.out.print("This will count as a loss. Are you sure you want to restart? (y/n): ");
                        //     input = scanner.nextLine().trim().toLowerCase();
                        //     if ("y".equals(input)) {
                        //         //gameState = server.getGameState(gameID);
                        //         accountManager.updateScore(name, false, true);
                        //         gameID = server.restartGame(gameID);
                        //     }
                        //     printUserGame(gameID);
                        // }
                        break;

                    case "?":
                        System.out.print("Enter word to look up: ");
                        String word = scanner.nextLine().trim(); // Store input separately
                        System.out.println(wordServer.checkWord(word));
                        break;

                    case "":
                        break;

                    default:
                        System.out.println("Guess received: '" + input + "'\n");

                        
                        //currentGrid = server.getCurrentGrid(gameID); 
                        //client.sendMessage(gameState.getPlayerNames(), name, input); //TODO resend+fix
                        //gameID = server.updateGuess(server.getGameState(gameID), input); //TODO replace logic
                        game.checkGuess(input,name);
                        client.sendGame(game.getNamesOfPlayers(), name, game);
                        System.out.println("Updated Board:\n");
                        game.displayGrid("player");
                        break;
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
    // private boolean isStateEqual(CrosswordGameState currentState, CrosswordGameState lastState) {
    //     // Null check to avoid NullPointerException
    //     if (currentState == null || lastState == null) {
    //         return false;
    //     }

    //     try {
    //         // Compare game states field by field
    //         boolean isEqual = Objects.equals(currentState.getLives(), lastState.getLives()) &&
    //               Objects.equals(currentState.getGameID(), lastState.getGameID());
    //               System.out.println("\nLAST STATE LIVES: " + lastState.getLives());
    //               System.out.println("\nCURRENT STATE LIVES: " + currentState.getLives());
    //         return isEqual;
    //     } catch (RemoteException e) {
    //         System.err.println("Error comparing game states: " + e.getMessage());
    //         return false;
    //     }
    // }

    // public void printUserGame(String gameID) throws RemoteException {
    //     CrosswordGameState gameServer = server.getGameState(gameID);
        
    //     for (int i = 0; gameServer.getPlayerGrid() == null; i++) {
    //         try {
    //             Thread.sleep(1000);  // Sleep for 1 second
    //         } catch (InterruptedException e) {
    //             e.printStackTrace();  // Handle the exception (you can log it or re-throw it depending on your needs)
    //         }

    //         // Back to main menu in case of error
    //         if (i == 6) {
    //             System.out.println("Game Loading error");
    //             handleReady();
    //         }
    //     }

    //     System.out.print("\n");
    //     for (char[] row : gameServer.getPlayerGrid()) {
    // 		    System.out.println(new String(row) + "+");  // Append '+' at the end of each row
    // 		}
    //     System.out.print("\nLives: " + gameServer.getLives() + "\n");
    // }


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