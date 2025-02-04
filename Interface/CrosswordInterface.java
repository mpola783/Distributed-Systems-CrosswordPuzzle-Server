
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.*;

/**
 * @author Tin Toone
 * 
 *         COMP 4635 - Distributed Systems - Winter 2025
 * 
 *         Code Reference: Course Material provided by Instructor Maryam Elahi
 * 
 *         Description: This program provides a middle-man between a client node
 *         (connected by TCP) and a set of server nodes (connected by UDP or
 *         TCP), allowing the client to play a crossword game. Each client is
 *         handled by its own thread.
 * 
 *         Implementation Notes: - The user and interface both exist in one of
 *         four 'states'. + The login state, in which the client has not been
 *         verified as a user yet.
 * 
 */
public class CrosswordInterface {
	private static final String USAGE = "Usage: java CrosswordInterface";
	private static final String LOGIN_PROMPT = "LOGIN   OR   NEW   OR   QUIT";
	private static final String WELCOME_MESSAGE = "WELCOME!";

	private static final String INVALID_CMD_ERR = "Invalid Command.";
	private static final String AUTH_ERR = "Incorrect or Invalid Username or Password. Please Try Again.";
	private static final String ATTEMPTS_ERR = "Max attempts reached.";
	private static final String RESPONSE_ERR = "Error Getting Response.";
	private static final String GAME_SERVER_ERROR = "Game Disconnect Error.";

	private static final String GAME_RESPONSE_ERR = "FAIL";
	private static final String GAME_RESPONSE_CMD = "SUCCESS";
	private static final String GAME_CHECK_CMD = "CHECK";

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
	private static final int ACCOUNT_PORT = 6969;
	private static final int GAME_PORT = 420;
	private static final int WORD_PORT = 666;

	private static final String ACCOUNT_HOST = "localhost";
	private static final String GAME_HOST = "localhost";
	private static final String WORD_HOST = "localhost";

	private static final int DEFUALT_STATE = 1; // FOR TESTING

	private static final int QUIT_STATE = 0;
	private static final int LOGIN_STATE = 1;
	private static final int MENU_STATE = 2;
	private static final int GAME_STATE = 3;

	private static final int THREAD_COUNT = 20;
	private static final int MAX_ATTEMPTS = 10;

	public static void main(String[] args) throws IOException {
		if (args.length != 0) {
			System.err.println(USAGE);
			System.exit(1);
		}

		int port = INTERFACE_PORT;
		ServerSocket server = null;

		try {
			server = new ServerSocket(port);
			System.out.println("The Interface Server is running...");

			ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
			while (true) {
				fixedThreadPool.execute(new CrosswordClientHandler(server.accept()));
			}
		} catch (IOException i) {
			System.out.println(
					"Exception caught when trying to listen on port " + port + " or listening for a connection");
			System.out.println(i.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Server Closed.");
	}

	private static class CrosswordClientHandler implements Runnable {
		private Socket clientSocket;
		private String clientUsername;

		CrosswordClientHandler(Socket socket) {
			this.clientSocket = socket;
		}

		/**
		 * @param host
		 * 
		 *              The host of the target server.
		 * 
		 * @param port
		 * 
		 *              The target port at the target server.
		 * 
		 * @param query
		 * 
		 *              The query to be sent to the target server.
		 * 
		 * @return response
		 * 
		 *         The response received from the target server.
		 * 
		 */
		String handleUDP(String host, int port, String query) {
			try {
				// get a datagram socket
				DatagramSocket socket = new DatagramSocket();

				// send request
				byte[] requestBuf = new byte[BUFFER_LIMIT];
				requestBuf = query.getBytes();

				InetAddress address = InetAddress.getByName(host);
				DatagramPacket packet = new DatagramPacket(requestBuf, requestBuf.length, address, port);
				socket.send(packet);
				System.out.println("Packet sent to connected server for user: " + clientUsername);

				// get response
				byte[] responseBuf = new byte[BUFFER_LIMIT];
				packet = new DatagramPacket(responseBuf, requestBuf.length);
				socket.receive(packet);
				System.out.println("Packet receicved from connected server for user: " + clientUsername);

				socket.close();

				String received = new String(packet.getData(), 0, packet.getLength());

				return received;
			} catch (NumberFormatException n) {
				System.err.println("Invalid port number: " + port + ".");
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}

			return RESPONSE_ERR;

		}

		/**
		 * @param fromUser
		 * 
		 *                 This is an input stream, assumed to be receiving input from a
		 *                 client node.
		 * 
		 * @param toUser
		 * 
		 *                 This is an output stream, assumed to be sending output to a
		 *                 client node.
		 * 
		 * @return state
		 * 
		 *         This is an integer, representing the current thread's state.
		 * 
		 *         QUIT_STATE = 0 LOGIN_STATE = 1 MENU_STATE = 2 GAME_STATE = 3
		 * 
		 */
		int menu(BufferedReader fromUser, PrintStream toUser) {
			try {
				while (true) {
					fromUser.mark(BUFFER_LIMIT);
					String query = fromUser.readLine();
					System.out.println("Command received from user: " + clientUsername);

					String[] parsedQuery = query.split(" ");
					switch (parsedQuery[0]) {
					case START_GAME_CMD:
						if (parsedQuery.length == 1) {
							toUser.println(INVALID_CMD_ERR);
							System.out.println("INVALID_CMD_ERR sent to user: " + clientUsername);
							break;
						}
						toUser.println("SUCCESS");
						fromUser.reset(); // could do this on client side instead, by sending a duplicate cmd
						return GAME_STATE;
					case HISTORY_CMD:
						toUser.println(handleUDP(ACCOUNT_HOST, ACCOUNT_PORT, query));
						System.out.println("Response sent to user: " + clientUsername);
						break;
					case LOOKUP_WORD_CMD:
						if (parsedQuery.length == 1) {
							toUser.println(INVALID_CMD_ERR);
							System.out.println("INVALID_CMD_ERR sent to user: " + clientUsername);
							break;
						}
						toUser.println(handleUDP(WORD_HOST, WORD_PORT, query));
						System.out.println("Response sent to user: " + clientUsername);
						break;
					case ADD_WORD_CMD:
						if (parsedQuery.length == 1) {
							toUser.println(INVALID_CMD_ERR);
							break;
						}
						toUser.println(handleUDP(WORD_HOST, WORD_PORT, query));
						System.out.println("Response sent to user: " + clientUsername);
						break;
					case REMOVE_WORD_CMD:
						if (parsedQuery.length == 1) {
							toUser.println(INVALID_CMD_ERR);
							System.out.println("INVALID_CMD_ERR sent to user: " + clientUsername);
							break;
						}
						toUser.println(handleUDP(WORD_HOST, WORD_PORT, query));
						System.out.println("Response sent to user: " + clientUsername);
						break;
					case QUIT_CMD:
						return QUIT_STATE;
					default:
						toUser.println(INVALID_CMD_ERR);
						System.out.println("INVALID_CMD_ERR sent to user: " + clientUsername);
					}
				}
			} catch (IOException i) {
				return QUIT_STATE;
			}
		}

		/**
		 * @param fromUser
		 * 
		 *                 This is an input stream, assumed to be receiving input from a
		 *                 client node.
		 * 
		 * @param toUser
		 * 
		 *                 This is an output stream, assumed to be sending output to a
		 *                 client node.
		 * 
		 * @return state
		 * 
		 *         This is an integer, representing the current thread's state.
		 * 
		 *         QUIT_STATE = 0 LOGIN_STATE = 1 MENU_STATE = 2 GAME_STATE = 3
		 * 
		 */
		int game(BufferedReader fromUser, PrintStream toUser) {
			Socket link = null;

			try {
				link = new Socket(GAME_HOST, GAME_PORT);

				BufferedReader fromGame = new BufferedReader(new InputStreamReader(link.getInputStream()));
				PrintWriter toGame = new PrintWriter(link.getOutputStream(), true);
				System.out.println("User " + clientUsername + " connected to Game Server.");

				String response = "";
				String gameResponse = "";

				String gameSetting = fromUser.readLine(); // initial game set-up
				System.out.println("Received game settings from user: " + clientUsername);

				toGame.println(gameSetting);
				System.out.println("Sending user " + clientUsername + " settings to Game Server...");
				System.out.println("Settings: " + gameSetting);
				
				gameResponse = fromGame.readLine();
				String[] parsedGameResponse = gameResponse.split(" ");
				
				System.out.println("Game server responded to user: " + clientUsername);
				System.out.println("Response: " + parsedGameResponse[0]);
				
				if (parsedGameResponse[0].equals(GAME_RESPONSE_ERR)) {
					System.out.println("Error receiving game state data for user: " + clientUsername);
					return MENU_STATE;
				}
				
				String gameCounter = parsedGameResponse[1];
				System.out.println("Game Counter returned from Game Server for user: " + clientUsername);
				System.out.println("Counter: " + gameCounter);

				String gameState = parsedGameResponse[2];
				System.out.println("Game State returned from Game Server for user: " + clientUsername);
				System.out.println("Game State: " + gameState);

				while (true) {
					toUser.println(parsedGameResponse[0]);
					System.out.println("Game response: \"" + parsedGameResponse[0] + "\" sent to user: " + clientUsername);

					toUser.println(gameCounter);
					System.out.println("Game counter: \"" + gameCounter + "\" sent to user: " + clientUsername);

					toUser.println(gameState);
					System.out.println("Game State sent to user: " + clientUsername);

					toUser.println(response);
					System.out.println("UDP response: \"" + response + "\" sent to user: " + clientUsername);

					String query = fromUser.readLine();
					String[] parsedQuery = query.split(" ");

					switch (parsedQuery[0]) {
					case END_GAME_CMD:
						handleUDP(ACCOUNT_HOST, ACCOUNT_PORT, LOG_LOSS_CMD);
						try {
							link.close();
						} catch (IOException i) {
							System.err.println(GAME_SERVER_ERROR);
						}
						return MENU_STATE;
					case RESTART_GAME_CMD:
						handleUDP(ACCOUNT_HOST, ACCOUNT_PORT, LOG_LOSS_CMD);

						toGame.println(gameSetting);
						System.out.println("Sending user " + clientUsername + " settings to Game Server...");
						System.out.println("Settings: " + gameSetting);
						
						gameResponse = fromGame.readLine();
						parsedGameResponse = gameResponse.split(" ");
						System.out.println("Game server responded to user: " + clientUsername);
						System.out.println("Response: " + parsedGameResponse[0]);
						
						if (parsedGameResponse[0].equals(GAME_RESPONSE_ERR)) {
							System.out.println("Error receiving game state data for user: " + clientUsername);
							return MENU_STATE;
						}
						
						gameCounter = parsedGameResponse[1];
						System.out.println("Game Counter returned from Game Server for user: " + clientUsername);
						System.out.println("Counter: " + gameCounter);

						gameState = parsedGameResponse[2];
						System.out.println("Game State returned from Game Server for user: " + clientUsername);
						System.out.println("Game State: " + gameState);
						break;
					case CHECK_SCORE_CMD:
						response = handleUDP(ACCOUNT_HOST, ACCOUNT_PORT, query);
						break;
					case CHECK_WORD_CMD:
						if (parsedQuery.length == 1) {
							response = INVALID_CMD_ERR;
							break;
						}
						response = handleUDP(WORD_HOST, WORD_PORT, query);
						break;
					case WORD_REGEX:

						toGame.println(gameSetting);
						System.out.println("Sending user " + clientUsername + " settings to Game Server...");
						System.out.println("Settings: " + gameSetting);
						
						gameResponse = fromGame.readLine();
						parsedGameResponse = gameResponse.split(" ");
						
						System.out.println("Game server responded to user: " + clientUsername);
						System.out.println("Response: " + parsedGameResponse[0]);
						
						if (parsedGameResponse[0].equals(GAME_RESPONSE_ERR)) {
							System.out.println("Error receiving game state data for user: " + clientUsername);
							return MENU_STATE;
						}
						
						if (gameResponse.equals(LOG_WIN_CMD)) {

							toUser.println(gameState);
							System.out.println("Win Message sent to user: " + clientUsername);

							handleUDP(ACCOUNT_HOST, ACCOUNT_PORT, LOG_WIN_CMD);
							try {
								link.close();
							} catch (IOException i) {
								System.err.println(GAME_SERVER_ERROR);
							}
							return MENU_STATE;
						} else if (gameResponse.equals(LOG_LOSS_CMD)) {
							toUser.println(gameState);
							System.out.println("Loss Message sent to user: " + clientUsername);

							handleUDP(ACCOUNT_HOST, ACCOUNT_PORT, LOG_LOSS_CMD);
							try {
								link.close();
							} catch (IOException i) {
								System.err.println(GAME_SERVER_ERROR);
							}
							return MENU_STATE;
						}
						gameCounter = parsedGameResponse[1];
						System.out.println("Game Counter returned from Game Server for user: " + clientUsername);
						System.out.println("Counter: " + gameCounter);

						gameState = parsedGameResponse[2];
						System.out.println("Game State returned from Game Server for user: " + clientUsername);
						System.out.println("Game State: " + gameState);
					case QUIT_CMD:
						handleUDP(ACCOUNT_HOST, ACCOUNT_PORT, LOG_LOSS_CMD);
						try {
							link.close(); // Step 4.
						} catch (IOException i) {
							System.err.println(GAME_SERVER_ERROR);
						}
						return QUIT_STATE;
					default:
						response = INVALID_CMD_ERR;
						break;
					}
				}
			} catch (IOException i) {
				System.err.println(i.getMessage());
			} finally {
				try {
					link.close(); // Step 4.
				} catch (IOException i) {
					System.out.println("Error: " + i.getMessage());
					return MENU_STATE;
				}
			}

			try {
				link.close();
				return MENU_STATE;
			} catch (IOException i) {
				System.out.println("Error: " + i.getMessage());
				return MENU_STATE;
			}
		}

		/**
		 * @param fromUser
		 * 
		 *                 This is an input stream, assumed to be receiving input from a
		 *                 client node.
		 * 
		 * @param toUser
		 * 
		 *                 This is an output stream, assumed to be sending output to a
		 *                 client node.
		 * 
		 * @return state
		 * 
		 *         This is an integer, representing the current thread's state.
		 * 
		 *         QUIT_STATE = 0 LOGIN_STATE = 1 MENU_STATE = 2 GAME_STATE = 3
		 * 
		 */
		int login(BufferedReader fromUser, PrintStream toUser) {
			String host = ACCOUNT_HOST;
			int port = ACCOUNT_PORT;

			int valid = LOGIN_STATE;

			int attempts = 0;

			try {
				String clientCMD;

				do {
					toUser.println(LOGIN_PROMPT);
					System.out.println("Login prompt sent to socket: " + clientSocket);

					clientCMD = fromUser.readLine();
					System.out.println("Response received from socket: " + clientSocket);

					if (clientCMD.equals(QUIT_CMD)) {
						return QUIT_STATE;
					}

					if (!clientCMD.equals(LOGIN_USER_CMD) && !clientCMD.equals(ADD_USER_CMD)) {
						toUser.println(INVALID_CMD_ERR);
						System.out.println("INVALID_CMD_ERR sent to socket: " + clientSocket);
					}
				} while (!clientCMD.equals(LOGIN_USER_CMD) && !clientCMD.equals(ADD_USER_CMD));

				DatagramSocket socket = new DatagramSocket();

				String username;
				String password;

				do {
					toUser.println("Username: ");
					System.out.println("Username prompt sent to socket: " + clientSocket);

					username = fromUser.readLine();
					System.out.println("Response received from socket: " + clientSocket);

					if (username.equals(QUIT_CMD)) {
						return QUIT_STATE;
					}

					toUser.println("Password: ");
					System.out.println("Password prompt sent to socket: " + clientSocket);

					password = fromUser.readLine();
					System.out.println("Response received from socket: " + clientSocket);

					if (password.equals(QUIT_CMD)) {
						return QUIT_STATE;
					}

					byte[] userBuf = new byte[BUFFER_LIMIT];

					if (clientCMD.equals(LOGIN_USER_CMD)) {
						userBuf = (LOGIN_USER_CMD + " " + username + " " + password).getBytes();
					} else if (clientCMD.equals(ADD_USER_CMD)) {
						userBuf = (ADD_USER_CMD + " " + username + " " + password).getBytes();
					}

					InetAddress address = InetAddress.getByName(host);
					DatagramPacket packet = new DatagramPacket(userBuf, userBuf.length, address, port);

					socket.send(packet);
					System.out.println("Verification packet sent to Account Server.");

					// get response
					byte[] responseBuf = new byte[BUFFER_LIMIT];
					packet = new DatagramPacket(responseBuf, userBuf.length);
					socket.receive(packet);

					String received = new String(packet.getData(), 0, packet.getLength());

					System.out.println("Account Server Response \"" + received + "\" for socket: " + clientSocket);

					if (received.endsWith("1")) {
						valid = MENU_STATE;
						clientUsername = username;
						toUser.println(WELCOME_MESSAGE);
						System.out.println("WELCOME_MESSAGE sent to socket: " + clientSocket);
					}

					if (valid == LOGIN_STATE) {
						toUser.println(AUTH_ERR);
						System.out.println("AUTH_ERR sent to socket: " + clientSocket);
						if (clientCMD.equals(LOGIN_USER_CMD)) {
							attempts++;
							if (attempts <= MAX_ATTEMPTS) {
								toUser.println((MAX_ATTEMPTS - attempts) + " attempts left.");
								System.out.println("Remaining attempts sent to socket: " + clientSocket);
							}
						}
					}
				} while (valid == LOGIN_STATE && attempts <= MAX_ATTEMPTS);

				if (valid == LOGIN_STATE) {
					toUser.println(ATTEMPTS_ERR);
					System.out.println("ATTEMPTS_ERR sent to socket: " + clientSocket);
					valid = QUIT_STATE;
				}

				socket.close();
			} catch (NumberFormatException n) {
				System.err.println("Invalid port number: " + port + ".");
				return QUIT_STATE;
			} catch (IOException i) {
				System.out.println("Error: " + i.getMessage());
				return QUIT_STATE;
			} catch (Exception e) {
				System.err.println("Error: " + e.getMessage());
				return QUIT_STATE;
			}

			return valid;
		}

		@Override
		public void run() {
			System.out.println("Connected, handling new client: " + clientSocket);
			try {
				PrintStream toUser = new PrintStream(clientSocket.getOutputStream());
				BufferedReader fromUser = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

				int state = DEFUALT_STATE;

				do {
					switch (state) {
					case LOGIN_STATE:
						state = login(fromUser, toUser);
						break;
					case MENU_STATE:
						state = menu(fromUser, toUser);
						break;
					case GAME_STATE:
						state = game(fromUser, toUser);
					default:
						state = QUIT_STATE;
						break;
					}
				} while (state != QUIT_STATE);

				fromUser.close();
				toUser.close();
			} catch (SocketException e) {
				System.out.println("Error: " + e.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					clientSocket.close();
				} catch (IOException i) {

				}
				System.out.println("Closed: " + clientSocket);
			}
		}
	}
}
