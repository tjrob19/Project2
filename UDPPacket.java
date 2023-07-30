import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * UDP Packet object.
 */
public class UDPPacket {

    private static final int SEGMENT_SIZE = 54;
    private int _srcPort = 0;   // sender port number
    private String _srcHost = ""; // sender IP address
    private int _rcvPort = 0;  // server port number
    private String _rcvHost = ""; // server IP address

    private String _request; // request from user
    private int _seq;   // sequence 0 or 1
    private String _ack;    // acknowledgement
    private byte[] _segment;

    /**
     * Constructs a UDPSender object.
     */
    UDPPacket(int srcPort, String srcHost,
              int rcvPort, String rcvHost,
              String request,int seq, String ack)
    {
        this._srcPort = srcPort;
        this._srcHost = srcHost;
        this._rcvPort = rcvPort;
        this._rcvHost = rcvHost;
        this._request = request;
        this._seq = seq;
        this._ack = ack;
        this._segment = new byte[SEGMENT_SIZE];
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
     * Get the request.
     */
    public String getRequest(){
        return _request;
    }

    /*
     * Get the sequence.
     */
    public int getSequence(){
        return _seq;
    }

    /*
     * Get acknowledgement.
     */
    public String getAck(){
        return _ack;
    }

    /*
     * Make the packet;
     */
    public byte[] makePacket() throws UnknownHostException {

        // copy message into data
        byte data[] = _request.getBytes();
        byte[] srcPort = intToBytes(_srcPort, 6);
        byte[] rcvPort = intToBytes(_rcvPort, 6);
        byte[] srcHost = byteToByteBuffer(_srcHost.getBytes(), 16);
        byte[] rcvHost = byteToByteBuffer(_rcvHost.getBytes(), 16);
        byte[] seqNum = new byte[]{(byte) _seq};

        System.arraycopy(srcHost, 0, _segment, 0, srcHost.length);
        System.arraycopy(srcPort, 0, _segment, 16, srcPort.length);
        System.arraycopy(rcvHost, 0, _segment, 22, rcvHost.length);
        System.arraycopy(rcvPort, 0, _segment, 38, rcvPort.length);
        System.arraycopy(data, 0, _segment, 44, data.length);
        System.arraycopy(seqNum, 0, _segment, 53, seqNum.length);

        return _segment;
    }

    /*
     * convert integer to bytes.
     * @param num - integer number.
     * @param capacity - capacity to allocate the byte buffer.
     */
    public byte[] intToBytes(int num, int capacity) {
        return ByteBuffer.allocate(capacity).
                order(ByteOrder.BIG_ENDIAN).putInt(num).array();
    }

    /*
     * add the buffer to byte array.
     * @param b - byte array.
     * @param capacity - capacity to allocate the byte buffer.
     */
    public byte[] byteToByteBuffer(byte[] b, int capacity) {
        return ByteBuffer.allocate(capacity).
                order(ByteOrder.BIG_ENDIAN).put(b).array();
    }

    @Override
    public String toString() {

        return "Packet {\n" +
                "\t\t\t[Src port   = " + _srcPort + "]\n" +
                "\t\t\t[Dest port = " + _rcvPort + "]\n" +
                "\t\t\t[Seq num    = " + _seq + "]\n" +
                "\t\t\t[Ack num    = " + _ack+ "]\n" +
                "\t\t\t[Segment ("+ _segment.length +" bytes) = " +
                Arrays.toString(_segment) + "]\n" +
                "\t}";
    }

    /** Test the UDP packet */
    public static void main(String[] args) {

        UDPPacket packet = new UDPPacket(60000, "127.0.0.1",
                60100,"localhost","hello wor",0 , "ACK)");
        try {
            byte[] pSegment = packet.makePacket();
            System.out.println(packet);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


