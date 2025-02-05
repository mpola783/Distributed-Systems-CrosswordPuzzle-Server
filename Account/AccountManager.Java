import java.io.*;
import java.net.*;
import java.util.*;

public class AccountManager {
    private static final int PORT = 6969; // Port for the UDP server
    private static final String FILE_NAME = "accounts.txt"; // File storing account information

    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(PORT)) {
            System.out.println("UDP Account Manager Service running on port " + PORT);

            // Loop to handle incoming requests
            while (true) {
                byte[] inData = new byte[1024];
                DatagramPacket inPacket = new DatagramPacket(inData, inData.length);
                serverSocket.receive(inPacket); // Receive packet from client

                // Create a new thread to process the request
                Thread clientThread = new Thread(() -> serviceRequest(serverSocket, inPacket));
                clientThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void serviceRequest(DatagramSocket serverSocket, DatagramPacket inPacket) {
        try {
            // Extract the request data from the received packet
            String request = new String(inPacket.getData(), 0, inPacket.getLength()).trim();
            System.out.println("Received from client: " + request);
            
            // Handle the request and prepare a response
            String response = handleRequest(request);
            byte[] sendData = response.getBytes();
            
            // Send the response back to the client
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, inPacket.getAddress(), inPacket.getPort());
            serverSocket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String handleRequest(String request) {
        // Tokenize the incoming request
        String[] tokens = request.split(" ");
        System.out.println("Token count: " + tokens.length);
        for (int i = 0; i < tokens.length; i++) {
            System.out.println("Token " + i + ": '" + tokens[i] + "'");
        }
        
        // Ensure request has at least a command and username
        if (tokens.length < 2) return "ERROR Invalid Request";

        String command = tokens[0].toUpperCase(); // Convert command to uppercase
        String name = tokens[1]; // Extract username
        String password = tokens.length > 2 ? tokens[2] : ""; // Extract password if available

        // Handle different commands
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
                    return "LOGIN " + name + " 1"; // Login successful
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "LOGIN " + name + " 0"; // Login failed
    }

    private static String createUser(String name, String password) {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(name + ",")) {
                    return "NEW " + name + " 0"; // User already exists
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create new user entry
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
            bw.write(name + "," + password + ",0,0\n"); // Initialize wins and losses to 0
        } catch (IOException e) {
            e.printStackTrace();
            return "NEW " + name + " 0"; // User creation failed
        }
        return "NEW " + name + " 1"; // User created successfully
    }

    private static String getHistory(String name) {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 4 && data[0].equals(name)) {
                    return "HISTORY " + name + " " + data[2] + "/" + data[3] + " 1"; // Return user's win/loss record
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "HISTORY " + name + " */* 0"; // No history found
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

        if (!updated) return (isWin ? "WIN " : "LOSE ") + name + " 0"; // User not found

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (String line : lines) {
                bw.write(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return (isWin ? "WIN " : "LOSE ") + name + " 0"; // Failed to update file
        }
        return (isWin ? "WIN " : "LOSE ") + name + " 1"; // Score update successful
    }
}
