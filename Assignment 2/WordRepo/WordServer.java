import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.io.*;
import java.util.*;

// Remote Interface
public interface WordServer extends Remote {
    boolean checkWord(String word) throws RemoteException;
    boolean removeWord(String word) throws RemoteException;
    boolean createWord(String word) throws RemoteException;
    
    // Overloaded getRandomWord methods:
    // 1. Filter words by minimum length.
    // adjusted for asg-specs
    String getRandomVertWord(int length) throws RemoteException;
    
    // 2. Filter words using a command and letter (or substring).
    // m = contains
    // f = starts with
    // e = ends with
    String getRandomWord(String command, String letter) throws RemoteException;
}