import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Sender Packet object.
 */
public class UDPSender {

	private static final int BUFFER_SIZE = 54;
	private DatagramSocket _socket; // the socket for communication with a server
	private final int _srcPort;    // sender port number.
	private final String _rcvHost; // server name.
	private final int _rcvPort;  // server port number.
	private String _networkHost; // network name.
	private int _networkPort; // network port number.
	private String _request;  // request string.
	private int SegmentNum = -1; // number of segment.
	byte[] _packet;

	/**
	 * Constructs a UDPSender object.
	 */
	public UDPSender(int portNum, String rcvHost, int rcvPort, String networkHost, int networkPort) {
		_srcPort = portNum;
		_rcvHost = rcvHost;
		_rcvPort = rcvPort;
		_networkHost = networkHost;
		_networkPort = networkPort;
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

		DatagramPacket newDatagramPacket = createDatagramPacket(_packet, _rcvHost, _rcvPort);
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
	public String receiveResponse() {
		byte[] buffer = new byte[BUFFER_SIZE];
		DatagramPacket newDatagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);
		try {
			_socket.receive(newDatagramPacket);
		} catch (IOException ex) {
			System.err.println("unable to receive message from server");
			return null;
		}

		return new String(buffer).trim();

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
	 * Start request that get input from user.
	 */
	public void StartRequest() throws UnknownHostException {

		int requestSize = _request.length();
		int subRequestSize = 9;
		int start = -1;
		int index = 0;
		int pos = subRequestSize;
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

			// make packet.
			UDPPacket packet = new UDPPacket(_srcPort, "127.0.0.1", _rcvPort,
					"localhost",req,0 , "ACK0");
			_packet = new byte[BUFFER_SIZE];
			_packet = packet.makePacket();
			if (sendRequest() < 0) {
				closeSocket();
				return;
			}
			String response = receiveResponse();
			if (response != null) {
				UDPSender.printResponse(response.trim());
			}
			else {
				System.err.println ("incorrect response from server");
			}
			if (requestSize <= 0) {
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

	public static void main(String[] args) throws UnknownHostException {
		// TODO Auto-generated method stub
		int portNum;
		String rcvHost;
		int rcvPort;
		String networkHost;
		int networkPort;

		UDPSender sender;
		if (args.length != 5) {
			System.err.println("Usage: UDPSender <sender port number> <serverName> <receiver port number>" +
					"<networkName> <network port number\n");
			return;
		}
		try {
			portNum = Integer.parseInt(args[0]);
			rcvHost = args[1];
			rcvPort = Integer.parseInt(args[2]);
			networkHost = args[3];
			networkPort = Integer.parseInt(args[4]);
		} catch (NullPointerException xcp) {
			System.err.println("Usage: UDPSender <sender port number> <serverName> <receiver port number>" +
					"<networkName> <network port number\n");
			return;
		}

		// construct sender and sender socket
		sender = new UDPSender(portNum, rcvHost, rcvPort, networkHost, networkPort);
		if (sender.createSocket() < 0) {
			return;
		}

		System.out.println("***************************** RDT SENDER *********************************");
		System.out.print("Enter a request: ");
		String request = System.console().readLine();
		//String test = "This is a test sender in here";
		System.out.println("Sending the packet to: " + rcvHost + " " + rcvPort);
		sender.SetRequest(request);
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
