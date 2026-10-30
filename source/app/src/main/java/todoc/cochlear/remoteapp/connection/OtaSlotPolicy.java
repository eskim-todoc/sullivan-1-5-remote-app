package todoc.cochlear.remoteapp.connection;

/**
 * 실제 슬롯 조회 결과를 바탕으로 현재 실행 중인 이미지를 덮어쓰지 않게 한다.
 */
public final class OtaSlotPolicy
{
    private OtaSlotPolicy()
    {
    }

    public static boolean canWrite(Integer currentSlot, int targetSlot)
    {
        if (currentSlot == null || (targetSlot != 1 && targetSlot != 2))
        {
            return false;
        }
        boolean known = currentSlot == 0xFF || (currentSlot >= 1 && currentSlot <= 4);
        return known && targetSlot != currentSlot;
    }
}
