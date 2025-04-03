/**
 * COMP4635 Tutorial
 * Example implementation of a peer-to-peer application that allows peers to exchange messages.
 * The application implements Lamport Clocks.
 *
 * Code Structure:
 * 1. LamportClock.java       - Implements the Lamport logical clock.
 * 2. Message.java            - Represents a message with content and timestamp.
 * 3. ReceiverInterface.java  - Defines the remote interface for receiving messages.
 * 4. ReceiverImpl.java       - Implements ReceiverInterface, processes messages, and maintains the clock.
 * 5. PeerProcess.java        - The main entry point for starting a peer node.
 */

/**
 * ReceiverInterface.java
 * Defines the remote interface for receiving messages via RMI.
 */

 import java.rmi.Remote;
 import java.rmi.RemoteException;
 
 /**
  * Receiver interface for handling incoming messages.
  */
 public interface ReceiverInterface extends Remote {

     boolean receiveMessage(String guess, String senderID, int timestamp) throws RemoteException;
     void sendMessage(String[] players, String sender, String message) throws RemoteException;
     void doEvent(String event, char[][] grid) throws RemoteException;
 }
 