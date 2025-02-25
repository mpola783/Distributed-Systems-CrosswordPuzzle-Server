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

    @Override
    public String toString() {
        return "Lobby [gameID=" + gameID + ", host=" + hostName 
                + ", players=" + currentPlayers + "/" + expectedPlayers 
                + ", level=" + gameLevel + "]";
    }
}
