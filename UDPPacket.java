import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.Checksum;
import java.util.zip.CRC32;

/**
 * UDP Packet object.
 *
 * @author Chakrya Ros
 * @author Trevor Robinson
 */
public class UDPPacket implements Serializable {

    private static final int PACKET_SIZE = 54;
    private String _srcPort;   // sender port number
    private String _srcHost = ""; // sender IP address
    private String _rcvPort;  // server port number
    private String _rcvHost = ""; // server IP address

    private String _request;

    public int _seqNum;   // sequence 0 or 1
    private String _ack;    // acknowledgement

    private int _checksum;
    private byte[] _segment;

    public boolean isLastMessage;


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
     * Corrupting the packet.
     */
    public void requestCorrupt(){
        this._request = this._request +"C";
    }

    /*
     * Send the request successful.
     */
    public void requestSuccess(){
        this._request = this._request.substring(0, this._request.length()-1);
    }


    /*
     * Get the sequence.
     */
    public Integer getSequence(){
        return _seqNum;
    }

    /*
     * Get acknowledgement.
     */
    public String getAck(){
        return _ack;
    }

    /*
     * Generate checksum
     *
     * @param s: sender message.
     * @return the integer of checksum.
     */
    public Integer generateChecksum(String s) {
        int charToInt;
        int sum = 0;
        if(s == null) return -1;
        for (int i = 0; i < s.length(); i++) {
            charToInt = (int) s.charAt(i);
            sum = sum + charToInt;
            if ((int) s.charAt(i) == 46) {
                isLastMessage = true;
            }
        }
        return sum;
    }

    /*
     * Check if the message is corrupt or not.
     *
     * @return the acknowledgment.
     */
    public String validateMessage() {
        Integer newChecksum = generateChecksum(_request);
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
     * Flip the sequence number.
     */
    public void getSequenceNum() {
        if (_seqNum == 0) {
            _seqNum = 1;
        }
        else {
            _seqNum = 0;
        }
    }

    /*
     * Make the packet;
     */
    public byte[] makePacket(String request) {
        _segment = new byte[PACKET_SIZE];

        // empty message into buffer
        for (int i = 0; i < PACKET_SIZE; i++) {
            _segment[i] = '\0';
        }

        _request = request;
        _checksum = generateChecksum(_request);  // generate the checksum
        Charset charset = StandardCharsets.US_ASCII;
        String srcIP_Port = _srcHost + " " + _srcPort + " "; // Combine source IP and port to single string.
        String destIP_Port = _srcHost + " " + _rcvPort + " "; // Combine destination IP and port to single string.
        String seqNumStr = Integer.toString(_seqNum);  //Convert sequence number to string.
        String checksum = Integer.toString(_checksum); //Convert checksum number to string.
        String payload = seqNumStr + checksum + _request;

        // convert to byte array.
        byte[] srcPort = srcIP_Port.getBytes(charset);
        byte[] destPort = destIP_Port.getBytes(charset);
        byte [] header = payload.getBytes(charset);

        //Copy to array
        System.arraycopy(srcPort, 0, _segment, 0, srcPort.length);
        System.arraycopy(destPort, 0, _segment, 22, destPort.length);
        System.arraycopy(header, 0, _segment, 44, header.length);

        return _segment;
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
        if(sizeStr == 16)
        {
            return str;
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

        return "Packet {\n" +
                "\t\t\t[Src port   = " + _srcPort + "]\n" +
                "\t\t\t[Dest port = " + _rcvPort + "]\n" +
                "\t\t\t[Seq num    = " + _seqNum + "]\n" +
                "\t\t\t[Ack num    = " + _ack+ "]\n" +
                "\t\t\t[Check sum    = " + _checksum+ "]\n" +
                "\t\t\t[Segment ("+ _segment.length +" bytes) = " +
                Arrays.toString(_segment) + "]\n" +
                "\t}";
    }

    /** Test the UDP packet */
    public static void main(String[] args) {

        String request = "hello wo";
        long checksum = CheckSum.CalculateChecksum(request.getBytes());
        UDPPacket packet = new UDPPacket("60000", "localhost",
                "60100","localhost", 0);
        try {
            byte[] pSegment = packet.makePacket(request);
            System.out.println(packet.generateChecksum(request));
            System.out.println(packet);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}




