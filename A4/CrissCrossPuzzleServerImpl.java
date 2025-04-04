/* CrissCrossPuzzleServerImpl 
 * handles game identification, player management, game settings, and score management. 
 * This interface is implemented by a server-side class and exposes methods to clients 
 * in a distributed RMI system. */

import java.net.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.*;
import java.util.List;
import java.rmi.NotBoundException;
import java.net.MalformedURLException;
import game.Game;


public class CrissCrossPuzzleServerImpl extends UnicastRemoteObject implements CrissCrossPuzzleServer {

    
    // Instead of storing game-specific data internally, we keep a map of game states.
    private Map<String, CrosswordGameState> gameStates;
    private Map<String, GameLobbyInfo> pendingLobbies = new HashMap<>();


    // Constructor
    public CrissCrossPuzzleServerImpl() throws RemoteException {
        super();
        gameStates = new HashMap<>();
    }


    // Inner helper class for grid dimensions.
    public class GridDimensions {
        int maxLength;
        int verticalX;
        public GridDimensions(int maxLength, int verticalX) {
            this.maxLength = maxLength;
            this.verticalX = verticalX;
        }
    }

    // Method to get the game state by gameID
    @Override
    public CrosswordGameState getGameState(String gameID) throws RemoteException {
        return gameStates.get(gameID);
    }

    
///////////////////////////NEW LOGIC TEST////////////////////////////////////////////////////////////////////////
    @Override
    public List<GameLobbyInfo> listLobbies() throws RemoteException {
        // Return a list of all pending lobbies.
        //return new ArrayList<>(pendingLobbies.values());

        return null;
    }

    public static Game translateToGame(CrosswordGameState gameState) throws RemoteException {
        Game game = new Game(
            gameState.getGameID(),
            gameState.getNumWords(),
            gameState.getTotalLives(),
            gameState.getGameWords(), // optional field
            false,  // multiplayer off for now
            0       // expectedPlayers unused
        );

        // Add players
        for (String name : gameState.getPlayerNames()) {
            game.addPlayer(name);
        }

        // Set current state
        game.setActivePlayer(gameState.getActivePlayer());
        game.setLives(gameState.getLives());
        game.setGameStatus(gameState.getGameStatus());

        // Copy grids
        char[][] finishedGrid = gameState.getFinishedGrid();
        char[][] playerGrid = gameState.getPlayerGrid();
        for (int i = 0; i < finishedGrid.length; i++) {
            game.setFinishedGridRow(i, finishedGrid[i]);
        }
        for (int i = 0; i < playerGrid.length; i++) {
            game.setPlayerGridRow(i, playerGrid[i]);
        }

        // Copy guesses
        for (char letter : gameState.getLettersGuessed()) {
            game.addGuessedLetter(letter);
        }
        for (String word : gameState.getWordsGuessed()) {
            game.addGuessedWord(word);
        }

        return game;
    }

   @Override
    public Game startMultiplayer(String name, int numberOfPlayers, int gameLevel) throws RemoteException {
        String gameID = null;
        boolean isNewLobby = true;
    
        // Check for an existing open lobby with the same settings
        for (GameLobbyInfo lobby : pendingLobbies.values()) {
            if (lobby.getExpectedPlayers() == numberOfPlayers && lobby.getGameLevel() == gameLevel
                && lobby.getCurrentPlayers() < numberOfPlayers) {
            
                // Add the player to the existing game
                gameID = lobby.getGameID();
                CrosswordGameState gameState = gameStates.get(gameID);
                gameState.addPlayer(name);
                gameStates.put(gameID, gameState);
                lobby.incrementPlayers();
                isNewLobby = false;
            
                System.out.println("Joined lobby");
            }
        }

        // If no existing lobby was found, create a new one
        if (isNewLobby) {
            gameID = UUID.randomUUID().toString();
            CrosswordGameState gameState = new CrosswordGameStateImpl(gameID, numberOfPlayers, gameLevel);
            gameState.addPlayer(name);
            gameState.setExpectedPlayers(numberOfPlayers);
            gameStates.put(gameID, gameState);

            GameLobbyInfo newLobby = new GameLobbyInfo(gameID, name, numberOfPlayers, gameLevel);
            pendingLobbies.put(gameID, newLobby);

            System.out.println("Created a new lobby. Waiting for more players...");
        }

        // Keep waiting in the lobby until it's full or the player exits
        while (true) {  // Keep looping until game starts or player exits
            GameLobbyInfo lobby = pendingLobbies.get(gameID);

            // If all players have joined, start the game
            if (lobby.getCurrentPlayers() == numberOfPlayers) {
                System.out.println("All players have joined! Starting game...");

                // Start the game using startGame()
                if(pendingLobbies.get(gameID).getHostName().equals(name)) {
                    gameID = startGame(name, numberOfPlayers, gameLevel, gameID);
                }

                Game game = translateToGame(getGameState(gameID))
                return game;
            }
            try {
                Thread.sleep(3000); // Wait 3 seconds before checking again
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

    @Override
    public void updateActivePlayer(String gameID) throws RemoteException {
        CrosswordGameState gameState = gameStates.get(gameID);
        gameState.nextActivePlayer();
    }

    @Override
    public String getActivePlayer(String gameID) throws RemoteException {
        CrosswordGameState gameState = gameStates.get(gameID);
        
        return gameState.getActivePlayer();
    }

    @Override
    public char[][] getCurrentGrid(String gameID) throws RemoteException {
        CrosswordGameState gameState = gameStates.get(gameID);
        
        return gameState.getPlayerGrid();
    }


    @Override
    public void updatePlayerScore(String gameID, String playerName, int points) throws RemoteException{
        CrosswordGameState gameState = gameStates.get(gameID);
        
        gameState.setPlayerScore(playerName, points);
    }

    @Override
    public String displayAllScores(String gameID) throws RemoteException {
        CrosswordGameState gameState = gameStates.get(gameID);
        
        return gameState.displayScores();
    }
    

/////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // NEWGAME START
    /*
        Main function to create a new game
        Connects to word server and generates user game grid
        Creates randomly selected crossword layout using game input from user

        Returns a SUCCESS, life counter, and user game layout after completion
    */

    @Override
    public String startGame(String player, int numberOfWords, int failedAttemptFactor, String gameID) throws RemoteException {
        
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
            gameState = new CrosswordGameStateImpl(gameID, numberOfWords, failedAttemptFactor);

            gameState.addPlayer(player);

            // Store game state
            gameStates.put(gameID, gameState);
            
        } else {
            // Retrieve existing game state
            gameState = gameStates.get(gameID);
        }

        gameState.setActivePlayer(player); 

        // Fetch the vertical word from the word server
        String vertWord = fetchVerticalWord(numberOfWords);
        gameState.setGameWords(0, vertWord);
        System.out.println("\nVertical Word: " + vertWord);

        // Select random indexes for vertical-horizontal crossing
        int[] vertCrossIndex = getRandomIndexes(vertWord, numberOfWords - 1);
        String[] crossingValues = getValuesAtRandomIndexes(vertWord, vertCrossIndex);

        // Fetch the horizontal words
        String[] horizWords = fetchHorizontalWords(vertWord, crossingValues, numberOfWords - 1);
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
        gameState.setPlayerGrid(maskGrid(gameState.getFinishedGrid()));

        // Determine Faults count
        int numLetters = countLetters(gameState.getPlayerGrid());
        gameState.setLives(failedAttemptFactor * numLetters);
        System.out.println("Fault counter: " + gameState.getLives() + "\n");

        // Print grids for debugging
        printGrid(gameState.getFinishedGrid());
        System.out.println();
        printGrid(gameState.getPlayerGrid());

        if (gameStates.containsKey(gameID)) {  
            gameStates.get(gameID).setGameStatus("In-progress");
        }

        gameStates.put(gameState.getGameID(), gameState);
        

        return gameState.getGameID();
    }
    

    @Override
    public String restartGame(String gameID) throws RemoteException {
        // Retrieve the game state using gameID
        CrosswordGameState gameState = gameStates.get(gameID);
        
        // Check if the game exists
        if (gameState != null) {
            // Retrieve existing game details
            String[] players = gameState.getPlayerNames();
            int numWords = gameState.getNumWords();
            int totalLives = gameState.getTotalLives(); // Total lives from the previous game

            for (int i = 0; i < numWords; i++) {
                gameState.setGameWords(i, null);  // Reset horizontal words
            }

            // Reset the game state (e.g., keeping players and words)
            // Clear the grid and user grid
            gameState.setFinishedGrid(null);
            gameState.setPlayerGrid(null);


            // Call startGame to reset the game with the same configuration
            startGame(players[0], numWords, totalLives, gameID); // Reuse startGame logic

            // Notify the players that the game has been reset
            System.out.println("Game has been reset successfully for game ID: " + gameID);
            
            // Return a success message
            return gameID;
        } else {
            // If the game doesn't exist
            return "Game with ID " + gameID + " not found.";
        }
    }

    @Override
    public void exitGame(String gameID) throws RemoteException {
        gameStates.remove(gameID);
    }


    /*
        Main function to check and update grid based on users guess
        Updates global variables userGrid/finishedGrid and lettersGuessed/wordsGuessed to return a new game grid
        Called in Main

        Returns a WIN if grid is completed with new guess
        Returns existing grid layout and life counter if not
    */
   @Override
    public String updateGuess(CrosswordGameState gameState, String guess) throws RemoteException {
        System.out.println("\nUpdating guess: " + guess);

        gameState.setLives((gameState.getLives()) - 1);

        if (guess.length() == 1) {
            char guessedChar = guess.charAt(0); // Convert string to char
            guessedChar = Character.toUpperCase(guessedChar); 

            gameState.addLetterGuess(guessedChar);
        }
        else {
            String guessedWord = guess.toUpperCase();
            gameState.addWordGuess(guessedWord);
        }

        gameState.setPlayerGrid(updateUserGrid(gameState, gameState.getFinishedGrid()));
        
        if(isComplete(gameState.getPlayerGrid())) {
            gameState.setGameStatus("WIN");
            endGame(gameState.getGameID());
            System.out.print("\nGAME WON\n");
        } else if (gameState.getLives() <= 0){
            gameState.setGameStatus("LOSE");
            endGame(gameState.getGameID());
            System.out.print("\nGAME LOST\n");
        }

        gameStates.put(gameState.getGameID(), gameState);

        return gameState.getGameID();
    }

    @Override
    public char[][] checkGuess(CrosswordGameState gameState, String guess) throws RemoteException {
        System.out.println("\nChecking guess: " + guess);
        char guessedChar;
        String guessedWord = guess.toUpperCase();

        if (guess.length() == 1) {
            guessedChar = guess.charAt(0); // Convert string to char
            guessedChar = Character.toUpperCase(guessedChar); 

            gameState.addLetterGuess(guessedChar);
        }
        else {
            gameState.addWordGuess(guessedWord);
        }

        
        char [][] grid = updateUserGrid(gameState, gameState.getFinishedGrid());
        
        if (guess.length() == 1)
        {
            guessedChar = guess.charAt(0);
            gameState.removeLetterGuess(guessedChar);
        }
        else {
            gameState.removeWordGuess(guessedWord);
        }

        return grid;
    }


    private String sendToWordServerRMI(int length, String command, String letter) throws RemoteException {
        try {
            // Look up the WordServer object from the RMI registry
            WordServer wordServer = (WordServer) Naming.lookup("rmi://localhost/WordServer");
            String returnedWord;

            if (command == null) {
                // Ensure length is not null before calling the method
                if (length <= 0) {
                    throw new IllegalArgumentException("Length parameter must be greater than 0.");
                }

                returnedWord =  wordServer.getRandomVertWord(length);
                return returnedWord;
            } else {
                returnedWord = wordServer.getRandomWord(command, letter);
                return returnedWord;
            }

        } catch (NotBoundException | MalformedURLException e) {
            throw new RemoteException("Failed to connect to WordServer via RMI.", e);
        } catch (Exception e) {
            throw new RemoteException("An unexpected error occurred while communicating with WordServer.", e);
        }
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

   public String[] getValuesAtRandomIndexes(String word, int[] randomIndexes) {
        if (word == null || word.isEmpty() || randomIndexes == null || randomIndexes.length == 0) {
            throw new IllegalArgumentException("Invalid input: word or indexes cannot be null or empty.");
        }

        String[] selectedStrings = new String[randomIndexes.length];

        for (int i = 0; i < randomIndexes.length; i++) {
            if (randomIndexes[i] < 0 || randomIndexes[i] >= word.length()) {
                throw new IndexOutOfBoundsException("Index out of bounds for word: " + randomIndexes[i]);
            }
            selectedStrings[i] = String.valueOf(word.charAt(randomIndexes[i])); // Convert char to String
        }

        return selectedStrings;
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
    private String fetchVerticalWord(int length) {
        try {
            String vert_word = sendToWordServerRMI(length, null, null);

            if (vert_word == null) {
                throw new RuntimeException("Error: Invalid response from word server for query");
            }

            return vert_word;
        } catch (RemoteException e) {
            // Handle the RemoteException
            throw new RuntimeException("Error occurred while fetching the vertical word", e);
        }
    }


    // Function to fetch the horizontal words from word server
    private String[] fetchHorizontalWords(String vertWord, String[] letters, int wordCount) {
        String[] horiz_words = new String[wordCount];

        for (int i = 0; i < wordCount; i++) {
            try {
                String command = "m";
                String horiz_word = sendToWordServerRMI(0, command, letters[i]);

                if (horiz_word == null) {
                    throw new RuntimeException("Error: Invalid response from word server for query");
               }

                horiz_words[i] = horiz_word;
                System.out.print("\nHorizontal Word: " + horiz_words[i]);
            } catch (RemoteException e) {
                // Handle the exception, e.g., log it or rethrow as a RuntimeException
                throw new RuntimeException("Error occurred while fetching horizontal words", e);
            }
        }

        return horiz_words;
    }

    // Function to determine horizontal cross indices
    private int[] determineCrossovers(String vert_word, int[] vert_cross_index, String[] horiz_words) {
        if (horiz_words.length < vert_cross_index.length) {
            throw new IllegalArgumentException("Mismatch between vertical cross index count and horizontal words count.");
        }

        int[] horiz_cross_index = new int[vert_cross_index.length];

        for (int i = 0; i < vert_cross_index.length; i++) {
            char targetChar = Character.toUpperCase(vert_word.charAt(vert_cross_index[i]));
            String horizWordUpper = horiz_words[i].toUpperCase();
            horiz_cross_index[i] = horizWordUpper.indexOf(targetChar);

            if (horiz_cross_index[i] < 0) {
                throw new IllegalArgumentException("Character '" + vert_word.charAt(vert_cross_index[i]) + 
                    "' is not found in horizontal word: " + horiz_words[i]);
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
    public char[][] updateUserGrid(CrosswordGameState gameState, char[][] grid) throws RemoteException {
        int rows = grid.length;
        int cols = grid[0].length;
        char[][] maskedGrid = new char[rows][cols];

        // Initialize masked grid with '-'
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                char cell = grid[y][x];

                // Check if the cell is in letters_guessed or if it's '.'
                if (cell == '.' || isGuessedLetter(gameState, cell)) {
                    maskedGrid[y][x] = cell; // Keep revealed
                } else {
                    maskedGrid[y][x] = '-'; // Mask initially
                }
            }
        }

        revealWords(gameState, grid, maskedGrid);
        return maskedGrid;
    }

    /*
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
    } */

    // Helper function to check if a letter is in lettersGuessed
    private boolean isGuessedLetter(CrosswordGameState gameState, char letter) throws RemoteException{
        if (gameState == null || gameState.getLettersGuessed() == null) {
            throw new IllegalArgumentException("Invalid gameState or lettersGuessed is null.");
        }

        for (char guessed : gameState.getLettersGuessed()) {
            if (guessed == letter) {
                return true; // Letter has been guessed
            }
        }
        return false; // Letter not guessed yet
    }

	// Helper function to reveal words that exist in word_guessed
    private void revealWords(CrosswordGameState gameState, char[][] grid, char[][] maskedGrid) throws RemoteException {
        int rows = grid.length;
        int cols = grid[0].length;

        for (String word : gameState.getWordsGuessed()) {
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


    @Override
    public void endGame(String gameID) {
        // Check if the game exists in the map
        if (gameStates.containsKey(gameID)) {
            // Optionally, perform any cleanup or notification here
            CrosswordGameState gameState = gameStates.get(gameID);
            System.out.println("Ending game with ID: " + gameID);
        
            // Remove the game state from the map to end the game
            gameStates.remove(gameID);
        
            // Optionally, notify that the game has ended
            System.out.println("Game " + gameID + " has been ended.");
        } else {
            // If the game ID does not exist
            System.out.println("Game with ID " + gameID + " not found.");
        }
    }


	public static void main(String[] args) {
        try {
            CrissCrossPuzzleServerImpl server = new CrissCrossPuzzleServerImpl();
            // Bind the server object to the RMI registry with a unique name
            Naming.rebind("CrissCrossPuzzleServer", server);

            System.out.println("Game Server is running...");
        } catch (Exception e) {
            System.err.println("Error starting Game Server");
            e.printStackTrace();
        }
    }
}