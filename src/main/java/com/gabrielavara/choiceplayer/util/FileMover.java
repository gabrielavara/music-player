package com.gabrielavara.choiceplayer.util;

import java.io.IOException;
import java.util.Comparator;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gabrielavara.choiceplayer.views.TableItem;

import javafx.collections.ObservableList;

public abstract class FileMover {
    protected static Logger log = LoggerFactory.getLogger("com.gabrielavara.choiceplayer.utils.FileMover");

    private PlaylistUtil playlistUtil;
    private ObservableList<TableItem> mp3Files;

    FileMover(PlaylistUtil playlistUtil, ObservableList<TableItem> mp3Files) {
        this.playlistUtil = playlistUtil;
        this.mp3Files = mp3Files;
    }

    public void moveFile() {
        log.info("Move file to {}", getTarget());
        playlistUtil.getCurrentlyPlayingTableItem().ifPresent(tableItem -> {
            playlistUtil.getNextTableItem().ifPresent(playlistUtil::select);
            try {
                mp3Files.removeAll(tableItem);
                moveFile(tableItem);
                IntStream.range(0, mp3Files.size()).forEach(i -> mp3Files.get(i).setIndex(i + 1));
            } catch (IOException e) {
                mp3Files.add(tableItem.getIndex().get() - 1, tableItem);
                log.error("Could not move {} to {}", tableItem.getMp3(), getTarget());
                sortPlaylist();
            }
        });
    }

    protected abstract String getTarget();

    protected abstract void moveFile(TableItem tableItem) throws IOException;

    private void sortPlaylist() {
        mp3Files.sort(Comparator.comparingInt(o -> o.getIndex().get()));
    }
}
