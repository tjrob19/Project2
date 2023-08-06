import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Receiver Packet object.
 *
 * @author Chakrya Ros
 * @author Trevor Robinson
 */

public class UDPReceiver {

	private static final int BUFFER_SIZE = 54;
	private DatagramSocket _socket; // the socket for communication with clients
	private int            _rcvPort;   // the port number for communication with this server

	private String _rcvHost;  // the receiver host name.
	private boolean        _continueService;

	int seqNum = 0;

	byte[] _packetIn;

	byte[] _packetOut;

	/**
	 * Constructs a UDPserver object.
	 */
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
	 * Run server.
	 *
	 */
	public void run() {
		// run server until gracefully shut down
		_continueService = true;

		int totalReceived = 0;
		String msg="";
		int prevSeq = 1;
		System.out.println("Waiting... connect sender.......");

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

			if (request != null) {

				String srcIP = request.substring(0, 15);
				String srcPort = request.substring(16, 21);
				String destIP = request.substring(22, 37);
				String destPort = request.substring(38, 43);
				int rcvSeq= Integer.parseInt(request.substring(44, 45));
				String checkSum =  (request.substring(45, 48));
				String payload = request.substring(48);

				// Create the packet that receive.
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
				}

				// Check if the packet is corrupt.
				if(checkSum != rcvCheckSum)
				{
					System.out.println("Packet: " + totalReceived + " received corrupted");
					System.out.println("Resend the packet " + (totalReceived-1));
					totalReceived -= 1;
					if(rcvSeq == 0){
						seqNum = 1;
					}else{
						seqNum = 0;
					}
				}

				// Create packet to send out.
				UDPPacket packet = new UDPPacket(destPort, destIP, srcPort, srcIP, seqNum);
				packet.makePacket(payload);
				_packetOut = new byte[BUFFER_SIZE];
				_packetOut = packet.getSegment();

				// Send the response.
				sendResponse(request, newDatagramPacket.getAddress().getHostName(),
						newDatagramPacket.getPort());
				seqNum = rcvSeq;
				msg += payload;
				totalReceived += 1;
				// print the full message when the last packet receive.
				if(rcvPacket.isLastMessage) //&& rcvSeq == seqNum)
				{
					String output = msg.substring(0, msg.length()-1);
					System.out.println("--------------------------------------------------");
					System.out.println("Packet completely received: " + output);

					// clear the old message.
					msg = "";
					totalReceived = 0;
				}else {
					System.out.println("Received packet: " + totalReceived  + ", Seq: " + rcvPacket.getSequence() + ", "  + ack + ", Message " + payload);
					System.out.println("Sending ACK for : " + totalReceived);
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
	 * @param response - the response to be sent
	 * @param hostAddr - the ip or hostname of the server
	 * @param port - the port number of the server
	 *
	 * @return - 0, if no error; otherwise, a negative number indicating the error
	 */
	public int sendResponse(String response, String hostAddr, int port) {
		DatagramPacket newDatagramPacket = createDatagramPacket(response, hostAddr, port);
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
	 * Receives a client's request.
	 *
	 * @return - the datagram containing the client's request or NULL if an error occured
	 */
	public DatagramPacket receiveRequest() {
		byte[] buffer = new byte[BUFFER_SIZE];
		DatagramPacket newDatagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);
		try {
			_socket.receive(newDatagramPacket);
		} catch (IOException ex) {
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
	public int closeSocket() {
		_socket.close();

		return 0;
	}

	/**
	 * The main function. Use this function for
	 * testing your code. We will provide a new main function on the day of the lab demo.
	 */
	public static void main(String[] args) {
		UDPReceiver  server;
		if (args.length != 1) {
			System.err.println("Usage: UDPserver <port number>\n");
			return;
		}

		int portNum;
		try {
			portNum = Integer.parseInt(args[0]);
			System.err.println("Port number: " + portNum);
		} catch (NumberFormatException xcp) {
			System.err.println("Usage: UDPserver <port number>\n");
			return;
		}

		// construct client and client socket
		server = new UDPReceiver (portNum);
		if (server.createSocket() < 0) {
			return;
		}

		server.run();
		server.closeSocket();
	}

	/**
	 * Creates a datagram from the specified request and destination host and port information.
	 *
	 * @param request - the request to be submitted to the server
	 * @param hostname - the hostname of the host receiving this datagram
	 * @param port - the port number of the host receiving this datagram
	 *
	 * @return a complete datagram or null if an error occurred creating the datagram
	 */
	private DatagramPacket createDatagramPacket(String request, String hostname, int port)
	{
		byte buffer[] = new byte[BUFFER_SIZE];

		// empty message into buffer
		for (int i = 0; i < BUFFER_SIZE; i++) {
			buffer[i] = '\0';
		}

		// copy message into buffer
		byte data[] = request.getBytes();
		System.arraycopy(data, 0, buffer, 0, Math.min(data.length, buffer.length));

		InetAddress hostAddr;
		try {
			hostAddr = InetAddress.getByName(hostname);
		} catch (UnknownHostException ex) {
			System.err.println ("invalid host address");
			return null;
		}

		return new DatagramPacket (buffer, BUFFER_SIZE, hostAddr, port);
	}

}