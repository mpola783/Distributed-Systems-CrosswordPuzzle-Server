import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.io.*;
import java.util.*;

// Remote Interface
public interface AccountManager extends Remote {
    String loginUser(String name, String password) throws RemoteException;
    String createUser(String name, String password) throws RemoteException;
    String getHistory(String name) throws RemoteException;
    String updateScore(String name, boolean isWin, boolean isMultiplayer) throws RemoteException;
}