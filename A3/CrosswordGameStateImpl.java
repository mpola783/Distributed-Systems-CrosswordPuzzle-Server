/*
GAME STATE
The game server operates based on interaction from other servers, updates individual game states
handles game identification, player management, game settings, and score management. 
*/

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.*;
import java.io.*;
import java.rmi.Naming;

public class CrosswordGameStateImpl extends UnicastRemoteObject implements CrosswordGameState {
    private static final long serialVersionUID = 1L;

    private String gameID;
    private List<PlayerScore> players; // List of players (each with a name and score)
    private String activePlayer;       // Currently active player's name
    private int numWords;
    private int lives;
    private int totalLives;
    private String[] gameWords;
	private char[] lettersGuessed;
	private String[] wordsGuessed;
    private char[][] finishedGrid;
    private char[][] playerGrid;
	private int lettersGuessedCount = 0; // Tracks number of letters added
    private int wordsGuessedCount = 0;   // Tracks number of words added
	private int maxlettersGuessed = 50;
	private int maxwordsGuessed = 50;
    private String gameStatus;
    private boolean multiplayer;
    private int expectedPlayers;
    
    public CrosswordGameStateImpl(String gameID, int numWords, int lives) throws RemoteException {
        super(); // Required for RMI
        this.gameID = gameID;
        this.players = new ArrayList<>();
        this.numWords = numWords;
        this.lives = lives;
        this.totalLives = lives;
        this.gameWords = new String[numWords];
		this.lettersGuessed = new char[maxlettersGuessed];
		this.wordsGuessed = new String[maxwordsGuessed];
        this.gameStatus = "Waiting";
    }
    

    public static void main(String[] args) {
        try {
            // Create the instance of the CrosswordGameStateImpl
            CrosswordGameState gameState = new CrosswordGameStateImpl("", 0, 0);

            // Bind the game state object to the RMI registry with a unique name
            Naming.rebind("CrosswordGameState", gameState);

            System.out.println("CrosswordGame RMI Server is running...");
        } catch (Exception e) {
            System.err.println("RMI Server failed: " + e.getMessage());
            e.printStackTrace();
        }
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
    

	/**
	 * 	PLAYER / Players GETTERS AND SETTERS
	*/
    // Add a player with an initial score of 0.
    @Override
    public void addPlayer(String playerName) {
        players.add(new PlayerScore(playerName, 0));
        // If no active player is set, use the first added player.
        if (activePlayer == null) {
            activePlayer = playerName;
        }
    }
    
    // Remove a player from the game.
    @Override
    public void removePlayer(String playerName) {
        players.removeIf(p -> p.getPlayerName().equals(playerName));
        // If the removed player was active, move to the next available player.
        if (playerName.equals(activePlayer)) {
            nextActivePlayer();
        }
    }
    
	// Returns the list of players with their scores.
    @Override
    public List<PlayerScore> getPlayers() {
        return players;
    }

    @Override
	public String[] getPlayerNames() {
    	String[] playerNames = new String[players.size()];
    	for (int i = 0; i < players.size(); i++) {
    	    playerNames[i] = players.get(i).getPlayerName();
    	}
    	return playerNames;
	}

    //@Override
    public void setPlayers(List<PlayerScore> players) {
        this.players = players;
    }

    @Override
    public boolean checkMultiplayer() {
        return multiplayer;
    }

    @Override
    public void setMultiplayer(boolean multiplayer) {
        this.multiplayer = multiplayer;
    }

    @Override
	public void setExpectedPlayers(int num_players) {
        this.expectedPlayers = num_players;
    }

    @Override
	public int getExpectedPlayers() {
        return expectedPlayers;
    }

	/**
	 * 	LOBBY AND TURN BASED GETTERS AND SETTERS
	*/
	// Returns the current active player's name.
    @Override
    public String getActivePlayer() {
        return activePlayer;
    }

    @Override
    public void setActivePlayer(String name) {
        this.activePlayer = name;
    }
	
    @Override
	public int getLobbySize() {
        return players.size();
    }
    
    
    public void setGameStatus(String status) {
        this.gameStatus = status;
    }

    
    public String getGameStatus() {
        return gameStatus;
    }


    // Cycle to the next active player; wraps around at the end.
    @Override
    public void nextActivePlayer() {
        
        if (players.isEmpty()) {
            this.activePlayer = null;
            return;
        }
        if (activePlayer == null) {
            this.activePlayer = players.get(0).getPlayerName();
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
            this.activePlayer = players.get(0).getPlayerName();
        } else {
            this.activePlayer = players.get((index + 1) % players.size()).getPlayerName();
        }
    }
    

    /**
	 * 	GAME ID
	*/
    @Override
    public String getGameID() {
        return gameID;
    }
    
    @Override
    public void setGameID(String gameID) {
        this.gameID = gameID;
    }
    

	/**
	 * 	GAME LIVES
	*/
    @Override
    public int getLives() {
        return lives;
    }
    
    @Override
    public void setLives(int lives) {
        this.lives = lives;
    }
    
    @Override
    public int getTotalLives() {
        return totalLives;
    }
    
    @Override
    public void setTotalLives(int totalLives) {
        this.totalLives = totalLives;
    }
    

	/**
	 * 	GAME SETTINGS SETTERS AND GETTERS
	*/
    @Override
    public void setNumWords(int numWords) {
        this.numWords = numWords;
    }
    
    @Override
    public int getNumWords() {
        return numWords;
    }
    
    @Override
    public String[] getGameWords() {
        return gameWords;
    }
    
    @Override
    public void setGameWords(int position, String word) {
        this.gameWords[position] = word;
    }
    
    @Override
    public char[][] getFinishedGrid() {
        return finishedGrid;
    }
    
    @Override
    public void setFinishedGrid(char[][] finishedGrid) {
        this.finishedGrid = finishedGrid;
    }
    
    @Override
    public char[][] getPlayerGrid() {
        return playerGrid;
    }
    
    @Override
    public void setPlayerGrid(char[][] playerGrid) {
        this.playerGrid = playerGrid;
    }

	

	/**
	 * 	GAME GUESSES for WORDS and CHARS
	*/
	// Add a letter to lettersGuessed array
    @Override
    public void addLetterGuess(char letter) {
    	if (lettersGuessedCount < lettersGuessed.length) {
    	    lettersGuessed[lettersGuessedCount++] = letter;
    	} else {
    	    throw new IllegalStateException("Max letters guessed reached!");
    	}
	}

    @Override
	// Add a word to wordsGuessed array
	public void addWordGuess(String word) {
	    if (wordsGuessedCount < wordsGuessed.length) {
    	    wordsGuessed[wordsGuessedCount++] = word;
    	} else {
    	    throw new IllegalStateException("Max words guessed reached!");
    	}
	}

    @Override
    public char[] getLettersGuessed() {
        return Arrays.copyOf(lettersGuessed, lettersGuessedCount); // Return only valid entries
    }

    @Override
    public String[] getWordsGuessed() {
        return Arrays.copyOf(wordsGuessed, wordsGuessedCount); // Return only valid entries
    }

    @Override
    public int getLettersGuessedCount() {
        return lettersGuessedCount;
    }

    @Override
    public int getWordsGuessedCount() {
        return wordsGuessedCount;
    }

    
    /**
     * Prints out a grid along with game state details.
     * @param gridType Specify "finished" to print the finishedGrid or "player" to print the playerGrid.
     */
    @Override
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
    @Override
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

