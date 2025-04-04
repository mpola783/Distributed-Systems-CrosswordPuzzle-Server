/**
 * The CrissCrossPuzzleServer interface defines the remote methods that allow interaction with the CrissCrossPuzzleServerImpl
*/


import java.rmi.*;
import java.rmi.RemoteException;
import java.util.List;
import game.Game;

public interface CrissCrossPuzzleServer extends Remote{

    
    void endGame(String gameID) throws RemoteException;
    
    String restartGame(String player) throws RemoteException; 
    
    void exitGame(String gameID) throws RemoteException; 
    /* These are part of checkGuess
        public String guessLetter(String player,char letter) throws RemoteException;
        public String guessWord(String player, String word) throws RemoteException;
    */
    String updateGuess(CrosswordGameState gameState, String guess) throws RemoteException;
    char[][] checkGuess(CrosswordGameState gameState, String guess) throws RemoteException;


    CrosswordGameState getGameState(String gameID) throws RemoteException;


    // Starts a single-player game for the given player.
    // Returns a game ID that the client can use to interact with the game.
    String startGame(String player, int numberOfWords, int failedAttemptFactor, String gameID) throws RemoteException;

    // Starts a multiplayer game lobby for the given player and number of players.
    // Returns a game ID.
    Game startMultiplayer(String name, int numberOfPlayers, int gameLevel) throws RemoteException;

    // Joins an existing multiplayer game using its game ID.
    // When the lobby becomes full, the game logic is started.



    String getActivePlayer(String gameID) throws RemoteException;
    void updateActivePlayer(String gameID) throws RemoteException;
    String displayAllScores(String gameID) throws RemoteException;
    void updatePlayerScore(String gameID, String playerName, int points) throws RemoteException;
    char[][] getCurrentGrid(String gameID) throws RemoteException;

    // list current multiplayer lobbies that haven't started.
    List<GameLobbyInfo> listLobbies() throws RemoteException;
}