import java.rmi.*;
import java.rmi.RemoteException;
import java.util.List;

public interface CrissCrossPuzzleServer extends Remote{

    /*
        Name: startGame
        Desc: Starts a new game for the user identified by user_id
        
        Inputs: player- the name ID of the user who's requesting a new game to be started
                number_of_words- the difficulty level of the game
                failed_attempt_factor- the number of faults allowed (later multiplied by number of letters in game)
    
    */

    
    /*
    public String guessLetter(String player,char letter) throws RemoteException;
    
    
    public String guessWord(String player, String word) throws RemoteException;
    
    
    public String endGame(String player) throws RemoteException;
    
    */
    String restartGame(String player) throws RemoteException;
    /*
    
    public String addWord() throws RemoteException;
    
    
    public String removeWord() throws RemoteException;
    
    */
    String checkGuess(CrosswordGameState gameState, String guess) throws RemoteException;


    CrosswordGameState getGameState(String gameID) throws RemoteException;


    // Starts a single-player game for the given player.
    // Returns a game ID that the client can use to interact with the game.
    String startGame(String player, int numberOfWords, int failedAttemptFactor, String gameID) throws RemoteException;

    // Starts a multiplayer game lobby for the given player and number of players.
    // Returns a game ID.
    String startMultiplayer(String name, int numberOfPlayers, int gameLevel) throws RemoteException;

    // Joins an existing multiplayer game using its game ID.
    // When the lobby becomes full, the game logic is started.
    String joinMultiplayer(String name, String gameID) throws RemoteException;

    // list current multiplayer lobbies that haven't started.
    List<GameLobbyInfo> listLobbies() throws RemoteException;
}