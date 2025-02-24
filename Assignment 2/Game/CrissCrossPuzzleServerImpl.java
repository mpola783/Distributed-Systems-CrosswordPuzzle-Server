import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collections; // For shuffling the list
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.io.*;


public class CrissCrossPuzzleServerImpl extends UnicastRemoteObject implements CrissCrossPuzzleServer {
    private static final int BUFFER_LIMIT = 1024;
    private char[][] finishedGrid;
    private char[][] userGrid;

    private String[] game_words = new String[20];
    private String[] word_guessed = new String[20];
    private char[] letters_guessed = new char[40];
    private int numLetterGuesses = 0;
    private int numWordGuesses = 0;
    private int lives;
    private String userName;
    private int num_words = 0;
    private int faults = 0;
    
    private static final long serialVersionUID = 1L;
    
    // Map to store game state using gameID as key, and each game holds a map of players
    private Map<String, CrosswordGameState> gameStates;


    // Constructor
    public CrissCrossPuzzleServerImpl() throws RemoteException {
        super();
        this.gameStates = new HashMap<>();
    }

    // Grid Dimensions
    public class GridDimensions {
        int maxLength;
        int verticalX;

        public GridDimensions(int maxLength, int verticalX) {
            this.maxLength = maxLength;
            this.verticalX = verticalX;
        }
    }


    // NEWGAME START
    /*
        Main function to create a new game
        Connects to word server and generates user game grid
        Creates randomly selected crossword layout using game input from user

        Returns a SUCCESS, life counter, and user game layout after completion
    */

    @Override
    public String startGame(String[] players, int numberOfWords, int failedAttemptFactor, String gameID) throws RemoteException {
        // Validate number of words
        if (!validateWordCount(numberOfWords)) {
            System.out.println("Words chosen are not within 2 - 10, invalid prompt");
            return "FAIL";
        }

        CrosswordGameState gameState;

        //If game exists and need to restart
        if (gameID == null) {
            // Generate a unique game ID
            gameID = UUID.randomUUID().toString();

            //create a new game state
            gameState = new CrosswordGameState(gameID, numberOfWords, failedAttemptFactor);

            // Add players to the new game state
            for (String player : players) {
                gameState.addPlayer(player);
            }

            // Store game state
            gameStates.put(gameID, gameState);
        } else {
            // Retrieve existing game state
            gameState = gameStates.get(gameID);
        }

        // Fetch the vertical word from the word server
        String vertWord = fetchVerticalWord(numberOfWords);
        gameState.setGameWords(0, vertWord);

        // Select random indexes for vertical-horizontal crossing
        int[] vertCrossIndex = getRandomIndexes(vertWord, numberOfWords - 1);

        // Fetch the horizontal words
        String[] horizWords = fetchHorizontalWords(vertWord, vertCrossIndex, numberOfWords - 1);
        for (int i = 0; i < horizWords.length; i++) {
            gameState.setGameWords(i + 1, horizWords[i]);
        }

        // Determine the horizontal cross indices
        int[] horizCrossIndex = determineCrossovers(vertWord, vertCrossIndex, horizWords);

        // Determine the grid dimensions
        GridDimensions gridX = getGridDimensions(horizWords, horizCrossIndex);
        int gridY = vertWord.length();

        // Adjust horizontal cross indices
        adjustCrossovers(horizCrossIndex, gridX.verticalX);

        // Create and mask the grid
        gameState.setFinishedGrid(createGrid(vertWord, gridX, horizWords, vertCrossIndex, horizCrossIndex));
        gameState.setUserGrid(maskGrid(gameState.getFinishedGrid()));

        // Determine Faults count
        int numLetters = countLetters(gameState.getUserGrid());
        gameState.setLives(failedAttemptFactor * numLetters);
        System.out.println("Fault counter: " + gameState.getLives() + "\n");

        // Print grids for debugging
        printGrid(gameState.getFinishedGrid());
        System.out.println();
        printGrid(gameState.getUserGrid());

        // Prepare the response
        StringBuilder returnQuery = new StringBuilder("SUCCESS " + gameState.getLives() + " ");
        returnQuery.append(gridToString(gameState.getUserGrid()));

        return returnQuery.toString();
    }

    @Override
    public String restartGame(String gameID) throws RemoteException {
        // Retrieve the game state using gameID
        CrosswordGameState gameState = gameStates.get(gameID);

        // Check if the game exists
        if (gameState != null) {
            // Retrieve existing game details
            String[] players = gameState.getPlayers();
            int numWords = gameState.getNumWords();
            int totalLives = gameState.getTotalLives(); // Total lives from the previous game

            for (int i = 0; i < numWords; i++) {
                gameState.setGameWords(i, null);  // Reset horizontal words
            }

            // Reset the game state (e.g., keeping players and words)
            // Clear the grid and user grid
            gameState.setFinishedGrid(null);
            gameState.setUserGrid(null);

            // Convert List<String> to String[]
            String[] playersArray = player.toArray(new String[0]);

            // Call startGame to reset the game with the same configuration
            startGame(playersArray, numWords, totalLives, gameID); // Reuse startGame logic

            // Notify the players that the game has been reset
            System.out.println("Game has been reset successfully for game ID: " + gameID);

            // Return a success message
            return "Game with ID " + gameID + " has been reset successfully!";
        } else {
            // If the game doesn't exist
            return "Game with ID " + gameID + " not found.";
        }
    }

    private String sendToWordServerRMI(String queryType) {
    /*    String wordServerResponse = null;

        try {
            // Look up the WordServer object from the RMI registry
            WordServer wordServer = (WordServer) Naming.lookup("rmi://localhost/WordServer");

            // Call the remote fetchWord method
            wordServerResponse = wordServer.fetchWord(queryType);
        
        } catch (Exception e) {
            System.err.println("Error communicating with the WordServer via RMI: " + e.getMessage());
        }

        return wordServerResponse;
    */
        return "Success word 0";
    }

    // Chooses random indices of given word
    public int[] getRandomIndexes(String input, int n) {
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

    // Determines the length/width of grid
    public GridDimensions getGridX(String[] horiz_words, int[] horiz_cross_index) {
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
    public char[][] createGrid(String vert_word, GridDimensions grid_x, String[] horiz_words, int[] vert_cross_index, int[] horiz_cross_index) {
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
	public void printGrid(char[][] grid) {
    	for (char[] row : grid) {
    	    System.out.println(new String(row) + "+");  // Append '+' at the end of each row
    	}
	}

	//Output to stream row by row
	public void sendGrid(PrintStream out, char[][] grid) {
    	for (char[] row : grid) {
    	    out.println(new String(row) + "+");  // Append '+' at the end of each row
    	}
	}

	// Method to convert grid to a single string representation
	public String gridToString(char[][] grid) {
	    StringBuilder result = new StringBuilder();
    
	    for (char[] row : grid) {
	        result.append(new String(row)).append("+"); // Append '+' at the end of each row
	    }
	
	    return result.toString();
	}


	//Prepares new grid for user
	public char[][] maskGrid(char[][] grid) {
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
	public int countLetters(char[][] grid) {
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
    private boolean validateWordCount(int words) {
        if(words <= 10 && (words != 0 && words != 1)) {
			return true;
		}
		return false;
    }

    // Function to fetch the vertical word from the word server
    private String fetchVerticalWord(int words) {
        String word_query = "FETCH l " + words;
        String return_query = sendToWordServerRMI(word_query);
        String[] parts_vert = return_query.split(" ");

        if (parts_vert.length != 3 || !parts_vert[2].equals("1")) {
            throw new RuntimeException("Error: Invalid response from word server for query: " + word_query);
        }

        String vert_word = parts_vert[1];
        System.out.println("\nVertical Word: " + vert_word + "\n");
        return vert_word;
    }


    // Function to fetch the horizontal words from word server
    private String[] fetchHorizontalWords(String vert_word, int[] vert_cross_index, int horiz_count) {
        String[] horiz_words = new String[horiz_count];
        for (int i = 0; i < horiz_count; i++) {
            char contains = vert_word.charAt(vert_cross_index[i]);
            String word_query = "FETCH m " + contains;
            String return_query = sendToWordServerRMI(word_query);
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
    private int[] determineCrossovers(String vert_word, int[] vert_cross_index, String[] horiz_words) {
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
    private GridDimensions getGridDimensions(String[] horiz_words, int[] horiz_cross_index) {
        return getGridX(horiz_words, horiz_cross_index);
    }

    // Function to adjust horizontal cross indices
    private void adjustCrossovers(int[] horiz_cross_index, int verticalX) {
        for (int i = 0; i < horiz_cross_index.length; i++) {
            horiz_cross_index[i] = verticalX - horiz_cross_index[i];
        }
    }

    // CREATE FUNCTIONS
    // Prepares grid for user by masking all characters except those in letters_guessed or word_guessed
    public char[][] updateUserGrid(char[][] grid) {
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

        revealWords(grid, maskedGrid);
        return maskedGrid;
    }

    public String[] getMatching(String[] array1, String[] array2) {
        HashSet<String> set = new HashSet<>();
        ArrayList<String> matchingWords = new ArrayList<>();

        // Add all elements from the second array to a HashSet for quick lookup
        for (String word : array2) {
            if (word != null) { // Avoid null values
                set.add(word);
            }
        }

        // Check if words in the first array exist in the set
        for (String word : array1) {
            if (word != null && set.contains(word)) {
                matchingWords.add(word);
            }
        }

        // Convert ArrayList to String[]
        return matchingWords.toArray(new String[0]);
    }

    // Helper function to check if a letter is in letters_guessed
    private boolean isGuessedLetter(char letter) {
        for (char guessed : this.letters_guessed) {
            if (guessed == letter) {
                return true; // Letter has been guessed
            }
        }
        return false; // Letter not guessed yet
    }

	// Helper function to reveal words that exist in word_guessed
    private void revealWords(char[][] grid, char[][] maskedGrid) {
        int rows = grid.length;
        int cols = grid[0].length;

        for (String word : this.word_guessed) {
            if (word == null || word.isEmpty()) continue; // Skip empty/null words

            // Check horizontally (row)
            // Converts each row into a string and checks if the word exists
            for (int y = 0; y < rows; y++) {
                String rowString = new String(grid[y]);
                int index = rowString.indexOf(word);
                while (index != -1) {
                    revealWordInRow(maskedGrid, grid, y, index, word.length());
                    index = rowString.indexOf(word, index + 1); // Check for multiple occurrences
                }
            }

            // Check vertically (column)
            // Converts each column into a string and checks if the word exists
            for (int x = 0; x < cols; x++) {
                StringBuilder colString = new StringBuilder();
                for (int y = 0; y < rows; y++) {
                    colString.append(grid[y][x]);
                }
                String column = colString.toString();
                int index = column.indexOf(word);
                while (index != -1) {
                    revealWordInColumn(maskedGrid, grid, x, index, word.length());
                    index = column.indexOf(word, index + 1); // Check for multiple occurrences
                }
            }
        }
    }

    // Reveals a word in a specific row
    private void revealWordInRow(char[][] maskedGrid, char[][] grid, int row, int startIndex, int length) {
        for (int i = 0; i < length; i++) {
            maskedGrid[row][startIndex + i] = grid[row][startIndex + i];
        }
    }

    // Reveals a word in a specific column
    private void revealWordInColumn(char[][] maskedGrid, char[][] grid, int col, int startIndex, int length) {
        for (int i = 0; i < length; i++) {
            maskedGrid[startIndex + i][col] = grid[startIndex + i][col];
        }
    }

    // Function to check if the grid is fully revealed (i.e., contains no '-' characters)
    public boolean isComplete(char[][] grid) {
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
        Main function to check and update grid based on users guess
        Updates global variables userGrid/finishedGrid and lettersGuessed/wordsGuessed to return a new game grid
        Called in Main

        Returns a WIN if grid is completed with new guess
        Returns existing grid layout and life counter if not
    */
    public void checkGuess(PrintStream out, String guess) {
    
        this.lives = this.lives - 1;

        if (guess.length() == 1) {
            char guessedChar = guess.charAt(0); // Convert string to char
            guessedChar = Character.toUpperCase(guessedChar); 

            this.letters_guessed[this.numLetterGuesses] = guessedChar;
            this.numLetterGuesses++;
        }
        else {
            String guessedWord = guess.toUpperCase();
            this.word_guessed[this.numWordGuesses] = guessedWord;
            this.numWordGuesses++;
        }

        this.userGrid = updateUserGrid(this.finishedGrid);

        if(isComplete(this.userGrid)) {
            out.println("WIN");
            //reset();
        } else if (this.lives <= 0){
            out.println("LOSE");
            //reset();
        }

        else {
            // Send the grid to the client
            StringBuilder returnQuery = new StringBuilder("SUCCESS " + this.lives + " " );
            returnQuery.append(gridToString(this.userGrid));

            out.println(returnQuery.toString());
        }
    }

    /*
    @Override
    public void run() {
        System.out.println("Connected, in communication with interface");
        try {
            PrintStream out = new PrintStream(interfaceSocket.getOutputStream());
            Scanner in = new Scanner(new InputStreamReader(interfaceSocket.getInputStream()));

            String query = "";

            while (!query.equals("QUIT")) {
                // Check if the interface has sent a message
                if (in.hasNextLine()) { // Wait until input is available
                    query = in.nextLine().trim();
                    System.out.println("\nReceived the following message from Interface:" + query + "\n");

                    String[] parts;
                    try {
                        parts = query.split(" ");
                    } catch (Exception e) {
                        parts = new String[0]; // Default to an empty array in case of an error
                        System.err.println("Error splitting query: " + e.getMessage());
                    }

                    parts[0] = parts[0].toUpperCase();
                    this.userName = parts[1];

                    switch (parts[0]) {
                        case "START":
                            int words = 3;
                            int faults = 3;

                            if (parts.length == 3 || parts.length == 1) {
                                if (parts.length == 3) {
                                    words = Integer.parseInt(parts[1]);
                                    faults = Integer.parseInt(parts[2]);
                                }

                                if (parts[1].matches("\\d+") && parts[2].matches("\\d+")) {
                                    System.out.print("Starting New Game\n\n");
                                    newGame(out, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                                } else {
                                    out.println("FAIL");
                                }
                            } else {
                                out.println("FAIL");
                            }
                            reset();
                            break;

                        case "CHECK":
                            System.out.print("Checking User Guess " + parts[1] + "\n");

                            if (this.userGrid != null) {
                                if (parts.length != 2) {
                                    out.println("FAIL");
                                } else {
                                    checkGuess(out, parts[1]);
                                }
                            } else {
                                out.println("FAIL");
                            }
                            break;

                        case "RESET":
                            reset();
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

            reset();
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
    } */
}