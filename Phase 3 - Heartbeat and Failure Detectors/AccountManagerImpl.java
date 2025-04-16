
/*
 * AccountManagerImpl
 * 
 * This class implements the AccountManager interface and provides remote 
 * functionalities for managing user accounts. 
 * */ 

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.io.*;
import java.util.*;

// Implementation of AccountManager
public class AccountManagerImpl extends UnicastRemoteObject implements AccountManager {
    private static final String FILE_NAME = "accounts.txt";

    protected AccountManagerImpl() throws RemoteException {
        super();
    }

    @Override
    public String loginUser(String name, String password) throws RemoteException {
        System.out.println("Logging in user: " + name);
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 6 && data[0].equals(name) && data[1].equals(password)) {
                    System.out.println("Login successful for user: " + name);
                    return "LOGIN SUCCESS";
                }
            }
        } catch (IOException e) {
            System.err.println("File I/O error while logging in user: " + name);
            throw new RemoteException("File I/O error", e);
        }
        System.out.println("Login failed for user: " + name);
        throw new RemoteException("Invalid login credentials.");
    }

    @Override
    public String createUser(String name, String password) throws RemoteException {
        System.out.println("Creating user: " + name);
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(name + ",")) {
                    System.out.println("User already exists: " + name);
                    br.close();
                    throw new RemoteException();
                }
            }    
            br.close();
    	} catch (RemoteException e) {
            throw new RemoteException("User already exists.");        
    	} catch (IOException e) {
            System.err.println("File I/O error while creating user: " + name);
            throw new RemoteException("File I/O error ", e);
        }
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
            bw.write(name + "," + password + ",0,0,0,0\n");
            System.out.println("User created successfully: " + name);
            bw.close();
        } catch (IOException e) {
            System.err.println("Failed to create user: " + name);
            throw new RemoteException("Failed to create user ", e);
        }
        return "User created successfully!";
    }

    @Override
    public String getHistory(String name) throws RemoteException {
        System.out.println("Fetching history for user: " + name);
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 6 && data[0].equals(name)) {
                    System.out.println("History found for user: " + name);
                    return "History: Multiplayer - " + data[2] + " wins, " + data[3] + " losses | Singleplayer - " + data[4] + " wins, " + data[5] + " losses";
                }
            }
        } catch (IOException e) {
            System.err.println("File I/O error while fetching history for user: " + name);
            throw new RemoteException("File I/O error", e);
        }
        System.out.println("No history found for user: " + name);
        throw new RemoteException("User not found");
    }

    @Override
    public String updateScore(String name, boolean isWin, boolean isMultiplayer) throws RemoteException {
        System.out.println("Updating score for user: " + name + " | Win: " + isWin + " | Multiplayer: " + isMultiplayer);
        List<String> lines = new ArrayList<>();
        boolean updated = false;

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 6 && data[0].equals(name)) {
                    int mpWins = Integer.parseInt(data[2]);
                    int mpLosses = Integer.parseInt(data[3]);
                    int spWins = Integer.parseInt(data[4]);
                    int spLosses = Integer.parseInt(data[5]);
                    if (isMultiplayer) {
                        if (isWin) mpWins++;
                        else mpLosses++;
                    } else {
                        if (isWin) spWins++;
                        else spLosses++;
                    }
                    lines.add(name + "," + data[1] + "," + mpWins + "," + mpLosses + "," + spWins + "," + spLosses);
                    updated = true;
                    System.out.println("Updated score for user: " + name);
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("File I/O error while updating score for user: " + name);
            throw new RemoteException("File I/O error", e);
        }

        if (!updated) {
            System.out.println("User not found while updating score: " + name);
            throw new RemoteException("User not found");
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (String line : lines) {
                bw.write(line + "\n");
            }
            System.out.println("Score update successful for user: " + name);
        } catch (IOException e) {
            System.err.println("Failed to update score for user: " + name);
            throw new RemoteException("Failed to update score", e);
        }
        return "Score updated successfully";
    }
    
    public String getScore(String name) throws RemoteException{
        System.out.println("Fetching history for user: " + name);
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 6 && data[0].equals(name)) {
                    System.out.println("History found for user: " + name);
                    return data[2] +","+ data[3];
                }
            }
        } catch (IOException e) {
            System.err.println("File I/O error while getting score for user: " + name);
            throw new RemoteException("File I/O error", e);
        }
        System.out.println("No history found for user: " + name);
        throw new RemoteException("User not found");
    }

    public static void main(String[] args) {
        try {
            AccountManagerImpl server = new AccountManagerImpl();
            Naming.rebind("AccountManager", server);
            System.out.println("Account Manager is running...");
        } catch (Exception e) {
            System.err.println("Error starting Account Manager");
            e.printStackTrace();
        }
    }
}