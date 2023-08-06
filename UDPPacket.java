import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * UDP Packet object.
 *
 * @author Chakrya Ros
 * @author Trevor Robinson
 */
public class UDPPacket {

    private static final int PACKET_SIZE = 54;
    private String _srcPort;   // sender port number
    private String _srcHost; // sender IP address
    private String _rcvPort;  // server port number
    private String _rcvHost; // server IP address
    private String _request;
    public int _seqNum;   // sequence 0 or 1
    private String _checksum;  //CheckSum
    private byte[] _segment;  //Store header

    public boolean isLastMessage;  //Check for end of message


    /**
     * Constructs a UDPSender object.
     */
    UDPPacket(String srcPort, String srcHost,
              String rcvPort, String rcvHost, int seqNum)
    {
        this._srcPort = srcPort;
        this._srcHost = make16ByteString(srcHost);
        this._rcvPort = rcvPort;
        this._rcvHost = make16ByteString(rcvHost);
        this._seqNum = seqNum;
    }

    /*
     * Get the sequence.
     */
    public Integer getSequence(){
        return _seqNum;
    }

    /*
     * Get segment.
     */
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
        int charToInt;
        int sum = 0;
        if(s == null) return null;
        for (int i = 0; i < s.length(); i++) {
            charToInt = (int) s.charAt(i);
            sum = sum + charToInt;

            // Set the end of message
            if ((int) s.charAt(i) == 46) {
                isLastMessage = true;
            }
        }
        //Convert to String and make it three byte.
        String checkSum = Integer.toString(sum);
        if(checkSum.length() > 3)
        {
            return checkSum.substring(0,2);
        }else if(checkSum.length() == 2)
        {
            return  checkSum + '0';
        }else {
            return checkSum;
        }
    }

    /*
     * Check if the message is corrupt or not.
     *
     * @return the acknowledgment.
     */
    public String validateMessage() {
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
     * Make the packet;
     */
    public void makePacket(String request) {
        _segment = new byte[PACKET_SIZE];

        // empty message into buffer
        for (int i = 0; i < PACKET_SIZE; i++) {
            _segment[i] = '\0';
        }

        _request = request;     // user
        _checksum = generateChecksum(request);  // generate the checksum
        Charset charset = StandardCharsets.US_ASCII;
        String srcIP_Port = _srcHost + " " + _srcPort + " "; // Combine source IP and port to single string.
        String destIP_Port = _rcvHost + " " + _rcvPort + " "; // Combine destination IP and port to single string.
        String seqNumStr = Integer.toString(_seqNum);  //Convert sequence number to string.
        String headerString = seqNumStr + _checksum + _request;

        // convert to byte array.
        byte[] srcPort = srcIP_Port.getBytes(charset);
        byte[] destPort = destIP_Port.getBytes(charset);
        byte [] header = headerString.getBytes(charset);

        //Copy to array
        System.arraycopy(srcPort, 0, _segment, 0, srcPort.length);
        System.arraycopy(destPort, 0, _segment, 22, destPort.length);
        System.arraycopy(header, 0, _segment, 44, header.length);

    }


    /*
     * Create host name to 16 byte string.
     * @param str - host name.
     * @return - new 16 byte string.
     */
    private String make16ByteString(String str){
        int sizeStr = str.length();
        String hostName = GetHostName(str); // Make sure it's not "localhost"
        String second = "000.000.00";
        String newStr = "";
        if(sizeStr == 15 || sizeStr > 16)
        {
            return str.substring(0, 15);
        }else if (sizeStr == 9)
        {
            String first = hostName.substring(10, 14);
            String third = hostName.substring(18);
            newStr =  first + second + third;
        }
        return newStr;
    }

    /*
     * Convert localhost to numeric string.
     * @param str - host name.
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

    @Override
    public String toString() {

        return "Packet {\t" + "\n[Seq num    = " + _seqNum + "]\n" +
                "\t[Check sum    = " + _checksum+ "]\n" +
                "\t[Request    = " + _request + "]\n" +
                "\t[Segment ("+ _segment.length +" bytes)]\n" +
                "\t}";
    }

    /** Test the UDP packet */
    public static void main(String[] args) {

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




