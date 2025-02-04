import java.io.*;
import java.net.*;
import java.util.*;

public class AccountManager {
    private static final int PORT = 6969;
    private static final String FILE_NAME = "accounts.txt";

    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(PORT)) {
            System.out.println("UDP Account Manager Service running on port " + PORT);

            while (true) {
                byte[] inData = new byte[1024];
                DatagramPacket inPacket = new DatagramPacket(inData, inData.length);
                serverSocket.receive(inPacket);

                Thread clientThread = new Thread(() -> serviceRequest(serverSocket, inPacket));
                clientThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void serviceRequest(DatagramSocket serverSocket, DatagramPacket inPacket) {
        try {
            String request = new String(inPacket.getData(), 0, inPacket.getLength()).trim();
            System.out.println("Received from client: " + request);
            
            String response = handleRequest(request);
            byte[] sendData = response.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, inPacket.getAddress(), inPacket.getPort());
            serverSocket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String handleRequest(String request) {
        String[] tokens = request.split(" ");
        System.out.println("Token count: " + tokens.length);
        for (int i = 0; i < tokens.length; i++) {
            System.out.println("Token " + i + ": '" + tokens[i] + "'");}
        if (tokens.length < 2) return "ERROR Invalid Request";

        String command = tokens[0].toUpperCase();
        String name = tokens[1];
        String password = tokens.length > 2 ? tokens[2] : "";

        switch (command) {
            case "LOGIN":
                return loginUser(name, password);
            case "NEW":
                return createUser(name, password);
            case "HISTORY":
                return getHistory(name);
            case "WIN":
                return updateScore(name, true);
            case "LOSE":
                return updateScore(name, false);
            default:
                return "ERROR Unknown Command";
        }   
    }

    private static String loginUser(String name, String password) {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 4 && data[0].equals(name) && data[1].equals(password)) {
                    return "LOGIN " + name + " 1";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "LOGIN " + name + " 0";
    }

    private static String createUser(String name, String password) {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(name + ",")) {
                    return "NEW " + name + " 0";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
            bw.write(name + "," + password + ",0,0\n");
        } catch (IOException e) {
            e.printStackTrace();
            return "NEW " + name + " 0";
        }
        return "NEW " + name + " 1";
    }

    private static String getHistory(String name) {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 4 && data[0].equals(name)) {
                    return "HISTORY " + name + " " + data[2] + "/" + data[3] + " 1";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "HISTORY " + name + " */* 0";
    }

    private static String updateScore(String name, boolean isWin) {
        List<String> lines = new ArrayList<>();
        boolean updated = false;

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 4 && data[0].equals(name)) {
                    int wins = Integer.parseInt(data[2]);
                    int losses = Integer.parseInt(data[3]);
                    if (isWin) wins++;
                    else losses++;
                    lines.add(name + "," + data[1] + "," + wins + "," + losses);
                    updated = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return (isWin ? "WIN " : "LOSE ") + name + " 0";
        }

        if (!updated) return (isWin ? "WIN " : "LOSE ") + name + " 0";

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (String line : lines) {
                bw.write(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return (isWin ? "WIN " : "LOSE ") + name + " 0";
        }
        return (isWin ? "WIN " : "LOSE ") + name + " 1";
    }
}
