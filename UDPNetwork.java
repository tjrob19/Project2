import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * This program listens for outside communications, and forwards the messages
 * to their correct locations.
 *
 * @author Chakrya Ros
 * @author Trevor Robinson
 * @date 8/6/2023
 * @info Course COP5518
 */
public class UDPNetwork {

	private static final int BUFFER_SIZE = 54;
	private DatagramSocket   _socket; // the socket for communication with clients
	private final int              _port;   // the port number for communication with this server
	private boolean          _continueService; // whether or not to continue iteration
	private final Random           random = new Random(); // Random object to calculate random numbers

	// Variables to hold values provided by command-line arguments
	private final int _lostPercent;
	private final int _delayedPercent;
	private final int _errorPercent;
	byte[]      _packetIn;

	/**
	 * Constructs a UDPserver object.
  	 *
    	 * @param portNum - network port number
      	 * @param lostPercent - percent chance to drop a packet
	 * @param delayedPercent - percent chance to delay packet transmission
	 * @param errorPercent - percent chance to corrupt a packet
	 */
	public UDPNetwork (int portNum, int lostPercent, int delayedPercent, int errorPercent) {
		_port = portNum;
		_lostPercent = lostPercent;
		_delayedPercent = delayedPercent;
		_errorPercent = errorPercent;
	}

	/**
	 * Creates a datagram socket and binds it to a free port.
	 *
	 * @return - 0 or a negative number describing an error code if the connection could not be established
	 */
	public int createSocket() {
		try {
			// Try opening a new socket with the provided port number
			_socket = new DatagramSocket(_port);
		} catch (SocketException ex) {
			System.err.println("unable to create and bind socket");
			return -1;
		}

		return 0;
	}

	/**
 	 * Runs the program to retrieve necessary information and forward
    	 * the received packet.
      	 *
 	 */
	public void run()
	{
		int delayTotal = 0;
		int errorTotal = 0;
		int lostTotal = 0;
		// run server until gracefully shut down
		_continueService = true;
		int totalReceived = 0;
		while (_continueService) {
			DatagramPacket newDatagramPacket = receiveRequest();  //receive the packet
			Charset charset = StandardCharsets.US_ASCII;
			String request = charset.decode(ByteBuffer.wrap(newDatagramPacket.getData()))
					.toString(); 		//Convert the packet to string.

			System.out.println("----------------------------------------------");
			System.out.println("Sender IP: " + newDatagramPacket.getAddress().getHostAddress() +
					" Port: " + newDatagramPacket.getPort());
			totalReceived += 1;
			System.out.println("Receiver Packet: " + totalReceived);

			// Stop iterations if user is done
			if (request.equals("<shutdown/>")) {
				_continueService = false;
			}

			// If user has a request
			if (request != null) {
				// Extract necessary information from packet
				String srcIP = request.substring(0, 15);
				String srcPort = request.substring(16, 21);
				String destIP = request.substring(22, 37);
				String destPort = request.substring(38, 43);
				int    rcvSeq = Integer.parseInt(request.substring(44, 45));
				String checkSum =  (request.substring(45, 48));
				String payload = request.substring(48);
				

				// Create the packet to send to destination with information provided
				UDPPacket rcvPacket = new UDPPacket(srcPort, srcIP, destPort, destIP, rcvSeq);
				rcvPacket.makePacket(payload);
				_packetIn = new byte[BUFFER_SIZE];
				_packetIn = rcvPacket.getSegment();

				// Calculate random number
				double random = Math.random();
				double x = random*100;
				int rand = (int)x + 1; //Add 1 to change the range to 1 - 100
				
				/*
				 * If the random number is within the range provided by the user for delay,
				 * delay the transmission using a separate thread, then send to destination
				 *
				 * Simulate packet delayed, corrupt packet and packet loss
				 */
				if (rand <= _delayedPercent) { // Delayed
					
					delayTotal++;
					int finalTotalReceived = totalReceived;
					double delayedTime = ((5000 * 1.5) + (5000 * 2.0)) / 2;
                    new Thread(() -> {
						try {
							Thread.sleep((long) delayedTime); // Put thread to sleep for delayedTime
							System.out.println("Packet delayed!");
							sendResponse(_packetIn, destIP, Integer.parseInt(destPort));
							System.out.println("Received: Packet " + finalTotalReceived + ", SENDING");
							
							System.out.println("----------------------------------------------");
						} catch (InterruptedException ex) {
							System.err.println("Unable to send delayed message to server");
						}
					}).start();
				}else if(rand <= _errorPercent) //Corrupt
				{
					errorTotal++;
					String corruptPayload = "0000"; //Add the corrupt to payload
					rcvPacket.makePacket(corruptPayload);
					System.out.println("Received: Packet " + totalReceived + ", CORRUPTED");
					byte[] packetC = new byte[BUFFER_SIZE];
					packetC = rcvPacket.getSegment();
					sendResponse(packetC, destIP, Integer.parseInt(destPort));
				} else if (rand <= _lostPercent) // Drop Packet
				{
					lostTotal++;
					System.err.println("Lost ACK");
					System.out.println("Received: Packet" + totalReceived + ", DROPPED");
					totalReceived -= 1;
				}else{
					// Send the packet to correct destination
					sendResponse(_packetIn, destIP, Integer.parseInt(destPort));
					System.out.println("Received: Packet" + totalReceived + ", SEND");
				}
			}
			else {
				System.err.println ("incorrect response from server");
			}
			
			System.out.println("Total packets delayed: " + delayTotal);
			System.out.println("Total packets lost: " + lostTotal);
			System.out.println("Total packets corrupted: " + errorTotal);
		}
	}

	/**
	 * Sends a request for service to the server. Do not wait for a reply in this function. This will be
	 * an asynchronous call to the server.
	 *
	 * @param packet - the packet to be sent
	 * @param hostAddr - the ip or hostname of the server
	 * @param port - the port number of the server
	 *
	 * @return - 0, if no error; otherwise, a negative number indicating the error
	 */
	public int sendResponse(byte[] packet, String hostAddr, int port) {

		// Create datagram packet with provided parameters
		DatagramPacket newDatagramPacket = createDatagramPacket(packet, hostAddr, port);
		if (newDatagramPacket != null) {
			try {
				_socket.send(newDatagramPacket);
			} catch (IOException ex) {
				System.err.println("unable to send message to server");
				return -1;
			}
			return 0;
		}
		System.err.println("unable to send message to server");
		return -1;
	}

	/**
	 * Receives a client's request.
	 *
	 * @return - the datagram containing the client's request or NULL if an error occured
	 */
	public DatagramPacket receiveRequest() {

		// Create new packet with buffer
		byte[] buffer = new byte[BUFFER_SIZE];
		DatagramPacket newDatagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);
		
		// Receive new packet and store in created packet
		try {
			_socket.receive(newDatagramPacket);
		} catch (IOException ex) {
			System.err.println("unable to receive message from server");
			return null;
		}

		// Return the received packet
		return newDatagramPacket;
	}

	/*
	 * Prints the response to the screen in a formatted way.
	 *
	 * response - the server's response as an XML formatted string
	 *
	 */
	public static void printResponse(String response) {
		System.out.println("FROM SERVER: " + response);
	}


	/*
	 * Closes an open socket.
	 *
	 * @return - 0, if no error; otherwise, a negative number indicating the error
	 */
	public int closeSocket() {
		_socket.close();

		return 0;
	}

	/**
 	 * Main method to obtain args and call methods to receive/send packets
   	 *
      	 * @param args - arguments provided by user for port and simulation
     	 */
	public static void main(String[] args) {
		
		// Variables to store simulation data 
		int lostPercent;
		int delayedPercent;
		int errorPercent;
		int portNum;

		// Network object
		UDPNetwork  server;

		// Ensure user runs program with proper arguments
		if (args.length != 4) {
			System.err.println("Usage: UDPNetwork <port number>  <lostPercent> <delayedPercent> <errorPercent>\n");
			return;
		}

		// Try to store provided arguments and print provided port number
		try {
			portNum = Integer.parseInt(args[0]);
			lostPercent = Integer.parseInt(args[1]);
			delayedPercent = Integer.parseInt(args[2]);
			errorPercent = Integer.parseInt(args[3]);
			System.err.println("*********************** NETWORK PORT NUMBER: " + portNum + " ***********************");
		} catch (NumberFormatException xcp) {
			System.err.println("Usage: UDPNetwork <port number>  <lostPercent> <delayedPercent> <errorPercent>\n");
			return;
		}

		// Construct network and network socket
		server = new UDPNetwork (portNum, lostPercent, delayedPercent, errorPercent);
		if (server.createSocket() < 0) {
			return;
		}

		// Print percentages provided by user
		System.out.println("Packets Lost: " + lostPercent + "%\t" +
						   "Packets Delayed: " + delayedPercent  + "%\t" +
						   "Packets Corrupt: " + errorPercent + "%");

		// Run the program and close the socket when finished
		server.run();
		server.closeSocket();
	}

	/**
	 * Creates a datagram from the specified request and destination host and port information.
	 *
	 * @param packet - the  packet request to be submitted to the server
	 * @param hostname - the hostname of the host receiving this datagram
	 * @param port - the port number of the host receiving this datagram
	 *
	 * @return a complete datagram or null if an error occurred creating the datagram
	 */
	private DatagramPacket createDatagramPacket(byte[] packet, String hostname, int port)
	{
		InetAddress hostAddr;
		try {
			hostAddr = InetAddress.getByName(hostname);
		} catch (UnknownHostException ex) {
			System.err.println ("invalid host address");
			return null;
		}

		return new DatagramPacket (packet, BUFFER_SIZE, hostAddr, port);
	}

}
