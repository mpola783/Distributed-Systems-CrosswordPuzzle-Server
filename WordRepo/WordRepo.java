package WordRepo;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.*;
import java.util.Random;

public class WordRepo {

     public static void main(String[] args){
        final int PORT = 666;

        try (DatagramSocket serverSocket = new DatagramSocket(PORT)){
            System.out.println("UDP Wordrepo is running on port " + PORT);

            while (true) {
                byte[] inData = new byte[1024];

                DatagramPacket inPKT = new DatagramPacket(inData, inData.length);
                serverSocket.receive(inPKT);

                Thread clientThread = new Thread(() -> serviceRequest(serverSocket, inPKT));
                clientThread.start();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
     }

     private static void serviceRequest(DatagramSocket serverSocket, DatagramPacket inPKT) {
        try {
            // Process client request
            String clientData = new String(inPKT.getData(), 0, inPKT.getLength());
            System.out.println("Received from client at " + inPKT.getAddress().getHostAddress() + ": " + clientData);
    
            // Parse the data message
            String[] parts = clientData.split("[,\\s]+"); 
    
            // Check if the parsed message is in the expected format
            if (parts.length < 1) {
                System.err.println("Invalid request format");
                return;
            }
    
            // Determine the command
            String command = parts[0];
    
            // Prepare response based on the command
            String response = requestRouter(command, clientData);
    
            // Send response back to the client
            byte[] sendData = response.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, inPKT.getAddress(), inPKT.getPort());
            serverSocket.send(sendPacket);
    
            System.out.println("Sent response to client at " + inPKT.getAddress().getHostAddress() + ":" + inPKT.getPort());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    private static String requestRouter(String command, String input) {
        String response = "";
    
        switch (command) {
            case "LOOKUP":
                response = LookupWord(input);
                break;
            case "REMOVE":
                response = RemoveWord(input);
                break;
            case "ADD":
                response = AddWord(input);
                break;
            case "FETCH":
                response = FetchWord(input);
                break;
            default:
                response = " 0 Invalid request";
        }
    
        return response;
    }


    // word checker, takes "check <word>" as input and returns whether it exists or not.
    private static String LookupWord(String input){
        String[] tokens = input.split(" ");

        if (tokens.length != 2 || !tokens[0].equals("LOOKUP")) {
            return " 0 Invalid request";
        }

        String wordToken = tokens[1];

        try (BufferedReader br = new BufferedReader(new FileReader("words.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().equalsIgnoreCase(wordToken)) {
                    return wordToken + " 1 exists";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return " 0 Error occurred during file I/O in LookupWord";
        }

        return wordToken + " 0 does not exist";

    }


    //removes word from 
    private static String RemoveWord(String input){
        String[] tokens = input.split(" ");
        Boolean exists = false;

        if (tokens.length != 2 || !tokens[0].equals("REMOVE")) {
            return " 0 Invalid request";
        }
        String wordToken = tokens[1];

        try (BufferedReader br = new BufferedReader(new FileReader("words.txt"))) {
            StringBuilder fileContent = new StringBuilder();
            String line;

            // Iterate through each line in the "words.txt" file
            while ((line = br.readLine()) != null) {
                // Check if the trimmed line matches the word to be deleted (case-insensitive)
                if (line.trim().equalsIgnoreCase(wordToken)) {
                    exists = true;
                } else {
                    fileContent.append(line); // Keep the existing line
                }
                fileContent.append(System.lineSeparator()); // Add newline character
            }

            // Update the file with the modified content
            try (BufferedWriter bw = new BufferedWriter(new FileWriter("words.txt"))) {
                bw.write(fileContent.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return " 0 Error occurred while removing the word during file I/O";
        }



        if (exists) {
            return wordToken + " 1 deleted";
        }
        else {
            return wordToken + " 0 wasn't found in words.txt";
        }        
    }



    private static String AddWord(String input){
        String[] tokens = input.split(" ");

        if (tokens.length != 2 || !tokens[0].equals("ADD")) {
            return "Invalid request";
        }

        String wordToAdd = tokens[1];

        try (BufferedReader br = new BufferedReader(new FileReader("words.txt"))) {
            String line;

            // Iterate through each line in the "words.txt" file
             while ((line = br.readLine()) != null) {
                // Check if the trimmed line matches the word to be added (case-insensitive)
                if (line.trim().equalsIgnoreCase(wordToAdd)) {
                    return wordToAdd + " 0 already exists, not added";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return " 0 Error occurred while checking the word during file I/O";
        }

        // The word does not exist, so add it to the file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("words.txt", true))) {
            bw.write(wordToAdd);
            bw.newLine(); // Add a newline character after the word
        } catch (Exception e) {
            e.printStackTrace();
            return " 0 Error occurred while adding the word during file I/O";
        }

        return wordToAdd + " 1 added";
    }



    private static String FetchWord(String input){
        String[] parts = input.split("|");

        // Check if the input message is in the expected format
        if (parts.length != 7 || !parts[0].equals("FETCH")) {
            return " 0 Invalid request";
        }


        return " 0 stub";
    }

}