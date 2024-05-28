package bms.player.beatoraja.song;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Logger;

public class FilteredSongDBAccessor extends SQLiteSongDatabaseAccessor{
    private HashSet<String> availSongHash;

    public FilteredSongDBAccessor(String filepath, String[] bmsroot) throws ClassNotFoundException {
        super(filepath, bmsroot);
    }

    public void setFilter(String[] hashes) {
        availSongHash = new HashSet<>(Arrays.asList(hashes));
    }

    private SongData[] filter(SongData[] original) {
        if (availSongHash == null) {
            Logger.getGlobal().warning("filter is not initialized");
            return original;
        }
        if (availSongHash.isEmpty()) {
            Logger.getGlobal().warning("no available songs");
            return SongData.EMPTY;
        }
        ArrayList<SongData> filtered = new ArrayList<>();
        for(SongData sd: original) {
            if (availSongHash.contains(sd.getSha256())) {
                filtered.add(sd);
            }
        }
        return filtered.toArray(SongData.EMPTY);
    }

    @Override
    public SongData[] getSongDatas(String[] hashes) {
        Logger.getGlobal().info("getSongDatas-hashes.");
        return filter(super.getSongDatas(hashes));
    }

    @Override
    public SongData[] getSongDatas(String key, String value) {
        Logger.getGlobal().info("getSongDatas-keyvalue. "+key+","+value);
        return filter(super.getSongDatas(key, value));
    }

    @Override
    public SongData[] getSongDatas(String sql, String score, String scorelog, String info){
        Logger.getGlobal().info(String.format("getSongDatas: %s, %s, %s, %s", sql, score, scorelog, info));
        return filter(super.getSongDatas(sql, score, scorelog, info));
    }
}
