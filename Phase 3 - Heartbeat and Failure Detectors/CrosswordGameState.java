/**
 * The CrosswordGameState interface defines the remote methods that allow interaction 
 * of with the crossword game state
*/

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface CrosswordGameState extends Remote {
    enum PlayerState {
    	Alive,Sus,Dead
    }
    
    PlayerState getState(String playerName) throws RemoteException;
    void setState(PlayerState state, String playerName) throws RemoteException;
    void setAlive(String playerName) throws RemoteException;
    void setSus(String playerName) throws RemoteException;
    void setDead(String playerName) throws RemoteException;
    
    /** GAME IDENTIFICATION */
    String getGameID() throws RemoteException;
    void setGameID(String gameID) throws RemoteException;

    /** PLAYER MANAGEMENT */
    List<CrosswordGameStateImpl.PlayerScore> getPlayers() throws RemoteException;
    void addPlayer(String playerName) throws RemoteException;
    void removePlayer(String playerName) throws RemoteException;
    void setActivePlayer(String name) throws RemoteException;
    String getActivePlayer() throws RemoteException;
    int getLobbySize() throws RemoteException;
    void nextActivePlayer() throws RemoteException;
    int getPlayerScore(String playerName) throws RemoteException;
    String[] getPlayerNames() throws RemoteException;
    void setMultiplayer(boolean multiplayer) throws RemoteException;
    boolean checkMultiplayer() throws RemoteException;
    int getExpectedPlayers() throws RemoteException;
    void setExpectedPlayers(int num_players) throws RemoteException;
    boolean playerInGame(String name) throws RemoteException;
    //void setPlayers(List<PlayerScore> players) throws RemoteException;

    
    /** GAME SETTINGS */
    int getNumWords() throws RemoteException;
    void setNumWords(int numWords) throws RemoteException;
    String[] getGameWords() throws RemoteException;
    void setGameWords(int position, String word) throws RemoteException;
    void setGameStatus(String status) throws RemoteException;
    String getGameStatus() throws RemoteException;

    /** GAME LIVES */
    int getLives() throws RemoteException;
    void setLives(int lives) throws RemoteException;
    int getTotalLives() throws RemoteException;
    void setTotalLives(int totalLives) throws RemoteException;

    /** GAME GRID */
    char[][] getPlayerGrid() throws RemoteException;
    void setPlayerGrid(char[][] playerGrid) throws RemoteException;
    char[][] getFinishedGrid() throws RemoteException;
    void setFinishedGrid(char[][] finishedGrid) throws RemoteException;

    /** GAME GUESSES (LETTERS & WORDS) */
    void addLetterGuess(char letter) throws RemoteException;
    void addWordGuess(String word) throws RemoteException;
    char[] getLettersGuessed() throws RemoteException;
    String[] getWordsGuessed() throws RemoteException;
    int getLettersGuessedCount() throws RemoteException;
    int getWordsGuessedCount() throws RemoteException;

    /** DISPLAY GRID */
    void displayGrid(String gridType) throws RemoteException;

    /** SCORE MANAGEMENT */
    void updateActivePlayerScore(int scoreDelta) throws RemoteException;
    void setPlayerScore(String playerName, int points) throws RemoteException;
    String displayScores() throws RemoteException;
}