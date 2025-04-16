import java.util.concurrent.TimeUnit;

public class ClientHeartbeat implements Runnable{
	private static final int TIMELIMIT_SECONDS = 1;
	private CrissCrossPuzzleServer server;
	private String name;
	private String gameID;
	private boolean active;
	
	public ClientHeartbeat(CrissCrossPuzzleServer server, String name, String gameID) {
		this.server = server;
		this.name = name;
		this.gameID = gameID;
		this.active = true;
	}
	
	public void run () {
		System.out.println("\nEstablishing Connection.");
		while(active) {
			try {
				TimeUnit.SECONDS.sleep(TIMELIMIT_SECONDS);
				this.active = server.heartbeat(name, gameID);
			} catch (Exception e) {
				System.err.println("\nConnection Failure.");
				e.printStackTrace();
				break;
			}
		}
		System.out.println("\nConnection Closed.");
	}
}