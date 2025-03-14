/**
 * The Scoreboard interface defines the remote methods that allow interaction with the ScoreboardImpl
*/

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.io.*;
import java.util.*;


public interface Scoreboard extends Remote {
	public boolean updateScoreboard(String name) throws RemoteException;
	public String getScoreboard(int count) throws RemoteException;
	public void initScorboard() throws RemoteException;
}