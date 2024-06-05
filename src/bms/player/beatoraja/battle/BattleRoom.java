package bms.player.beatoraja.battle;

import bms.player.beatoraja.PlayerData;
import bms.player.beatoraja.ir.IRPlayerData;
import bms.player.beatoraja.song.SongData;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class BattleRoom {
    private final String roomId;
    private final String roomKey;
    private IRPlayerData opponent;
    private String[] availSongs;
    private LinkedHashMap<String, String> chosenSongs;
    private Iterator<Map.Entry<String, String>> songIterator;

    public BattleRoom (String roomId, String roomKey) {
        this.roomId = roomId;
        this.roomKey = roomKey;
    }
    public String getRoomId() {
        return roomId;
    }
    public String getRoomKey(){
        return roomKey;
    }

    public IRPlayerData getOpponent() {
        return opponent;
    }
    public void setOpponent(IRPlayerData opponent){
        this.opponent = opponent;
    }

    public String[] getAvailableSongs(){
        return availSongs;
    }
    public void setAvailSongs(String[] availSongs){
        this.availSongs = availSongs;
    }

    public boolean isChoiceReady() {
        return chosenSongs != null;
    }
    public Map.Entry<String, String> nextSong() {
        if (songIterator == null){
            songIterator = chosenSongs.entrySet().iterator();
        }
        if (songIterator.hasNext()) {
            return songIterator.next();
        }
        return null;
    }
    public void setChoices(LinkedHashMap<String, String> chosenSongs){
        this.chosenSongs = chosenSongs;
    }
}
