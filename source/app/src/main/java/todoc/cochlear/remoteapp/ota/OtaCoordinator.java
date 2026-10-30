package todoc.cochlear.remoteapp.ota;

import java.util.Arrays;

import todoc.cochlear.remoteapp.connection.OtaProgress;
import todoc.cochlear.remoteapp.params.PacketInfo;

/**
 * Main-thread, one-GATT OTA sequence. It owns preparation, requests, ACKs and the fixed transfer manifest.
 */
public final class OtaCoordinator
{
    public interface Transport
    {
        boolean ready();

        boolean send(byte[] packet);

        void changed();

        void log(String message);
    }

    private static final int[] FILE_ORDER = {Ota.FILE_NUM_MFST, Ota.FILE_NUM_APP0, Ota.FILE_NUM_APP1, Ota.FILE_NUM_APP2};
    private final Ota ota;
    private final Transport transport;
    private OtaProgress.State state = OtaProgress.State.IDLE;
    private String message = "OTA 준비를 진행하세요.";
    private boolean mappingReady, mappingUncertain;
    private byte[][] buffers;
    private int[] lengths;
    private int fileIndex = -1, acknowledgedFileBytes, pendingBytes;
    private long totalBytes, acknowledgedBytes;
    private int targetSlot;
    private final OtaTransferRate transferRate = new OtaTransferRate();
    private final java.util.function.LongSupplier clock;

    public long bytesPerSecond()
    {
        return transferRate.bytesPerSecond(clock.getAsLong());
    }

    public long averageBytesPerSecond()
    {
        return transferRate.averageBytesPerSecond();
    }

    public OtaCoordinator(Ota ota, Transport transport)
    {
        this(ota, transport, () -> System.nanoTime() / 1_000_000L);
    }

    OtaCoordinator(Ota ota, Transport transport, java.util.function.LongSupplier clock)
    {
        this.ota = ota;
        this.transport = transport;
        this.clock = clock;
    }

    public OtaProgress.State state()
    {
        return state;
    }

    public String message()
    {
        return message;
    }

    public boolean busy()
    {
        return state == OtaProgress.State.PREPARING || state == OtaProgress.State.WRITING;
    }

    public boolean preparing()
    {
        return state == OtaProgress.State.PREPARING;
    }

    public boolean waiting()
    {
        return preparing() || (state == OtaProgress.State.WRITING && (ota.commState == Ota.COMM_STATE_WAIT_RESP_COMMAND || ota.commState == Ota.COMM_STATE_WAIT_RESP_DATA));
    }

    public boolean blocksOrdinaryTraffic()
    {
        return preparing() || mappingReady || mappingUncertain;
    }

    public long totalBytes()
    {
        return totalBytes;
    }

    public long acknowledgedBytes()
    {
        return acknowledgedBytes;
    }

    public int currentFileNum()
    {
        return fileIndex >= 0 && fileIndex < FILE_ORDER.length ? FILE_ORDER[fileIndex] : -1;
    }

    public int percent()
    {
        if (state == OtaProgress.State.COMPLETE)
        {
            return 100;
        }
        return totalBytes <= 0 ? 0 : (int) Math.min(99, acknowledgedBytes * 100L / totalBytes);
    }

    private boolean validSlot()
    {
        return ota.currentSlotNum == Ota.SLOT_NUM_1 || ota.currentSlotNum == Ota.SLOT_NUM_2;
    }

    public long collectedTotalBytes()
    {
        if (ota.threadState != Ota.THREAD_STATE_IDLE)
        {
            return 0;
        }
        long sum = 0;
        for (int number : FILE_ORDER)
        {
            Ota.OtaFile file = null;
            if (ota.fileList != null)
            {
                for (Ota.OtaFile existing : ota.fileList)
                {
                    if (existing.slotNum == Ota.SLOT_NUM_COLLECT && existing.fileNum == number)
                    {
                        file = existing;
                        break;
                    }
                }
            }
            if (file == null || file.collectPercent != 100 || file.totalBytes <= 0 || file.collectSize != file.totalBytes
                || file.buffer == null || file.buffer.length < file.totalBytes)
            {
                return 0;
            }
            sum += file.totalBytes;
        }
        return sum;
    }

    public boolean canPrepare()
    {
        return !busy() && transport.ready();
    }

    public boolean canStart()
    {
        return state == OtaProgress.State.READY && mappingReady && validSlot() && collectedTotalBytes() > 0 && transport.ready();
    }

    public boolean prepare()
    {
        if (!canPrepare())
        {
            return reject("다른 통신이 끝난 뒤 OTA 준비를 진행하세요.");
        }
        acknowledgedBytes = 0;
        totalBytes = 0;
        transferRate.reset();
        fileIndex = -1;
        if (mappingReady)
        {
            state = OtaProgress.State.READY;
            message = "OTA 준비 완료";
            transport.changed();
            return true;
        }
        state = OtaProgress.State.PREPARING;
        message = "OTA 준비 응답을 기다리고 있습니다.";
        mappingUncertain = true;
        transport.changed();
        if (!transport.send(new byte[]{PacketInfo.HEADER_MAPPING_CONNECT}))
        {
            mappingUncertain = false;
            fail("OTA 준비 요청을 전송하지 못했습니다.");
            return false;
        }
        return true;
    }

    public boolean start()
    {
        if (!canStart())
        {
            return reject("OTA 준비 완료 후 4개 파일과 기록할 슬롯을 확인하세요.");
        }
        targetSlot = ota.currentSlotNum;
        buffers = new byte[FILE_ORDER.length][];
        lengths = new int[FILE_ORDER.length];
        totalBytes = collectedTotalBytes();
        acknowledgedBytes = 0;
        fileIndex = 0;
        // Freeze the manifest and bytes. A later folder selection cannot alter an in-flight transfer.
        for (int i = 0; i < FILE_ORDER.length; i++)
        {
            Ota.OtaFile file = ota.getFile(Ota.SLOT_NUM_COLLECT, FILE_ORDER[i]);
            lengths[i] = file.totalBytes;
            buffers[i] = Arrays.copyOf(file.buffer, file.totalBytes);
            file.writeSize = 0;
            file.writePercent = 0;
        }
        transferRate.start(clock.getAsLong());
        state = OtaProgress.State.WRITING;
        message = "업데이트 전송 중";
        transport.log("OTA 전송 시작: 슬롯 " + targetSlot + ", 4개 파일, " + totalBytes + " bytes");
        return beginFile();
    }

    private boolean beginFile()
    {
        int length = lengths[fileIndex];
        acknowledgedFileBytes = 0;
        pendingBytes = 0;
        ota.currentFileNum = FILE_ORDER[fileIndex];
        ota.commSlotNum = targetSlot;
        ota.commFileNum = ota.currentFileNum;
        ota.commOption = Ota.COMM_RW_WRITE;
        ota.commTotalByte = length;
        ota.commDataIndex = 0;
        ota.commEndDataIndexNum = (int) (((long) length + PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT - 1) / PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT);
        ota.commEndDataIndexByte = length % PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT;
        if (ota.commEndDataIndexByte == 0)
        {
            ota.commEndDataIndexByte = PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT;
        }
        ota.commBuffer = buffers[fileIndex];
        ota.commBufferIndex = 0;
        byte[] packet = new byte[PacketInfo.PACKET_SIZE_OTA_SEND_COMMAND];
        packet[0] = PacketInfo.HEADER_OTA;
        packet[4] = (byte) targetSlot;
        packet[5] = (byte) (ota.currentFileNum >> 8);
        packet[6] = (byte) ota.currentFileNum;
        packet[7] = 1;
        putInt(packet, 8, length);
        putInt(packet, 12, ota.commEndDataIndexNum);
        packet[16] = (byte) (ota.commEndDataIndexByte >> 8);
        packet[17] = (byte) ota.commEndDataIndexByte;
        ota.commState = Ota.COMM_STATE_WAIT_RESP_COMMAND;
        transport.changed();
        if (!transport.send(packet))
        {
            fail("파일 전송 요청을 보내지 못했습니다.");
            return false;
        }
        return true;
    }

    private static void putInt(byte[] packet, int offset, int value)
    {
        packet[offset] = (byte) (value >> 24);
        packet[offset + 1] = (byte) (value >> 16);
        packet[offset + 2] = (byte) (value >> 8);
        packet[offset + 3] = (byte) value;
    }

    private boolean reject(String reason)
    {
        message = reason;
        transport.changed();
        return false;
    }

    /**
     * Only the outstanding phase may release its transport timer and pending request.
     */
    public boolean expectsResponse(byte[] packet)
    {
        if (!waiting() || packet == null || packet.length == 0)
        {
            return false;
        }
        int expected = preparing() ? PacketInfo.HEADER_MAPPING_CONNECT & 255 : PacketInfo.HEADER_OTA & 255;
        int header = packet[0] & 255;
        return header == expected || ((header == 0xF0 || header == 0xFE) && packet.length >= 2 && (packet[1] & 255) == expected);
    }

    public void receive(byte[] packet)
    {
        if (!expectsResponse(packet))
        {
            return;
        }
        int header = packet[0] & 255;
        if ((header == 0xF0 || header == 0xFE) && packet.length >= 2)
        {
            int command = packet[1] & 255;
            if (command == (PacketInfo.HEADER_MAPPING_CONNECT & 255) || command == (PacketInfo.HEADER_OTA & 255))
            {
                if (preparing())
                {
                    mappingUncertain = false;
                }
                fail("장치가 OTA 요청을 거절했습니다.");
            }
            return;
        }
        if (preparing())
        {
            if (header != (PacketInfo.HEADER_MAPPING_CONNECT & 255))
            {
                return;
            }
            if (packet.length != 2 || packet[1] != 1)
            {
                if (packet.length == 2 && packet[1] == 0)
                {
                    mappingUncertain = false;
                }
                fail("OTA 준비 응답이 올바르지 않습니다.");
                return;
            }
            mappingUncertain = false;
            mappingReady = true;
            state = OtaProgress.State.READY;
            message = "OTA 준비 완료";
            transport.log("OTA 준비 완료: 매핑 진입 ACK 확인");
            transport.changed();
            return;
        }
        if (header != (PacketInfo.HEADER_OTA & 255))
        {
            return;
        }
        if (ota.commState == Ota.COMM_STATE_WAIT_RESP_COMMAND)
        {
            receiveCommand(packet);
        }
        else if (ota.commState == Ota.COMM_STATE_WAIT_RESP_DATA)
        {
            receiveData(packet);
        }
    }

    private int index(byte[] packet)
    {
        return ((packet[1] & 255) << 16) | ((packet[2] & 255) << 8) | (packet[3] & 255);
    }

    private void receiveCommand(byte[] packet)
    {
        if (packet.length < 9 || index(packet) != 0 || (packet[4] & 255) != targetSlot
            || (((packet[5] & 255) << 8) | (packet[6] & 255)) != ota.commFileNum || packet[7] != 1 || packet[8] != 1)
        {
            fail("파일 전송 준비 응답의 슬롯·파일·결과가 요청과 다릅니다.");
            return;
        }
        sendData();
    }

    private void sendData()
    {
        int length = lengths[fileIndex];
        pendingBytes = Math.min(PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT, length - acknowledgedFileBytes);
        ota.commDataIndex++;
        byte[] packet = new byte[4 + pendingBytes];
        packet[0] = PacketInfo.HEADER_OTA;
        packet[1] = (byte) (ota.commDataIndex >> 16);
        packet[2] = (byte) (ota.commDataIndex >> 8);
        packet[3] = (byte) ota.commDataIndex;
        System.arraycopy(buffers[fileIndex], acknowledgedFileBytes, packet, 4, pendingBytes);
        ota.commBufferIndex = acknowledgedFileBytes + pendingBytes;
        ota.commState = Ota.COMM_STATE_WAIT_RESP_DATA;
        if (!transport.send(packet))
        {
            fail("파일 데이터 요청을 보내지 못했습니다.");
        }
    }

    private void receiveData(byte[] packet)
    {
        if (packet.length < 5 || index(packet) != ota.commDataIndex || packet[4] != 1)
        {
            fail("파일 데이터 응답의 인덱스·결과가 요청과 다릅니다.");
            return;
        }
        acknowledgedFileBytes += pendingBytes;
        acknowledgedBytes += pendingBytes;
        transferRate.acknowledge(pendingBytes, clock.getAsLong());
        pendingBytes = 0;
        Ota.OtaFile file = ota.getFile(Ota.SLOT_NUM_COLLECT, FILE_ORDER[fileIndex]);
        file.writeSize = acknowledgedFileBytes;
        file.writePercent = (int) ((long) acknowledgedFileBytes * 100L / lengths[fileIndex]);
        if (acknowledgedFileBytes == lengths[fileIndex])
        {
            transport.log("OTA 파일 ACK 완료: " + Ota.getFileName(FILE_ORDER[fileIndex]));
            if (++fileIndex == FILE_ORDER.length)
            {
                transferRate.finish(clock.getAsLong());
                state = OtaProgress.State.COMPLETE;
                ota.commState = Ota.COMM_STATE_IDLE;
                message = "4개 파일 업데이트 전송 완료";
                buffers = null;
                lengths = null;
                transport.log(message + " (" + acknowledgedBytes + " bytes)");
                transport.changed();
                return;
            }
            beginFile();
        }
        else
        {
            transport.changed();
            sendData();
        }
    }

    public void fail(String reason)
    {
        if (!busy())
        {
            return;
        }
        transferRate.finish(clock.getAsLong());
        state = OtaProgress.State.FAILED;
        message = reason;
        ota.commState = Ota.COMM_STATE_IDLE;
        buffers = null;
        lengths = null;
        transport.log("OTA 실패: " + reason);
        transport.changed();
    }

    public void disconnected()
    {
        if (busy())
        {
            fail("BLE 연결이 끊겨 전송을 중단했습니다.");
        }
        mappingReady = false;
        mappingUncertain = false;
        ota.commState = Ota.COMM_STATE_IDLE;
        if (state == OtaProgress.State.READY)
        {
            state = OtaProgress.State.IDLE;
            message = "다시 연결한 뒤 OTA 준비를 진행하세요.";
        }
        transport.changed();
    }

    /**
     * C2 reboot resets CM3 mapping state even when the nRF keeps the same BLE connection.
     */
    public void bootSelected()
    {
        mappingReady = false;
        mappingUncertain = false;
        ota.commState = Ota.COMM_STATE_IDLE;
        if (state == OtaProgress.State.READY || state == OtaProgress.State.PREPARING)
        {
            state = OtaProgress.State.IDLE;
            message = "슬롯 부팅 후 OTA 준비를 다시 진행하세요.";
        }
        transport.changed();
    }
}
