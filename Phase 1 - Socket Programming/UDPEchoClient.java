import java.net.*;

public class UDPEchoClient {
    public static void main(String[] args) {
        // Check for command line argument
        if (args.length < 1) {
            System.out.println("Usage: java UDPEchoClient <message>");
            System.exit(1);
        }

        // Join all the command-line arguments into a single message
        String message = String.join(" ", args);
        String serverHost = "127.0.0.1"; // Change to the server's address if needed
        int serverPort = 6969;          // Change to the target server's port if needed

        DatagramSocket socket = null;
        try {
            // Resolve server address
            InetAddress serverAddress = InetAddress.getByName(serverHost);

            // Create a datagram socket
            socket = new DatagramSocket();

            // Convert message to bytes and create a datagram packet
            byte[] sendData = message.getBytes("UTF-8");
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);

            // Send the packet to the server
            socket.send(sendPacket);
            System.out.println("Sent message to " + serverHost + ":" + serverPort);

            // Set a timeout for the response (in milliseconds)
            socket.setSoTimeout(2000);

            // Prepare a packet to receive the response
            byte[] buffer = new byte[4096];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

            // Wait for the server's response
            socket.receive(receivePacket);
            String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");
            System.out.println("Received echo: " + receivedMessage);
        } catch (SocketTimeoutException e) {
            System.out.println("No response received within timeout period.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}
