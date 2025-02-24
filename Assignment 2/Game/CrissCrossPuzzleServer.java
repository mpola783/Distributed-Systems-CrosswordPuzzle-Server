import java.rmi.*;
import java.rmi.RemoteException;


public interface CrissCrossPuzzleServer extends Remote{

    /*
        Name: startGame
        Desc: Starts a new game for the user identified by user_id
        
        Inputs: player- the name ID of the user who's requesting a new game to be started
                number_of_words- the difficulty level of the game
                failed_attempt_factor- the number of faults allowed (later multiplied by number of letters in game)
    
    */
    public String startGame(String[] players, int number_of_words, int failed_attempt_factor, String gameID) throws RemoteException;
    
    /*
    public String guessLetter(String player,char letter) throws RemoteException;
    
    
    public String guessWord(String player, String word) throws RemoteException;
    
    
    public String endGame(String player) throws RemoteException;
    
    */
    public String restartGame(String player) throws RemoteException;
    /*
    
    public String addWord() throws RemoteException;
    
    
    public String removeWord() throws RemoteException;
    
    
    public String checkWord() throws RemoteException; */
}