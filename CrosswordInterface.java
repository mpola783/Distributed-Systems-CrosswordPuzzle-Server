
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Scanner;

import java.io.*;

public class CrosswordInterface {
	private static final String USAGE = "Usage: java MultithreadReverseEchoServer [port]";
	private static final String LOGIN_QUERY = "LOGIN  or  NEWACCOUNT";
	private static final String INVALID_COMMAND = "Invalid Command.";
	private static final String RESPONSE_ERROR = "Error Getting Response.";
	private static final String VALIDATE_USER_CMD = "CHECK ";
	private static final String ADD_USER_CMD = "ADD ";

	private static final int BUFFER_LIMIT = 1000;

	private static final int ACCOUNT_PORT = 6969;
	private static final int GAME_PORT = 420;
	private static final int WORD_PORT = 666;

	private static final String ACCOUNT_HOST = "localhost";
	private static final String GAME_HOST = "localhost";
	private static final String WORD_HOST = "localhost";

	private static final int QUIT_STATE = 0;
	private static final int LOGIN_STATE = 1;
	private static final int MENU_STATE = 2;
	private static final int GAME_STATE = 3;

	private static final int MAX_ATTEMPTS = 10;

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println(USAGE);
			System.exit(1);
		}

		int port = 0;
		ServerSocket server = null;

		try {
			port = Integer.parseInt(args[0]);
			server = new ServerSocket(port);
			System.out.println("The server is running...");

			ExecutorService fixedThreadPool = Executors.newFixedThreadPool(20);
			while (true) {
				fixedThreadPool.execute(new CrosswordClientHandler(server.accept()));
			}

		} catch (IOException e) {
			System.out.println(
					"Exception caught when trying to listen on port " + port + " or listening for a connection");
			System.out.println(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static class CrosswordClientHandler implements Runnable {
		private Socket clientSocket;

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
			} catch (NumberFormatException e) {
				System.err.println("Invalid port number: " + port + ".");
				System.exit(1);
			} catch (Exception e) {
				System.err.println(e.getMessage());
				System.exit(1);
			}

			return RESPONSE_ERROR;
		}

		int menu(Scanner in, PrintStream out) {
			while (true) {
				String query = in.nextLine();
				String[] parsedQuery = query.split(" ");
				switch (parsedQuery[0]) {
				case "START":
					return GAME_STATE;
				case "HISTORY":
					out.print(handleUDP(ACCOUNT_HOST, ACCOUNT_PORT, query));
				case "CHECK":
					out.print(handleUDP(WORD_HOST, WORD_PORT, query));
				case "ADD":
					out.print(handleUDP(WORD_HOST, WORD_PORT, query));
				case "REMOVE":
					out.print(handleUDP(WORD_HOST, WORD_PORT, query));
				case "QUIT":
					return QUIT_STATE;
				default:
					out.println(INVALID_COMMAND);
				}
			}
		}

		int game(Scanner in, PrintStream out) {
			while (true) {
				String query = in.nextLine();
				if (query.startsWith("?")) {
					query.replace("?", "? ");
				}
				String[] parsedQuery = query.split(" ");

				switch (parsedQuery[0]) {
				case "#":
					return MENU_STATE;
				case "!":

				case "$":

				case "?":

				case "QUIT":
					return QUIT_STATE;
				default:
					out.println(INVALID_COMMAND);
				}
			}

		}

		Boolean login(Scanner in, PrintStream out) {

			String host = ACCOUNT_HOST;
			int port = ACCOUNT_PORT;

			Boolean valid = false;

			int attempts = 0;

			out.println(LOGIN_QUERY);
			String response = in.nextLine();

			while (!response.equals("LOGIN") && !response.equals("NEWACCOUNT") && !response.equals("QUIT")) {
				out.println(INVALID_COMMAND);
			}

			try {
				// get a datagram socket
				DatagramSocket socket = new DatagramSocket();

				out.print("Username: ");
				String username = in.nextLine();

				out.print("Password: ");
				String password = in.nextLine();

				while (!valid && attempts < MAX_ATTEMPTS) {

					// send request
					byte[] userBuf = new byte[BUFFER_LIMIT];
					if (response.equals("LOGIN")) {
						userBuf = (VALIDATE_USER_CMD + username + password).getBytes();
					} else if (response.equals("NEWACCOUNT")) {
						userBuf = (ADD_USER_CMD + username + password).getBytes();
					}

					InetAddress address = InetAddress.getByName(host);
					DatagramPacket packet = new DatagramPacket(userBuf, userBuf.length, address, port);
					socket.send(packet);

					// get response
					byte[] responseBuf = new byte[BUFFER_LIMIT];
					packet = new DatagramPacket(responseBuf, userBuf.length);
					socket.receive(packet);

					if (Boolean.parseBoolean(packet.toString())) {
						valid = true;
					}

					if (!valid) {
						out.println(packet.toString());
						if(response.equals("LOGIN")) {
							attempts++;
							out.println(attempts + "attempts left. \n");
						}
					}
				}

				socket.close();

			} catch (NumberFormatException e) {
				System.err.println("Invalid port number: " + port + ".");
				System.exit(1);
			} catch (Exception e) {
				System.err.println(e.getMessage());
				System.exit(1);
			}

			return valid;
		}

		@Override
		public void run() {
			System.out.println("Connected, handling new client: " + clientSocket);
			try {
				PrintStream userOut = new PrintStream(clientSocket.getOutputStream());
				Scanner userIn = new Scanner(new InputStreamReader(clientSocket.getInputStream()));

				int state = LOGIN_STATE;
				Boolean authentication = false;

				do {
					switch (state) {
					case LOGIN_STATE:
						authentication = login(userIn, userOut);
						state = MENU_STATE;
						break;
					case MENU_STATE:
						state = menu(userIn, userOut);
						break;
					case GAME_STATE:
						state = game(userIn, userOut);
					default:
						state = 0;
						break;
					}
				} while (authentication && state != QUIT_STATE);

				userIn.close();
				userOut.close();

			} catch (SocketException e) {
				System.out.println("Error: " + e.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					clientSocket.close();
				} catch (IOException e) {

				}
				System.out.println("Closed: " + clientSocket);
			}
		}
	}
}
