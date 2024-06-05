package bms.player.beatoraja.battle;

import bms.player.beatoraja.ir.IRAccount;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.ir.IRPlayerData;
import bms.player.beatoraja.song.SongData;

public interface BattleConnection {
    BattleRoom createRoom();
    BattleRoom joinRoom(String roomKeyword);
    BattleRoom getBattleRoom();

    String getRecvBuffer();
    void putSendBuffer(String cmd, String content);
    void sendSongList(SongData[] songs);
    void sendChoice(SongData choice);

    enum BattleConnectionState{
        NO_CONNECTION, CONNECTING, FAILED, SUCCEEDED;
    }


}
