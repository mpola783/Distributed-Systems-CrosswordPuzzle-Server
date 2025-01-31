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
            System.out.println("UDP WordRepo is running on port " + PORT);

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
                response = "INVALID * 0";
        }
    
        return response;
    }


    // word checker, takes "check <word>" as input and returns whether it exists or not.
    private static String LookupWord(String input){
        String[] tokens = input.split(" ");

        if (tokens.length != 2 || !tokens[0].equals("LOOKUP")) {
            System.out.println("LOOKUP 0 invalid request");
            return "LOOKUP * 0";
        }

        String wordToken = tokens[1];

        try (BufferedReader br = new BufferedReader(new FileReader("words.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().equalsIgnoreCase(wordToken)) {
                    System.out.println(wordToken + " found");
                    return "LOOKUP " + wordToken + " 1";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error occurred during file I/O in LookupWord");
            return "LOOKUP * 0";
        }
        System.out.println(wordToken + " notfound in words.txt");
        return "LOOKUP " + wordToken + " 0";

    }


    //removes word from 
    private static String RemoveWord(String input){
        String[] tokens = input.split(" ");
        Boolean exists = false;

        if (tokens.length != 2 || !tokens[0].equals("REMOVE")) {
            System.out.println(tokens[0] + " 0 invalid request in REMOVE");
            return "REMOVE * 0";
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
            System.out.println(wordToken + " Error occurred while removing the word during file I/O");
            return "REMOVE " + wordToken +  " 0";
        }



        if (exists) {
            System.out.println(wordToken + " deleted");
            return "REMOVE " + wordToken + " 1";
            
        }
        else {
            System.out.println(wordToken + " wasn't found in words.txt");
            return "REMOVE " + wordToken + " 0";
        }        
    }



    private static String AddWord(String input){
        String[] tokens = input.split(" ");

        if (tokens.length != 2 || !tokens[0].equals("ADD")) {
            return "Invalid request";
        }

        String wordToken = tokens[1];

        try (BufferedReader br = new BufferedReader(new FileReader("words.txt"))) {
            String line;

            // Iterate through each line in the "words.txt" file
             while ((line = br.readLine()) != null) {
                // Check if the trimmed line matches the word to be added (case-insensitive)
                if (line.trim().equalsIgnoreCase(wordToken)) {
                    System.out.println(wordToken + " Already exists, not added");
                    return "ADD " + wordToken + " 0";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(" Error occurred while checking the word during file I/O");
            return "ADD "+ wordToken +" 0";
        }

        // The word does not exist, so add it to the file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("words.txt", true))) {
            bw.write(wordToken);
            bw.newLine(); // Add a newline character after the word
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(wordToken + " Error occurred while adding the word during file I/O");
            return "ADD " + wordToken + " 0";
        }
        System.out.println(wordToken + " added");
        return "ADD " + wordToken + " 1";
    }



    private static String FetchWord(String input){
        String[] tokens = input.split(" ");
        

        // Check if the input message is in the expected format
        if (tokens.length != 3 || !tokens[0].equals("FETCH")) {
            System.out.println("Error, input expected: FETCH <command> <char/length>");
            return "FETCH * 0";
        }

        if(tokens[1].equals("l")){ //l=length
            int len = 0;
            try{
                len = Integer.parseInt(tokens[2]);
            }
            catch (NumberFormatException ex){
                ex.printStackTrace();
                return "FETCH * 0";
            }
            return FindLength(len);
        }
        else if(tokens[1].equals("m")){ //m=middle
            return Contains(tokens[2]);
        }
        else if(tokens[1].equals("f")){ //f=first
            return StartsWith(tokens[2]);
        }
        else if(tokens[1].equals("e")){ //e=ends
            return EndsWith(tokens[2]);
        }
        else{
            System.out.println("error in fetch word, inccorect argument structure expect <command> = l,m,f,e");
        }

        return "FETCH * 0";
    }

    private static String FindLength(int len){
        String foundWord = "";
        List<String> matchedWords = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("words.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() >= len) {
                    matchedWords.add(line.trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error occurred while reading words.txt");
            return "FETCH * 0";
        }

        if (matchedWords.isEmpty()) {
            System.out.println("No words found at desired length " + len + " or greater");
            return "FETCH * 0";
        }

        Random random = new Random();
        foundWord = matchedWords.get(random.nextInt(matchedWords.size()));
        
        
        return "FETCH " + foundWord + " 1";
    }
    private static String StartsWith(String prefix) {
        List<String> matchedWords = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("words.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().toLowerCase().startsWith(prefix.toLowerCase())) {
                    matchedWords.add(line.trim());
                }
            }
        } catch (Exception e) {
            System.out.println("Error occurred while reading words.txt");
            return "FETCH * 0";
        }
        if (matchedWords.isEmpty()) return "FETCH * 0";
        return "FETCH " + matchedWords.get(new Random().nextInt(matchedWords.size())) + " 1";
    }
    private static String EndsWith(String suffix) {
        List<String> matchedWords = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("words.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().toLowerCase().endsWith(suffix.toLowerCase())) {
                    matchedWords.add(line.trim());
                }
            }
        } catch (Exception e) {
            System.out.println("Error occurred while reading words.txt");
            return "FETCH * 0";
        }
        if (matchedWords.isEmpty()) return "FETCH * 0";
        return "FETCH " + matchedWords.get(new Random().nextInt(matchedWords.size())) + " 1";
    }
    private static String Contains(String letter) {
        List<String> matchedWords = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("words.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().toLowerCase().contains(letter.substring(0,0).toLowerCase())) {
                    matchedWords.add(line.trim());
                }
            }
        } catch (Exception e) {
            System.out.println("Error occurred while reading words.txt");
            return "FETCH * 0";
        }
        if (matchedWords.isEmpty()) return "FETCH * 0";
        return "FETCH " + matchedWords.get(new Random().nextInt(matchedWords.size())) + " 1";
    }
}