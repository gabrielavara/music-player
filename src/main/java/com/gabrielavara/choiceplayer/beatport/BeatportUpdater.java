package com.gabrielavara.choiceplayer.beatport;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gabrielavara.choiceplayer.dto.Mp3;
import com.gabrielavara.choiceplayer.views.PlaylistItemView;
import com.google.common.base.Joiner;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;

public class BeatportUpdater {
    private static Logger log = LoggerFactory.getLogger("com.gabrielavara.choiceplayer.beatport.BeatportUpdater");

    private ObservableList<PlaylistItemView> playlistItems;
    private Task<Void> updaterTask;
    private Thread thread;
    private BeatportSearcher beatportSearcher = new BeatportSearcher();

    public BeatportUpdater(ObservableList<PlaylistItemView> playlistItems) {
        this.playlistItems = playlistItems;
    }

    public void update() {
        if (updaterTask != null) {
            log.info("Cancel task");
            updaterTask.cancel();
            thread.interrupt();
        }

        log.info("Start task");
        updaterTask = createUpdaterTask();
        thread = new Thread(updaterTask);
        thread.setDaemon(true);
        thread.start();
    }

    private Task<Void> createUpdaterTask() {
        return new Task<Void>() {
            @Override
            protected Void call() {
                playlistItems.forEach(pi -> {
                    Mp3 mp3 = pi.getMp3();
                    if (mp3.shouldSearchForInfo()) {
                        update(mp3);
                    }
                });
                return null;
            }
        };
    }

    private void update(Mp3 mp3) {
        log.info("Search for: {}", mp3);
        Optional<BeatportAlbum> beatportAlbum = beatportSearcher.search(mp3);
        beatportAlbum.ifPresent(album -> {
            Optional<BeatportTrack> track = getBestTrack(mp3, album);
            track.ifPresent(t -> update(mp3, t, album));
        });
    }

    private Optional<BeatportTrack> getBestTrack(Mp3 mp3, BeatportAlbum album) {
        List<Integer> distances = getDistances(mp3, album);
        Optional<Integer> minDistance = distances.stream().min(comparingInt(i -> i));

        if (minDistance.isPresent()) {
            int minIndex = distances.indexOf(minDistance.get());
            return Optional.of(album.getTracks().get(minIndex));
        } else {
            return Optional.empty();
        }
    }

    private List<Integer> getDistances(Mp3 mp3, BeatportAlbum album) {
        return album.getTracks().stream().map(t -> {
            String title = t.getTitle() + " (" + t.getMix() + ")";
            return LevenshteinDistance.calculate(title, mp3.getTitle());
        }).collect(toList());
    }

    private void update(Mp3 mp3, BeatportTrack track, BeatportAlbum album) {
        String artist = getArtist(mp3, track);
        mp3.setArtist(artist);
        String title = getTitle(track) + " (" + track.getMix() + ")";
        mp3.setTitle(title);
        String trackNumber = getTrackString(track.getTrackNumber()) + "/" + getTrackString(album.getTracks().size());
        mp3.setTrack(trackNumber);
        mp3.setAlbum(album.getTitle());
        mp3.setYear(album.getReleaseDate());
        String albumArtist = Joiner.on(", ").join(album.getArtists());
        mp3.setAlbumArtist(albumArtist);
        String genre = Joiner.on(" / ").join(track.getGenres());
        mp3.setGenre(genre);
        String comment = album.getLabel() + " [" + album.getCatalog() + "]";
        mp3.setComment(comment);
        mp3.setBpm(track.getBpm());

        log.info("Artist: {}", artist);
        log.info("Title: {}", title);
        log.info("Track: {}", trackNumber);
        log.info("Album: {}", album.getTitle());
        log.info("Release date: {}", album.getReleaseDate());
        log.info("Album artist: {}", albumArtist);
        log.info("Genre: {}", genre);
        log.info("Comment: {}", comment);
        log.info("BPM: {}\n", track.getBpm());
        setAlbumArt(mp3, album);
    }

    static String getTitle(BeatportTrack track) {
        String title = track.getTitle();
        for (String a : track.getArtists()) {
            if (title.contains(a)) {
                int i = title.indexOf(a);
                if (i > 0) {
                    int spaceIndex = title.substring(0, i - 1).lastIndexOf(' ');
                    title = title.substring(0, spaceIndex);
                }
            }
        }
        return title;
    }

    private void setAlbumArt(Mp3 mp3, BeatportAlbum album) {
        try (InputStream is = new URL(album.getAlbumArtUrl()).openStream()) {
            byte[] imageBytes = IOUtils.toByteArray(is);
            mp3.setAlbumArtAndSaveTags(imageBytes);
        } catch (IOException e) {
            log.error("Could not load album art");
        }
    }

    private String getArtist(Mp3 mp3, BeatportTrack track) {
        String artist = mp3.getArtist();
        for (RegexPattern regexPattern : RegexPattern.values()) {
            for (Pattern pattern : regexPattern.getPatterns()) {
                if (regexPattern == RegexPattern.COMMA && track.getArtists().size() == 2) {
                    artist = pattern.matcher(artist).replaceAll(" & ");
                } else {
                    artist = pattern.matcher(artist).replaceAll(regexPattern.getReplaceWith());
                }
            }
        }
        return artist;
    }

    private String getTrackString(int num) {
        return num < 10 ? "0" + num : "" + num;
    }

    private String getTrackString(String num) {
        return num.length() < 2 ? "0" + num : num;
    }

}