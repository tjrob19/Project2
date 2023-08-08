import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * This server program listens for packets being sent over the network
 *
 * @author Chakrya Ros
 * @author Trevor Robinson
 * @date 8/6/2023
 * @info Course COP5518
 */
public class UDPReceiver {

	// Global variables
	private static final int BUFFER_SIZE = 54;
	private DatagramSocket   _socket;  // the socket for communication with clients
	private int              _rcvPort; // the port number for communication with this server
	private boolean          _continueService; // whether to continue iterations
	int seqNum = 0;    // sequence number
	byte[] _packetIn;  // packet received
	byte[] _packetOut; // packet Sent

	// Constructs a UDPserver object
	public UDPReceiver (int port) {
		_rcvPort = port;
	}

	/**
	 * Creates a datagram socket and binds it to a free port.
	 *
	 * @return - 0 or a negative number describing an error code if the connection could not be established
	 */
	public int createSocket() {
		try {
			_socket = new DatagramSocket(_rcvPort);
		} catch (SocketException ex) {
			System.err.println("unable to create and bind socket");
			return -1;
		}
		return 0;
	}

	/**
	 * Run receiver code to receive packets and send responses
	 */
	public void run() {
		
		// Run server until gracefully shut down
		_continueService = true;
		int totalReceived = 0;
		String msg = "";
		boolean corrupted = false;
		System.out.println("Waiting... connect sender.......");

		// While the user is still sending packets
		while (_continueService) {
			DatagramPacket newDatagramPacket = receiveRequest();  //receive the packet
			Charset charset = StandardCharsets.US_ASCII;
			String request = charset.decode(ByteBuffer.wrap(newDatagramPacket.getData()))
					.toString(); 		//Convert the packet to string.

			System.out.println("sender IP: " + newDatagramPacket.getAddress().getHostAddress() +
					" Port: " + newDatagramPacket.getPort());
			if (request.equals("<shutdown/>")) {
				_continueService = false;
			}

			// If request is not empty
			if (request != null) {

				// Obtain necessary information from packet received
				String srcIP = request.substring(0, 15);
				String srcPort = request.substring(16, 21);
				String destIP = request.substring(22, 37);
				String destPort = request.substring(38, 43);
				int    rcvSeq = Integer.parseInt(request.substring(44, 45));
				String checkSum =  (request.substring(45, 48));
				String payload = request.substring(48);

				// Create the packet that receive
				UDPPacket rcvPacket = new UDPPacket(srcPort, srcIP, destPort, destIP, rcvSeq);
				rcvPacket.makePacket(payload);
				_packetIn = new byte[BUFFER_SIZE];
				_packetIn = rcvPacket.getSegment();
				String ack = rcvPacket.validateMessage();
				String rcvCheckSum = rcvPacket.generateChecksum(payload);

				// Check if duplicate packet
				if(rcvSeq == seqNum && totalReceived != 0)
				{
					totalReceived -= 1;
					System.out.println("******** There is a duplicate packet **********");
					payload = ""; // Does not print payload if it is a duplicate
				}

				totalReceived += 1;
				seqNum = rcvSeq;
				// Check if the packet is corrupt
				if(!checkSum.equals(rcvCheckSum))
				{
					System.out.println("Packet: " + totalReceived + " received corrupted");
					totalReceived -= 1;
					corrupted = true;
				}

				// if not corrupt
				if(!corrupted){
					if(!Objects.equals(payload, "0000"))
					{
						msg += payload;
					}
				}

				// Create packet to send out
				UDPPacket packet = new UDPPacket(destPort, destIP, srcPort, srcIP, seqNum);
				packet.makePacket(payload);
				_packetOut = new byte[BUFFER_SIZE];
				_packetOut = packet.getSegment();
				// Send the response
				sendResponse(_packetOut, newDatagramPacket.getAddress().getHostName(),
						newDatagramPacket.getPort());
				// Print the full message when the last packet receive
				if (rcvPacket.isLastMessage) //&& rcvSeq == seqNum)
				{
					String output = msg;
					System.out.println("--------------------------------------------------");
					System.out.println("Packet completely received: " + output + "\n");

					// Clear the old message
					msg = "";
					totalReceived = 0;
				} else{
					System.out.println("Received packet: " + totalReceived  + ", Seq: " + rcvPacket.getSequence() + ", "  + ack + ", Message: " + payload);
					System.out.println("Sending ACK for: " + totalReceived);
				}

			}
			else {
				System.err.println ("incorrect response from server");
			}
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
		
		// Create datagram packet with provided information
		DatagramPacket newDatagramPacket = createDatagramPacket(packet, hostAddr, port);

		// With new packet, try to send to destination
		if (newDatagramPacket != null) {
			try {
				_socket.send(newDatagramPacket);
			} catch (IOException ex) {
				System.err.println("unable to send message to server");
				return -1;
			}

			return 0;
		}
		System.err.println("unable to create message");
		return -1;
	}

	/**
	 * Receives a client's request
	 *
	 * @return - the datagram containing the client's request or NULL if an error occured
	 */
	public DatagramPacket receiveRequest() {
		byte[] buffer = new byte[BUFFER_SIZE];
		
		// Make new packet and store received packet into it
		DatagramPacket newDatagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);
		try {
			_socket.receive(newDatagramPacket);
		} catch (SocketTimeoutException ignored)   // Socket timeout, print the error.
		{
			System.err.println("Unable to receive message from server, it's timeout.");
		}catch (IOException ex) {
			System.err.println("unable to receive message from server");
			return null;
		}

		return newDatagramPacket;
	}

	/*
	 * Closes an open socket.
	 *
	 * @return - 0, if no error; otherwise, a negative number indicating the error
	 */
	public void closeSocket() {
		_socket.close();
	}

	/**
	 * Main method to test program
	 */
	public static void main(String[] args) {
		
		// UDPReceiver object
		UDPReceiver  server;
		
		// Ensure proper arguments are used
		if (args.length != 1) {
			System.err.println("Usage: UDPReceiver <port number>\n");
			return;
		}

		// Try to store port number and display
		int portNum;
		try {
			portNum = Integer.parseInt(args[0]);
			System.err.println("*********************** RECEIVER PORT NUMBER: " + portNum + " ***********************");
		} catch (NumberFormatException xcp) {
			System.err.println("Usage: UDPReceiver <port number>\n");
			return;
		}

		// Construct UDPReceiver and socket
		server = new UDPReceiver (portNum);
		if (server.createSocket() < 0) {
			return;
		}

		// Run the program and close socket when complete
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
