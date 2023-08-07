import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * This program creates packets with information provided by other classes
 *
 * @author Chakrya Ros
 * @author Trevor Robinson
 * @date 8/6/2023
 * @info Course COP5518
 */
public class UDPPacket {

    private static final int PACKET_SIZE = 54;
    private String _srcPort;  // sender port number
    private String _srcHost;  // sender IP address
    private String _rcvPort;  // server port number
    private String _rcvHost;  // server IP address
    private String _request;  // message
    public  int    _seqNum;   // sequence 0 or 1
    private String _checksum; // checksum value
    private byte[] _segment;  // store header
    public boolean isLastMessage;  // check for end of message


    /**
     * Constructs a UDPPacket object
     */
    UDPPacket(String srcPort, String srcHost, String rcvPort, String rcvHost, int seqNum)
    {
        this._srcPort = srcPort;
        this._srcHost = make16ByteString(srcHost);
        this._rcvPort = rcvPort;
        this._rcvHost = make16ByteString(rcvHost);
        this._seqNum = seqNum;
    }

    // Get the sequence
    public Integer getSequence(){
        return _seqNum;
    }

    // Get the segment
    public byte[] getSegment(){
        return _segment;
    }

    /*
     * Generate checksum
     *
     * @param s: sender message.
     * @return the integer of checksum.
     */
    public String generateChecksum(String s) {

        // Local variables
        int charToInt;
        int sum = 0;

        // If message is null, don't calculate
        if(s == null) return null;

        // Loop to add values to variable
        for (int i = 0; i < s.length(); i++) {
            charToInt = (int) s.charAt(i);
            sum = sum + charToInt;

            // Set the end of message
            if ((int) s.charAt(i) == 46) {
                isLastMessage = true;
            }
        }
        
        // Convert to String and make it three byte.
        String checkSum = Integer.toString(sum);
        if (checkSum.length() > 3) {
            return checkSum.substring(0,2);
        } else if (checkSum.length() == 2) {
            return  checkSum + '0';
        } else {
            return checkSum;
        }
    }

    /*
     * Check if the message is corrupt or not.
     *
     * @return the acknowledgment
     */
    public String validateMessage() {
        
        // Calculate checksum & generate ack
        String newChecksum = generateChecksum(_request);
        String ack;

        if (newChecksum.equals(_checksum)) {
            ack = "ACK" + _seqNum;
            return ack;
        } else {
            if (Objects.equals(_seqNum, 0)) {
                _seqNum = 1;
                ack =  "ACK" + _seqNum;
            }
            else {
                _seqNum = 0;
                ack =  "ACK" + _seqNum;
            }
        }
        return ack;
    }

    /*
     * Make the packet using the user request
     *
     * @param request - user request
     */
    public void makePacket(String request) {
        _segment = new byte[PACKET_SIZE];

        // empty message into buffer
        for (int i = 0; i < PACKET_SIZE; i++) {
            _segment[i] = '\0';
        }

        _request = request;     // user
        _checksum = generateChecksum(request);  // Generate the checksum
        Charset charset = StandardCharsets.US_ASCII;
        String srcIP_Port = _srcHost + " " + _srcPort + " "; // Combine source IP and port to single string
        String destIP_Port = _rcvHost + " " + _rcvPort + " "; // Combine destination IP and port to single string
        String seqNumStr = Integer.toString(_seqNum);  // Convert sequence number to string
        String headerString = seqNumStr + _checksum + _request;

        // Convert to byte array
        byte[] srcPort = srcIP_Port.getBytes(charset);
        byte[] destPort = destIP_Port.getBytes(charset);
        byte [] header = headerString.getBytes(charset);

        // Copy to array
        System.arraycopy(srcPort, 0, _segment, 0, srcPort.length);
        System.arraycopy(destPort, 0, _segment, 22, destPort.length);
        System.arraycopy(header, 0, _segment, 44, header.length);

    }


    /*
     * Create host name to 16 byte string.
     *
     * @param str - host name.
     * @return - new 16 byte string.
     */
    private String make16ByteString(String str){
        
        // Local variables
        int    sizeStr = str.length();
        String hostName = GetHostName(str); // Make sure it's not "localhost"
        String second = "000.000.00";
        String newStr = "";
        
        // Check size
        if (sizeStr == 15 || sizeStr > 16)
        {
            return str.substring(0, 15);
        } else if (sizeStr == 9) {
            String first = hostName.substring(10, 14);
            String third = hostName.substring(18);
            newStr =  first + second + third;
        }
        return newStr;
    }

    /*
     * Convert localhost to numeric string
     *
     * @param hostname - host name.
     * @return - hostname as numeric string.
     */
    public String GetHostName(String hostname) {
        InetAddress hostAddr;
        if("localhost".equals(hostname))
        {
            try {
                hostAddr = InetAddress.getByName(hostname);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            return hostAddr.toString();
        }
        return hostname;
    }

    /*
     * Display packet information
     *
     * @Override
     */
    public String toString() {
        return "Packet {\t" + "\n[Seq num    = " + _seqNum + "]\n" +
                "\t[Check sum    = " + _checksum+ "]\n" +
                "\t[Request    = " + _request + "]\n" +
                "\t[Segment ("+ _segment.length +" bytes)]\n" +
                "\t}";
    }

    /** 
     * Test the UDP packet 
     *
     * @param args - user provided arguments
     */
    public static void main(String[] args) {

        // Tests the UDPPacket methods
        String request = "hello wo";
        UDPPacket packet = new UDPPacket("60000", "localhost",
                                         "60100","localhost", 0);
        try {
            packet.makePacket(request);
            System.out.println(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}




