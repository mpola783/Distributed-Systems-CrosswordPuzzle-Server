import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * ClientMicroservice acts as the client-side application that interacts with remote RMI services,
 * including AccountManager, WordServer, and CrosswordInterface. It also listens for game events
 * like win/loss notifications.
 */
public class ClientMicroservice extends UnicastRemoteObject implements GameEventListener {

    private String name = null; // Stores the player's username after login
    private String gameID = null; // Stores the active game ID if the player is in a game
    private GameState state = GameState.LOGIN; // Tracks the client's current state

    private AccountManager accountManager;
    private WordServer wordServer;
    private CrosswordInterface crosswordInterface;

    private Scanner scanner = new Scanner(System.in); // Scanner instance for user input
    private BlockingQueue<String> eventQueue = new LinkedBlockingQueue<>(); // Event queue for handling game events

    /**
     * Enum representing different states of the client.
     */
    private enum GameState {
        LOGIN, READY, INGAME
    }

    /**
     * Constructor initializes the client and binds itself as a GameEventListener for notifications.
     */
    protected ClientMicroservice() throws RemoteException {
        super(); //esentially UnicastRemoteObject.exportObject(this,0). UnicastRemoteObject does not have a 0 argument constructer so super must be called to accept rmi game updates
        try {
            accountManager = (AccountManager) Naming.lookup("rmi://localhost/AccountManager");
            wordServer = (WordServer) Naming.lookup("rmi://localhost/WordServer");
            crosswordInterface = (CrosswordInterface) Naming.lookup("rmi://localhost/CrosswordInterface");
        } catch (Exception e) {
            System.err.println("Error connecting to RMI services: " + e.getMessage());
            e.printStackTrace();
        }

        // Start background thread for event handling
        new Thread(() -> {
            while (true) {
                processGameEvents();
            }
        }, "GameEventProcessor").start();

        // Ensure scanner is closed when exiting
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scanner.close();
        }));
    }

	/**
     * Registers this client instance as an event listener for its specific username for accepting W/L events.
     */
    private void registerEventListener() {
        if (name != null) {
            try {
                Naming.rebind("rmi://localhost/GameEventListener_" + name, this);
                System.out.println("Registered as event listener: GameEventListener_" + name);
            } catch (Exception e) {
                System.err.println("Error binding event listener: " + e.getMessage());
            }
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
     * Processes game events from the queue.
     */
    private void processGameEvents() {
        try {
            String event = eventQueue.take(); // Blocks until an event is available
            System.out.println("Game event received: " + event);
            if (event.equals("WIN")) {
                System.out.println("Congratulations, " + name + "! You won the game!");
            } else if (event.equals("LOSE")) {
                System.out.println("Game over, " + name + ". Better luck next time!");
            }
            state = GameState.READY;
            gameID = null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Error processing game event: " + e.getMessage());
        }
    }


    /**
     * Handles logout by unregistering the event listener and resetting the client state.
     */
    private void handleLogout() {
        unregisterEventListener();
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
                        registerEventListener(); // Register user-specific listener after login
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
					System.out.println(wordServer.lookupWord(scanner.nextLine()));
					break;
				case 3:
					System.out.print("Enter word to remove: ");
					System.out.println(wordServer.removeWord(scanner.nextLine()));
					break;
				case 4:
					System.out.print("Enter word to add: ");
					System.out.println(wordServer.addWord(scanner.nextLine()));
					break;
				case 5:
					gameID = crosswordInterface.startSingleplayer(name); ///////////////////////// TODO interface doesnt exist
					if (gameID != null) {
						state = GameState.INGAME;
					}
					break;
				case 6:
					gameID = crosswordInterface.startMultiplayer(name);/////////////////////////// TODO interface doesnt exist add # of players
					if (gameID != null) {
						state = GameState.INGAME;
					}
					break;
				case 7:
					System.out.print("Enter game ID: ");
					gameID = crosswordInterface.joinMultiplayer(name, scanner.nextLine());//////////////////// TODO interface doesnt exist
					if (gameID != null) {
						state = GameState.INGAME;
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
    private void handleInGame() {
        System.out.println("TODO: Implement handleInGame() logic");
    }

    /**
     * Listens for game win/loss events and places them into the event queue to be processed.
     */
    @Override
    public void onGameEvent(String username, String event) throws RemoteException {
        if (username.equals(this.name)) { //a little redundant due to refactoring the rmi address to name but too superstitious to cut
            eventQueue.add(event); // Add event to the queue for processing
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











