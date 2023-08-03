
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
		this._port = port;
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
        	
        	byte[] rcvPortBytes = new byte[6];
        	System.arraycopy(buffer, 38, rcvPortBytes, 0, 6);
        	destPort = ByteBuffer.wrap(rcvPortBytes).order(ByteOrder.BIG_ENDIAN).getInt(); // Convert to int

		} catch (IOException ex) {
			System.err.println("unable to receive message from server");
			return null;
		}

		return newDatagramPacket;
	}
  
	public void run()
	{

		DatagramPacket newDatagramPacket = receiveRequest();
		System.out.println ("Destination IP: " + destIP);
		System.out.println ("sender request: " + destPort);
		newDatagramPacket.setPort(destPort);
		newDatagramPacket.setAddress(_destIP);
				
				try {
					_socket.send(newDatagramPacket);
				} catch (IOException ex) {
					System.err.println("unable to send message to server");
				}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
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
		
		test network = new test(portNum);
		if (network.createSocket() < 0) {
			return;
		}
		
		network.createSocket();
		network.run();
		network.closeSocket();
		
	}

}
