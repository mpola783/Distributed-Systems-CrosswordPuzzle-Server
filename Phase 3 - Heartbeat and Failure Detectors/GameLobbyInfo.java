/*
 * GameLobbyInfo
 * 
 * This class represents the state of a game lobby in a multiplayer game system. It holds information 
 * about the game lobby such as the game ID, host name, number of current players, expected players, 
 * and the game level.
 * */

import java.io.Serializable;

public class GameLobbyInfo implements Serializable {
    private String gameID;
    private String hostName;
    private int currentPlayers;
    private int expectedPlayers;
    private int gameLevel; // optionally include level or other config

    public GameLobbyInfo(String gameID, String hostName, int expectedPlayers, int gameLevel) {
        this.gameID = gameID;
        this.hostName = hostName;
        this.expectedPlayers = expectedPlayers;
        this.currentPlayers = 1; // lobby creator is the first player
        this.gameLevel = gameLevel;
    }

    public String getGameID() {
        return gameID;
    }

    public String getHostName() {
        return hostName;
    }
    

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public int getExpectedPlayers() {
        return expectedPlayers;
    }

    public int getGameLevel() {
        return gameLevel;
    }

    public void incrementPlayers() {
        this.currentPlayers++;
    }

    public void decrementPlayers() {
        this.currentPlayers--;
    }

    @Override
    public String toString() {
        return "Lobby [gameID=" + gameID + ", host=" + hostName 
                + ", players=" + currentPlayers + "/" + expectedPlayers 
                + ", level=" + gameLevel + "]";
    }
}