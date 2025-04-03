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
import java.util.*;   

/**
 * Implements the ReceiverInterface, handling message reception and processing.
 */
public class ReceiverImpl extends UnicastRemoteObject implements ReceiverInterface {
    private String processName;  // The name of this process
    private LamportClock clock;  // The Lamport clock instance
    private PriorityQueue<Message> messageQueue = new PriorityQueue<>();
    private Map<String, Integer> lastSeenTimestamps = new HashMap<>();

    public class Message implements Comparable<Message> {
        private final String content;
        private final String senderID;
        private final int timestamp;

        public Message(String content, String senderID, int timestamp) {
            this.content = content;
            this.senderID = senderID;
            this.timestamp = timestamp;
        }

        public String getContent() { return content; }
        public String getSenderID() { return senderID; }
        public int getTimestamp() { return timestamp; }

        @Override
        public int compareTo(Message other) {
            // First compare timestamps
            int timeCompare = Integer.compare(this.timestamp, other.timestamp);
            // Tie-breaker: compare sender IDs (assumes lexicographical order is consistent)
            return (timeCompare != 0) ? timeCompare : this.senderID.compareTo(other.senderID);
        }

        @Override
        public String toString() {
            return "[" + senderID + " @ " + timestamp + "] " + content;
        }
    } 

    public ReceiverImpl(String name) throws RemoteException {
        super();
        this.processName = name;
        this.clock = new LamportClock();
    }


    @Override
    public boolean receiveMessage(String guess, String senderID, int receivedTimestamp) throws RemoteException {
        int oldInternalTimestamp = clock.getTime();
        int newTimestamp = clock.update(receivedTimestamp);

        // Track the latest message timestamp received from sender
        lastSeenTimestamps.put(senderID, receivedTimestamp);

        // Add to the priority queue
        Message msg = new Message(guess, senderID, receivedTimestamp);
        messageQueue.add(msg);

        System.out.println("[" + processName + "] Queued: " + msg);

        deliverMessagesInOrder(); // Try to deliver if safe

        return true;
    }

    private void deliverMessagesInOrder() {
        while (!messageQueue.isEmpty()) {
            Message head = messageQueue.peek();  // Get the earliest message

            // Check if all senders have sent something newer than this
            boolean safeToDeliver = true;

            for (String sender : lastSeenTimestamps.keySet()) {
                if (sender.equals(head.getSenderID())) continue; // skip FIFO part; already guaranteed

                int lastSeen = lastSeenTimestamps.getOrDefault(sender, -1);
                if (lastSeen <= head.getTimestamp()) {
                    safeToDeliver = false;
                    break;
                }
            }

            if (safeToDeliver) {
                // All conditions satisfied, deliver
                System.out.println("[" + processName + "] Delivered: " + head);
                messageQueue.poll();  // Remove the head
            } else {
                // Not safe to deliver yet
                break;
            }
        }
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
    public void sendMessage(String[] players, String senderID, String guess) throws RemoteException {
        try {
            int newTimestamp = clock.tick();
            //Message msg = new Message(message, newTimestamp);

            for (String player : players) {
                if (!player.equals(senderID)) { // Skip the sender
                    ReceiverInterface target = (ReceiverInterface) Naming.lookup("rmi://localhost/ReceiverInterface/" + player);
                    target.receiveMessage(guess, senderID, newTimestamp);
                    System.out.println("[" + processName + "] Guessed: " + guess + " -> Sent to " + player);
                }
                else {
                    System.out.println("\nSkipping Sender\n");
                    System.out.println("Updated Timestamp: " + newTimestamp);
                }
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
