/**
 * The ReceiverImpl class implements the ReceiverInterface and provides an RMI-based receiver that
 *  processes the game messages from other client processes to achieve FIFO - total order Synchronization
 * 
 * Key responsibilities of this class:
 * 1. **Receive Game Messages**: Receives and processes game updates sent by other client processes.
 * 2. **Deliver Game Messages in Order**: Ensures that game messages are delivered in the correct order based on Lamport logical timestamps.
 * 3. **Clock Synchronization**: Uses a Lamport clock to synchronize messages from different clients.
 * 4. **Gossip Protocol**: Sends the received game message to all other players in the game (via RMI lookup).

 * Dependencies:
 * - `LamportClock`: A logical clock for timestamp synchronization between distributed processes.
 * - `GameMessage`: A class that encapsulates the game object, sender ID, and timestamp of each received game update.
 * - `GameUpdateHandler`: An interface for applying received game updates to the game state. Found in Client
 * 
 * Methods:
 * 1. `receiveGame(Game game, String senderID, int timestamp)`: Handles the reception of a game message, updates the Lamport clock,
 *    stores the message in the priority queue, and attempts to deliver all messages in the correct order.
 * 2. `sendGame(String[] players, String senderID, Game game)`: Sends the game message to other players, using RMI to deliver the message.
 * 3. `doEvent(String event, char[][] grid)`: Handles game events and updates the timestamp.
 * 4. `main(String[] args)`: Entry point that starts the receiver server and binds it to the RMI registry.
 */


import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.*;
import java.util.*;
import java.io.*;
import game.Game;

public class ReceiverImpl extends UnicastRemoteObject implements ReceiverInterface {

    private String processName;
    private LamportClock clock;
    private PriorityQueue<GameMessage> gameQueue = new PriorityQueue<>(); //Keeps track of meesages in queue
    private Map<String, Integer> lastSeenTimestamps = new HashMap<>(); // Keeps track of latest timestamp of each client nodes

    // Interface for applying received game updates
    public interface GameUpdateHandler extends Serializable {
        void applyGameUpdate(Game game);
    }

    private static GameUpdateHandler gameHandler = null;

    public static void registerGameHandler(GameUpdateHandler handler) {
        gameHandler = handler;
    }

    public ReceiverImpl(String name) throws RemoteException {
        super();
        this.processName = name;
        this.clock = new LamportClock();
    }

    // GameMessage used in the priority queue
    public static class GameMessage implements Comparable<GameMessage>, Serializable {
        private final Game game;
        private final String senderID;
        private final int timestamp;

        public GameMessage(Game game, String senderID, int timestamp) {
            this.game = game;
            this.senderID = senderID;
            this.timestamp = timestamp;
        }

        public Game getGame() { return game; }
        public String getSenderID() { return senderID; }
        public int getTimestamp() { return timestamp; }

        @Override
        public int compareTo(GameMessage other) {
            int timeCompare = Integer.compare(this.timestamp, other.timestamp);
            return (timeCompare != 0) ? timeCompare : this.senderID.compareTo(other.senderID);
        }

        @Override
        public String toString() {
            return "[" + senderID + " @ " + timestamp + "] Game";
        }
    }

    /**
        * Handles the receival of a Game object from another client
        * 
        * Performs the following steps:
        * 1. Updates the local Lamport clock using the received timestamp.
        * 2. Records the latest timestamp received from the sender.
        * 3. Adds the incoming game message to the local priority queue.
        * 4. Attempts to deliver all safe-to-process messages in order by calling deliverGameMessagesInOrder().
        * 
        * @param game      The Game object received from the sender.
        * @param senderID  The ID of the sender process.
        * @param timestamp The Lamport timestamp associated with the message.
        * @return true     Always returns true after processing the received message.
        * @throws RemoteException If an RMI communication error occurs.
    */

    @Override
    public boolean receiveGame(Game game, String senderID, int timestamp) throws RemoteException {
        clock.update(timestamp);                            //Updates to the sender timestamp + 1;
        lastSeenTimestamps.put(senderID, timestamp);        // Keeps track of latest timestamp of each client nodes

        gameQueue.add(new GameMessage(game, senderID, timestamp));  //Adds message to the back of the queue
        System.out.println("[" + processName + "] Received game from " + senderID + " @ " + timestamp);

        deliverGameMessagesInOrder();
        return true;
    }


    /**
        * Processes and delivers game messages from the gameQueue in order, ensuring that messages are delivered 
        * only when it is safe to do so. Messages are considered safe to deliver if all messages from other senders 
        * have a higher timestamp than the current message. The function follows the Lamport Clock logic to ensure 
        * proper message delivery order and synchronization across processes.
        * 
        * The function operates as follows:
        * 1. It checks the first (head) message in the queue.
        * 2. It verifies that all messages from other senders have a higher timestamp than the current message.
        * 3. If it is safe to deliver (i.e., all conditions are satisfied), the message is delivered:
        *    - The game update from the message is applied using the registered `gameHandler`.
        *    - The message is removed from the queue after successful delivery.
        * 4. If it is not safe to deliver, the function will exit and wait for further messages.
    */
    private void deliverGameMessagesInOrder() {
        while (!gameQueue.isEmpty()) {
            GameMessage head = gameQueue.peek();

            boolean safeToDeliver = true;
            for (String sender : lastSeenTimestamps.keySet()) {
                if (sender.equals(head.getSenderID())) continue;

                int lastSeen = lastSeenTimestamps.getOrDefault(sender, -1);
                if (lastSeen <= head.getTimestamp()) {
                    safeToDeliver = false;
                    break;
                }
            }

            if (safeToDeliver) {
                System.out.println("[" + processName + "] DELIVERED Game from " + head.getSenderID() + " @ " + head.getTimestamp());

                Game receivedGame = head.getGame();

                if (gameHandler != null) {
                    gameHandler.applyGameUpdate(receivedGame);
                } else {
                    System.out.println("No game handler registered to apply game update.");
                }

                gameQueue.poll(); // remove from queue
            } else {
                break; // not ready yet
            }
        }
    }

    /**
     * This function: 
     *      First increments the sender timestamp, 
     *      Then gossips the sender message to all other players in game using RMI lookup
     */

    @Override
    public void sendGame(String[] players, String senderID, Game game) throws RemoteException {
        int timestamp = clock.tick();  //Updates current timestamp for sender

        //Initializes player timestamps to -1 if no messages received
        for (String player : players) {
            lastSeenTimestamps.putIfAbsent(player, -1);
        }

        for (String player : players) {
            try {
                ReceiverInterface target = (ReceiverInterface) Naming.lookup("rmi://localhost/ReceiverInterface/" + player);
                target.receiveGame(game, senderID, timestamp);
                System.out.println("[" + processName + "] Sent Game to " + player + " @ " + timestamp);
            } catch (Exception e) {
                System.err.println("Failed to send Game to " + player + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void doEvent(String event, char[][] grid) throws RemoteException {
        int newTimestamp = clock.tick();
        System.out.println("[" + processName + "] Event: " + event + " | Timestamp: " + newTimestamp);
    }

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter process name: ");
            String processName = scanner.nextLine().trim();

            ReceiverImpl server = new ReceiverImpl(processName);
            Naming.rebind("rmi://localhost/ReceiverInterface/" + processName, server);
            System.out.println("Receiver Interface is running as '" + processName + "'.");

        } catch (Exception e) {
            System.err.println("Error starting Receiver Interface");
            e.printStackTrace();
        }
    }
}