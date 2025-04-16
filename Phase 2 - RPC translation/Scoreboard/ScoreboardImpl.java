import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.io.*;
import java.util.*;

public class ScoreboardImpl extends UnicastRemoteObject implements Scoreboard {

	private static final String FILE_NAME = "scoreboard.txt";

	private List<Rank> topPlayers = new ArrayList<Rank>();

	private AccountManager accountManager;

	protected ScoreboardImpl() throws RemoteException {
		super();
	}

	public class Rank {
		private String name;
		private int rating;

		public Rank(String name, int rating) {
			this.name = name;
			this.rating = rating;
		}
	}

	public class compareRank implements Comparator<Rank> {
		public int compare(Rank a, Rank b) {
			return a.rating - b.rating;
		}
	}

	public boolean updateScoreboard(String name) throws RemoteException {
		try {
			String[] score = accountManager.getScore(name).split(",");
			int rating = (Integer.parseInt(score[0]) / (Integer.parseInt(score[0]) + Integer.parseInt(score[1])))
					* 1000;
			Rank rank = new Rank(name, rating);
			boolean found = false;
			try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
				String line;
				int ranking = 0;
				while ((line = br.readLine()) != null || found) {
					if (line.startsWith(name + ",")) {
						topPlayers.remove(ranking);
						topPlayers.add(rank);
						found = true;
					}
				}
			} catch (IOException e) {
				System.err.println("File I/O error while searching ranking.");
				throw new RemoteException("File I/O error", e);
			}
			if (!found) {
				topPlayers.add(rank);
				
			}
			Collections.sort(topPlayers, new compareRank());
			

			try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
				File file = new File(FILE_NAME);
				if(file.delete()) {
					file.createNewFile();
				}
				for (Rank r : topPlayers) {
					bw.write(r.name + "," + r.rating + '\n');
				}
				System.out.println("Ranks updated successfully");
			} catch (IOException e) {
				System.err.println("Failed to update ranks.");
				throw new RemoteException("Failed to update ranks.", e);
			}
			return true;
		} catch (RemoteException e) {
			System.err.println("Error: " + e.getMessage());
		}

		return false;
	}

	public String getScoreboard(int count) throws RemoteException {
		String scoreboard = "";
		try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
			String line;
			while ((line = br.readLine()) != null) {
				scoreboard.concat(line + '+');
			}
		} catch (IOException e) {
			System.err.println("File I/O error when initializing scoreboard");
			throw new RemoteException("File I/O error", e);
		}
		return "";
	}

	public void initScorboard() throws RemoteException {

		try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
			String line;
			File file = new File(FILE_NAME);
			file.createNewFile();
			while ((line = br.readLine()) != null) {
				topPlayers.add(new Rank(line.split(",")[0], Integer.parseInt(line.split(",")[1])));
			}
			Collections.sort(topPlayers, new compareRank());
		} catch (IOException e) {
			System.err.println("File I/O error when initializing scoreboard");
			throw new RemoteException("File I/O error", e);
		}
	}

	public static void main(String[] args) {
		try {
			ScoreboardImpl server = new ScoreboardImpl();
			Naming.rebind("Scoreboard", server);
			System.out.println("Scoreboard is running...");

		} catch (Exception e) {
			System.err.println("Error starting Scoreboard");
			e.printStackTrace();
		}
	}
}