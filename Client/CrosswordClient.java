
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class CrosswordClient {
	private static final String USAGE = "java CrosswordClient";

	private static final String MENU_UI = "\n" + "START\n" + "HISTORY\n" + "LOOKUP\n" + "ADD\n" + "REMOVE\n";
	private static final String GAME_UI = "";
	private static final String WIN_MESSAGE = "";
	private static final String LOSS_MESSAGE = "";
	
	
	private static final String INVALID_CMD_ERR = "Invalid Command.";
	private static final String AUTH_ERR = "Incorrect or Invalid Username or Password. Please Try Again.";
	private static final String ATTEMPTS_ERR = "Max attempts reached.";
	private static final String RESPONSE_ERR = "Error Getting Response.";
	private static final String GENERIC_ERR = "An error has occured.";

	private static final String LOGIN_USER_CMD = "LOGIN";
	private static final String ADD_USER_CMD = "NEW";

	private static final String START_GAME_CMD = "START";
	private static final String HISTORY_CMD = "HISTORY";

	private static final String LOOKUP_WORD_CMD = "LOOKUP";
	private static final String ADD_WORD_CMD = "ADD";
	private static final String REMOVE_WORD_CMD = "REMOVE";

	private static final String END_GAME_CMD = "#";
	private static final String RESTART_GAME_CMD = "!";
	private static final String CHECK_SCORE_CMD = "$";
	private static final String CHECK_WORD_CMD = "?";
	private static final String WORD_REGEX = "[Aa-zA-Z]+";
	
	private static final String LOG_LOSS_CMD = "LOSS";
	private static final String LOG_WIN_CMD = "WIN";

	private static final String QUIT_CMD = "QUIT";

	private static final int BUFFER_LIMIT = 1000;

	private static final int INTERFACE_PORT = 69;

	private static final String INTERFACE_HOST = "localhost";

	private static final int QUIT_STATE = 0;
	private static final int LOGIN_STATE = 1;
	private static final int MENU_STATE = 2;
	private static final int GAME_STATE = 3;

	public static void main(String[] args) {
		CrosswordClient instance = new CrosswordClient();
		instance.accessServer();
	}

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

			if (cmd.equals(START_GAME_CMD)) { // start check
				return GAME_STATE;
			}

			while (true) {
				response = fromServer.nextLine(); // server response
				System.out.println(response);
			}
		}
	}

	static int game(Scanner userEntry, Scanner fromServer, PrintWriter toServer) {
		String cmd, response, gameState;
		while (true) {

			gameState = fromServer.nextLine();
			if(gameState.equals(LOG_WIN_CMD)) {
				System.out.println(WIN_MESSAGE);
				return MENU_STATE;
			} else if(gameState.equals(LOG_LOSS_CMD)) {
				System.out.println(LOSS_MESSAGE);
				return MENU_STATE;
			}
			
			String gameDisplay = gameState.replace("+", "\n");
			System.out.println(gameDisplay);

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

	static int login(Scanner userEntry, Scanner fromServer, PrintWriter toServer) {
		String message, response, cmd, auth;

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

	private static void accessServer() {
		Socket link = null; // Step 1.
		System.out.println("Connecting to Server...");

		try {
			link = new Socket(INTERFACE_HOST, INTERFACE_PORT); // Step 1.
			System.out.println("\nConnected.");

			Scanner fromServer = new Scanner(link.getInputStream()); // Step 2.
			PrintWriter toServer = new PrintWriter(link.getOutputStream(), true); // Step 2.

			Scanner userEntry = new Scanner(System.in);

			int state = LOGIN_STATE;

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
			// handle exception
		} finally {
			try {
				link.close(); // Step 4.
			} catch (IOException i) {
				// handle exception
			}
		}
	}
}
