/**
 * The CrissCrossPuzzleServer interface defines the remote methods that allow interaction with the CrissCrossPuzzleServerImpl
*/


import java.rmi.*;
import java.rmi.RemoteException;
import java.util.List;

public interface CrissCrossPuzzleServer extends Remote{

    long registerClient(String clientID) throws RemoteException;
    void incrementSequence(String name) throws RemoteException;

    String restartGame(String player, long sequenceNumber) throws RemoteException; 
    
    void exitGame(String gameID, long sequenceNumber) throws RemoteException; 
    /* These are part of checkGuess
        public String guessLetter(String player,char letter) throws RemoteException;
        public String guessWord(String player, String word) throws RemoteException;
    */
    String checkGuess(CrosswordGameState gameState, String guess, String name, long sequenceNumber) throws RemoteException;


    CrosswordGameState getGameState(String gameID) throws RemoteException;


    // Starts a single-player game for the given player.
    // Returns a game ID that the client can use to interact with the game.
    String startGame(String player, int numberOfWords, int failedAttemptFactor, String gameID, long sequenceNumber) throws RemoteException;

    // Starts a multiplayer game lobby for the given player and number of players.
    // Returns a game ID.
    String startMultiplayer(String name, int numberOfPlayers, int gameLevel, long sequenceNumber) throws RemoteException;

    String getActivePlayer(String gameID) throws RemoteException;
    void updateActivePlayer(String gameID, long sequenceNumber) throws RemoteException;
    String displayAllScores(String gameID, long sequenceNumber) throws RemoteException;
    void updatePlayerScore(String gameID, String playerName, int points, long sequenceNumber) throws RemoteException;
    char[][] getCurrentGrid(String gameID) throws RemoteException;

}