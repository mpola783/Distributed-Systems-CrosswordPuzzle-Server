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
 * ReceiverImpl.java
 * Implements the ReceiverInterface, allowing processes to receive messages via RMI.
 */

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.*;
import java.util.Scanner;
import java.io.Serializable;

/**
 * Implements the ReceiverInterface, handling message reception and processing.
 */
public class ReceiverImpl extends UnicastRemoteObject implements ReceiverInterface {
    private String processName;  // The name of this process
    private LamportClock clock;  // The Lamport clock instance
    

    public class Message implements Serializable {
        private String content; // Message content
        private int timestamp;  // Associated Lamport timestamp
 

        public Message(String content, int timestamp) {
            this.content = content;
            this.timestamp = timestamp;
        }
 
        public String getContent() {
            return content;
        }
 
        public int getTimestamp() {
            return timestamp;
        }

        public String toString() {
            return "Message: '" + content + "' | Timestamp: " + timestamp;
        }
    }

    public ReceiverImpl(String name) throws RemoteException {
        super();
        this.processName = name;
        this.clock = new LamportClock();
    }


    @Override
    public boolean receiveMessage(String m, int receivedTimestamp) throws RemoteException {
        int oldInternalTimestamp = clock.getTime();
        int newTimestamp = clock.update(receivedTimestamp);
        System.out.println("[" + processName + "] Received: " + m 
            + " | Previous Internal Timestamp: " + oldInternalTimestamp 
            + " | Updated Timestamp: " + newTimestamp);
        return true;
    }


    @Override
    public void doEvent(String event, char[][] grid) throws RemoteException {
        int newTimestamp = clock.tick();
        System.out.println("[" + processName + "] Event: " + event + " | Timestamp: " + newTimestamp);
    }

    /**
     * Sends a message to another process over RMI.
     * @param targetProcess The name of the recipient process.
     * @param message The message content.
     */
    @Override
    public void sendMessage(String[] players, String message) throws RemoteException {
        try {
            int newTimestamp = clock.tick();
            //Message msg = new Message(message, newTimestamp);
            System.out.println("got here");

            for (String player : players) {
                ReceiverInterface target = (ReceiverInterface) Naming.lookup("rmi://localhost/ReceiverInterface/" + player);
                target.receiveMessage(message, newTimestamp);
                System.out.println("[" + processName + "] Sent: " + message + " to " + player);
            }
        } catch (Exception e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter process name: ");
            String processName = scanner.nextLine().trim(); // Get user-defined name

            // Create the ReceiverImpl instance with the chosen name
            ReceiverImpl server = new ReceiverImpl(processName);
            
            // Bind it with a unique RMI name
            Naming.rebind("rmi://localhost/ReceiverInterface/" + processName, server);
            System.out.println("Receiver Interface is running...");

        } catch (Exception e) {
            System.err.println("Error starting Receiver Interface");
            e.printStackTrace();
        }
    }
}
