import java.io.IOException;
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
	byte[] _packetOut;

	private String _seqNum;

	private String _ack;
	DatagramPacket _newDatagramPacket;

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
	public int sendRequest(){

		_newDatagramPacket = createDatagramPacket(_packetOut, _networkHost, _networkPort);
		if (_newDatagramPacket != null) {
			try {
				_socket.send(_newDatagramPacket);
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
	public UDPPacket receiveResponse() throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		DatagramPacket newDatagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);
		UDPPacket packet = null;
		try {
			_socket.receive(newDatagramPacket);
			//Convert the packet to string.
			Charset charset = StandardCharsets.US_ASCII;
			String request = charset.decode(ByteBuffer.wrap(newDatagramPacket.getData())).toString();
			String srcIP = request.substring(0, 15);
			String srcPort = request.substring(16, 21);
			String destIP = request.substring(22, 37);
			String destPort = request.substring(38, 43);
			_seqNum = request.substring(44, 45);
			String payload = request.substring(48);

			// Create packet.
			packet = new UDPPacket(destPort, destIP, srcPort, srcIP, Integer.parseInt(_seqNum));
			packet.makePacket(payload);

		} catch (SocketTimeoutException e)   // Socket timeout,
		{
			System.err.println("Unable to receive message from server, it's timeout.");
			return null;
		}catch (IOException ex) {
			System.err.println("Unable to receive message from server: " + ex);
			return null;
		}

		return packet;
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
	public void StartRequest() throws IOException /*throws IOException*/ {

		int requestSize = _request.length(); // Size of request from user.
		int subRequestSize = 6;		// size of sub-request for each packet.
		int start = -1;
		int index = 0;
		int pos = subRequestSize;
		int packetNum = 0;  		// Packet number
		int seqNum = 0; 			// Sequence number of packet
		int prevSeqNum = 1;    		// Previous sequence number of the packet
		boolean delayed = false;  	// Boolen for delay
		UDPPacket sendPacket = null;	// sended packet
		UDPPacket rcvPacket = null;		// Received packet
		String msg = "";			//Final message

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
				}else {
					index += subRequestSize;
					start += subRequestSize;
				}
			}else {
				req = _request;
				requestSize -= req.length();
			}
			//Execute only if packet is not sent
			if(!delayed)
			{
				// make packet.
				sendPacket = new UDPPacket(_srcPort, _srcHost, _rcvPort,
						_rcvHost, seqNum);
				sendPacket.makePacket(req);
				_packetOut = new byte[BUFFER_SIZE];
				_packetOut = sendPacket.getSegment();
                msg += req;
			}

			delayed = false;

			// Sending the packet.
			if (sendRequest() < 0) {
				closeSocket();
				return;
			}
			packetNum += 1; 		//increment the packet number

			// Receive the response
			rcvPacket = receiveResponse();

			//Time out, re-send packet
			while(rcvPacket == null)
			{
				System.out.println("The packet number: " + packetNum + " Timeout, re-sending");
				packetNum -= 1;
				if (sendRequest() < 0) {
					return;
				}
				rcvPacket = receiveResponse();
			}
			String rcvCheckSum = rcvPacket.generateChecksum(rcvPacket.getPayload());
			String sendCheckSum = sendPacket.generateChecksum(sendPacket.getPayload());
			_ack = rcvPacket.validateMessage();

			// If packet is corrupt, resend the packet.
            if (!rcvCheckSum.equals(sendCheckSum)) {
                if (Objects.equals(rcvPacket.getSequence(), sendPacket.getSequence())) {
                    System.out.println("The packet number: " + packetNum + " CORRUPT, re-sending");
					packetNum -= 1;
                    // re-sending the packet.
                    if (sendRequest() < 0) {
                        return;
                    }
                } else {
                    System.out.println("Recieved " + _ack + " for packet " + packetNum);
                }
            } else {
                System.out.println("Recieved " + _ack + " for packet " + packetNum);
            }

           if ((!rcvPacket.isLastMessage && requestSize > 0 )) {
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
	 * Flip the sequence number.
	 */
	public Integer getSequenceNum(int seqNum) {
		if (seqNum == 0) {
			seqNum = 1;
		}
		else seqNum = 0;
		return  seqNum;
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
		//String request = "Hello world!";
		System.out.println("Sending the packet to: " + rcvHost + " " + rcvPort);
		sender.SetRequest(request + ".");
		// read input from user.
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
