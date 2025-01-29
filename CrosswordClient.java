
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class CrosswordClient {	
	private static final String USAGE = 
	"java CrosswordClient [host] [port] "
	+ "\n or \n"
	+ "java CrosswordClient";
	
	private static final int BUFFER_LIMIT = 1000;
	
	private Socket clientSocket;
	

	public CrosswordClient(String host, int port) {
		System.out.println("\nConnecting to Server . . .");
		try {
			clientSocket = new Socket(host, port);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("\nConnected.");
	}
	
	void clientToServer(String request) {
		try {
			// Create output streams & write the request to the server
			PrintStream out = 
				new PrintStream(clientSocket.getOutputStream());
			out.println (request.toUpperCase());
			out.println();
		} catch (IOException i) {
			i.printStackTrace();
			System.exit(1);
		}
	}

	String[] serverToClient() {
		String[] response = new String[BUFFER_LIMIT];
		
		try {
			BufferedReader in = new BufferedReader(
					new InputStreamReader(clientSocket.getInputStream()));
			int i = 0;
			do {
				response[i] = in.readLine();
				i++;	
			}  while(in.ready());			
			
			in.close();
		} catch (IOException i) {
			System.out.println(i.getMessage());
			System.exit(1);
		}
		return response;
	}
	
	void responseHandeler(String[] response) {
		if(response[0] != "QUIT") {
			
		}
	}

	public static void main(String[] args) throws IOException {
        if (args.length != 2 && args.length != 0) {
            System.out.println(USAGE);
            System.exit(1);
        }
		
		CrosswordClient client = null;
		Scanner userInput = null;
		String query = "";
		String[] response = new String[BUFFER_LIMIT];
		
		String serverHost = "localhost";  	
		int serverPort = 69;				
		
        if(args.length == 2) {
        	serverHost = args[0];
        	serverPort = Integer.parseInt(args[1]);
        }
        
        try {
        	client = new CrosswordClient(serverHost, serverPort);
    		userInput = new Scanner(System.in);
        }  catch (Exception e) {
        	System.err.println(e.getMessage());
        	System.exit(1);
        }
    	
    	while(!query.equals("QUIT") && !response[0].equals("QUIT")) {
    		try {
    			response = client.serverToClient();
    			client.responseHandeler(response);
    			query = userInput.nextLine();
    			client.clientToServer(query);
    		} catch (Exception e) {
            	System.err.println(e.getMessage());
            	System.exit(1);
    		}
    	}
    	
    	try {
    		userInput.close();
    		client.clientSocket.close();
    	} catch (IOException i) {
        	System.err.println(i.getMessage());
        	System.exit(1);
    	} catch(Exception e) {
        	System.err.println(e.getMessage());
        	System.exit(1);
    	}
    	
	}
}
