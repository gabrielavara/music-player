package com.gabrielavara.choiceplayer.util;

import static com.gabrielavara.choiceplayer.Constants.ANIMATION_DURATION;
import static com.gabrielavara.choiceplayer.Constants.DELAY;
import static com.gabrielavara.choiceplayer.Constants.TRANSLATE_Y;
import static java.util.Arrays.asList;

import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.gabrielavara.choiceplayer.ChoicePlayerApplication;
import com.gabrielavara.choiceplayer.api.service.Mp3;
import com.gabrielavara.choiceplayer.api.service.PlaylistLoader;
import com.gabrielavara.choiceplayer.views.TableItem;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTreeTableColumn;
import com.jfoenix.controls.JFXTreeTableRow;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.cells.editors.TextFieldEditorBuilder;
import com.jfoenix.controls.cells.editors.base.GenericEditableTreeTableCell;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Point2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class PlaylistInitializer {

    private JFXTreeTableView<TableItem> playlist;
    private ObservableList<TableItem> mp3Files;
    private HBox rootContainer;
    private JFXSpinner spinner;
    private StackPane playlistStackPane;
    private ResourceBundle resourceBundle;
    private List<JFXTreeTableRow<TableItem>> rows = new ArrayList<>();

    public PlaylistInitializer(JFXTreeTableView<TableItem> playlist, ObservableList<TableItem> mp3Files, HBox rootContainer, JFXSpinner spinner,
                    StackPane playlistStackPane) {
        this.playlist = playlist;
        this.mp3Files = mp3Files;
        this.rootContainer = rootContainer;
        this.spinner = spinner;
        this.playlistStackPane = playlistStackPane;
        resourceBundle = ResourceBundle.getBundle("language.player");
    }

    public void loadPlaylist() {
        TreeItem<TableItem> root = new RecursiveTreeItem<>(mp3Files, RecursiveTreeObject::getChildren);
        playlist.setRoot(root);
        playlist.setShowRoot(false);

        playlist.setRowFactory(row -> {
            JFXTreeTableRow<TableItem> newRow = new JFXTreeTableRow<>();
            rows.add(newRow);
            return newRow;
        });
        addColumns();
        loadMp3Files();
        playlist.getSelectionModel().selectedItemProperty().addListener(new PlaylistSelectionChangedListener());
    }

    private void addColumns() {
        JFXTreeTableColumn<TableItem, Number> indexColumn = new JFXTreeTableColumn<>("#");
        indexColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<TableItem, Number> param) -> {
            if (indexColumn.validateValue(param)) {
                return param.getValue().getValue().getIndex();
            } else {
                return indexColumn.getComputedValue(param);
            }
        });

        JFXTreeTableColumn<TableItem, String> artistColumn = new JFXTreeTableColumn<>(resourceBundle.getString("artist").toUpperCase());
        artistColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<TableItem, String> param) -> {
            if (artistColumn.validateValue(param)) {
                return param.getValue().getValue().getArtist();
            } else {
                return artistColumn.getComputedValue(param);
            }
        });

        JFXTreeTableColumn<TableItem, String> titleColumn = new JFXTreeTableColumn<>(resourceBundle.getString("title").toUpperCase());
        titleColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<TableItem, String> param) -> {
            if (titleColumn.validateValue(param)) {
                return param.getValue().getValue().getTitle();
            } else {
                return titleColumn.getComputedValue(param);
            }
        });

        JFXTreeTableColumn<TableItem, String> lengthColumn = new JFXTreeTableColumn<>(resourceBundle.getString("length").toUpperCase());
        lengthColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<TableItem, String> param) -> {
            if (lengthColumn.validateValue(param)) {
                return param.getValue().getValue().getLength();
            } else {
                return lengthColumn.getComputedValue(param);
            }
        });

        indexColumn.setCellFactory((TreeTableColumn<TableItem, Number> param) -> new GenericEditableTreeTableCell<>(new TextFieldEditorBuilder()));
        artistColumn.setCellFactory((TreeTableColumn<TableItem, String> param) -> new GenericEditableTreeTableCell<>(new TextFieldEditorBuilder()));
        titleColumn.setCellFactory((TreeTableColumn<TableItem, String> param) -> new GenericEditableTreeTableCell<>(new TextFieldEditorBuilder()));
        lengthColumn.setCellFactory((TreeTableColumn<TableItem, String> param) -> new GenericEditableTreeTableCell<>(new TextFieldEditorBuilder()));

        indexColumn.prefWidthProperty().bind(playlist.widthProperty().multiply(0.1));
        artistColumn.prefWidthProperty().bind(playlist.widthProperty().multiply(0.35));
        titleColumn.prefWidthProperty().bind(playlist.widthProperty().multiply(0.35));
        lengthColumn.prefWidthProperty().bind(playlist.widthProperty().multiply(0.2));

        List<JFXTreeTableColumn<TableItem, ? extends Serializable>> columns = asList(indexColumn, artistColumn, titleColumn, lengthColumn);

        columns.forEach(c -> c.setResizable(false));

        playlist.getColumns().setAll(columns);
    }

    private void loadMp3Files() {
        Task<List<TableItem>> playListLoaderTask = new Task<List<TableItem>>() {
            @Override
            protected List<TableItem> call() {
                List<Mp3> files = new PlaylistLoader().load(Paths.get(ChoicePlayerApplication.getSettings().getFolder()));
                return IntStream.range(0, files.size()).mapToObj(index -> new TableItem(index + 1, files.get(index))).collect(Collectors.toList());
            }
        };

        playListLoaderTask.setOnSucceeded(e -> {
            List<TableItem> tableItems = playListLoaderTask.getValue();
            mp3Files.addAll(tableItems);

            PauseTransition wait = new PauseTransition(Duration.millis(1));
            wait.setOnFinished(ev -> {
                animateOutSpinner();
                animateTableItems();
            });
            wait.play();
        });

        new Thread(playListLoaderTask).start();
    }

    private void animateOutSpinner() {
        ParallelTransition parallelTransition = getSpinnerOutAnimation();
        parallelTransition.setOnFinished(e -> playlistStackPane.getChildren().remove(spinner));
        parallelTransition.play();
    }

    private void animateTableItems() {
        int[] delay = new int[1];
        delay[0] = 0;
        final Color backgroundColor = getBackgroundColor();
        rows.forEach((JFXTreeTableRow<TableItem> row) -> {
            ImageView imageView = createImageView(row, backgroundColor);
            delay[0] += DELAY;
            animateRow(row, imageView, delay[0]);
        });
    }

    private Color getBackgroundColor() {
        java.awt.Color color = ChoicePlayerApplication.getSettings().getTheme().getStyle().getBackgroundColor();
        double r = (double) color.getRed() / 255;
        double g = (double) color.getGreen() / 255;
        double b = (double) color.getBlue() / 255;
        double a = (double) color.getAlpha() / 255;
        return new Color(r, g, b, a);
    }

    private ImageView createImageView(final JFXTreeTableRow<TableItem> row, Color color) {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(color);
        final Image image = row.snapshot(params, new WritableImage((int) row.getWidth() - 12, (int) row.getHeight()));
        final ImageView imageView = new ImageView(image);
        imageView.setStyle("-fx-background-color: BLACK");
        imageView.setManaged(false);
        return imageView;
    }

    private void animateRow(JFXTreeTableRow<TableItem> row, ImageView imageView, int delay) {
        row.setOpacity(0);
        setStartStateForImageView(row, imageView);
        rootContainer.getChildren().add(imageView);

        ParallelTransition parallelTransition = getRowTransition(imageView, delay);

        parallelTransition.setOnFinished(e -> {
            row.setOpacity(1);
            imageView.setOpacity(0);
            rootContainer.getChildren().remove(imageView);
        });

        parallelTransition.play();
    }

    private void setStartStateForImageView(JFXTreeTableRow<TableItem> row, ImageView imageView) {
        final Point2D animationStartPoint = row.localToScene(new Point2D(0, -13));
        final Point2D startInRoot = rootContainer.sceneToLocal(animationStartPoint);
        imageView.relocate(startInRoot.getX(), startInRoot.getY());
        imageView.setOpacity(0);
        imageView.setTranslateY(TRANSLATE_Y);
    }

    private ParallelTransition getRowTransition(ImageView imageView, int delay) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(ANIMATION_DURATION), imageView);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);

        TranslateTransition translateTransition = new TranslateTransition(Duration.millis(ANIMATION_DURATION), imageView);
        translateTransition.setByY(-TRANSLATE_Y);

        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.setDelay(Duration.millis(delay));
        parallelTransition.getChildren().addAll(fadeTransition, translateTransition);
        return parallelTransition;
    }

    private ParallelTransition getSpinnerOutAnimation() {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(ANIMATION_DURATION / 7 * 5), spinner);
        fadeTransition.setFromValue(1);
        fadeTransition.setToValue(0);

        TranslateTransition translateTransition = new TranslateTransition(Duration.millis(ANIMATION_DURATION / 7 * 5), spinner);
        translateTransition.setByY(350);

        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.getChildren().addAll(fadeTransition, translateTransition);
        return parallelTransition;
    }
}