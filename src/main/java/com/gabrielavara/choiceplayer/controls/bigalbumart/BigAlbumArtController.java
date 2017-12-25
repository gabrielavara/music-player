package com.gabrielavara.choiceplayer.controls.bigalbumart;

import static com.gabrielavara.choiceplayer.Constants.ALBUM_ART_SCALE;
import static com.gabrielavara.choiceplayer.Constants.ANIMATION_DURATION;
import static com.gabrielavara.choiceplayer.Constants.BIG_ALBUM_ART_TRANSLATE_X;
import static com.gabrielavara.choiceplayer.Constants.BIG_ALBUM_ART_TRANSLATE_Y;
import static com.gabrielavara.choiceplayer.controls.bigalbumart.AnimationDirection.IN;
import static com.gabrielavara.choiceplayer.controls.bigalbumart.AnimationDirection.OUT;
import static com.gabrielavara.choiceplayer.controls.bigalbumart.Direction.FORWARD;
import static javafx.animation.Interpolator.EASE_IN;
import static javafx.animation.Interpolator.EASE_OUT;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import com.gabrielavara.choiceplayer.util.ImageUtil;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

public class BigAlbumArtController implements Initializable {
    @FXML
    public ImageView albumArt;
    @FXML
    public ImageView grayScaleAlbumArt;
    @FXML
    public AnchorPane pane;

    private boolean isAlbumArtShowed = true;

    private ImageView getFrontImageView() {
        return isAlbumArtShowed ? albumArt : grayScaleAlbumArt;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        albumArt.setImage(ImageUtil.getDefaultImage());
        grayScaleAlbumArt.setImage(ImageUtil.getDefaultImage());
        grayScaleAlbumArt.setOpacity(0);
        pane.getChildren().remove(grayScaleAlbumArt);
    }

    public void animatePlayPause(AnimationDirection animationDirection) {
        pane.getChildren().add(0, animationDirection == OUT ? grayScaleAlbumArt : albumArt);
        FadeTransition fadeTransition = getFadeTransition(animationDirection, albumArt);
        FadeTransition grayScaleFadeTransition = getFadeTransition(animationDirection.getInverse(), grayScaleAlbumArt);
        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.getChildren().addAll(fadeTransition, grayScaleFadeTransition);
        parallelTransition.setOnFinished(e -> {
            if (animationDirection == OUT) {
                pane.getChildren().remove(albumArt);
                isAlbumArtShowed = false;
            } else {
                pane.getChildren().remove(grayScaleAlbumArt);
                isAlbumArtShowed = true;
            }
        });
        parallelTransition.play();
    }

    public void setImage(Optional<byte[]> albumArtData, Direction direction) {
        Image albumArtImage = ImageUtil.getAlbumArt(albumArtData);
        Image grayScaleAlbumArtImage = ImageUtil.getGrayScaleAlbumArt(albumArtData);

        ParallelTransition outTransition = getParallelTransition(OUT, direction);

        outTransition.setOnFinished(e -> {
            albumArt.setImage(albumArtImage);
            grayScaleAlbumArt.setImage(grayScaleAlbumArtImage);
            if (!isAlbumArtShowed) {
                showAlbumArt();
            }
            ParallelTransition inTransition = getParallelTransition(IN, direction.getInverse());
            inTransition.play();
        });

        outTransition.play();
    }

    private void showAlbumArt() {
        albumArt.setTranslateX(grayScaleAlbumArt.getTranslateX());
        albumArt.setScaleX(grayScaleAlbumArt.getScaleX());
        albumArt.setScaleY(grayScaleAlbumArt.getScaleY());
        albumArt.setOpacity(0);
        pane.getChildren().add(albumArt);

        pane.getChildren().remove(grayScaleAlbumArt);
        grayScaleAlbumArt.setTranslateX(0);
        grayScaleAlbumArt.setScaleX(1);
        grayScaleAlbumArt.setScaleY(1);
        isAlbumArtShowed = true;
    }

    private ParallelTransition getParallelTransition(AnimationDirection animationDirection, Direction direction) {
        ImageView imageView = getFrontImageView();
        FadeTransition fadeTransition = getFadeTransition(animationDirection, imageView);
        ScaleTransition scaleXTransition = getScaleXTransition(animationDirection, imageView);
        ScaleTransition scaleYTransition = getScaleYTransition(animationDirection, imageView);
        TranslateTransition translateXTransition = getTranslateXTransition(animationDirection, direction, imageView);
        TranslateTransition translateYTransition = getTranslateYTransition(animationDirection, imageView);

        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.getChildren().addAll(fadeTransition, scaleXTransition, scaleYTransition, translateXTransition, translateYTransition);
        return parallelTransition;
    }

    private FadeTransition getFadeTransition(AnimationDirection animationDirection, ImageView imageView) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(ANIMATION_DURATION), imageView);
        fadeTransition.setFromValue(animationDirection == OUT ? 1 : 0);
        fadeTransition.setToValue(animationDirection == OUT ? 0 : 1);
        fadeTransition.setInterpolator(animationDirection == IN ? EASE_IN : EASE_OUT);
        return fadeTransition;
    }

    private ScaleTransition getScaleXTransition(AnimationDirection animationDirection, ImageView imageView) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(ANIMATION_DURATION), imageView);
        scaleTransition.setFromX(imageView.getScaleX());
        scaleTransition.setToX(animationDirection == OUT ? ALBUM_ART_SCALE : 1);
        return scaleTransition;
    }

    private ScaleTransition getScaleYTransition(AnimationDirection animationDirection, ImageView imageView) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(ANIMATION_DURATION), imageView);
        scaleTransition.setFromY(imageView.getScaleY());
        scaleTransition.setToY(animationDirection == OUT ? ALBUM_ART_SCALE : 1);
        return scaleTransition;
    }

    private TranslateTransition getTranslateXTransition(AnimationDirection animationDirection, Direction direction, ImageView imageView) {
        TranslateTransition translateTransition = new TranslateTransition(Duration.millis(ANIMATION_DURATION), imageView);
        int translateX = direction == FORWARD ? -BIG_ALBUM_ART_TRANSLATE_X : BIG_ALBUM_ART_TRANSLATE_X;
        translateTransition.setFromX(animationDirection == IN ? translateX : 0);
        translateTransition.setToX(animationDirection == IN ? 0 : translateX);
        translateTransition.setInterpolator(animationDirection == IN ? EASE_IN : EASE_OUT);
        return translateTransition;
    }

    private TranslateTransition getTranslateYTransition(AnimationDirection animationDirection, ImageView imageView) {
        TranslateTransition translateTransition = new TranslateTransition(Duration.millis(ANIMATION_DURATION), imageView);
        translateTransition.setFromY(imageView.getTranslateY());
        translateTransition.setToY(animationDirection == IN ? 0 : BIG_ALBUM_ART_TRANSLATE_Y);
        translateTransition.setInterpolator(animationDirection == IN ? EASE_OUT : EASE_IN);
        return translateTransition;
    }
}
