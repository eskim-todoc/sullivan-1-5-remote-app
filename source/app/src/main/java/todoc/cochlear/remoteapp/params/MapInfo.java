package todoc.cochlear.remoteapp.params;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

import todoc.cochlear.remoteapp.database.users.EntityUser;

public class MapInfo
{
    static public final String EMPTY_MAP_DATA_TD_OTE = "75,1,1,2,3,4,2,84,68,95,79,84,69,0,0,0,0,0,0,0/75,2,0,0,0,0,0,0,0,0,0,0,0,0,48,48,48,48,0,0/75,3,0,0,0,0,0,0,0,0,0,0/76,1,1,2,3,4,5,6,1,0,1,13,1,1,0,1/76,2,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18/76,3,19,20,21,22,23,24,25,26,27,28,29,30,31,32/76,4,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18/76,5,19,20,21,22,23,24,25,26,27,28,29,30,31,32/76,6,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18/76,7,19,20,21,22,23,24,25,26,27,28,29,30,31,32/76,8,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8/76,9,9,9,10,10,11,11,12,12,13,13,14,14,15,15,16,16/76,10,17,17,18,18,19,19,20,20,21,21,22,22,23,23,24,24/76,11,25,25,26,26,27,27,28,28,29,29,30,30,31,31,32,32/76,12,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8/76,13,9,9,10,10,11,11,12,12,13,13,14,14,15,15,16,16/76,14,17,17,18,18,19,19,20,20,21,21,22,22,23,23,24,24/76,15,25,25,26,26,27,27,28,28,29,29,30,30,31,31,32,32/76,1,1,2,3,4,5,6,1,0,1,13,1,1,0,1/76,2,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18/76,3,19,20,21,22,23,24,25,26,27,28,29,30,31,32/76,4,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18/76,5,19,20,21,22,23,24,25,26,27,28,29,30,31,32/76,6,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18/76,7,19,20,21,22,23,24,25,26,27,28,29,30,31,32/76,8,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8/76,9,9,9,10,10,11,11,12,12,13,13,14,14,15,15,16,16/76,10,17,17,18,18,19,19,20,20,21,21,22,22,23,23,24,24/76,11,25,25,26,26,27,27,28,28,29,29,30,30,31,31,32,32/76,12,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8/76,13,9,9,10,10,11,11,12,12,13,13,14,14,15,15,16,16/76,14,17,17,18,18,19,19,20,20,21,21,22,22,23,23,24,24/76,15,25,25,26,26,27,27,28,28,29,29,30,30,31,31,32,32/76,1,1,2,3,4,5,6,1,0,1,13,1,1,0,1/76,2,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18/76,3,19,20,21,22,23,24,25,26,27,28,29,30,31,32/76,4,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18/76,5,19,20,21,22,23,24,25,26,27,28,29,30,31,32/76,6,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18/76,7,19,20,21,22,23,24,25,26,27,28,29,30,31,32/76,8,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8/76,9,9,9,10,10,11,11,12,12,13,13,14,14,15,15,16,16/76,10,17,17,18,18,19,19,20,20,21,21,22,22,23,23,24,24/76,11,25,25,26,26,27,27,28,28,29,29,30,30,31,31,32,32/76,12,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8/76,13,9,9,10,10,11,11,12,12,13,13,14,14,15,15,16,16/76,14,17,17,18,18,19,19,20,20,21,21,22,22,23,23,24,24/76,15,25,25,26,26,27,27,28,28,29,29,30,30,31,31,32,32/76,1,1,2,3,4,5,6,1,0,1,13,1,1,0,1/76,2,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18/76,3,19,20,21,22,23,24,25,26,27,28,29,30,31,32/76,4,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18/76,5,19,20,21,22,23,24,25,26,27,28,29,30,31,32/76,6,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18/76,7,19,20,21,22,23,24,25,26,27,28,29,30,31,32/76,8,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8/76,9,9,9,10,10,11,11,12,12,13,13,14,14,15,15,16,16/76,10,17,17,18,18,19,19,20,20,21,21,22,22,23,23,24,24/76,11,25,25,26,26,27,27,28,28,29,29,30,30,31,31,32,32/76,12,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8/76,13,9,9,10,10,11,11,12,12,13,13,14,14,15,15,16,16/76,14,17,17,18,18,19,19,20,20,21,21,22,22,23,23,24,24/76,15,25,25,26,26,27,27,28,28,29,29,30,30,31,31,32,32";

    static public final int SLOT_MIN = 1;
    static public final int SLOT_MAX = 4;

    static public final int ID_USER_INDEX_MIN = 1;
    static public final int ID_USER_INDEX_MAX = 3;

    static public final int MAP_DATA_MAP_MIN = 1;
    static public final int MAP_DATA_MAP_MAX = 4;

    static public final int MAP_DATA_INDEX_MIN = 1;
    static public final int MAP_DATA_INDEX_MAX = 15;

    static public final int DATA_TYPE_ID_USER = 0;
    static public final int DATA_TYPE_MAP_DATA = 1;

    public int dataType;

    public Metadata metadata;
    public IdUser idUser;
    public MapData mapData;

    public void prepareWriting()
    {
        if (metadata != null && metadata.isFilled)
        {
            idUser.slotNum = SLOT_MIN;
            idUser.indexNum = ID_USER_INDEX_MIN;

            mapData.slotNum = SLOT_MIN;
            mapData.mapNum = MAP_DATA_MAP_MIN;
            mapData.indexNum = MAP_DATA_INDEX_MIN;

            for (int slot_i = 0; slot_i < SLOT_MAX; slot_i++) // 슬롯 4개
            {
                // IdUSer 인덱스 3개를 수행
                idUser.writing[slot_i][0][0] = PacketInfo.HEADER_WRITE_ISD_ID_AND_USER;
                idUser.writing[slot_i][0][1] = MapInfo.ID_USER_INDEX_MIN;
                idUser.writing[slot_i][0][2] = (byte) slot_i;
                System.arraycopy(idUser.data[slot_i][0], 2, idUser.writing[slot_i][0], 3, 17); // src, src_idx, dst, dst_idx, length

                idUser.writing[slot_i][1][0] = PacketInfo.HEADER_WRITE_ISD_ID_AND_USER;
                idUser.writing[slot_i][1][1] = (byte) (idUser.writing[slot_i][0][1] + 1);
                idUser.writing[slot_i][1][2] = idUser.data[slot_i][0][19];
                System.arraycopy(idUser.data[slot_i][1], 2, idUser.writing[slot_i][1], 3, 17);

                idUser.writing[slot_i][2][0] = PacketInfo.HEADER_WRITE_ISD_ID_AND_USER;
                idUser.writing[slot_i][2][1] = (byte) (idUser.writing[slot_i][1][1] + 1);
                idUser.writing[slot_i][2][2] = idUser.data[slot_i][1][19];
                System.arraycopy(idUser.data[slot_i][2], 2, idUser.writing[slot_i][2], 3, 4);

                // Map Data Map 4개와 인덱스 15개 수행
                for (int map_j = 0; map_j < MAP_DATA_MAP_MAX; map_j++)
                {
                    mapData.writing[slot_i][map_j][0][0] = PacketInfo.HEADER_WRITE_MAP_DATA;
                    mapData.writing[slot_i][map_j][0][1] = MapInfo.MAP_DATA_INDEX_MIN;
                    mapData.writing[slot_i][map_j][0][2] = (byte) slot_i;
                    mapData.writing[slot_i][map_j][0][3] = (byte) map_j;
                    System.arraycopy(mapData.data[slot_i][map_j][0], 2, mapData.writing[slot_i][map_j][0], 4, 14);

                    for (int index_k = 1; index_k < MAP_DATA_INDEX_MAX; index_k++)
                    {
                        Log.d("MapSharing", "prepare -> slot_i = " + slot_i + ", map_j = " + map_j + ", index_k = " + index_k + ", size = " + MapData.sizes[index_k]);
                        System.arraycopy(mapData.data[slot_i][map_j][index_k], 0, mapData.writing[slot_i][map_j][index_k], 0, MapData.sizes[index_k]);
                        mapData.writing[slot_i][map_j][index_k][0] = PacketInfo.HEADER_WRITE_MAP_DATA;
                    }
                }
            }
        }
    }

    public void updateMetadata()
    {
        if (metadata != null && metadata.isFilled)
        {
            // 메타데이터 업데이트 기능을 구현하자.
            for (int i = 0; i < 4; i++)
            {
                long stamp;
                String name, ear;

                byte[] bytesName1 = Arrays.copyOfRange(idUser.data[i][0], 7, 20);
                byte[] bytesName2 = Arrays.copyOfRange(idUser.data[i][1], 2, 14);
                byte[] bytesFullName = new byte[bytesName1.length + bytesName2.length];

                System.arraycopy(bytesName1, 0, bytesFullName, 0, bytesName1.length);
                System.arraycopy(bytesName2, 0, bytesFullName, bytesName1.length, bytesName2.length);

                int lastIndex = bytesFullName.length;

                for (int l = 0; l < bytesFullName.length; l++)
                {
                    if (bytesFullName[l] == 0)
                    {
                        lastIndex = l;
                        break;
                    }
                }

                name = new String(Arrays.copyOfRange(bytesFullName, 0, lastIndex), StandardCharsets.US_ASCII);

                if (idUser.data[i][0][6] == 1)
                {
                    ear = EntityUser.EAR_LEFT;
                }
                else
                {
                    ear = EntityUser.EAR_RIGHT;
                }

                byte[] bytesStamp = Arrays.copyOfRange(idUser.data[i][2], 6, 12);
                stamp = ((long) bytesStamp[0]) & 255L;
                stamp = (stamp << 8) | (((long) bytesStamp[1]) & 255L);
                stamp = (stamp << 8) | (((long) bytesStamp[2]) & 255L);
                stamp = (stamp << 8) | (((long) bytesStamp[3]) & 255L);
                stamp = (stamp << 8) | (((long) bytesStamp[4]) & 255L);
                stamp = (stamp << 8) | (((long) bytesStamp[5]) & 255L);

                metadata.names[i] = name;
                metadata.ears[i] = ear;
                metadata.stamps[i] = stamp;
            } // end for
            //metadata.userNames[i] = idUser.data[i][0]
        }
    }

    public MapInfo()
    {
        metadata = new Metadata();
        idUser = new IdUser();
        mapData = new MapData();
    }

    static public class Metadata
    {
        public boolean isFilled;
        public String[] names;
        public String[] ears;
        public long[] stamps;

        public Metadata()
        {
            isFilled = false;
            names = new String[4];
            ears = new String[4];
            stamps = new long[4];
        }
    }

    static public class IdUser
    {
        /*...*/static public final int[] sizes = new int[]{20, 20, 12};
        static public final int[] writingSizes = new int[]{20, 20, 13};

        public int slotNum;
        public int indexNum;
        public byte[][][] data;
        public byte[][][] writing;

        public IdUser()
        {
            data = new byte[4][][]; // 슬롯이 총 4개
            writing = new byte[4][][];

            for (int i = 0; i < 4; i++)
            {
                data[i] = new byte[3][]; // 각 슷롯 당 인덱스 3개
                writing[i] = new byte[3][];

                for (int j = 0; j < 3; j++)
                {
                    data[i][j] = new byte[sizes[j]]; // 각 인덱스 별 패킷 사이즈 설정
                    writing[i][j] = new byte[writingSizes[j]]; // 쓰기의 각 인덱스 별 패킷 사이즈 설정
                }
            }
        }
    }

    static public class MapData
    {
        /*...*/static public final int[] sizes = new int[]{16, 20, 16, 20, 16, 20, 16, 18, 18, 18, 18, 18, 18, 18, 18};
        static public final int[] writingSizes = new int[]{18, 20, 16, 20, 16, 20, 16, 18, 18, 18, 18, 18, 18, 18, 18};

        public int slotNum;
        public int mapNum;
        public int indexNum;
        public byte[][][][] data;
        public byte[][][][] writing;

        public MapData()
        {
            data = new byte[4][][][]; // 슬롯이 총 4개
            writing = new byte[4][][][];

            for (int i = 0; i < 4; i++)
            {
                data[i] = new byte[4][][]; // 각 슬롯 당 맵이 4개
                writing[i] = new byte[4][][];

                for (int k = 0; k < 4; k++)
                {
                    data[i][k] = new byte[15][]; // 각 슬롯의 맵 당 인덱스 15개
                    writing[i][k] = new byte[15][];

                    for (int j = 0; j < 15; j++)
                    {
                        data[i][k][j] = new byte[sizes[j]]; // 각 인덱스 별 패킷 사이즈 설정
                        writing[i][k][j] = new byte[writingSizes[j]]; // 쓰기의 각 인덱스 별 패킷 사이즈 설정
                    }
                }
            }
        }
    }

    //
    // 맵 정보에 활용되는 유틸리티 함수
    //
    static public boolean setMapInfoFromString(String string, MapInfo mapInfo, int slot_i)
    {
        String[] splits = string.split("/");

        if (splits.length != /*252*/63)
        {
            Log.d("MapInfo", "총 데이터 인덱스 개수가 올바르지 않습니다. 입력 받은 총 데이터 인덱스 개수 = " + splits.length);
            return false;
        }

        if (mapInfo == null)
        {
            mapInfo = new MapInfo();
        }

        int split_i = 0;
        //for (int slot_i = 0, split_i = 0; slot_i < SLOT_MAX; slot_i++)
        {
            for (int index_i = 0; index_i < ID_USER_INDEX_MAX; index_i++)
            {
                mapInfo.idUser.data[slot_i][index_i] = stringToBytes(splits[split_i++]);
            }

            for (int map_i = 0; map_i < MAP_DATA_MAP_MAX; map_i++)
            {
                for (int index_i = 0; index_i < MAP_DATA_INDEX_MAX; index_i++)
                {
                    mapInfo.mapData.data[slot_i][map_i][index_i] = stringToBytes(splits[split_i++]);
                }
            }
        }

        return true;
    }

    static public String getStringFromMapInfo(MapInfo mapInfo, int slot_i)
    {
        StringBuilder builder = new StringBuilder();

        // 1. Id and user information.
        //for (int slot_i = 0; slot_i < SLOT_MAX; slot_i++)
        {
            for (int index_i = 0; index_i < ID_USER_INDEX_MAX; index_i++)
            {
                builder.append(bytesToString(mapInfo.idUser.data[slot_i][index_i]));

                if (index_i < (ID_USER_INDEX_MAX - 1))
                {
                    builder.append("/");
                }
            }

            //if (slot_i < (SLOT_MAX - 1))
            {
                builder.append("/");
            }
        }

        // 2. Map data information.
        //for (int slot_i = 0; slot_i < SLOT_MAX; slot_i++)
        {
            for (int map_i = 0; map_i < MAP_DATA_MAP_MAX; map_i++)
            {
                for (int index_i = 0; index_i < MAP_DATA_INDEX_MAX; index_i++)
                {
                    builder.append(bytesToString(mapInfo.mapData.data[slot_i][map_i][index_i]));

                    if (index_i < (MAP_DATA_INDEX_MAX - 1))
                    {
                        builder.append("/");
                    }
                }

                if (map_i < (MAP_DATA_MAP_MAX - 1))
                {
                    builder.append("/");
                }
            }

            //if (slot_i < (SLOT_MAX - 1))
            {
                //builder.append("/");
            }
        }

        return builder.toString();
    }

    static public String bytesToString(byte[] bytes)
    {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < bytes.length - 1; i++)
        {
            builder.append(String.format(Locale.ENGLISH, "%d,", ((int) bytes[i]) & 0xFF));
        }

        builder.append(String.format(Locale.ENGLISH, "%d", ((int) bytes[bytes.length - 1]) & 0xFF));

        return builder.toString();
    }

    static public byte[] stringToBytes(String string)
    {
        String[] splits = string.split(",");
        byte[] bytes = new byte[splits.length];

        for (int i = 0; i < splits.length; i++)
        {
            bytes[i] = (byte) Integer.parseInt(splits[i]);
        }

        return bytes;
    }
}
