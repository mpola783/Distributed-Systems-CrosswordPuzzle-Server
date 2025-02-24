import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.io.*;
import java.util.*;

// Remote Interface
public interface WordServer extends Remote {
    String lookupWord(String word) throws RemoteException;
    String removeWord(String word) throws RemoteException;
    String addWord(String word) throws RemoteException;
    String fetchWord(String query) throws RemoteException;
}

// Implementation of WordServer
class WordServerImpl extends UnicastRemoteObject implements WordServer {
    private static final String FILE_PATH = "words.txt";

    protected WordServerImpl() throws RemoteException {
        super();
    }

    @Override
    public String lookupWord(String word) throws RemoteException {
        System.out.println("Looking up word: " + word);
        return processWordRequest("LOOKUP", word);
    }

    @Override
    public String removeWord(String word) throws RemoteException {
        System.out.println("Removing word: " + word);
        return processWordRequest("REMOVE", word);
    }

    @Override
    public String addWord(String word) throws RemoteException {
        System.out.println("Adding word: " + word);
        return processWordRequest("ADD", word);
    }

    @Override
    public String fetchWord(String query) throws RemoteException {
        System.out.println("Fetching word with query: " + query);
        return processFetchRequest(query);
    }

    private synchronized String processWordRequest(String command, String word) throws RemoteException {
        try {
            List<String> words = readWords();
            switch (command) {
                case "LOOKUP":
                    return words.contains(word) ? word : "LOOKUP * 0";
                case "REMOVE":
                    if (words.remove(word)) {
                        writeWords(words);
                        System.out.println("Successfully removed word: " + word);
                        return word;
                    }
                    System.out.println("Word not found for removal: " + word);
                    throw new RemoteException("Word not found for removal");
                case "ADD":
                    if (!words.contains(word)) {
                        words.add(word);
                        writeWords(words);
                        System.out.println("Successfully added word: " + word);
                        return word;
                    }
                    System.out.println("Word already exists: " + word);
                    throw new RemoteException("Word already exists");
                default:
                    throw new RemoteException("Invalid command");
            }
        } catch (IOException e) {
            System.err.println("Error processing request: " + command + " " + word);
            e.printStackTrace();
            throw new RemoteException("File I/O error", e);
        }
    }

    private synchronized String processFetchRequest(String input) throws RemoteException {
        String[] tokens = input.split(" ");
        if (tokens.length != 3 || !tokens[0].equals("FETCH")) {
            System.out.println("Invalid fetch request format: " + input);
            throw new RemoteException("Invalid fetch request format");
        }
        try {
            List<String> words = readWords();
            List<String> matchedWords = new ArrayList<>();
            switch (tokens[1]) {
                case "l":
                    int len = Integer.parseInt(tokens[2]);
                    for (String word : words) {
                        if (word.length() >= len) matchedWords.add(word);
                    }
                    break;
                case "m":
                    for (String word : words) {
                        if (word.contains(tokens[2])) matchedWords.add(word);
                    }
                    break;
                case "f":
                    for (String word : words) {
                        if (word.startsWith(tokens[2])) matchedWords.add(word);
                    }
                    break;
                case "e":
                    for (String word : words) {
                        if (word.endsWith(tokens[2])) matchedWords.add(word);
                    }
                    break;
                default:
                    System.out.println("Unknown fetch query type: " + tokens[1]);
                    throw new RemoteException("Unknown fetch query type");
            }
            if (matchedWords.isEmpty()) {
                System.out.println("No words matched the query: " + input);
                throw new RemoteException("No words matched the query");
            }
            String selectedWord = matchedWords.get(new Random().nextInt(matchedWords.size()));
            System.out.println("Fetched word: " + selectedWord);
            return selectedWord;
        } catch (IOException e) {
            System.err.println("Error processing fetch request: " + input);
            e.printStackTrace();
            throw new RemoteException("File I/O error", e);
        }
    }

    private List<String> readWords() throws IOException {
        List<String> words = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                words.add(line.trim());
            }
        }
        System.out.println("Loaded words from file: " + words.size() + " words found.");
        return words;
    }

    private void writeWords(List<String> words) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (String word : words) {
                bw.write(word);
                bw.newLine();
            }
        }
        System.out.println("Updated words.txt with " + words.size() + " words.");
    }

    public static void main(String[] args) {
        try {
            WordServerImpl server = new WordServerImpl();
            Naming.rebind("WordServer", server);
            System.out.println("WordServer is running...");
        } catch (Exception e) {
            System.err.println("Error starting WordServer");
            e.printStackTrace();
        }
    }
}
