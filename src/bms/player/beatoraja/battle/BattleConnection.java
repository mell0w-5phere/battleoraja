package bms.player.beatoraja.battle;

import bms.player.beatoraja.ir.IRAccount;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.ir.IRPlayerData;
import bms.player.beatoraja.song.SongData;

public interface BattleConnection {
    boolean createRoom();
    boolean joinRoom(String roomKeyword);

    String getRecvBuffer();
    void putSendBuffer(String cmd, String content);

    IRPlayerData getOpponent();
    String getRoomKey();
    void sendSongList(SongData[] songs);
    String[] getAvailableSongs();

    enum BattleConnectionState{
        NO_CONNECTION, CONNECTING, FAILED, SUCCEEDED;
    }


}
