package game;

import java.util.ArrayList;
import java.util.List;

public class Game implements java.io.Serializable{

    private String gameID;
    private String activePlayer;
    private int numWords;
    private int lives;
    private int totalLives;
    private String[] gameWords;
    private char[] lettersGuessed;
    private String[] wordsGuessed;
    private char[][] finishedGrid;
    private char[][] playerGrid;
    private int lettersGuessedCount = 0;
    private int wordsGuessedCount = 0;
    private int maxlettersGuessed = 50;
    private int maxwordsGuessed = 50;
    private String gameStatus;
    private boolean multiplayer;
    private int expectedPlayers;
    private List<String> playerNames = new ArrayList<>();

    public Game(String gameID, int numWords, int totalLives, String[] gameWords, boolean multiplayer, int expectedPlayers) {
        this.gameID = gameID;
        this.numWords = numWords;
        this.totalLives = totalLives;
        this.lives = totalLives;
        this.gameWords = gameWords;
        this.multiplayer = multiplayer;
        this.expectedPlayers = expectedPlayers;
        this.lettersGuessed = new char[maxlettersGuessed];
        this.wordsGuessed = new String[maxwordsGuessed];
        this.finishedGrid = null;
        this.playerGrid = null;
        this.gameStatus = "Waiting";
    }

    public void initializeGrids(int height, int width) {
        this.finishedGrid = new char[height][width];
        this.playerGrid = new char[height][width];
    }  

    // === Basic Getters/Setters ===
    public String getGameID() {
        return gameID;
    }

    public String getActivePlayer() {
        return activePlayer;
    }

    public void setActivePlayer(String activePlayer) {
        this.activePlayer = activePlayer;
    }

    public int getNumWords() {
        return numWords;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public void loseLife() {
        if (lives > 0) lives--;
        if (lives == 0) gameStatus = "lost";
    }

    public void gainLife() {
        if (lives < totalLives) lives++;
    }

    public char[][] getPlayerGrid() {
        return playerGrid;
    }

    public char[][] getFinishedGrid() {
        return finishedGrid;
    }

    public String getGameStatus() {
        return gameStatus;
    }

    public boolean isMultiplayer() {
        return multiplayer;
    }

    public int getExpectedPlayers() {
        return expectedPlayers;
    }

    public void setPlayerGrid(char[][] grid) {
        this.playerGrid = grid;
    }

    public void setPlayerGridRow(int rowIndex, char[] row) {
        playerGrid[rowIndex] = row;
    }

    public void setFinishedGridRow(int rowIndex, char[] row) {
        finishedGrid[rowIndex] = row;
    }

    public void setGameStatus(String status) {
        this.gameStatus = status;
    }

    public void addPlayer(String playerName) {
        playerNames.add(playerName);
    }

    public boolean hasPlayer(String playerName) {
        return playerNames.contains(playerName);
    }

    public List<String> getPlayerNames() {
        return playerNames;
    }

    public String[] getNamesOfPlayers() {
        return playerNames.toArray(new String[0]);
    }

    // === Game Actions ===
    public boolean addGuessedLetter(char letter) {
        if (lettersGuessedCount < maxlettersGuessed) {
            lettersGuessed[lettersGuessedCount++] = letter;
            return true;
        }
        return false;
    }

    public boolean addGuessedWord(String word) {
        if (wordsGuessedCount < maxwordsGuessed) {
            wordsGuessed[wordsGuessedCount++] = word;
            return true;
        }
        return false;
    }

    public char[] getLettersGuessed() {
        char[] result = new char[lettersGuessedCount];
        System.arraycopy(lettersGuessed, 0, result, 0, lettersGuessedCount);
        return result;
    }

    public String[] getWordsGuessed() {
        String[] result = new String[wordsGuessedCount];
        System.arraycopy(wordsGuessed, 0, result, 0, wordsGuessedCount);
        return result;
    }

    public boolean isPlayerGridComplete() {
        for (int y = 0; y < playerGrid.length; y++) {
            for (int x = 0; x < playerGrid[y].length; x++) {
                if (playerGrid[y][x] == '-') {
                    return false;
                }
            }
        }
        return true;
    }

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

        System.out.println("\nDisplaying " + gridType + " grid:");
        for (char[] row : gridToPrint) {
            for (char c : row) {
                System.out.print(c + " ");
            }
            System.out.println();
        }
    }

    public String checkGuess(String guess, String playerName) {
        //System.out.println("\nChecking guess: " + guess); for client
        boolean correctGuess = false;

        if (guess.length() == 1) {
            char guessedChar = Character.toUpperCase(guess.charAt(0));
            this.addGuessedLetter(guessedChar);
            correctGuess = isLetterInGrid(guessedChar);
        } else {
            String guessedWord = guess.toUpperCase();
            this.addGuessedWord(guessedWord);
            correctGuess = isWordInGrid(guessedWord);
        }
        
        if (!correctGuess) {
            this.lives--;
        }
        // Update playerGrid based on new guess
        this.playerGrid = updateUserGrid();

        // Check win/loss state
        if (isPlayerGridComplete()) {
            this.gameStatus = "WIN";
            //System.out.println("\nGAME WON\n"); letting client print
        } else if (this.lives <= 0) {
            this.gameStatus = "LOSE";
            //System.out.println("\nGAME LOST\n"); same as ^^
        }

        return this.gameID;
    }

    public char[][] updateUserGrid() {
        int rows = finishedGrid.length;
        int cols = finishedGrid[0].length;
        char[][] maskedGrid = new char[rows][cols];

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                char cell = finishedGrid[y][x];

                if (cell == '.' || isGuessedLetter(cell)) {
                    maskedGrid[y][x] = cell;
                } else {
                    maskedGrid[y][x] = '-';
                }
            }
        }

        revealWords(maskedGrid);
        return maskedGrid;
    }

    private void revealWords(char[][] maskedGrid) {
        int rows = finishedGrid.length;
        int cols = finishedGrid[0].length;

        for (int i = 0; i < wordsGuessedCount; i++) {
            String word = wordsGuessed[i];
            if (word == null || word.isEmpty()) continue;

            word = word.toUpperCase();

            // Horizontal
            for (int y = 0; y < rows; y++) {
                String rowString = new String(finishedGrid[y]).toUpperCase();
                int index = rowString.indexOf(word);
                while (index != -1) {
                    revealWordInRow(maskedGrid, finishedGrid, y, index, word.length());
                    index = rowString.indexOf(word, index + 1);
                }
            }

            // Vertical
            for (int x = 0; x < cols; x++) {
                StringBuilder colBuilder = new StringBuilder();
                for (int y = 0; y < rows; y++) {
                    colBuilder.append(finishedGrid[y][x]);
                }
                String colString = colBuilder.toString().toUpperCase();

                int index = colString.indexOf(word);
                while (index != -1) {
                    revealWordInColumn(maskedGrid, finishedGrid, x, index, word.length());
                    index = colString.indexOf(word, index + 1);
                }
            }
        }
    }

    private void revealWordInRow(char[][] maskedGrid, char[][] fullGrid, int row, int startIndex, int length) {
        for (int i = 0; i < length; i++) {
            maskedGrid[row][startIndex + i] = fullGrid[row][startIndex + i];
        }
    }

    private void revealWordInColumn(char[][] maskedGrid, char[][] fullGrid, int col, int startIndex, int length) {
        for (int i = 0; i < length; i++) {
            maskedGrid[startIndex + i][col] = fullGrid[startIndex + i][col];
        }
    }

    private boolean isGuessedLetter(char letter) {
        if (lettersGuessed == null) return false;

        letter = Character.toUpperCase(letter);
        for (int i = 0; i < lettersGuessedCount; i++) {
            if (lettersGuessed[i] == letter) {
                return true;
            }
        }
        return false;
    }
    private boolean isWordInGrid(String word) {
        int rows = finishedGrid.length;
        int cols = finishedGrid[0].length;
    
        // Check rows (horizontal)
        for (char[] row : finishedGrid) {
            String rowStr = new String(row).toUpperCase();
            if (rowStr.contains(word)) return true;
        }
    
        // Check columns (vertical)
        for (int x = 0; x < cols; x++) {
            StringBuilder col = new StringBuilder();
            for (int y = 0; y < rows; y++) {
                col.append(finishedGrid[y][x]);
            }
            if (col.toString().toUpperCase().contains(word)) return true;
        }
    
        return false;
    }
    private boolean isLetterInGrid(char letter) {
        for (char[] row : finishedGrid) {
            for (char cell : row) {
                if (Character.toUpperCase(cell) == letter) {
                    return true;
                }
            }
        }
        return false;
    }
}