package com.gabrielavara.choiceplayer.controls.settings;

import static java.util.Arrays.asList;
import static javafx.geometry.Pos.CENTER_LEFT;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gabrielavara.choiceplayer.ChoicePlayerApplication;
import com.gabrielavara.choiceplayer.controls.animatedlabel.AnimatedLabel;
import com.gabrielavara.choiceplayer.messages.SettingsClosedMessage;
import com.gabrielavara.choiceplayer.messages.ThemeChangedMessage;
import com.gabrielavara.choiceplayer.settings.AccentColor;
import com.gabrielavara.choiceplayer.settings.ColorConverter;
import com.gabrielavara.choiceplayer.settings.ThemeStyle;
import com.gabrielavara.choiceplayer.util.Messenger;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXColorPicker;
import com.jfoenix.controls.JFXComboBox;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;

public class SettingsController implements Initializable {
    private static Logger log = LoggerFactory.getLogger("com.gabrielavara.choiceplayer.controls.settings.SettingsController");
    private static final int CLOSE_BUTTON_MARGIN = 12;

    @FXML
    public AnimatedLabel folderToLoadLabel;
    @FXML
    public AnimatedLabel folderToMoveLikedMusicLabel;
    @FXML
    public JFXComboBox<String> styleComboBox;
    @FXML
    public JFXColorPicker accentColorPicker;
    @FXML
    public JFXButton folderToLoadBrowseButton;
    @FXML
    public JFXButton folderToMoveLikedMusicBrowseButton;
    @FXML
    public AnchorPane titleContainer;
    @FXML
    public Label titleLabel;
    @FXML
    public JFXButton closeButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("language.player");
        folderToLoadLabel.setText(ChoicePlayerApplication.getSettings().getFolder());
        folderToMoveLikedMusicLabel.setText(ChoicePlayerApplication.getSettings().getLikedFolder());
        accentColorPicker.setValue(ChoicePlayerApplication.getColors().getAccentColor());
        ObservableList<String> styles = FXCollections
                .observableList(asList(resourceBundle.getString("settingsStyleLight"), resourceBundle.getString("settingsStyleDark")));
        styleComboBox.setItems(styles);
        styleComboBox.getSelectionModel().select(ChoicePlayerApplication.getSettings().getTheme().getStyle().ordinal());

        closeButton.translateXProperty().bind(titleContainer.widthProperty().subtract(closeButton.widthProperty()).subtract(CLOSE_BUTTON_MARGIN));
        closeButton.setTranslateY(CLOSE_BUTTON_MARGIN);
        titleLabel.translateXProperty().bind(titleContainer.widthProperty().subtract(titleLabel.widthProperty()).divide(2));
        titleLabel.translateYProperty().bind(titleContainer.heightProperty().subtract(titleLabel.heightProperty()).divide(2));

        setFolderLabelColors();
        folderToLoadLabel.setStackPaneAlignment(CENTER_LEFT);
        folderToMoveLikedMusicLabel.setStackPaneAlignment(CENTER_LEFT);

        closeButton.setOnMouseClicked(e -> Messenger.send(new SettingsClosedMessage()));
    }

    @FXML
    public void folderToLoadBrowseButtonClicked(MouseEvent mouseEvent) {
        showDirectoryChooser(folderToLoadLabel, value -> ChoicePlayerApplication.getSettings().setFolder(value));
    }

    @FXML
    public void folderToMoveLikedMusicBrowseButtonClicked(MouseEvent mouseEvent) {
        showDirectoryChooser(folderToMoveLikedMusicLabel, value -> ChoicePlayerApplication.getSettings().setLikedFolder(value));
    }

    private void showDirectoryChooser(AnimatedLabel label, SettingsSetter settingsSetter) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(Paths.get(label.getText()).toFile());
        File selectedDir = directoryChooser.showDialog(ChoicePlayerApplication.getStage());
        if (selectedDir == null) {
            log.info("No directory selected");
        } else {
            log.info("Directory changed to {}", selectedDir.getAbsolutePath());
            label.setText(selectedDir.getAbsolutePath());
            settingsSetter.set(selectedDir.getAbsolutePath());
        }
    }

    @FXML
    public void accentColorPickerChanged(ActionEvent actionEvent) {
        AccentColor accentColor = ColorConverter.convert(accentColorPicker.getValue());
        if (!ChoicePlayerApplication.getSettings().getTheme().getAccentColor().equals(accentColor)) {
            log.info("Accent color changed to {}", accentColor);
            ChoicePlayerApplication.getSettings().getTheme().setAccentColor(accentColor);
            sendThemeChangedMessage();
            setFolderLabelColors();
        }
    }

    @FXML
    public void styleComboBoxChanged(ActionEvent actionEvent) {
        int selectedIndex = styleComboBox.getSelectionModel().getSelectedIndex();
        ThemeStyle style = ThemeStyle.values()[selectedIndex];
        if (!ChoicePlayerApplication.getSettings().getTheme().getStyle().equals(style)) {
            log.info("Style changed to {}", style);
            ChoicePlayerApplication.getSettings().getTheme().setStyle(style);
            sendThemeChangedMessage();
            setFolderLabelColors();
        }
    }

    private void setFolderLabelColors() {
        folderToLoadLabel.setTextFill(ChoicePlayerApplication.getColors().getForegroundColor());
        folderToMoveLikedMusicLabel.setTextFill(ChoicePlayerApplication.getColors().getForegroundColor());
    }

    private void sendThemeChangedMessage() {
        Messenger.send(new ThemeChangedMessage());
    }

    private interface SettingsSetter {
        void set(String value);
    }
}
