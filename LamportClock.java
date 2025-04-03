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

 import java.io.Serializable;
 import java.util.concurrent.atomic.AtomicInteger;
 
 /**
  * LamportClock.java
  * Implements a Lamport Clock for logical time tracking.
  */
 public class LamportClock implements Serializable {
     private AtomicInteger timestamp; // Thread-safe integer for timestamp
 
     /**
      * Initializes the Lamport clock with time 0.
      */
     public LamportClock() {
         this.timestamp = new AtomicInteger(0);
     }
 
     /**
      * Increments the Lamport clock (for local events).
      * @return The new timestamp.
      */
     public synchronized int tick() {
         return timestamp.incrementAndGet();
     }
 
     /**
      * Updates the clock based on a received timestamp.
      * @param receivedTimestamp The timestamp received in a message.
      * @return The updated timestamp after applying the max rule.
      */
     public synchronized int update(int receivedTimestamp) {
         timestamp.set(Math.max(timestamp.get(), receivedTimestamp) + 1);
         return timestamp.get();
     }
 
     /**
      * Gets the current Lamport timestamp.
      * @return The current logical clock value.
      */
     public synchronized int getTime() {
         return timestamp.get();
     }
 }
 