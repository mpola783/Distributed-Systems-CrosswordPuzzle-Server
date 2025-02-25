import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CrosswordGameState implements Serializable {

    private String gameID;
    private List<PlayerScore> players; // List of players (each with a name and score)
    private String activePlayer;       // Currently active player's name
    private int numWords;
    private int lives;
    private int totalLives;
    private String[] gameWords;
    private char[][] finishedGrid;
    private char[][] playerGrid;
    
    public CrosswordGameState(String gameID, int numWords, int lives) {
        this.gameID = gameID;
        this.players = new ArrayList<>();
        this.numWords = numWords;
        this.lives = lives;
        this.totalLives = lives;
        this.gameWords = new String[numWords];
    }
    
    // Nested class to represent a tuple of player name and score.
    public static class PlayerScore implements Serializable {
        private String playerName;
        private int score;
        
        public PlayerScore(String playerName, int score) {
            this.playerName = playerName;
            this.score = score;
        }
        
        public String getPlayerName() {
            return playerName;
        }
        
        public int getScore() {
            return score;
        }
        
        public void setScore(int score) {
            this.score = score;
        }
        
        @Override
        public String toString() {
            return playerName + " (Score: " + score + ")";
        }
    }
    
    // Add a player with an initial score of 0.
    public void addPlayer(String playerName) {
        players.add(new PlayerScore(playerName, 0));
        // If no active player is set, use the first added player.
        if (activePlayer == null) {
            activePlayer = playerName;
        }
    }
    
    // Remove a player from the game.
    public void removePlayer(String playerName) {
        players.removeIf(p -> p.getPlayerName().equals(playerName));
        // If the removed player was active, move to the next available player.
        if (playerName.equals(activePlayer)) {
            nextActivePlayer();
        }
    }
    
    // Returns the number of players (lobby size).
    public int getLobbySize() {
        return players.size();
    }
    
    // Cycle to the next active player; wraps around at the end.
    public void nextActivePlayer() {
        if (players.isEmpty()) {
            activePlayer = null;
            return;
        }
        if (activePlayer == null) {
            activePlayer = players.get(0).getPlayerName();
            return;
        }
        int index = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getPlayerName().equals(activePlayer)) {
                index = i;
                break;
            }
        }
        if (index == -1) { // If not found, default to first player.
            activePlayer = players.get(0).getPlayerName();
        } else {
            activePlayer = players.get((index + 1) % players.size()).getPlayerName();
        }
    }
    
    // Returns the current active player's name.
    public String getActivePlayer() {
        return activePlayer;
    }
    
    // Getters and setters for game state variables.
    
    public String getGameID() {
        return gameID;
    }
    
    public void setGameID(String gameID) {
        this.gameID = gameID;
    }
    
    public int getLives() {
        return lives;
    }
    
    public void setLives(int lives) {
        this.lives = lives;
    }
    
    public int getTotalLives() {
        return totalLives;
    }
    
    public void setTotalLives(int totalLives) {
        this.totalLives = totalLives;
    }
    
    public void setNumWords(int numWords) {
        this.numWords = numWords;
    }
    
    public int getNumWords() {
        return numWords;
    }
    
    // Returns the list of players with their scores.
    public List<PlayerScore> getPlayers() {
        return players;
    }
    
    public void setPlayers(List<PlayerScore> players) {
        this.players = players;
    }
    
    // Getters and setters for game words.
    public String[] getGameWords() {
        return gameWords;
    }
    
    public void setGameWord(int position, String word) {
        this.gameWords[position] = word;
    }
    
    public char[][] getFinishedGrid() {
        return finishedGrid;
    }
    
    public void setFinishedGrid(char[][] finishedGrid) {
        this.finishedGrid = finishedGrid;
    }
    
    public char[][] getPlayerGrid() {
        return playerGrid;
    }
    
    public void setPlayerGrid(char[][] playerGrid) {
        this.playerGrid = playerGrid;
    }
    
    /**
     * Prints out a grid along with game state details.
     * @param gridType Specify "finished" to print the finishedGrid or "player" to print the playerGrid.
     */
    public void displayGrid(String gridType) {
        char[][] gridToPrint;
        if ("finished".equalsIgnoreCase(gridType)) {
            gridToPrint = finishedGrid;
        } else if ("player".equalsIgnoreCase(gridType)) {
            gridToPrint = playerGrid;
        } else {
            System.out.println("Invalid grid type specified. Please use 'finished' or 'player'.");
            return;
        }
        
        if (gridToPrint == null) {
            System.out.println("Grid not initialized.");
            return;
        }
        
        // Print the grid
        System.out.println("----- " + gridType.toUpperCase() + " GRID -----");
        for (int i = 0; i < gridToPrint.length; i++) {
            for (int j = 0; j < gridToPrint[i].length; j++) {
                System.out.print(gridToPrint[i][j] + " ");
            }
            System.out.println();
        }
        
        // Print additional game state information
        System.out.println("\n--- Game State Info ---");
        System.out.println("Active Player: " + (activePlayer != null ? activePlayer : "None"));
        System.out.println("Players and Scores:");
        for (PlayerScore ps : players) {
            System.out.println(" - " + ps.getPlayerName() + ": " + ps.getScore());
        }
        System.out.println("Total Lives Remaining: " + lives);
    }

	/**
	 * Updates the active player's score by adding the specified amount.
	 * @param scoreDelta The amount to add to the current score (can be negative to subtract).
	 */
	public void updateActivePlayerScore(int scoreDelta) {
		if (activePlayer == null) {
			System.out.println("No active player is set.");
			return;
		}
		boolean updated = false;
		for (PlayerScore ps : players) {
			if (ps.getPlayerName().equals(activePlayer)) {
				ps.setScore(ps.getScore() + scoreDelta);
				updated = true;
				break;
			}
		}
		if (!updated) {
			System.out.println("Active player not found in the players list.");
		}
	}	
}

/*

    private void reset() {
        this.userName = null;
        this.gameID = null;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println(USAGE);
            System.exit(1);
        }

        int port = 420;
        try (ServerSocket server = new ServerSocket(Integer.parseInt(args[0]))) {
            System.out.println("The Game Server is running on port " + port + "...");

            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(20);
            while (true) {
                fixedThreadPool.execute(new CrosswordGameState(server.accept()));
            }
        } catch (IOException e) {
            System.err.println("Error: Unable to start server on port " + port);
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("Connected, in communication with interface");

        try (
            PrintStream out = new PrintStream(interfaceSocket.getOutputStream());
            Scanner in = new Scanner(new InputStreamReader(interfaceSocket.getInputStream()))
        ) {
            String query = "";

            while (!query.equals("QUIT")) {
                if (in.hasNextLine()) {
                    query = in.nextLine().trim();
                    System.out.println("\nReceived message from Interface: " + query + "\n");

                    String[] parts = query.split(" ");
                    if (parts.length == 0) {
                        System.err.println("Error: Received an empty query.");
                        continue;
                    }

                    parts[0] = parts[0].toUpperCase();
                    this.userName = parts.length > 1 ? parts[1] : null;

                    switch (parts[0]) {
                        case "START":
                            handleStartCommand(parts, out);
                            reset();
                            break;

                        case "CHECK":
                            handleCheckCommand(parts, out);
                            break;

                        case "RESET":
                            reset();
                            break;

                        default:
                            out.println("Error: Invalid request");
                            break;
                    }
                } else {
                    Thread.sleep(100); // Reduce CPU usage while waiting
                }
            }
        } catch (SocketException e) {
            System.out.println("Connection error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeSocket();
        }
    }

    private void handleStartCommand(String[] parts, PrintStream out) {
        int words = 3;
        int faults = 3;

        if (parts.length == 3 && parts[1].matches("\\d+") && parts[2].matches("\\d+")) {
            words = Integer.parseInt(parts[1]);
            faults = Integer.parseInt(parts[2]);

            System.out.println("Starting New Game...");
            puzzleServer.newGame(out, words, faults);
        } else {
            out.println("FAIL");
        }
    }

    private void handleCheckCommand(String[] parts, PrintStream out) {
        if (this.gameID == null) {
            out.println("FAIL");
            return;
        }

        if (parts.length != 2) {
            out.println("FAIL");
        } else {
            System.out.println("Checking User Guess: " + parts[1]);
            puzzleServer.checkGuess(out, parts[1]);
        }
    }

    private void closeSocket() {
        try {
            if (interfaceSocket != null && !interfaceSocket.isClosed()) {
                interfaceSocket.close();
                System.out.println("Closed connection: " + interfaceSocket);
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
}



/*

	private static class CrosswordInterfaceHandler implements Runnable {
		
		private Socket interfaceSocket;
		private char[][] finishedGrid;
    	private char[][] userGrid;

		private String[] game_words = new String[20];
		private String[] word_guessed = new String[20];
		private char[] letters_guessed = new char[40];
		private int numLetterGuesses = 0;
		private int numWordGuesses = 0;
		private int lives;
		private String userName;

		private void reset() {
        	this.game_words = new String[20];
        	this.word_guessed = new String[20];
        	this.letters_guessed = new char[40];
        	this.numLetterGuesses = 0;
        	this.numWordGuesses = 0;
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

		//NEWGAME START
		// UDP Communication with WordServer
		private String sendToWordServerUDP(String query) {
		    String wordServerResponse = null;
		
			System.out.println(query + "\n");

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

		//Determines the length/width of grid
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
    	private String[] fetchHorizontalWords(String vert_word, int[] vert_cross_index, int horiz_count) {
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


		//CREATE FUNCTIONS
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
				//converts each row into a string and checks if the word exists
    		    for (int y = 0; y < rows; y++) {
    		        String rowString = new String(grid[y]);
    		        int index = rowString.indexOf(word);
    		        while (index != -1) {
    		            revealWordInRow(maskedGrid, grid, y, index, word.length());
    		            index = rowString.indexOf(word, index + 1); // Check for multiple occurrences
    		        }
    		    }

    		    // Check vertically (column)
				// converts each column into a string and checks if the word exists
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
			Main function to create a new game
			Connects to word server and generates user game grid
			Creates randomly selected crossword layout using game input from user

			Returns a SUCCESS, life counter, and user game layout after completion

		*/ /*
    	public void newGame(PrintStream out, int words, int faults) {

    	    // Validate the number of words
    	    if (!validateWordCount(words)) {
    	        System.out.println("Words chosen is not within 2 - 10, invalid prompt");
				out.println("FAIL");
    	        return;
    	    }

    	    // Fetch the vertical word from the word server
    	    String vert_word = fetchVerticalWord(words);
			this.game_words[0] = vert_word;

    	    // Select random indexes for vertical-horizontal crossing
    	    int[] vert_cross_index = getRandomIndexes(vert_word, words - 1);

    	    // Fetch the horizontal words
    	    String[] horiz_words = fetchHorizontalWords(vert_word, vert_cross_index, words - 1);
			
			for (int i = 0; i < horiz_words.length; i++) {
				this.game_words[i + 1] = horiz_words[i];
			}

    	    // Determine the horizontal cross indices
    	    int[] horiz_cross_index = determineCrossovers(vert_word, vert_cross_index, horiz_words);

    	    // Determine the grid dimensions
    	    GridDimensions grid_x = getGridDimensions(horiz_words, horiz_cross_index);
    	    int grid_y = vert_word.length();

    	    // Adjust horizontal cross indices
    	    adjustCrossovers(horiz_cross_index, grid_x.verticalX);

    	    // Create and mask the grid
			this.finishedGrid = createGrid(vert_word, grid_x, horiz_words, vert_cross_index, horiz_cross_index);
    	    this.userGrid = maskGrid(this.finishedGrid);

    	    // Determine Faults count
    	    int num_letters = countLetters(this.userGrid);
    	    this.lives = faults * num_letters;
    	    System.out.println("Fault counter: " + this.lives + "\n");

    	    printGrid(this.finishedGrid);
    	    System.out.print("\n");
    	    printGrid(this.userGrid);

    	    // Send the grid to the client
			StringBuilder returnQuery = new StringBuilder("SUCCESS " + this.lives + " " );
			returnQuery.append(gridToString(this.userGrid));

			out.println(returnQuery.toString());
    	}


		/*
			Main function to check and update grid based on users guess
			Updates global variables userGrid/finishedGrid and lettersGuessed/wordsGuessed to return a new game grid
			Called in Main

			Returns a WIN if grid is completed with new guess
			Returns existing grid layout and life counter if not
		*/ /*
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
				reset();
			} else if (this.lives <= 0){
				out.println("LOSE");
				reset();
			}

			else {
				// Send the grid to the client
				StringBuilder returnQuery = new StringBuilder("SUCCESS " + this.lives + " " );
				returnQuery.append(gridToString(this.userGrid));

				out.println(returnQuery.toString());
			}
		}

			CrosswordInterfaceHandler(Socket socket) {
				this.interfaceSocket = socket;
			}

			@Override
			public void run() {

		    	System.out.println("Connected, in communication with interface");
			    try {
    			    PrintStream out = new PrintStream(interfaceSocket.getOutputStream());
    		    	Scanner in = new Scanner(new InputStreamReader(interfaceSocket.getInputStream()));

   			     String query = "";
				 //String commands[] = ["NEWGAME", "CHECK"];

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

									reset();
    						    	break;

    							case "CHECK":
    							    System.out.print("Checking User Guess " + parts[1] + "\n");

									if(this.userGrid != null) {
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
								case "RESET":
									reset();

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
			}
		}
	}
*/
