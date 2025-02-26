// Implementation of WordServer
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.io.*;
import java.util.*;

public class WordServerImpl extends UnicastRemoteObject implements WordServer {
    private static final String FILE_PATH = "words.txt";

    protected WordServerImpl() throws RemoteException {
        super();
    }

    @Override
    public boolean checkWord(String word) throws RemoteException {
        System.out.println("Checking word: " + word);
        try {
            List<String> words = readWords();
            if (words.contains(word)) {
                return true;
            } else {
                throw new RemoteException("Word not found");
            }
        } catch (IOException e) {
            throw new RemoteException("File I/O error", e);
        }
    }

    @Override
    public boolean removeWord(String word) throws RemoteException {
        System.out.println("Removing word: " + word);
        try {
            List<String> words = readWords();
            if (words.remove(word)) {
                writeWords(words);
                return true;
            } else {
                throw new RemoteException("Word not found for removal");
            }
        } catch (IOException e) {
            throw new RemoteException("File I/O error", e);
        }
    }

    @Override
    public boolean createWord(String word) throws RemoteException {
        System.out.println("Adding word: " + word);
        try {
            List<String> words = readWords();
            if (!words.contains(word)) {
                words.add(word);
                writeWords(words);
                return true;
            } else {
                throw new RemoteException("Word already exists");
            }
        } catch (IOException e) {
            throw new RemoteException("File I/O error", e);
        }
    }

    // Overloaded method: fetch a random word with a minimum length requirement.
    @Override
    public String getRandomWord(int length) throws RemoteException {
        System.out.println("Fetching random word with minimum length: " + length);
        try {
            List<String> words = readWords();
            List<String> matchedWords = new ArrayList<>();
            for (String word : words) {
                if (word.length() >= length) {
                    matchedWords.add(word);
                }
            }
            if (matchedWords.isEmpty()) {
                throw new RemoteException("No words found with minimum length: " + length);
            }
            String selectedWord = matchedWords.get(new Random().nextInt(matchedWords.size()));
            System.out.println("Fetched word: " + selectedWord);
            return selectedWord;
        } catch (IOException e) {
            throw new RemoteException("File I/O error", e);
        }
    }

    // Overloaded method: fetch a random word based on a command and a letter (or substring).
    @Override
    public String getRandomWord(String command, String letter) throws RemoteException {
        System.out.println("Fetching random word with command: " + command + " and letter: " + letter);
        try {
            List<String> words = readWords();
            List<String> matchedWords = new ArrayList<>();

            switch (command) {
                case "m":
                    for (String word : words) {
                        if (word.contains(letter)) {
                            matchedWords.add(word);
                        }
                    }
                    break;
                case "f":
                    for (String word : words) {
                        if (word.startsWith(letter)) {
                            matchedWords.add(word);
                        }
                    }
                    break;
                case "e":
                    for (String word : words) {
                        if (word.endsWith(letter)) {
                            matchedWords.add(word);
                        }
                    }
                    break;
                default:
                    throw new RemoteException("Unknown command type: " + command);
            }
            if (matchedWords.isEmpty()) {
                throw new RemoteException("No words matched command '" + command + "' with argument '" + letter + "'");
            }
            String selectedWord = matchedWords.get(new Random().nextInt(matchedWords.size()));
            System.out.println("Fetched word: " + selectedWord);
            return selectedWord;
        } catch (IOException e) {
            throw new RemoteException("File I/O error", e);
        }
    }

    // Reads words from the file and returns them as a list.
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

    // Writes the list of words back to the file.
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
