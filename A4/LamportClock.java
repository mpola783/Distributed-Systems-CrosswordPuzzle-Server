
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
 