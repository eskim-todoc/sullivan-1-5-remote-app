package todoc.cochlear.remoteapp.ota;

import java.util.ArrayList;

public class Ota
{
    // Constants
    static public final String BASE_FOLDER = "/OTA/";

    static public final String FILE_NAME_MANIFEST = "MANIFEST.TXT";
    static public final String FILE_NAME_APP000   = "APP000.FEZ";
    static public final String FILE_NAME_APP001   = "APP001.FEZ";
    static public final String FILE_NAME_APP002   = "APP002.FEZ";

    static public final int THREAD_STATE_IDLE = 0;
    static public final int THREAD_STATE_BUSY = 1;

    static public final int FILE_NUM_MFST = 1;
    static public final int FILE_NUM_APP0 = 2;
    static public final int FILE_NUM_APP1 = 3;
    static public final int FILE_NUM_APP2 = 4;

    static public final int PRINT_LOG_HEX_LENGTH = 48;

    static public final int SLOT_NUM_1 = 1;
    static public final int SLOT_NUM_2 = 2;

    /* 수집한 파일을 담아 두는 자리.
     *
     * 예전에는 «슬롯 1 폴더에서 읽어 슬롯 1 에 쓴다» 였다. 그래서 파일 버퍼도 슬롯마다
     * 따로 있었다. 지금은 어느 폴더에서 읽든 어느 슬롯에도 쓸 수 있으므로 그 묶임을 끊었다.
     *
     * 읽은 것은 슬롯과 무관한 이 한 벌에 담고, 쓸 때 목표 슬롯만 갈아 끼운다.
     * 슬롯 번호와 겹치지 않게 0 을 쓴다. */
    static public final int SLOT_NUM_COLLECT = 0;

    static public final int COMM_STATE_IDLE              = 0;
    static public final int COMM_STATE_PREPARE_COMMAND   = 1;
    static public final int COMM_STATE_WAIT_RESP_COMMAND = 2;
    static public final int COMM_STATE_PREPARE_DATA      = 3;
    static public final int COMM_STATE_WAIT_RESP_DATA    = 4;

    static public final int COMM_RW_WRITE = 1;
    static public final int COMM_RW_READ  = 2;

    static public final int COMM_RESP_OK   = 1;
    static public final int COMM_RESP_FAIL = 2;

    // Instance
    private static final Ota mInstance = new Ota();

    public int currentSlotNum;
    public int currentFileNum;

    /* 마지막으로 수집한 폴더의 이름. BASE_FOLDER 아래의 한 칸이다.
     * 비어 있으면 아직 아무것도 수집하지 않은 상태다. */
    public String collectFolderName;

    // Members
    public static ArrayList<OtaFile> fileList;

    public int threadState;

    // 무선 프로토콜 관련
    public int    commState;
    public int    commDataIndex;
    public int    commSlotNum;
    public int    commFileNum;
    public int    commOption;
    public int    commTotalByte;
    public int    commEndDataIndexNum;
    public int    commEndDataIndexByte;
    public int    commBufferIndex;
    public byte[] commBuffer;

    // Constructor
    private Ota()
    {
        threadState = THREAD_STATE_IDLE;

        currentSlotNum = -1;
        currentFileNum = -1;

        collectFolderName = "";

        commState = COMM_STATE_IDLE;

        fileList = new ArrayList<>();
    }

    // Get instance
    public static Ota getInstance()
    {
        return mInstance;
    }

    public static String getFileName(int fileNum)
    {
        switch (fileNum)
        {
            case FILE_NUM_MFST:
                return FILE_NAME_MANIFEST;

            case FILE_NUM_APP0:
                return FILE_NAME_APP000;

            case FILE_NUM_APP1:
                return FILE_NAME_APP001;

            case FILE_NUM_APP2:
                return FILE_NAME_APP002;

            default:
                return null;
        }
    }

    public static OtaFile getFile(int slotNum, int fileNum)
    {
        OtaFile file = null;

        if (fileList == null)
        {
            fileList = new ArrayList<>();
        }

        for (int i = 0; i < fileList.size(); i++)
        {
            if ((fileList.get(i).slotNum == slotNum) && (fileList.get(i).fileNum == fileNum))
            {
                file = fileList.get(i);
                break;
            }
        }

        if (file == null)
        {
            file = new OtaFile(slotNum, fileNum);
            fileList.add(file);
        }

        return file;
    }

    public void writeOtaFile(int param, int index, int length, boolean readDone, int total)
    {
        /*
        OtaFile otaFile = getFile(param);

        if (otaFile != null)
        {
            otaFile.mIndex = index;
            otaFile.mLength = length;
            otaFile.mReadDone = readDone;
            //mBufferIndex = total;
        }
        */
    }

    public byte[] prepare_dfuCommPacket(int param)
    {
        /*
        OtaFile otaFile = getFileObject(param);
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
        */

        return null;
    }

    public static class OtaFile
    {
        public int    slotNum;
        public int    fileNum;
        public int    collectSize;
        public int    collectPercent;
        public int    writeSize;
        public int    writePercent;
        public int    totalBytes;
        public byte[] buffer;

        // Constructor
        public OtaFile(int slotNum, int fileNum)
        {
            this.slotNum = slotNum;
            this.fileNum = fileNum;
            this.collectSize = 0;
            this.collectPercent = 0;
            this.writeSize = 0;
            this.writePercent = 0;
            this.totalBytes = 0;
            this.buffer = null;
        }
    }
}
