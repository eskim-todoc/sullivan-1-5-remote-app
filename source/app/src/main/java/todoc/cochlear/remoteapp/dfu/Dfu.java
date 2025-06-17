package todoc.cochlear.remoteapp.dfu;

import todoc.cochlear.remoteapp.params.PacketInfo;

public class Dfu
{
    // Constants
    static public final String FILE_MANIFEST = "/DFU/bin_MANIFEST.bin";
    static public final String FILE_APP000   = "/DFU/bin_APP000.bin";
    static public final String FILE_APP001   = "/DFU/bin_APP001.bin";
    static public final String FILE_APP002   = "/DFU/bin_APP002.bin";
    static public final String FILE_STATUS   = "/DFU/ota_status.bin";

    static public final int THREAD_STATE_IDLE = 0;
    static public final int THREAD_STATE_BUSY = 1;

    static public final int THREAD_PARAM_NONE     = 0;
    static public final int THREAD_PARAM_MANIFEST = 1;
    static public final int THREAD_PARAM_APP000   = 2;
    static public final int THREAD_PARAM_APP001   = 3;
    static public final int THREAD_PARAM_APP002   = 4;
    static public final int THREAD_PARAM_STATUS   = 12345;

    static public final int PRINT_LOG_HEX_LENGTH = 48;

    // Instance
    private static final Dfu mInstance = new Dfu();

    // Members
    public byte[] mBuffer; // 2MB
    public int    mBufferIndex;

    public DfuInfo mManifest;
    public DfuInfo mApp000;
    public DfuInfo mApp001;
    public DfuInfo mApp002;
    public DfuInfo mStatus;

    public int mThreadState;
    public int mThreadParam;

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
    private Dfu()
    {
        mBuffer = new byte[2 * 1024 * 1024]; // 2MB
        mBufferIndex = 0;

        mManifest = new DfuInfo(FILE_MANIFEST, THREAD_PARAM_MANIFEST);
        mApp000 = new DfuInfo(FILE_APP000, THREAD_PARAM_APP000);
        mApp001 = new DfuInfo(FILE_APP001, THREAD_PARAM_APP001);
        mApp002 = new DfuInfo(FILE_APP002, THREAD_PARAM_APP002);
        mStatus = new DfuInfo(FILE_STATUS, THREAD_PARAM_STATUS);

        mThreadState = THREAD_STATE_IDLE;
        mThreadParam = THREAD_PARAM_NONE;
    }

    // Get instance
    public static Dfu getInstance()
    {
        return mInstance;
    }

    public DfuInfo getDfuInfo(int file)
    {
        DfuInfo dfuInfo;

        switch (file)
        {
            case THREAD_PARAM_MANIFEST:
                dfuInfo = mManifest;
                break;
            case THREAD_PARAM_APP000:
                dfuInfo = mApp000;
                break;
            case THREAD_PARAM_APP001:
                dfuInfo = mApp001;
                break;
            case THREAD_PARAM_APP002:
                dfuInfo = mApp002;
                break;
            case THREAD_PARAM_STATUS:
                dfuInfo = mStatus;
                break;
            default:
                dfuInfo = null;
        }

        return dfuInfo;
    }

    public void writeDfuInfo(int file, int index, int length, boolean readDone, int total)
    {
        DfuInfo dfuInfo;

        switch (file)
        {
            case THREAD_PARAM_MANIFEST:
                dfuInfo = mManifest;
                break;
            case THREAD_PARAM_APP000:
                dfuInfo = mApp000;
                break;
            case THREAD_PARAM_APP001:
                dfuInfo = mApp001;
                break;
            case THREAD_PARAM_APP002:
                dfuInfo = mApp002;
                break;
            case THREAD_PARAM_STATUS:
                dfuInfo = mStatus;
                break;
            default:
                return;
        }

        dfuInfo.index = index;
        dfuInfo.length = length;
        dfuInfo.readDone = readDone;
        mBufferIndex = total;
    }

    public byte[] prepare_dfuCommPacket(int param)
    {
        DfuInfo dfuInfo = getDfuInfo(param);
        byte[]  packet;

        if (dfuInfo == null)
        {
            return null;
        }

        mCommState = COMM_STATE_READY_COMMAND;
        mCommDataIndex = 0;
        mCommFileType = dfuInfo.fileType;
        mCommTotalBytes = dfuInfo.length;
        mCommCurrDfu_BufferIndex = dfuInfo.index;
        // 정수 올림 나눗셈 방식
        mCommLastPacketIndex = (mCommTotalBytes + PacketInfo.PACKET_SIZE_DFU_SEND_DATA_UNIT - 1) / PacketInfo.PACKET_SIZE_DFU_SEND_DATA_UNIT;
        mCommRw = COMM_RW_WRITE;
        mCommLastPacket_remainedBytes = mCommTotalBytes % PacketInfo.PACKET_SIZE_DFU_SEND_DATA_UNIT;

        mCommSendDataIndex = 0;
        mCommRespDataIndex = 0;

        packet = new byte[PacketInfo.PACKET_SIZE_DFU_SEND_COMMAND];

        packet[0] = PacketInfo.HEADER_DFU; // Header
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

    public static class DfuInfo
    {
        public boolean readDone;
        public int     index;
        public int     length;
        public String  name;
        public int     fileType;

        // Constructor
        private DfuInfo(String name, int fileType)
        {
            readDone = false;
            index = 0;
            length = 0;
            this.name = name;
            this.fileType = fileType;
        }
    }
}
