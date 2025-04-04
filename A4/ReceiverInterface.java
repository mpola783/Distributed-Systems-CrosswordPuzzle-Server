/**
 * The ReceiverImpl class implements the ReceiverInterface and provides an RMI-based receiver that
 *  processes the game messages from other client processes to achieve FIFO - total order Synchronization
 */

 import java.rmi.Remote;
 import java.rmi.RemoteException;
 import game.Game;
 
 /**
  * Receiver interface for handling incoming messages.
  */
 public interface ReceiverInterface extends Remote {

     void doEvent(String event, char[][] grid) throws RemoteException;

     boolean receiveGame(Game game, String senderID, int timestamp) throws RemoteException;
     void sendGame(String[] players, String senderID, Game game) throws RemoteException;
 }
