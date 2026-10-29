package org.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.function.Consumer;

public class Main extends Application {

    private final ImageView prikaz = new ImageView();
    private final ColorAdjust podesavanje = new ColorAdjust();

    // Istorija izmena: svaki element je {svetlina, kontrast, zasićenost}
    private final Deque<double[]> istorija = new ArrayDeque<>();
    private final Deque<double[]> buducnost = new ArrayDeque<>();
    private double[] poslednjeStanje = {0, 0, 0};
    private boolean bezBelezenja = false;
    private final Button undo = new Button("Undo");
    private final Button redo = new Button("Redo");

    @Override
    public void start(Stage prozor) {
        // Dugme za otvaranje slike
        Button otvori = new Button("Otvori sliku");
        otvori.setMaxWidth(Double.MAX_VALUE);
        otvori.setOnAction(e -> otvoriSliku(prozor));

        // Klizači (od -1 do 1, početno 0)
        Slider svetlina = new Slider(-1, 1, 0);
        Slider kontrast = new Slider(-1, 1, 0);
        Slider zasicenost = new Slider(-1, 1, 0);

        svetlina.valueProperty().addListener((o, s, n) -> podesavanje.setBrightness(n.doubleValue()));
        kontrast.valueProperty().addListener((o, s, n) -> podesavanje.setContrast(n.doubleValue()));
        zasicenost.valueProperty().addListener((o, s, n) -> podesavanje.setSaturation(n.doubleValue()));

        // Beleženje izmena za poništavanje
        pratiPromene(svetlina);
        pratiPromene(kontrast);
        pratiPromene(zasicenost);

        // Dugme za vraćanje na početne vrednosti
        Button resetuj = new Button("Resetuj");
        resetuj.setMaxWidth(Double.MAX_VALUE);
        resetuj.setOnAction(e -> {
            bezBelezenja = true;
            svetlina.setValue(0);
            kontrast.setValue(0);
            zasicenost.setValue(0);
            bezBelezenja = false;
            zabeleziStanje();
        });

        // Postavlja klizače na zadato stanje bez beleženja u istoriju
        Consumer<double[]> postaviStanje = stanje -> {
            bezBelezenja = true;
            svetlina.setValue(stanje[0]);
            kontrast.setValue(stanje[1]);
            zasicenost.setValue(stanje[2]);
            bezBelezenja = false;
            poslednjeStanje = stanje;
            osveziDugmad();
        };

        // Undo vraća korak unazad, Redo ponovo primenjuje poništeni korak
        undo.setMaxWidth(Double.MAX_VALUE);
        undo.setOnAction(e -> {
            if (!istorija.isEmpty()) {
                buducnost.push(poslednjeStanje);
                postaviStanje.accept(istorija.pop());
            }
        });
        redo.setMaxWidth(Double.MAX_VALUE);
        redo.setOnAction(e -> {
            if (!buducnost.isEmpty()) {
                istorija.push(poslednjeStanje);
                postaviStanje.accept(buducnost.pop());
            }
        });
        osveziDugmad();

        HBox undoRedo = new HBox(10, undo, redo);
        HBox.setHgrow(undo, Priority.ALWAYS);
        HBox.setHgrow(redo, Priority.ALWAYS);

        // Dugme za čuvanje izmenjene fotografije (aktivno tek kad je slika učitana)
        Button sacuvaj = new Button("Sačuvaj");
        sacuvaj.setMaxWidth(Double.MAX_VALUE);
        sacuvaj.disableProperty().bind(prikaz.imageProperty().isNull());
        sacuvaj.setOnAction(e -> sacuvajSliku(prozor));

        VBox kontrole = new VBox(10,
                otvori,
                new Label("Svetlina"), svetlina,
                new Label("Kontrast"), kontrast,
                new Label("Zasićenost"), zasicenost,
                undoRedo,
                resetuj,
                sacuvaj);
        kontrole.setPadding(new Insets(15));
        kontrole.setPrefWidth(220);

        // Prostor za prikaz fotografije
        StackPane centar = new StackPane(prikaz);
        centar.setStyle("-fx-background-color: #2b2b2b;");
        centar.setMinSize(0, 0);

        prikaz.setPreserveRatio(true);
        prikaz.setEffect(podesavanje);
        prikaz.fitWidthProperty().bind(centar.widthProperty().subtract(20));
        prikaz.fitHeightProperty().bind(centar.heightProperty().subtract(20));

        BorderPane koren = new BorderPane();
        koren.setLeft(kontrole);
        koren.setCenter(centar);

        prozor.setTitle("FotoEditor");
        Scene scena = new Scene(koren, 1100, 700);
        // Prečice: Ctrl+Z za Undo, Ctrl+Y ili Ctrl+Shift+Z za Redo
        scena.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), undo::fire);
        scena.getAccelerators().put(new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN), redo::fire);
        scena.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), redo::fire);
        prozor.setScene(scena);
        prozor.show();
    }

    // Beleži stanje kad korisnik završi pomeranje klizača (ili klikne na njega)
    private void pratiPromene(Slider klizac) {
        klizac.valueProperty().addListener((o, s, n) -> {
            if (!klizac.isValueChanging()) {
                zabeleziStanje();
            }
        });
        klizac.valueChangingProperty().addListener((o, s, menjaSe) -> {
            if (!menjaSe) {
                zabeleziStanje();
            }
        });
    }

    private void zabeleziStanje() {
        if (bezBelezenja) {
            return;
        }
        double[] trenutno = {podesavanje.getBrightness(), podesavanje.getContrast(), podesavanje.getSaturation()};
        if (Arrays.equals(trenutno, poslednjeStanje)) {
            return;
        }
        istorija.push(poslednjeStanje);
        poslednjeStanje = trenutno;
        // Nova izmena briše poništene korake
        buducnost.clear();
        osveziDugmad();
    }

    private void osveziDugmad() {
        undo.setDisable(istorija.isEmpty());
        redo.setDisable(buducnost.isEmpty());
    }

    private void otvoriSliku(Stage prozor) {
        FileChooser izbor = new FileChooser();
        izbor.setTitle("Izaberi fotografiju");
        izbor.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Slike", "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.gif"));
        File fajl = izbor.showOpenDialog(prozor);
        if (fajl != null) {
            prikaz.setImage(new Image(fajl.toURI().toString()));
        }
    }

    private void sacuvajSliku(Stage prozor) {
        FileChooser izbor = new FileChooser();
        izbor.setTitle("Sačuvaj fotografiju");
        FileChooser.ExtensionFilter png = new FileChooser.ExtensionFilter("PNG slika", "*.png");
        FileChooser.ExtensionFilter jpg = new FileChooser.ExtensionFilter("JPG slika", "*.jpg", "*.jpeg");
        izbor.getExtensionFilters().addAll(png, jpg);
        File fajl = izbor.showSaveDialog(prozor);
        if (fajl == null) {
            return;
        }

        // Format određujemo po ekstenziji, a ako je nema, po izabranom filteru
        String ime = fajl.getName().toLowerCase();
        boolean jeJpg;
        if (ime.endsWith(".jpg") || ime.endsWith(".jpeg")) {
            jeJpg = true;
        } else if (ime.endsWith(".png")) {
            jeJpg = false;
        } else {
            jeJpg = izbor.getSelectedExtensionFilter() == jpg;
            fajl = new File(fajl.getParentFile(), fajl.getName() + (jeJpg ? ".jpg" : ".png"));
        }

        // Efekat primenjujemo na sliku u punoj rezoluciji, ne na umanjeni prikaz
        ImageView puna = new ImageView(prikaz.getImage());
        ColorAdjust efekat = new ColorAdjust(
                podesavanje.getHue(),
                podesavanje.getSaturation(),
                podesavanje.getBrightness(),
                podesavanje.getContrast());
        puna.setEffect(efekat);

        SnapshotParameters parametri = new SnapshotParameters();
        // JPG ne podržava providnost, pa providne delove popunjavamo belom bojom
        parametri.setFill(jeJpg ? Color.WHITE : Color.TRANSPARENT);
        WritableImage snimak = puna.snapshot(parametri, null);

        try {
            ImageIO.write(uBufferedImage(snimak, jeJpg), jeJpg ? "jpg" : "png", fajl);
        } catch (IOException ex) {
            Alert greska = new Alert(Alert.AlertType.ERROR, "Čuvanje nije uspelo: " + ex.getMessage());
            greska.initOwner(prozor);
            greska.showAndWait();
        }
    }

    // Pretvara JavaFX sliku u AWT sliku koju ImageIO ume da upiše
    private static BufferedImage uBufferedImage(Image slika, boolean bezProvidnosti) {
        int sirina = (int) slika.getWidth();
        int visina = (int) slika.getHeight();
        BufferedImage rezultat = new BufferedImage(sirina, visina,
                bezProvidnosti ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        PixelReader citac = slika.getPixelReader();
        for (int y = 0; y < visina; y++) {
            for (int x = 0; x < sirina; x++) {
                rezultat.setRGB(x, y, citac.getArgb(x, y));
            }
        }
        return rezultat;
    }

    public static void main(String[] args) {
        launch(args);
    }
}