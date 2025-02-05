/*
GAME SERVER
DESIGNATED PORT: 420
Communication with: INTERFACE SERVER and WORD SERVER

COMMANDS: NEWGAME num_words num_faults, CHECK string_word, CHECK char_letter
The game server operates based on commands recieved from the interface server to return a desired outcome


*/

import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collections; // For shuffling the list
import java.util.Arrays;

import java.io.*;


public class CrosswordGameServer {
	private static final String USAGE = "Usage: java CrosswordGameServer [port]";
	private static final int BUFFER_LIMIT = 1024;

	private static char[][] finishedGrid;
    private static char[][] userGrid;

	private static String[] game_words;
	private static String[] word_guessed;
	private static char[] letters_guessed;
	private static int numLetterGuesses;
	private static int numWordGuesses;
	private static int lives;
	private static String userName;

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println(USAGE);
			System.exit(1);
		}

		int port = 420;
		ServerSocket server = null;

		try {
			port = Integer.parseInt(args[0]);
			server = new ServerSocket(port);
			System.out.println("The Game Server is running...");
			

			ExecutorService fixedThreadPool = Executors.newFixedThreadPool(20);
			while (true) {
				fixedThreadPool.execute(new CrosswordInterfaceHandler(server.accept()));
			}
			
		} catch (IOException e) {
			System.out.println(
					"Exception caught when trying to listen on port " + port + " or listening for a connection");
			System.out.println(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Grid Dimensions
    public static class GridDimensions {
        int maxLength;
        int verticalX;

        public GridDimensions(int maxLength, int verticalX) {
            this.maxLength = maxLength;
            this.verticalX = verticalX;
        }
    }

	//NEWGAME START
	// UDP Communication with WordServer
	private static String sendToWordServerUDP(String query) {
	    String wordServerResponse = null;
		
	    try (DatagramSocket socket = new DatagramSocket()) {
	        // Prepare request buffer
	        byte[] requestBuf = query.getBytes();
    	    InetAddress address = InetAddress.getByName("localhost");
    	    DatagramPacket requestPacket = new DatagramPacket(requestBuf, requestBuf.length, address, 666);

    	    // Send the request
    	    socket.send(requestPacket);

    	    // Prepare response buffer and receive response
    	    byte[] responseBuf = new byte[BUFFER_LIMIT];
    	    DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length);
    	    socket.receive(responsePacket);

    	    // Extract the response
    	    wordServerResponse = new String(responsePacket.getData(), 0, responsePacket.getLength());

    	} catch (IOException e) {
    	    System.err.println("Error communicating with the WordServer: " + e.getMessage());
    	}

    	return wordServerResponse;
	}



	//Chooses random indices of given word
	public static int[] getRandomIndexes(String input, int n) {
        // Create a list to store all possible indexes
        ArrayList<Integer> indices = new ArrayList<>();
        
        // Add indexes from 0 to the length of the string - 1
        for (int i = 0; i < input.length(); i++) {
            indices.add(i);
        }
        
        // Shuffle the list to randomize the order
        Collections.shuffle(indices);
        
        int[] randomIndexes = new int[n];
        
        // Copy the first n random indexes into array
        for (int i = 0; i < n; i++) {
            randomIndexes[i] = indices.get(i);
        }
		
        // Sort the array in ascending order
        Arrays.sort(randomIndexes);
        
        return randomIndexes;
    }

	//Determines the length/width of grid
	public static GridDimensions getGridX(String[] horiz_words, int[] horiz_cross_index) {
        int max_length = 0;
		int max_left = 0;
		int max_right = 0;

        for (int i = 0; i < horiz_words.length; i++) {
            // Calculate the length of the left side of the vertical cross
            int left_length = horiz_cross_index[i];
			max_left = Math.max(max_left, left_length);

			// Calculate the length of the right side of the vertical cross
            int right_length = Math.abs(horiz_words[i].length() - (horiz_cross_index[i] + 1));
			max_right = Math.max(max_right, right_length);
        }

		max_length = max_left + max_right;

		int vertical_x = max_left;
		
        return new GridDimensions(max_length, vertical_x);
    }



	// Function to create the grid
    public static char[][] createGrid(String vert_word, GridDimensions grid_x, String[] horiz_words, int[] vert_cross_index, int[] horiz_cross_index) {
    	int grid_y = vert_word.length();
    	int grid_x_max = grid_x.maxLength;

    	// Initialize with '.'
    	char[][] grid = new char[grid_y][grid_x_max + 1];  // Extra column for '+'
    	for (int y = 0; y < grid_y; y++) {
    	    for (int x = 0; x <= grid_x_max; x++) {
    	        grid[y][x] = '.';
    	    }
    	}

    	// Fill the vertical word
    	for (int y = 0; y < vert_word.length(); y++) {
    	    grid[y][grid_x.verticalX] = Character.toUpperCase(vert_word.charAt(y));
    	}

    	// Fill the horizontal words
    	for (int i = 0; i < horiz_words.length; i++) {
    	    for (int x = 0; x < horiz_words[i].length(); x++) {
    	        grid[vert_cross_index[i]][horiz_cross_index[i] + x] = Character.toUpperCase(horiz_words[i].charAt(x));
    	    }
    	}

		return grid;  // Return the generated 2D grid
	}


	//Output grid row by row
	public static void printGrid(char[][] grid) {
    	for (char[] row : grid) {
    	    System.out.println(new String(row) + "+");  // Append '+' at the end of each row
    	}
	}

	//Output to stream row by row
	public static void sendGrid(PrintStream out, char[][] grid) {
    	for (char[] row : grid) {
    	    out.println(new String(row) + "+");  // Append '+' at the end of each row
    	}
	}

	// Method to convert grid to a single string representation
	public static String gridToString(char[][] grid) {
	    StringBuilder result = new StringBuilder();
    
	    for (char[] row : grid) {
	        result.append(new String(row)).append("+"); // Append '+' at the end of each row
	    }
	
	    return result.toString();
	}


	//Prepares new grid for user
	public static char[][] maskGrid(char[][] grid) {
    	int rows = grid.length;
    	int cols = grid[0].length;
    	char[][] maskedGrid = new char[rows][cols];

    	for (int y = 0; y < rows; y++) {
    	    for (int x = 0; x < cols; x++) {
    	        char cell = grid[y][x];
				// Replace all characters except '.'
    	        if (cell != '.') {
    	            maskedGrid[y][x] = '-';
    	        } else {
    	            maskedGrid[y][x] = cell;
    	        }
    	    }
    	}
    	return maskedGrid;
	}


	//Determines fault counter
	public static int countLetters(char[][] grid) {
        int count = 0;

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                if (grid[i][j] == '-') {
                    count++;
                }
            }
        }

        return count;
    }

	// Function to validate the number of words
    private static boolean validateWordCount(int words) {
        if(words <= 10 && (words != 0 && words != 1)) {
			return true;
		}
		return false;
    }

    // Function to fetch the vertical word from the word server
    private static String fetchVerticalWord() {
        String word_query = "FETCH l " + (CrosswordGameServer.game_words.length - 1);
        String return_query = sendToWordServerUDP(word_query);
        String[] parts_vert = return_query.split(" ");

        if (parts_vert.length != 3 || !parts_vert[2].equals("1")) {
            throw new RuntimeException("Error: Invalid response from word server for query: " + word_query);
        }

        String vert_word = parts_vert[1];
        System.out.println("\nVertical Word: " + vert_word + "\n");
        return vert_word;
    }


    // Function to fetch the horizontal words from word server
    private static String[] fetchHorizontalWords(String vert_word, int[] vert_cross_index, int horiz_count) {
        String[] horiz_words = new String[horiz_count];
        for (int i = 0; i < horiz_count; i++) {
            char contains = vert_word.charAt(vert_cross_index[i]);
            String word_query = "FETCH m " + contains;
            String return_query = sendToWordServerUDP(word_query);
            String[] parts = return_query.split(" ");

            if (parts.length != 3 || !parts[2].equals("1")) {
                throw new RuntimeException("Error: Invalid response from word server for query: " + word_query);
            }

            horiz_words[i] = parts[1];
            System.out.println("\n" + horiz_words[i] + " ");
        }
        return horiz_words;
    }

    // Function to determine horizontal cross indices
    private static int[] determineCrossovers(String vert_word, int[] vert_cross_index, String[] horiz_words) {
        int[] horiz_cross_index = new int[vert_cross_index.length];
        for (int i = 0; i < vert_cross_index.length; i++) {
            char targetChar = Character.toUpperCase(vert_word.charAt(vert_cross_index[i]));
            String horizWordUpper = horiz_words[i].toUpperCase();
            horiz_cross_index[i] = horizWordUpper.indexOf(targetChar);

            if (horiz_cross_index[i] < 0) {
                throw new IllegalArgumentException("Character '" + vert_word.charAt(vert_cross_index[i]) + "' is not found in selected word");
            }
        }
        return horiz_cross_index;
    }

    // Function to get grid dimensions
    private static GridDimensions getGridDimensions(String[] horiz_words, int[] horiz_cross_index) {
        return getGridX(horiz_words, horiz_cross_index);
    }

    // Function to adjust horizontal cross indices
    private static void adjustCrossovers(int[] horiz_cross_index, int verticalX) {
        for (int i = 0; i < horiz_cross_index.length; i++) {
            horiz_cross_index[i] = verticalX - horiz_cross_index[i];
        }
    }


	//CREATE FUNCTIONS
	// Prepares grid for user by masking all characters except those in letters_guessed or word_guessed
    public static char[][] updateUserGrid(char[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;
        char[][] maskedGrid = new char[rows][cols];

        // Initialize masked grid with '-'
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                char cell = grid[y][x];

                // Check if the cell is in letters_guessed or if it's '.'
                if (cell == '.' || isGuessedLetter(cell)) {
                    maskedGrid[y][x] = cell; // Keep revealed
                } else {
                    maskedGrid[y][x] = '-'; // Mask initially
                }
            }
        }

        return maskedGrid;
    }

    // Helper function to check if a letter is in letters_guessed
    private static boolean isGuessedLetter(char letter) {
        for (char guessed : CrosswordGameServer.letters_guessed) {
            if (guessed == letter) {
                return true; // Letter has been guessed
            }
        }
        return false; // Letter not guessed yet
    }

    // Helper function to reveal words that exist in word_guessed
    private static void revealWords(char[][] grid, char[][] maskedGrid) {
        int rows = grid.length;
        int cols = grid[0].length;

        for (String word : CrosswordGameServer.word_guessed) {
            // Check horizontally (row-wise)
            for (int y = 0; y < rows; y++) {
                String rowString = new String(grid[y]);
                if (rowString.contains(word)) {
                    revealWordInRow(maskedGrid, grid, y, word);
                }
            }

            // Check vertically (column-wise)
            for (int x = 0; x < cols; x++) {
                StringBuilder colString = new StringBuilder();
                for (int y = 0; y < rows; y++) {
                    colString.append(grid[y][x]);
                }
                if (colString.toString().contains(word)) {
                    revealWordInColumn(maskedGrid, grid, x, word);
                }
            }
        }
    }

    // Reveals a word in a specific row
    private static void revealWordInRow(char[][] maskedGrid, char[][] grid, int row, String word) {
        int start = new String(grid[row]).indexOf(word); // Find word start index in row
        if (start != -1) {
            for (int i = 0; i < word.length(); i++) {
                maskedGrid[row][start + i] = grid[row][start + i]; // Reveal each character
            }
        }
    }

    // Reveals a word in a specific column
    private static void revealWordInColumn(char[][] maskedGrid, char[][] grid, int col, String word) {
        int rows = grid.length;
        StringBuilder colString = new StringBuilder();

        // Build the column string
        for (int y = 0; y < rows; y++) {
            colString.append(grid[y][col]);
        }

        int start = colString.toString().indexOf(word); // Find word start index in column
        if (start != -1) {
            for (int i = 0; i < word.length(); i++) {
                maskedGrid[start + i][col] = grid[start + i][col]; // Reveal each character
            }
        }
    }

	// Function to check if the grid is fully revealed (i.e., contains no '-' characters)
    public static boolean isComplete(char[][] grid) {
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[0].length; x++) {
                if (grid[y][x] == '-') {
                    return false; // Grid still has masked characters
                }
            }
        }
        return true; // No '-' found, grid is fully revealed
    }
	
	/*
		Main function to create a new game
		Connects to word server and generates user game grid
		Creates randomly selected crossword layout using game input from user

		Returns a SUCCESS, life counter, and user game layout after completion

	*/
    public static void newGame(PrintStream out, int words, int faults) {

        // Validate the number of words
        if (!validateWordCount(words)) {
            System.out.println("Words chosen is not within 2 - 10, invalid prompt");
			out.println("FAIL");
            return;
        }

        // Initialize global arrays
        CrosswordGameServer.game_words = new String[words];
        CrosswordGameServer.word_guessed = new String[words];
        CrosswordGameServer.letters_guessed = new char[words];

        // Fetch the vertical word from the word server
        String vert_word = fetchVerticalWord();

        // Select random indexes for vertical-horizontal crossing
        int[] vert_cross_index = getRandomIndexes(vert_word, words - 1);

        // Fetch the horizontal words
        String[] horiz_words = fetchHorizontalWords(vert_word, vert_cross_index, words - 1);

        // Determine the horizontal cross indices
        int[] horiz_cross_index = determineCrossovers(vert_word, vert_cross_index, horiz_words);

        // Determine the grid dimensions
        GridDimensions grid_x = getGridDimensions(horiz_words, horiz_cross_index);
        int grid_y = vert_word.length();

        // Adjust horizontal cross indices
        adjustCrossovers(horiz_cross_index, grid_x.verticalX);

        // Create and mask the grid
		CrosswordGameServer.finishedGrid = createGrid(vert_word, grid_x, horiz_words, vert_cross_index, horiz_cross_index);
        CrosswordGameServer.userGrid = maskGrid(CrosswordGameServer.finishedGrid);

        // Determine Faults count
        int num_letters = countLetters(CrosswordGameServer.userGrid);
        CrosswordGameServer.lives = faults * num_letters;
        System.out.println("Fault counter: " + CrosswordGameServer.lives + "\n");

        printGrid(CrosswordGameServer.finishedGrid);
        System.out.print("\n");
        printGrid(CrosswordGameServer.userGrid);

        // Send the grid to the client
		StringBuilder returnQuery = new StringBuilder("SUCCESS " + CrosswordGameServer.lives + " " );
		returnQuery.append(gridToString(CrosswordGameServer.userGrid));

		out.println(returnQuery.toString());
    }


	/*
		Main function to check and update grid based on users guess
		Updates global variables userGrid/finishedGrid and lettersGuessed/wordsGuessed to return a new game grid
		Called in Main

		Returns a WIN if grid is completed with new guess
		Returns existing grid layout and life counter if not
	*/
    public static void checkGuess(PrintStream out, String guess) {
		if (guess.length() == 1) {
        	char guessedChar = guess.charAt(0); // Convert string to char
			guessedChar = Character.toUpperCase(guessedChar); 

			CrosswordGameServer.letters_guessed[CrosswordGameServer.numLetterGuesses] = guessedChar;
			CrosswordGameServer.numLetterGuesses++;
		}
		else {
			String guessedWord = guess.toUpperCase();
			CrosswordGameServer.word_guessed[CrosswordGameServer.numWordGuesses] = guessedWord;
			CrosswordGameServer.numWordGuesses++;
		}

		CrosswordGameServer.userGrid = updateUserGrid(CrosswordGameServer.finishedGrid);

		if(isComplete(CrosswordGameServer.userGrid)) {
			out.println("WIN");
		}

		else {
			// Send the grid to the client
			StringBuilder returnQuery = new StringBuilder("SUCCESS " + CrosswordGameServer.lives + " " );
			returnQuery.append(gridToString(CrosswordGameServer.userGrid));

			out.println(returnQuery.toString());
		}
	}

	private static class CrosswordInterfaceHandler implements Runnable {
		private Socket interfaceSocket;

		CrosswordInterfaceHandler(Socket socket) {
			this.interfaceSocket = socket;
		}

		@Override
		public void run() {
			CrosswordGameServer.numLetterGuesses = 0;
			CrosswordGameServer.numWordGuesses = 0;

	    	System.out.println("Connected, in communication with interface");
		    try {
    		    PrintStream out = new PrintStream(interfaceSocket.getOutputStream());
    	    	Scanner in = new Scanner(new InputStreamReader(interfaceSocket.getInputStream()));

   		     String query = "";
			 //String commands[] = ["NEWGAME", "CHECK"];

    		    while (!query.equals("Quit")) {
        		    // Check if the interface has sent a message
            		if (in.hasNextLine()) { // Wait until input is available
                		query = in.nextLine().trim();

                		System.out.println("\nReceived the following message from Interface:" + query + "\n");

						String[] parts = query.split(" ");

						parts[0] = parts[0].toUpperCase();
						CrosswordGameServer.userName = parts[1];

						switch (parts[0]) {
    						case "START":
								int words = 3;
								int faults = 3;
									
								if (parts.length == 3 || parts.length == 1) {
									if(parts.length == 3) {
										words = Integer.parseInt(parts[1]);
										faults = Integer.parseInt(parts[2]);
									}

									if(parts[1].matches("\\d+") && parts[2].matches("\\d+")) {
										System.out.print("Starting New Game\n\n");
										newGame(out, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
									}
									else {
										out.println("FAIL");
									}
								}
								else {
									out.println("FAIL");
								}
    						    break;

    						case "CHECK":
    						    System.out.print("Checking User Guess\n");

								if(CrosswordGameServer.userGrid != null) {
									if(parts.length != 2) {
										out.println("FAIL");
									}
									else{
										checkGuess(out, parts[1]);
									}

								} else {
									out.println("FAIL");
								}
    						    break;

    						default:
    						    out.print("Error, invalid request");
    						    break;
							}

		            } else {
    		            // Do nothing and keep waiting
        		        Thread.sleep(100); // Optional: add a small delay to avoid excessive CPU usage
            		}
	        	}

    	    	in.close();

    		} catch (SocketException e) {
        		System.out.println("Error: " + e.getMessage());
    		} catch (Exception e) {
        		e.printStackTrace();
		    } finally {
    		    try {
        		    interfaceSocket.close();
        		} catch (IOException e) {
            		System.err.println("Error closing socket: " + e.getMessage());
		        }
    		    System.out.println("Closed: " + interfaceSocket);
	    	}
		}
	}
}


