import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.*;
import java.util.*;
import java.io.*;
import game.Game;

public class ReceiverImpl extends UnicastRemoteObject implements ReceiverInterface {

    private String processName;
    private LamportClock clock;
    private PriorityQueue<GameMessage> gameQueue = new PriorityQueue<>();
    private Map<String, Integer> lastSeenTimestamps = new HashMap<>();

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

    @Override
    public boolean receiveGame(Game game, String senderID, int timestamp) throws RemoteException {
        clock.update(timestamp);
        lastSeenTimestamps.put(senderID, timestamp);

        gameQueue.add(new GameMessage(game, senderID, timestamp));
        System.out.println("[" + processName + "] Received game from " + senderID + " @ " + timestamp);

        deliverGameMessagesInOrder();
        return true;
    }

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

    @Override
    public void sendGame(String[] players, String senderID, Game game) throws RemoteException {
        int timestamp = clock.tick();

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