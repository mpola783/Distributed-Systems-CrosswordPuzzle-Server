import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collections; // For shuffling the list
import java.util.Arrays;

import java.io.*;


public class CrosswordGameServer {
	private static final String USAGE = "Usage: java CrosswordGameServer [port]";
	

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println(USAGE);
			System.exit(1);
		}

		int port = 420;
		ServerSocket server = null;

		try {
			port = Integer.parseInt(args[0]);
			server = new ServerSocket(port);
			System.out.println("The Game Server is running...");
			
			
			ExecutorService fixedThreadPool = Executors.newFixedThreadPool(20);
			while (true) {
				fixedThreadPool.execute(new CrosswordInterfaceHandler(server.accept()));
			}
			
		} catch (IOException e) {
			System.out.println(
					"Exception caught when trying to listen on port " + port + " or listening for a connection");
			System.out.println(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Grid Dimensions
    public static class GridDimensions {
        int maxLength;
        int verticalX;

        public GridDimensions(int maxLength, int verticalX) {
            this.maxLength = maxLength;
            this.verticalX = verticalX;
        }
    }

	public static String removeWhitespace(String input) {
        if (input == null) {
            return null;
        }
        // Replace all whitespace characters
        return input.replaceAll("\\s+", " ");
    }


	// Method to send a request to the word server on port 666
	private static String sendToWordServer(String query) {
    	String wordServerResponse = null;
		
    	try (Socket wordServerSocket = new Socket("localhost", 666);
        	 PrintWriter out = new PrintWriter(wordServerSocket.getOutputStream(), true);
        	 BufferedReader in = new BufferedReader(new InputStreamReader(wordServerSocket.getInputStream()))) {

            out.println(query);

            // Read the response from the game server
            wordServerResponse = in.readLine();

        } catch (IOException e) {
            System.out.println("Error communicating with the game server: " + e.getMessage());
        }

        return wordServerResponse;
    }


	//Chooses random indices of given word
	public static int[] getRandomIndexes(String input, int n) {
        // Create a list to store all possible indexes
        ArrayList<Integer> indices = new ArrayList<>();
        
        // Add indexes from 0 to the length of the string - 1
        for (int i = 0; i < input.length(); i++) {
            indices.add(i);
        }
        
        // Shuffle the list to randomize the order
        Collections.shuffle(indices);
        
        // Create an array to store the first n random indices
        int[] randomIndexes = new int[n];
        
        // Copy the first n random indexes into the array
        for (int i = 0; i < n; i++) {
            randomIndexes[i] = indices.get(i);
        }
        
        // Sort the array in ascending order
        Arrays.sort(randomIndexes);
        
        return randomIndexes;
    }

	public static GridDimensions getGridX(String[] horiz_words, int[] horiz_cross_index) {
        int max_length = 0;
		int max_left = 0;
		int max_right = 0;

        for (int i = 0; i < horiz_words.length; i++) {
            // Calculate the length of the left side of the vertical cross
            int left_length = horiz_cross_index[i];
			max_left = Math.max(max_left, left_length);
			//System.out.println("Left of cross" + left_length + " for " + horiz_words[i] + "\n");

			// Calculate the length of the right side of the vertical cross
            int right_length = Math.abs(horiz_words[i].length() - (horiz_cross_index[i] + 1));
			max_right = Math.max(max_right, right_length);
			//System.out.println("Right of cross" + right_length + " for " + horiz_words[i] + "\n");
        }

		//System.out.println("TOTAL Left of cross" + max_left + "\n");
		//System.out.println("TOTAL Right of cross" + max_right + "\n");
		max_length = max_left + max_right + 1;

		int vertical_x = max_left;
		//System.out.println("Total " + max_length + "\n");
		
        return new GridDimensions(max_length, vertical_x);
    }

	// Function to print the grid
    public static void printGrid(String vert_word, GridDimensions grid_x, String[] horiz_words, int[] vert_cross_TEST, int[] horiz_cross_index) {
        int grid_y = vert_word.length();
        int grid_x_max = grid_x.maxLength;

        // Iterate over rows
        for (int y = 0; y < grid_y; y++) {
            StringBuilder row = new StringBuilder();

            // Iterate over columns
            for (int x = 0; x <= grid_x_max; x++) {
                char cell = '.';

                // Check if it's part of the vertical word
                if (x == grid_x.verticalX && y < vert_word.length()) {
                    cell = vert_word.charAt(y);
                }

                // Check if it's part of a horizontal word
                for (int i = 0; i < horiz_words.length; i++) {
                    if (y == vert_cross_TEST[i] && x >= horiz_cross_index[i] && x < horiz_cross_index[i] + horiz_words[i].length()) {
                        cell = horiz_words[i].charAt(x - horiz_cross_index[i]);
                    }
                }

                row.append(cell);
            }

            row.append('+'); // End each line with '+'
            System.out.println(row);
        }
    }


	public static void newGame(int words, int faults) {
		
		int horiz_count = words - 1;
		String letters = String.valueOf(words - 1);

		String horiz_words[] = new String[horiz_count]; //will store string and index value of char
		
		int horiz_cross_index[] = new int[horiz_count];


		//Fetch random vertical word with more letters than horizontal words in crossword
		//Ex. String query = "FETCH " + letters;
		
		//SIRNIPPLEZ add call to wordServer for random word with variable letters length
		//This will replace the example
		//String vert_word = "example";
		
		//String vertResponse = sendToWordServer("FETCH l " + words); //for when/if we add error handling
		
		String vert_word = sendToWordServer("FETCH l " + words).split(" ")[1];

		//Randomly selected indices
		int vert_cross_index[] = getRandomIndexes(vert_word, horiz_count);
		
		if(words > 10) {
			System.out.print("Words chosen is too high, limit is 10 words for this game");
		}


		for (int i = 0; i < vert_cross_index.length; i++) {
			//This will call word server to return a random word that contains the char
			//SIRNIPPLEZ add call to 
			String response = sendToWordServer("FETCH m " + vert_word.charAt(vert_cross_index[i]));
			//System.out.println(response); //TESTOUTPUT FOR TROUBLESHOOTING
			
			if (response.split(" ")[2] == "1"){ //checks for operation success
				horiz_words[i] = response.split(" ")[1];
			}
			//calls sendToWordServer("FETCH m <char>") and returns with the 2nd element of response "FETCH <word> <bool>"

			
		}

		//Testing purposes
		//vert_cross_index[0] = 0;
		//vert_cross_index[1] = 1;
		//vert_cross_index[2] = 2;

		//horiz_words[0] = "meat";
		//horiz_words[1] = "xray";
		//horiz_words[2] = "grape";

		// Determine horizontal cross indices based on intersection with vertical word
    	for (int i = 0; i < horiz_count; i++) {
        	// Convert the target character and horizontal word to uppercase
    		char targetChar = Character.toUpperCase(vert_word.charAt(vert_cross_index[i]));
    		String horizWordUpper = horiz_words[i].toUpperCase();

    		// Find the index of the target character in the horizontal word
    		horiz_cross_index[i] = horizWordUpper.indexOf(targetChar);

			if (horiz_cross_index[i] < 0) {
        		throw new IllegalArgumentException("Character '" + vert_word.charAt(vert_cross_index[i]) + "' is not found in selected word");
    		}
    	}

		int grid_y = vert_word.length();
		GridDimensions grid_x = getGridX(horiz_words, horiz_cross_index);
	
		System.out.println("Vertical word starts at: " + grid_x.verticalX + " with Max length of: " + grid_x.maxLength + "\n\n");
		
		for (int i = 0; i < horiz_cross_index.length; i++) {
			horiz_cross_index[i] = grid_x.verticalX - horiz_cross_index[i];
		}

		printGrid(vert_word, grid_x, horiz_words, vert_cross_index, horiz_cross_index);

	}


	private static class CrosswordInterfaceHandler implements Runnable {
		private Socket interfaceSocket;

		CrosswordInterfaceHandler(Socket socket) {
			this.interfaceSocket = socket;
		}

		@Override
		public void run() {
	    	System.out.println("Connected, in communication with interface");
		    try {
    		    PrintStream out = new PrintStream(interfaceSocket.getOutputStream());
    	    	Scanner in = new Scanner(new InputStreamReader(interfaceSocket.getInputStream()));

   		     String query = "";
			 //String commands[] = ["NEWGAME", "CHECK", "SCORE"];

    		    while (!query.equals("Quit")) {
        		    // Check if the interface has sent a message
            		if (in.hasNextLine()) { // Wait until input is available
                		query = in.nextLine().trim();

						//query = removeWhitespace(query);

                		System.out.println("\nReceived the following message from Interface:" + query + "\n");
	    	            /* Check Query for Operation arguments 
							name/number/number
						*/

						String[] parts = query.split(" ");

    	    	        if (parts.length == 4 || parts.length == 2) {
							
    						System.out.print("Valid Format: " + parts[0] + "\n");

							parts[0] = parts[0].toUpperCase();

							switch (parts[0]) {
    							case "NEWGAME":

									if(parts[2].matches("\\d+") && parts[3].matches("\\d+")) {
										out.print("Starting New Game\n\n");
										newGame(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
									}

									else {
										out.print("Must enter numeric values for game variables\n");
									}
    							    break;

    							case "CHECK":
    							    out.print("Checking User Guess\n");
    							    break;

    							case "SCORE":
    							    out.print("Checking Score");
    						    	break;

    							default:
    							    out.print("Error, invalid request");
    							    break;
							}

						} else {
						    out.print("Invalid message format");
						}

                		out.println();

		            } else {
    		            // Do nothing and keep waiting
        		        Thread.sleep(100); // Optional: add a small delay to avoid excessive CPU usage
            		}
	        	}

    	    	in.close();

    		} catch (SocketException e) {
        		System.out.println("Error: " + e.getMessage());
    		} catch (Exception e) {
        		e.printStackTrace();
		    } finally {
    		    try {
        		    interfaceSocket.close();
        		} catch (IOException e) {
            		System.err.println("Error closing socket: " + e.getMessage());
		        }
    		    System.out.println("Closed: " + interfaceSocket);
	    	}
		}
	}
}


