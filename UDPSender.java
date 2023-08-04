import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Sender Packet object.
 *
 * @author Chakrya Ros
 * @author Trevor Robinson
 */
public class UDPSender {

	private static final int BUFFER_SIZE = 54;
	private DatagramSocket _socket; // the socket for communication with a server
	private String _srcPort;    // sender host number.
	private String _srcHost;    // sender port number.
	private String _rcvHost; // receiver host name.
	private String _rcvPort;  // receiver port number.
	private String _networkHost; // network name.
	private int _networkPort; // network port number.
	private String _request;  // request string.
	byte[] _packetIn;
	byte[] _packetOut;

	private String _seqNum;

	private String _ack;

	/**
	 * Constructs a UDPSender object.
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
	 * Creates a datagram socket and binds it to a free port.
	 *
	 * @return - 0 or a negative number describing an error code if the connection could not be established
	 */
	public int createSocket() {
		try {
			_socket = new DatagramSocket();
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

		DatagramPacket newDatagramPacket = createDatagramPacket(_packetOut, _rcvHost, Integer.parseInt(_rcvPort));
		if (newDatagramPacket != null) {
			try {
				_socket.send(newDatagramPacket);

				// set time out.
				_socket.setSoTimeout(10000);
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
	public String receiveResponse() throws UnknownHostException {
		byte[] buffer = new byte[BUFFER_SIZE];
		DatagramPacket newDatagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);

		try {
			_socket.receive(newDatagramPacket);
		} catch (SocketTimeoutException ignored)   // Socket timeout, print the error.
		{
			System.err.println("Unable to receive message from server, it's timeout");
			return null;
		} catch (IOException ex) {
			System.err.println("unable to receive message from server");
			return null;
		}
		Charset charset = StandardCharsets.US_ASCII;
		//Convert the packet to string.
		String request = charset.decode(ByteBuffer.wrap(newDatagramPacket.getData())).toString().trim();

		String srcIP = request.substring(0, 15);
		String srcPort = request.substring(16, 20);
		String destIP = request.substring(22, 37);
		String destPort = request.substring(39, 43);
		_seqNum = request.substring(44, 45);
		String payload = request.substring(48);

		// Create packet.
		UDPPacket packet = new UDPPacket(destPort, destIP, srcPort, srcIP, Integer.parseInt(_seqNum));
		_packetIn = new byte[BUFFER_SIZE];

		// Make packet.
		_packetIn = packet.makePacket(payload);
		return payload;

	}

	/*
	 * Prints the response to the screen in a formatted way.
	 *
	 * response - the server's response as an XML formatted string
	 *
	 */
	public static void printResponse(String response , int seq) {
		System.out.println("FROM SERVER: " + response);
		// Create packet.
		System.out.println("Received: " + "ACK" + "  Seq: " + seq);
	}

	/*
	 * Start request that get input from user.
	 */
	public void StartRequest() throws IOException {

		int requestSize = _request.length();
		int subRequestSize = 6;
		int start = -1;
		int index = 0;
		int pos = subRequestSize;
		int packetNum = 0;  //Packet number
		int seq = 0; 		//sequence number of packet
		boolean delayed = false;
		long checksum = 0;
		String mes = " ";

		while (true) {
			String req = _request.substring(start + 1, subRequestSize + index);
			requestSize -= pos;
			if(requestSize > 0 && subRequestSize > requestSize)
			{
				subRequestSize = requestSize;
				index += req.length();
				start += req.length();
			}else {
				index += subRequestSize;
				start += subRequestSize;
			}

			//Execute only if packet is not sent
			/*if(!delayed)
			{
				_packetIn = req.getBytes();
				checksum = CheckSum.CalculateChecksum(_packetIn);
			}*/
			// make packet.
			UDPPacket packet = new UDPPacket(_srcPort, _srcHost, _rcvPort,
					_rcvHost, seq);
			_packetOut = new byte[BUFFER_SIZE];
			_packetOut = packet.makePacket(req);
			mes = mes + req;
			if (sendRequest() < 0) {
				closeSocket();
				return;
			}
			String response = receiveResponse();
			if (response != null) {
				System.out.println("SeqNumber: " + _seqNum);
				UDPSender.printResponse(response, Integer.parseInt(_seqNum));
			}
			else {
				System.err.println ("incorrect response from server");
			}
			if (requestSize <= 0) {
				System.err.println ("Packet sent: " + mes);
				return;
			}
		}
	}
	/*
	 * Get request from client.
	 */
	public void SetRequest(String request) {
		_request = request;
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

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String srcPort;
		String srcHost;
		String rcvHost;
		String rcvPort;
		String networkHost;
		String networkPort;

		UDPSender sender;
		if (args.length != 5) {
			System.err.println("Usage: UDPSender <sender port number> <serverName> <receiver port number>" +
					"<networkName> <network port number\n");
			return;
		}
		try {
			/*srcPort = Integer.parseInt(args[0]);
			rcvHost = args[1];
			rcvPort = Integer.parseInt(args[2]);
			networkHost = args[3];
			networkPort = Integer.parseInt(args[4]);*/
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

		// construct sender and sender socket
		sender = new UDPSender(srcPort, rcvHost, rcvPort, networkHost, networkPort);
		if (sender.createSocket() < 0) {
			return;
		}

		System.out.println("***************************** RDT SENDER *********************************");
		System.out.print("Enter a request: ");
		String request = System.console().readLine();
		//String request = "This is a test sender in here";
		System.out.println("Sending the packet to: " + rcvHost + " " + rcvPort);
		sender.SetRequest(request +".");
		// read input from user.
		while(!Objects.equals(request, "done"))
		{
			sender.StartRequest();
			System.out.print("Enter a request: ");
			request = System.console().readLine();
			sender.SetRequest(request);
			if("done".equals(request)) {
				if (sender.closeSocket() != 0) {
					System.out.println("There is an error when close socket");
				}
				return;
			}
		}
	}
}
