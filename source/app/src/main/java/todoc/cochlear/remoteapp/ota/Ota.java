package todoc.cochlear.remoteapp.ota;

import android.widget.TextView;

import todoc.cochlear.remoteapp.params.PacketInfo;

public class Ota
{
    // Constants
    static public final String OTA_PATH = "/OTA/";

    static public final String FILE_NAME_MANIFEST = "MANIFEST.TXT";
    static public final String FILE_NAME_APP000   = "APP000.FEZ";
    static public final String FILE_NAME_APP001   = "APP001.FEZ";
    static public final String FILE_NAME_APP002   = "APP002.FEZ";
    static public final String FILE_NAME_STATUS   = "STATUS.BIN";

    static public final int THREAD_STATE_IDLE = 0;
    static public final int THREAD_STATE_BUSY = 1;

    static public final int PARAM_NONE     = 0;
    static public final int PARAM_MANIFEST = 1;
    static public final int PARAM_APP000   = 2;
    static public final int PARAM_APP001   = 3;
    static public final int PARAM_APP002   = 4;
    static public final int PARAM_STATUS   = 12345;

    static public final int PRINT_LOG_HEX_LENGTH = 48;

    // Instance
    private static final Ota mInstance = new Ota();

    // Members
    public OtaFile mFile_manifest;
    public OtaFile mFile_app000;
    public OtaFile mFile_app001;
    public OtaFile mFile_app002;
    public OtaFile mFile_status;

    public int mThread_state;
    public int mThread_param;

    static public final int COMM_STATE_IDLE              = 0;
    static public final int COMM_STATE_READY_COMMAND     = 1;
    static public final int COMM_STATE_SEND_COMMAND      = 2;
    static public final int COMM_STATE_WAIT_RESP_COMMAND = 3;
    static public final int COMM_STATE_READY_DATA        = 4;
    static public final int COMM_STATE_SEND_DATA         = 5;
    static public final int COMM_STATE_WAIT_RESP_DATA    = 6;

    static public final int COMM_RW_WRITE = 1;
    static public final int COMM_RW_READ  = 2;

    static public final int COMM_RESP_OK   = 1;
    static public final int COMM_RESP_FAIL = 2;

    // 무선 프로토콜 관련
    public int mCommState;
    public int mCommDataIndex;
    public int mCommFileType;
    public int mCommTotalBytes;
    public int mCommLastPacketIndex;
    public int mCommRw;
    public int mCommLastPacket_remainedBytes;

    public int mCommSendDataIndex;
    public int mCommRespDataIndex;

    public int mCommCurrDfu_BufferIndex;

    // Constructor
    private Ota()
    {
        mThread_state = THREAD_STATE_IDLE;
        mThread_param = PARAM_NONE;
    }

    // Get instance
    public static Ota getInstance()
    {
        return mInstance;
    }

    public OtaFile getOtaFile(int param)
    {
        OtaFile otaFile;

        switch (param)
        {
            case PARAM_MANIFEST:
                otaFile = mFile_manifest;
                break;
            case PARAM_APP000:
                otaFile = mFile_app000;
                break;
            case PARAM_APP001:
                otaFile = mFile_app001;
                break;
            case PARAM_APP002:
                otaFile = mFile_app002;
                break;
            case PARAM_STATUS:
                otaFile = mFile_status;
                break;
            default:
                otaFile = null;
        }

        return otaFile;
    }

    public void writeOtaFile(int param, int index, int length, boolean readDone, int total)
    {
        OtaFile otaFile = getOtaFile(param);

        if (otaFile != null)
        {
            otaFile.mIndex = index;
            otaFile.mLength = length;
            otaFile.mReadDone = readDone;
            //mBufferIndex = total;
        }
    }

    public byte[] prepare_dfuCommPacket(int param)
    {
        OtaFile otaFile = getOtaFile(param);
        byte[]  packet;

        if (otaFile == null)
        {
            return null;
        }

        otaFile.mSendDone = false;
        otaFile.mIndex = 0;

        mCommState = COMM_STATE_READY_COMMAND;
        mCommDataIndex = 0;
        mCommFileType = otaFile.mParam;
        mCommTotalBytes = otaFile.mLength;
        mCommCurrDfu_BufferIndex = otaFile.mIndex;
        // 정수 올림 나눗셈 방식
        mCommLastPacketIndex = (mCommTotalBytes + PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT - 1) / PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT;
        mCommRw = COMM_RW_WRITE;
        mCommLastPacket_remainedBytes = mCommTotalBytes % PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT;

        mCommSendDataIndex = 0;
        mCommRespDataIndex = 0;

        packet = new byte[PacketInfo.PACKET_SIZE_OTA_SEND_COMMAND];

        packet[0] = PacketInfo.HEADER_OTA; // Header
        packet[1] = (byte) ((mCommDataIndex >> 16) & 0xFF); // Data index (MSB to LSB)
        packet[2] = (byte) ((mCommDataIndex >> 8) & 0xFF);
        packet[3] = (byte) (mCommDataIndex & 0xFF);
        packet[4] = (byte) ((mCommFileType >> 8) & 0xFF); // File Type (MSB to LSB)
        packet[5] = (byte) (mCommFileType & 0xFF);
        packet[6] = (byte) ((mCommTotalBytes >> 24) & 0xFF); // Total Bytes (MSB to LSB)
        packet[7] = (byte) ((mCommTotalBytes >> 16) & 0xFF);
        packet[8] = (byte) ((mCommTotalBytes >> 8) & 0xFF);
        packet[9] = (byte) (mCommTotalBytes & 0xFF);
        packet[10] = (byte) ((mCommLastPacketIndex >> 24) & 0xFF); // Last Packet Index (MSB to LSB)
        packet[11] = (byte) ((mCommLastPacketIndex >> 16) & 0xFF);
        packet[12] = (byte) ((mCommLastPacketIndex >> 8) & 0xFF);
        packet[13] = (byte) (mCommLastPacketIndex & 0xFF);
        packet[14] = (byte) (mCommLastPacket_remainedBytes & 0xFF);
        packet[15] = (byte) (mCommRw & 0xFF);

        // 0x80 00 00 00 00 02 00 00 5B EC 00 00 05 BF 0C 01

        return packet;
    }

    public static class OtaFile
    {
        public boolean  mReadDone;
        public boolean  mSendDone;
        public byte[]   mBuffer;
        public int      mLength;
        public int      mIndex;
        public int      mParam;
        public String   mName;
        public String   mPath;
        public TextView mTv_readPercent;
        public TextView mTv_sendPercent;

        // Constructor
        public OtaFile(String name, int param, int length)
        {
            mReadDone = false;
            mSendDone = false;
            mBuffer = new byte[length];
            mLength = length;
            mIndex = 0;
            mName = name;
            mParam = param;
        }
    }
}
