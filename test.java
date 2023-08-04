
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.*;

public class test {

	private static final int BUFFER_SIZE = 54;
	private DatagramSocket _socket;
	private int _port;
	private boolean _continueService;
	private String destIP;
	private int destPort;
	InetAddress _destIP;
	
	// Overload constructor for network object
	public test (int port){
		_port = port;
	}
	
	public int createSocket() {
		try {
			_socket = new DatagramSocket(_port);
		} catch (SocketException ex) {
			System.err.println("unable to create and bind socket");
			return -1;
		}

		return 0;
	}
	
	public int closeSocket() {
		_socket.close();

		return 0;
	}
	
	// public int sendToDest() {

	// 	DatagramPacket newDatagramPacket = createDatagramPacket(_packet, _rcvHost, _rcvPort);
	// 	if (newDatagramPacket != null) {
	// 		try {
	// 			_socket.send(newDatagramPacket);
	// 		} catch (IOException ex) {
	// 			System.err.println("unable to send message to server");
	// 			return -1;
	// 		}
	// 		return 0;

	// 	}
	// 	System.err.println("unable to send message to server");
	// 	return -1;
	// }
	
	public DatagramPacket receiveRequest() {
		byte[] buffer = new byte[BUFFER_SIZE];
		DatagramPacket newDatagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);
		try {
			_socket.receive(newDatagramPacket);

			// Extract rcvHost and rcvPort from the buffer array
        	byte[] rcvHostBytes = new byte[16];
        	System.arraycopy(buffer, 22, rcvHostBytes, 0, 16);
        	destIP = new String(rcvHostBytes).trim(); // Convert to string and trim to remove trailing zeros
        	InetAddress _destIP = InetAddress.getByName(destIP);
			System.out.println("Dest IP: " + _destIP);
        	
        	byte[] rcvPortBytes = new byte[6];
        	System.arraycopy(buffer, 38, rcvPortBytes, 0, 6);
        	destPort = ByteBuffer.wrap(rcvPortBytes).order(ByteOrder.BIG_ENDIAN).getInt(); // Convert to int
			System.out.println("Dest port: " + destPort);

		} catch (IOException ex) {
			System.err.println("unable to receive message from server");
			return null;
		}

		return newDatagramPacket;
	}
  
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

	public void run()
	{

		_continueService = true;
		while (_continueService) {

			DatagramPacket newDatagramPacket = receiveRequest();
			String request = new String (newDatagramPacket.getData()).trim();
			String segment = request.substring(43);
			System.out.println ("Destination IP: " + destIP);
			System.out.println ("sender request: " + destPort);
			newDatagramPacket.setPort(destPort);
			//newDatagramPacket.setAddress(_destIP);
			System.out.println("Forwarding packet with '" + segment + "' to following address (IP, PORT): " + destIP + 
						   		", " + destPort);
				
			DatagramPacket sendDatagramPacket = createDatagramPacket(request, destIP, destPort);	
				try {
					_socket.send(newDatagramPacket);
				} catch (IOException ex) {
					System.err.println("unable to send message to server");
				}
		}
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		if (args.length != 1) {
			System.err.println("Usage: UDPNetwork <port number>\n");
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
		
		test network = new test(portNum);
		if (network.createSocket() < 0) {
			return;
		}
		
		network.run();
		network.closeSocket();
		
	}

}
