package bms.player.beatoraja.battle;

import bms.player.beatoraja.PlayerData;
import bms.player.beatoraja.ir.IRPlayerData;
import bms.player.beatoraja.song.SongData;

public class BattleRoom {
    public String roomId;
    public String roomKey;
    public IRPlayerData opponent;
    public String[] availSongs;

    public BattleRoom (String roomId, String roomKey) {
        this.roomId = roomId;
        this.roomKey = roomKey;
    }
}
