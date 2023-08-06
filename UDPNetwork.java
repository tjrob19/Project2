import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Network object.
 *
 * @author Chakrya Ros
 * @author Trevor Robinson
 */
public class UDPNetwork {

	private static final int BUFFER_SIZE = 54;
	private DatagramSocket _socket; // the socket for communication with clients
	private int            _port;   // the port number for communication with this server
	private boolean        _continueService;

	private Random random = new Random();
	
	private int _lostPercent;
	private int _delayedPercent;
	private int _errorPercent;

	byte[] _packetIn;

	byte[] _packetOut;

	/**
	 * Constructs a UDPserver object.
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
			_socket = new DatagramSocket(_port);
		} catch (SocketException ex) {
			System.err.println("unable to create and bind socket");
			return -1;
		}

		return 0;
	}

	public void run()
	{
		// run server until gracefully shut down
		_continueService = true;
		int totalReceived = 0;
		int totalSended = 0;
		while (_continueService) {
			DatagramPacket newDatagramPacket = receiveRequest();  //receive the packet
			Charset charset = StandardCharsets.US_ASCII;
			String request = charset.decode(ByteBuffer.wrap(newDatagramPacket.getData()))
					.toString(); 		//Convert the packet to string.

			System.out.println("sender IP: " + newDatagramPacket.getAddress().getHostAddress() +
					" Port: " + newDatagramPacket.getPort());
			totalReceived += 1;
			System.out.println("Receiver Packet: " + totalReceived);

			/*String request = new String (newDatagramPacket.getData()).trim();
			String segment = request.substring(44);
			System.out.println ("sender IP: " + newDatagramPacket.getAddress().getHostAddress());
			System.out.println ("sender request: " + segment);
			System.out.println ("sender request: " + request);*/

			if (request.equals("<shutdown/>")) {
				_continueService = false;
			}

			if (request != null) {

				String srcIP = request.substring(0, 15);
				String srcPort = request.substring(16, 21);
				String destIP = request.substring(22, 37);
				String destPort = request.substring(38, 43);
				int rcvSeq = Integer.parseInt(request.substring(44, 45));
				String checkSum =  (request.substring(45, 48));
				String payload = request.substring(48);

				// Create the packet that receive.
				UDPPacket rcvPacket = new UDPPacket(srcPort, srcIP, destPort, destIP, rcvSeq);
				rcvPacket.makePacket(payload);
				_packetIn = new byte[BUFFER_SIZE];
				_packetIn = rcvPacket.getSegment();

				// Send the response.
				sendResponse(_packetIn, destIP, Integer.parseInt(destPort));
				totalSended  += 1;
				System.out.println("Packets Delayed:\t" + _delayedPercent  + "\t" +
						"Packets Corrupt:\t" + _errorPercent + "\t" +
						"Packets Lost:\t" + _lostPercent + "\t" +
						"Sender packets:\t" + totalSended);
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
		
		int rand = random.nextInt(100);

		// Simulate packet loss

		// Simulate corrupt packet
		
		DatagramPacket newDatagramPacket = createDatagramPacket(packet, hostAddr, port);
		if (newDatagramPacket != null) {
			
			if (rand < _delayedPercent) {
				new Thread(() -> {
                    			try {
                        			Thread.sleep((long) ((1.5 + random.nextFloat() * 0.5) * 100)); // Delay between 1.5 and 2 times 100 ms
                        			_socket.send(newDatagramPacket);
                        			System.out.println("Packet delayed!");
                    			} catch (InterruptedException | IOException ex) {
                        			System.err.println("Unable to send delayed message to server");
                    			}
                		}).start();
				return 0;
			}
			
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


	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int lostPercent;
		int delayedPercent;
		int errorPercent;

		UDPNetwork  server;
		String    serverName;
		String    req;

		if (args.length != 4) {
			System.err.println("Usage: UDPNetwork <port number>\n");
			return;
		}

		int portNum;
		try {
			portNum = Integer.parseInt(args[0]);
			lostPercent = Integer.parseInt(args[1]);
			delayedPercent = Integer.parseInt(args[2]);
			errorPercent = Integer.parseInt(args[3]);
			System.err.println("Port number: " + portNum);
		} catch (NumberFormatException xcp) {
			System.err.println("Usage: UDPNetwork <port number>\n");
			return;
		}

		// construct client and client socket
		server = new UDPNetwork (portNum, lostPercent, delayedPercent, errorPercent);
		if (server.createSocket() < 0) {
			return;
		}

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
