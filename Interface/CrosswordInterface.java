
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.*;

public class CrosswordInterface {
	private static final String USAGE = "Usage: java CrosswordInterface";
	private static final String LOGIN_PROMPT = "LOGIN   OR   NEW   OR   QUIT";
	private static final String WELCOME_MESSAGE = "WELCOME!";

	private static final String INVALID_CMD_ERR = "Invalid Command.";
	private static final String AUTH_ERR = "Incorrect or Invalid Username or Password. Please Try Again.";
	private static final String ATTEMPTS_ERR = "Max attempts reached.";
	private static final String RESPONSE_ERR = "Error Getting Response.";
	private static final String GAME_SERVER_ERROR = "Game Disconnect Error.";

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

	private static final String GET_GAME_CMD = "";

	private static final String QUIT_CMD = "QUIT";

	private static final int BUFFER_LIMIT = 1000;

	private static final int INTERFACE_PORT = 69;

	private static final int ACCOUNT_PORT = 6969;
	private static final int GAME_PORT = 420;
	private static final int WORD_PORT = 666;

	private static final String ACCOUNT_HOST = "localhost";
	private static final String GAME_HOST = "localhost";
	private static final String WORD_HOST = "localhost";

	private static final int DEFUALT_STATE = 2; // FOR TESTING

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
			System.out.println("The server is running...");

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

				// get response
				byte[] responseBuf = new byte[BUFFER_LIMIT];
				packet = new DatagramPacket(responseBuf, requestBuf.length);
				socket.receive(packet);

				socket.close();

				return packet.toString();
			} catch (NumberFormatException n) {
				System.err.println("Invalid port number: " + port + ".");
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}

			return RESPONSE_ERR;

		}

		int menu(BufferedReader fromUser, PrintStream toUser) {
			try {
				while (true) {
					fromUser.mark(BUFFER_LIMIT);
					String query = fromUser.readLine();
					String[] parsedQuery = query.split(" ");
					switch (parsedQuery[0]) {
					case START_GAME_CMD:
						fromUser.reset(); // could do this on client side instead, by sending a duplicate cmd
						return GAME_STATE;
					case HISTORY_CMD:
						toUser.println(handleUDP(ACCOUNT_HOST, ACCOUNT_PORT, query));
						break;
					case LOOKUP_WORD_CMD:
						toUser.println(handleUDP(WORD_HOST, WORD_PORT, query));
						break;
					case ADD_WORD_CMD:
						toUser.println(handleUDP(WORD_HOST, WORD_PORT, query));
						break;
					case REMOVE_WORD_CMD:
						toUser.println(handleUDP(WORD_HOST, WORD_PORT, query));
						break;
					case QUIT_CMD:
						return QUIT_STATE;
					default:
						toUser.println(INVALID_CMD_ERR);
					}
				}
			} catch (IOException i) {
				return QUIT_STATE;
			}
		}

		int game(BufferedReader fromUser, PrintStream toUser) {
			Socket link = null;

			try {
				link = new Socket(GAME_HOST, GAME_PORT);

				BufferedReader fromGame = new BufferedReader(new InputStreamReader(link.getInputStream()));
				PrintWriter toGame = new PrintWriter(link.getOutputStream());

				String gameSetting = fromUser.readLine(); // initial game set-up
				toGame.println(gameSetting);
				String gameState = fromGame.readLine();
				String response = "";

				while (true) {
					toUser.println(gameState);
					toUser.println(response);

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
					case CHECK_SCORE_CMD:
						response = handleUDP(ACCOUNT_HOST, ACCOUNT_PORT, query);
					case CHECK_WORD_CMD:
						response = handleUDP(WORD_HOST, WORD_PORT, query);
					case WORD_REGEX:
						toGame.println(query);
						gameState = fromGame.readLine();
						if (gameState == LOG_WIN_CMD) {
							handleUDP(ACCOUNT_HOST, ACCOUNT_PORT, LOG_WIN_CMD);
							try {
								link.close();
							} catch (IOException i) {
								System.err.println(GAME_SERVER_ERROR);
							}
							return MENU_STATE;
						} else if (gameState == LOG_LOSS_CMD) {
							handleUDP(ACCOUNT_HOST, ACCOUNT_PORT, LOG_LOSS_CMD);
							try {
								link.close();
							} catch (IOException i) {
								System.err.println(GAME_SERVER_ERROR);
							}
							return MENU_STATE;
						}
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
					}

					toGame.println();
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
				link.close(); // Step 4.
				return MENU_STATE;
			} catch (IOException i) {
				System.out.println("Error: " + i.getMessage());
				return MENU_STATE;
			}

		}

		int login(BufferedReader fromUser, PrintStream toUser) {
			String host = ACCOUNT_HOST;
			int port = ACCOUNT_PORT;

			int valid = LOGIN_STATE;

			int attempts = 0;

			try {
				String clientCMD;

				do {
					toUser.println(LOGIN_PROMPT);
					clientCMD = fromUser.readLine();
					if (clientCMD.equals(QUIT_CMD)) {
						return QUIT_STATE;
					}

					if (!clientCMD.equals(LOGIN_USER_CMD) && !clientCMD.equals(ADD_USER_CMD)) {
						toUser.println(INVALID_CMD_ERR);
					}

				} while (!clientCMD.equals(LOGIN_USER_CMD) && !clientCMD.equals(ADD_USER_CMD));

				DatagramSocket socket = new DatagramSocket();

				String username;
				String password;

				do {
					toUser.println("Username: ");
					username = fromUser.readLine();

					if (username.equals(QUIT_CMD)) {
						return QUIT_STATE;
					}

					toUser.println("Password: ");
					password = fromUser.readLine();

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

					// get response
					byte[] responseBuf = new byte[BUFFER_LIMIT];
					packet = new DatagramPacket(responseBuf, userBuf.length);
					socket.receive(packet);

					if (Boolean.parseBoolean(packet.toString())) {
						valid = MENU_STATE;
						clientUsername = username;
						toUser.println(WELCOME_MESSAGE);
					}

					if (valid == LOGIN_STATE) {
						toUser.println(AUTH_ERR);
						if (clientCMD.equals(LOGIN_USER_CMD)) {
							attempts++;
							if (attempts <= MAX_ATTEMPTS) {
								toUser.println((MAX_ATTEMPTS - attempts) + " attempts left.");
							}
						}
					}
				} while (valid == LOGIN_STATE && attempts <= MAX_ATTEMPTS);

				if (valid == LOGIN_STATE) {
					toUser.println(ATTEMPTS_ERR);
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
