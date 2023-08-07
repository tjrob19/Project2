import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * This program takes user input and sends created packets to the receiver
 *
 * @author Chakrya Ros
 * @author Trevor Robinson
 * @date 8/6/2023
 * @info Course COP5518
 */
public class UDPSender {

	// Global variables
	private static final int BUFFER_SIZE = 54;
	private DatagramSocket   _socket; // the socket for communication with a server
	private String _srcPort;          // sender host number.
	private String _srcHost;          // sender port number.
	private String _rcvHost;          // receiver host name.
	private String _rcvPort;          // receiver port number.
	private String _networkHost;      // network name.
	private int    _networkPort;      // network port number.
	private String _request;          // request string.
	byte[]         _packetOut;
	private String _seqNum;
	private String _ack;

	/**
	 * Constructs a UDPSender object
	 */
	public UDPSender( String srcPort, String rcvHost, String rcvPort, String networkHost, String networkPort) {
		_srcPort = srcPort;
		_srcHost = "localhost";
		_rcvHost = rcvHost;
		_rcvPort = rcvPort;
		_networkHost = networkHost;
		_networkPort = Integer.parseInt(networkPort);
	}

	/**
	 * Creates a datagram socket and binds it to a free port
	 *
	 * @return - 0 or a negative number describing an error code if the connection could not be established
	 */
	public int createSocket() {
		try {
			_socket = new DatagramSocket(Integer.parseInt(_srcPort));
		} catch (SocketException ex) {
			System.err.println("unable to create and bind socket");
			return -1;
		}
		return 0;
	}

	/**
	 * Sends a request for service to the server. Do not wait for a reply in this function. This will be
	 * an asynchronous call to the server.
	 *
	 * @return - 0, if no error; otherwise, a negative number indicating the error
	 */
	public int sendRequest() {

		// Create new packet and try to send to the server
		DatagramPacket newDatagramPacket = createDatagramPacket(_packetOut, _networkHost, _networkPort);
		if (newDatagramPacket != null) {
			try {
				_socket.send(newDatagramPacket);
				// set time out
				_socket.setSoTimeout(100000);
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
	 * Creates a datagram from the specified request and destination host and port information.
	 *
	 * @param packet - the request to be submitted to the server
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

	/**
	 * Receives the server's response following a previously sent request.
	 *
	 * @return - the server's response or NULL if an error occured
	 */
	public UDPPacket receiveResponse() {
		
		// Create packet to receive and packet object, try to receive the response packet
		byte[] buffer = new byte[BUFFER_SIZE];
		DatagramPacket newDatagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);
		UDPPacket packet = null;
		try {
			_socket.receive(newDatagramPacket);

			//Convert the packet to string.
			Charset charset = StandardCharsets.US_ASCII;
			String  request = charset.decode(ByteBuffer.wrap(newDatagramPacket.getData())).toString().trim();
			String  srcIP = request.substring(0, 15);
			String  srcPort = request.substring(16, 21);
			String  destIP = request.substring(22, 37);
			String  destPort = request.substring(38, 43);
			_seqNum = request.substring(44, 45);
			String  payload = request.substring(48);

			// Create packet object 
			packet = new UDPPacket(destPort, destIP, srcPort, srcIP, Integer.parseInt(_seqNum));
			packet.makePacket(payload);

		} catch (SocketTimeoutException ignored) {  // Socket timeout, print the error.
			System.err.println("Unable to receive message from server, it's timeout: " + ignored);
			return null;
		} catch (IOException ex) {
			System.err.println("Unable to receive message from server: " + ex);
			return null;
		}

		return packet;
	}

	/*
	 * Prints the response to the screen in a formatted way
	 *
	 * @param response - the server's response as an XML formatted string
  	 * @param seq - sequence number 
	 */
	public static void printResponse(String response , int seq) {
		System.out.println("FROM SERVER: " + response);
		// Create packet.
		System.out.println("Received: " + "ACK" + "  Seq: " + seq);
	}

	/*
	 * Start request that get input from user
	 */
	public void StartRequest() /*throws IOException*/ {

		// Local variables
		int       requestSize = _request.length(); // Size of request from user.
		int       subRequestSize = 6;   // size of sub-request for each packet.
		int       start = -1;
		int       index = 0;
		int       pos = subRequestSize;
		int       packetNum = 0;  	// Packet number
		int       seqNum = 0; 		// Sequence number of packet
		int       prevSeqNum = 1;       // Previous sequence number of the packet
		boolean   delayed = false;  	// Boolen for delay
		UDPPacket sendPacket = null;	// sended packet
		UDPPacket recPacket = null;	// Received packet
		String    msg = "";		//Final message

		// Loop to set header information
		while (true) {

			String req ="";
			// Check if the request is greater than 6 byte.
			if(_request.length() > 6)
			{
				req = _request.substring(start + 1, subRequestSize + index);
				requestSize -= pos;
				if(requestSize > 0 && subRequestSize > requestSize)
				{
					subRequestSize = requestSize;
					index += req.length();
					start += req.length();
				} else {
					index += subRequestSize;
					start += subRequestSize;
				}
			} else {
				req = _request;
				requestSize -= req.length();
			}
			// Execute only if packet is not sent
			if (!delayed)
			{
				// make packet
				sendPacket = new UDPPacket(_srcPort, _srcHost, _rcvPort,
						_rcvHost, seqNum);
				sendPacket.makePacket(req);
				_packetOut = new byte[BUFFER_SIZE];
				_packetOut = sendPacket.getSegment();
                		msg += req;
			}

			delayed = false;

			// Sending the packet
			if (sendRequest() < 0) {
				closeSocket();
				return;
			}
			packetNum += 1; 		//increment the packet number
			System.out.println("Waiting for packet " + packetNum + " to response.");

			// Receive the response
			recPacket = receiveResponse();
			
			//Previous timeout checking
			if(recPacket != null && recPacket.getSequence() == prevSeqNum && recPacket.getSegment() != sendPacket.getSegment() )
			{
				recPacket = receiveResponse();
				_ack = recPacket.validateMessage();
				System.out.println("Received: " + _ack);
			}

			// If the packet is null, it times out
			if(recPacket == null)
			{
				System.out.println("The packet number: " + packetNum + " couldn't be received and timed out");
				delayed = true;
				packetNum -= 1;
				seqNum = getSequenceNum(seqNum);
			}else{
				_ack = recPacket.validateMessage();
				System.out.println("Recieved " + _ack + " for packet " + packetNum);
			}

           		if ((!recPacket.isLastMessage && requestSize > 0 )) {
                		prevSeqNum = seqNum;  // update previousely sequence
                		seqNum = getSequenceNum(seqNum); // update the current sequence
		   	} else {
			   	// Print the whole message
                		String output = msg.substring(0, msg.length() - 1);
                		System.err.println("Packet completely sent: " + output);
                		return;
		   	}
        	}
	}

	/*
	 * Flip the sequence number
	 */
	public Integer getSequenceNum(int seqNum) { // FLIP THIS ONE AND THE ONE BELOW IT *****************************************************
		if (seqNum == 0) {
			seqNum = 1;
		}
		else seqNum = 0;
		return  seqNum;
	}

	/*
	 * Get request from client
	 */
	public void SetRequest(String request) { //*******************************************************************
		_request = request;
	}

	/*
	 * Closes an open socket
	 *
	 * @return - 0, if no error; otherwise, a negative number indicating the error
	 */
	public int closeSocket() {
		_socket.close();

		return 0;
	}

	/**
	 * Main method to test program
	 *
	 * @param args - user defined arguments
	 *
	 */
	public static void main(String[] args) throws IOException {
		
		// Local variables
		String srcPort;
		String srcHost;
		String rcvHost;
		String rcvPort;
		String networkHost;
		String networkPort;

		// UDPSender object
		UDPSender sender;
		
		// Ensure program is run with proper arguments
		if (args.length != 5) {
			System.err.println("Usage: UDPSender <sender port number> <serverName> <receiver port number>" +
					"<networkName> <network port number\n");
			return;
		}

		// Try to store argument values
		try {
			srcPort = args[0];
			rcvHost = args[1];
			rcvPort = args[2];
			networkHost = args[3];
			networkPort = args[4];
		} catch (NullPointerException xcp) {
			System.err.println("Usage: UDPSender <sender port number> <serverName> <receiver port number>" +
					"<networkName> <network port number\n");
			return;
		}

		// Construct sender and sender socket
		sender = new UDPSender(srcPort, rcvHost, rcvPort, networkHost, networkPort);
		if (sender.createSocket() < 0) {
			return;
		}

		// Formatted display to prompt user for input and send message
		System.out.println("***************************** RDT SENDER *********************************");
		System.out.print("Enter a request: ");
		String request = System.console().readLine(); // read input from user
		System.out.println("Sending the packet to: " + rcvHost + " " + rcvPort);
		sender.SetRequest(request + ".");

		// Send the message using other methods, until user types 'done'
		while(!Objects.equals(request, "done"))
		{
			sender.StartRequest();
			System.out.print("Enter a request: ");
			request = System.console().readLine();
			sender.SetRequest(request + ".");
			if("done".equals(request)) {
				if (sender.closeSocket() != 0) {
					System.out.println("There is an error when close socket");
				}
				return;
			}
		}
	}
}
