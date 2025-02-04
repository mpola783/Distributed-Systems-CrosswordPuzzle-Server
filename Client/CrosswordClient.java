
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.lang.*;

/**
 * @author Tin Toone
 * 
 *         COMP 4635 - Distributed Systems - Winter 2025
 * 
 *         Code Reference: Course Material provided by Instructor Maryam Elahi
 * 
 *         Description: This program creates a client that establishes a TCP
 *         connection to a interface server, allowing the user to play a
 *         crossword game.
 */
public class CrosswordClient {
	private static final String MENU_UI = "\n" + "START\n" + "HISTORY\n" + "LOOKUP\n" + "ADD\n" + "REMOVE\n";
	private static final String GAME_UI = "";
	private static final String WIN_MESSAGE = "";
	private static final String LOSS_MESSAGE = "";

	private static final String INVALID_CMD_ERR = "Invalid Command.";
	private static final String AUTH_ERR = "Incorrect or Invalid Username or Password. Please Try Again.";
	private static final String ATTEMPTS_ERR = "Max attempts reached.";

	private static final String LOGIN_USER_CMD = "LOGIN";

	private static final String START_GAME_CMD = "START";

	private static final String END_GAME_CMD = "#";

	private static final String LOG_LOSS_CMD = "LOSS";
	private static final String LOG_WIN_CMD = "WIN";
	
	private static final String GAME_RESPONSE_ERR = "FAIL";

	private static final String QUIT_CMD = "QUIT";

	private static final int INTERFACE_PORT = 69;

	private static final String INTERFACE_HOST = "localhost";

	private static final int DEFUALT_STATE = 1; // FOR TESTING

	private static final int QUIT_STATE = 0;
	private static final int LOGIN_STATE = 1;
	private static final int MENU_STATE = 2;
	private static final int GAME_STATE = 3;

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CrosswordClient instance = new CrosswordClient();
		instance.accessServer();
	}

	/**
	 * 
	 * @param userEntry
	 * @param fromServer
	 * @param toServer
	 * @return
	 */
	static int menu(Scanner userEntry, Scanner fromServer, PrintWriter toServer) {

		while (true) {
			System.out.print(MENU_UI);
			String cmd, response;

			do {
				cmd = userEntry.nextLine(); // send cmd to server
			} while (cmd.isBlank());
			toServer.println(cmd);

			if (cmd.equals(QUIT_CMD)) { // quit check
				return QUIT_STATE;
			}
			
			response = fromServer.nextLine(); // server response
			
			if (cmd.equals(START_GAME_CMD) && response.equals("SUCCESS")) { // start check
				return GAME_STATE;
			}
			
			System.out.println(response);
			
		}
	}

	/**
	 * 
	 * @param userEntry
	 * @param fromServer
	 * @param toServer
	 * @return
	 */
	static int game(Scanner userEntry, Scanner fromServer, PrintWriter toServer) {
		String cmd, response, gameResponse, gameCounter, gameState;
		while (true) {
			gameResponse = fromServer.nextLine();
			
			if (gameResponse.equals(LOG_WIN_CMD)) {
				System.out.println(WIN_MESSAGE);
				return MENU_STATE;
			} else if (gameResponse.equals(LOG_LOSS_CMD)) {
				System.out.println(LOSS_MESSAGE);
				return MENU_STATE;
			} else if (gameResponse.equals(GAME_RESPONSE_ERR)){
				System.out.println("Error receiving game state data.");
				return MENU_STATE;
			}
			
			gameCounter = fromServer.nextLine();
			gameState = fromServer.nextLine();

			String[] gameDisplay = gameState.split("+");
			
			for(String row: gameDisplay) {
				System.out.println(row);
			}
			
			System.out.println("Counter: " + gameCounter);

			response = fromServer.nextLine();
			System.out.println(response);

			do {
				cmd = userEntry.nextLine(); // send cmd to server
			} while (cmd.isBlank());
			toServer.println(cmd);

			String[] parsedQuery = cmd.split(" ");
			if (parsedQuery[0].equals(END_GAME_CMD)) {
				return MENU_STATE;
			} else if (parsedQuery[0].equals(QUIT_CMD)) {
				return QUIT_STATE;
			}
		}

	}

	/**
	 * 
	 * @param userEntry
	 * @param fromServer
	 * @param toServer
	 * @return
	 */
	static int login(Scanner userEntry, Scanner fromServer, PrintWriter toServer) {
		String message, response, cmd, auth;

		System.out.println("Logging in...");
		do {
			response = fromServer.nextLine(); // login prompt
			System.out.println(response);

			do {
				cmd = userEntry.nextLine(); // send cmd to server
			} while (cmd.isBlank());
			toServer.println(cmd);

			if (cmd.equals(QUIT_CMD)) { // quit check
				return QUIT_STATE;
			}

			response = fromServer.nextLine(); // username prompt or error
			System.out.println(response);
		} while (response.equals(INVALID_CMD_ERR));

		do {
			do {
				message = userEntry.nextLine(); // send username to server to server
			} while (message.isBlank());
			toServer.println(message);

			if (message.equals(QUIT_CMD)) { // quit check
				return QUIT_STATE;
			}

			response = fromServer.nextLine(); // password prompt
			System.out.println(response);

			do {
				message = userEntry.nextLine(); // send password to server to server
			} while (message.isBlank());
			toServer.println(message);

			if (message.equals(QUIT_CMD)) { // quit check
				return QUIT_STATE;
			}

			auth = fromServer.nextLine(); // error message or welcome
			System.out.println(auth);

			if (auth.equals(AUTH_ERR)) {
				if (cmd.equals(LOGIN_USER_CMD)) {
					response = fromServer.nextLine(); // attempts errors
					System.out.println(response);
					if (auth.equals(ATTEMPTS_ERR)) {
						return QUIT_STATE;
					}
				}
				response = fromServer.nextLine(); // username reprompt
				System.out.println(response);
			}
		} while (auth.equals(AUTH_ERR));

		return MENU_STATE;
	}

	/**
	 * 
	 */
	private static void accessServer() {
		Socket link = null;
		System.out.println("Connecting to Server...");

		try {
			link = new Socket(INTERFACE_HOST, INTERFACE_PORT);
			System.out.println("\nConnected.");

			Scanner fromServer = new Scanner(link.getInputStream());
			PrintWriter toServer = new PrintWriter(link.getOutputStream(), true);

			Scanner userEntry = new Scanner(System.in);

			int state = DEFUALT_STATE;

			do {
				switch (state) {
				case LOGIN_STATE:
					state = login(userEntry, fromServer, toServer);
					break;
				case MENU_STATE:
					state = menu(userEntry, fromServer, toServer);
					break;
				case GAME_STATE:
					state = game(userEntry, fromServer, toServer);
					break;
				default:
					state = QUIT_STATE;
					break;
				}
			} while (state != QUIT_STATE);

			userEntry.close();
			fromServer.close();
		} catch (IOException i) {
			System.err.println(i.getMessage());
		} finally {
			try {
				link.close();
			} catch (IOException i) {
				System.err.println(i.getMessage());
			}
		}
	}
}
