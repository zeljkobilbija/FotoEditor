package org.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.ColorInput;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.effect.MotionBlur;
import javafx.scene.effect.SepiaTone;
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
import javafx.stage.Screen;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.prefs.Preferences;

public class Main extends Application {

    private final ImageView prikaz = new ImageView();

    // Trajno čuvanje podešavanja programa (npr. poslednji korišćeni folder)
    private static final String KLJUC_FOLDER = "poslednjiFolder";
    private final Preferences podesavanjaPrograma = Preferences.userNodeForPackage(Main.class);

    // Podešavanje boja (od -1 do 1, početno 0)
    private final Slider svetlina = new Slider(-1, 1, 0);
    private final Slider kontrast = new Slider(-1, 1, 0);
    private final Slider zasicenost = new Slider(-1, 1, 0);
    private final Slider nijansa = new Slider(-1, 1, 0);

    // Pojačanje pojedinačnih RGB kanala (-1 = kanal isključen, 0 = bez promene, 1 = dvostruko)
    private final Slider crvena = new Slider(-1, 1, 0);
    private final Slider zelena = new Slider(-1, 1, 0);
    private final Slider plava = new Slider(-1, 1, 0);
    // Providnost (alfa kanal): 0 = neprovidno, 1 = potpuno providno
    private final Slider providnost = new Slider(0, 1, 0);

    // Filteri (0 znači da je filter isključen)
    private final Slider sepija = new Slider(0, 1, 0);
    private final Slider sjaj = new Slider(0, 1, 0);
    private final Slider bloom = new Slider(0, 1, 0);
    private final Slider zamucenje = new Slider(0, 30, 0);
    private final Slider zamucenjePokretom = new Slider(0, 30, 0);
    private final Slider ugaoPokreta = new Slider(0, 180, 0);

    // Svi klizači čije se vrednosti pamte u istoriji (novi klizač treba dodati ovde)
    private final List<Slider> sviKlizaci = List.of(
            svetlina, kontrast, zasicenost, nijansa,
            crvena, zelena, plava, providnost,
            sepija, sjaj, bloom, zamucenje, zamucenjePokretom, ugaoPokreta);

    // Istorija izmena: svaki element su vrednosti svih klizača redom
    private final Deque<double[]> istorija = new ArrayDeque<>();
    private final Deque<double[]> buducnost = new ArrayDeque<>();
    private double[] poslednjeStanje = trenutnoStanje();
    private boolean bezBelezenja = false;
    // Stanje klizača pri poslednjem čuvanju (ili otvaranju slike, kad su sve vrednosti 0)
    private double[] sacuvanoStanje = trenutnoStanje();
    private final Button undo = new Button("Undo");
    private final Button redo = new Button("Redo");

    @Override
    public void start(Stage prozor) {
        // Dugme za otvaranje slike
        Button otvori = new Button("Otvori sliku");
        otvori.setMaxWidth(Double.MAX_VALUE);
        otvori.setOnAction(e -> otvoriSliku(prozor));

        // Svaka promena klizača osvežava prikaz i beleži se za Undo/Redo
        for (Slider klizac : sviKlizaci) {
            klizac.valueProperty().addListener((o, s, n) -> osveziEfekat());
            pratiPromene(klizac);
        }

        // Dugme za vraćanje na početne vrednosti
        Button resetuj = new Button("Resetuj");
        resetuj.setMaxWidth(Double.MAX_VALUE);
        resetuj.setOnAction(e -> {
            bezBelezenja = true;
            for (Slider klizac : sviKlizaci) {
                klizac.setValue(0);
            }
            bezBelezenja = false;
            zabeleziStanje();
        });

        // Undo vraća korak unazad, Redo ponovo primenjuje poništeni korak
        undo.setMaxWidth(Double.MAX_VALUE);
        undo.setOnAction(e -> {
            if (!istorija.isEmpty()) {
                buducnost.push(poslednjeStanje);
                postaviStanje(istorija.pop());
            }
        });
        redo.setMaxWidth(Double.MAX_VALUE);
        redo.setOnAction(e -> {
            if (!buducnost.isEmpty()) {
                istorija.push(poslednjeStanje);
                postaviStanje(buducnost.pop());
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

        // Dugme za zatvaranje slike bez čuvanja; brišu se i sve izmene i istorija
        Button zatvori = new Button("Zatvori");
        zatvori.setMaxWidth(Double.MAX_VALUE);
        zatvori.disableProperty().bind(prikaz.imageProperty().isNull());
        zatvori.setOnAction(e -> {
            if (smeDaOdbaciIzmene(prozor, "Zatvori sliku", "Zatvoriti bez čuvanja?")) {
                zatvoriSliku();
            }
        });

        // Pri zatvaranju programa takođe pitamo za nesačuvane izmene
        prozor.setOnCloseRequest(e -> {
            if (!smeDaOdbaciIzmene(prozor, "Izlaz", "Izaći iz programa bez čuvanja?")) {
                e.consume();
            }
        });

        // Poseban prozor sa karticama za sve komande, efekte i filtere.
        // Nova grupa komandi se dodaje kao nova kartica preko napraviKarticu(...)
        TabPane kartice = new TabPane(
                napraviKarticu("Svetlo",
                        new Label("Svetlina"), svetlina,
                        new Label("Kontrast"), kontrast),
                napraviKarticu("Boja",
                        new Label("Zasićenost"), zasicenost,
                        new Label("Nijansa (Hue)"), nijansa,
                        new Separator(),
                        new Label("Crvena (R)"), crvena,
                        new Label("Zelena (G)"), zelena,
                        new Label("Plava (B)"), plava,
                        new Separator(),
                        new Label("Providnost (Alfa)"), providnost),
                napraviKarticu("Filteri",
                        new Label("Sepija"), sepija,
                        new Label("Sjaj (Glow)"), sjaj,
                        new Label("Bloom"), bloom,
                        new Label("Zamućenje"), zamucenje,
                        new Label("Zamućenje pokretom"), zamucenjePokretom,
                        new Label("Ugao pokreta"), ugaoPokreta));

        Stage komande = new Stage();
        komande.initOwner(prozor);
        komande.setTitle("Komande");
        Scene scenaKomandi = new Scene(kartice, 300, 700);
        dodajPrecice(scenaKomandi);
        komande.setScene(scenaKomandi);

        // Dugme koje otvara prozor sa komandama (nije vidljiv pri pokretanju)
        Button prikaziKomande = new Button("Otvori Komande");
        prikaziKomande.setMaxWidth(Double.MAX_VALUE);
        prikaziKomande.setOnAction(e -> {
            // Desno od glavnog prozora, na razmaku od 5 mm
            double razmak = 5 / 25.4 * Screen.getPrimary().getDpi();
            komande.setX(prozor.getX() + prozor.getWidth() + razmak);
            komande.setY(prozor.getY());
            komande.show();
            komande.toFront();
        });

        // Na glavnom prozoru ostaju samo dugmad
        VBox kontrole = new VBox(10,
                otvori,
                sacuvaj,
                zatvori,
                undoRedo,
                resetuj,
                prikaziKomande);
        kontrole.setPadding(new Insets(15));
        kontrole.setPrefWidth(180);

        // Prostor za prikaz fotografije
        StackPane centar = new StackPane(prikaz);
        centar.setStyle("-fx-background-color: #2b2b2b;");
        centar.setMinSize(0, 0);

        prikaz.setPreserveRatio(true);
        // RGB efekat zavisi od veličine prikaza, pa ga osvežavamo i kad se prozor menja
        prikaz.layoutBoundsProperty().addListener((o, s, n) -> osveziEfekat());
        osveziEfekat();
        prikaz.fitWidthProperty().bind(centar.widthProperty().subtract(20));
        prikaz.fitHeightProperty().bind(centar.heightProperty().subtract(20));

        BorderPane koren = new BorderPane();
        koren.setLeft(kontrole);
        koren.setCenter(centar);

        prozor.setTitle("FotoEditor");
        Scene scena = new Scene(koren, 1100, 700);
        dodajPrecice(scena);
        prozor.setScene(scena);
        prozor.show();

        // Prozor sa komandama je iste visine kao glavni prozor
        komande.setHeight(prozor.getHeight());
        prozor.heightProperty().addListener((o, s, n) -> komande.setHeight(n.doubleValue()));
    }

    // Pravi lanac efekata od trenutnih vrednosti klizača. Filteri čija je vrednost 0 se preskaču.
    // sirina, visina: veličina slike na koju se efekat primenjuje
    // razmera: koliko je puta slika veća od prikaza (zamućenje se zadaje u pikselima prikaza)
    private Effect napraviEfekat(double sirina, double visina, double razmera) {
        Effect efekat = new ColorAdjust(
                nijansa.getValue(), zasicenost.getValue(), svetlina.getValue(), kontrast.getValue());

        // RGB kanali: množenje bojom smanjuje kanal (faktor do 1),
        // a za faktor preko 1 dodaje se još jedna pomnožena kopija slike
        double r = 1 + crvena.getValue();
        double g = 1 + zelena.getValue();
        double b = 1 + plava.getValue();
        if (r != 1 || g != 1 || b != 1) {
            Effect osnova = efekat;
            Blend smanjenje = new Blend(BlendMode.MULTIPLY, osnova, new ColorInput(0, 0, sirina, visina,
                    Color.color(Math.min(r, 1), Math.min(g, 1), Math.min(b, 1))));
            efekat = smanjenje;
            if (r > 1 || g > 1 || b > 1) {
                Blend dodatak = new Blend(BlendMode.MULTIPLY, osnova, new ColorInput(0, 0, sirina, visina,
                        Color.color(Math.max(r - 1, 0), Math.max(g - 1, 0), Math.max(b - 1, 0))));
                efekat = new Blend(BlendMode.ADD, smanjenje, dodatak);
            }
        }

        if (sepija.getValue() > 0) {
            SepiaTone sepiaTone = new SepiaTone(sepija.getValue());
            sepiaTone.setInput(efekat);
            efekat = sepiaTone;
        }
        if (sjaj.getValue() > 0) {
            Glow glow = new Glow(sjaj.getValue());
            glow.setInput(efekat);
            efekat = glow;
        }
        if (bloom.getValue() > 0) {
            // Bloom osvetljava delove svetlije od praga; jači klizač = niži prag
            Bloom bl = new Bloom(1 - bloom.getValue());
            bl.setInput(efekat);
            efekat = bl;
        }
        if (zamucenje.getValue() > 0) {
            // GaussianBlur podržava poluprečnik najviše 63
            GaussianBlur blur = new GaussianBlur(Math.min(63, zamucenje.getValue() * razmera));
            blur.setInput(efekat);
            efekat = blur;
        }
        if (zamucenjePokretom.getValue() > 0) {
            MotionBlur motion = new MotionBlur(
                    ugaoPokreta.getValue(), Math.min(63, zamucenjePokretom.getValue() * razmera));
            motion.setInput(efekat);
            efekat = motion;
        }
        return efekat;
    }

    private void osveziEfekat() {
        prikaz.setEffect(napraviEfekat(prikaz.getLayoutBounds().getWidth(), prikaz.getLayoutBounds().getHeight(), 1));
        prikaz.setOpacity(1 - providnost.getValue());
    }

    // Prečice: Ctrl+Z za Undo, Ctrl+Y ili Ctrl+Shift+Z za Redo (rade u oba prozora)
    private void dodajPrecice(Scene scena) {
        scena.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), undo::fire);
        scena.getAccelerators().put(new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN), redo::fire);
        scena.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), redo::fire);
    }

    private static Tab napraviKarticu(String naziv, Node... sadrzaj) {
        VBox okvir = new VBox(10, sadrzaj);
        okvir.setPadding(new Insets(15));
        Tab kartica = new Tab(naziv, okvir);
        kartica.setClosable(false);
        return kartica;
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

    private double[] trenutnoStanje() {
        return sviKlizaci.stream().mapToDouble(Slider::getValue).toArray();
    }

    // Postavlja klizače na zadato stanje bez beleženja u istoriju
    private void postaviStanje(double[] stanje) {
        bezBelezenja = true;
        for (int i = 0; i < sviKlizaci.size(); i++) {
            sviKlizaci.get(i).setValue(stanje[i]);
        }
        bezBelezenja = false;
        poslednjeStanje = stanje;
        osveziDugmad();
    }

    private void zabeleziStanje() {
        if (bezBelezenja) {
            return;
        }
        double[] trenutno = trenutnoStanje();
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
        postaviPocetniFolder(izbor);
        File fajl = izbor.showOpenDialog(prozor);
        if (fajl != null) {
            zapamtiFolder(fajl);
        }
        if (fajl != null && smeDaOdbaciIzmene(prozor, "Otvori sliku", "Otvoriti novu sliku bez čuvanja?")) {
            // Nova slika počinje od nule: bez izmena i bez istorije
            zatvoriSliku();
            prikaz.setImage(new Image(fajl.toURI().toString()));
        }
    }

    // Dijalog se otvara u poslednjem korišćenom folderu (ako još uvek postoji)
    private void postaviPocetniFolder(FileChooser izbor) {
        String putanja = podesavanjaPrograma.get(KLJUC_FOLDER, null);
        if (putanja != null) {
            File folder = new File(putanja);
            if (folder.isDirectory()) {
                izbor.setInitialDirectory(folder);
            }
        }
    }

    // Pamti folder izabranog fajla; čuva se i posle zatvaranja programa
    private void zapamtiFolder(File fajl) {
        File folder = fajl.getParentFile();
        if (folder != null) {
            podesavanjaPrograma.put(KLJUC_FOLDER, folder.getAbsolutePath());
        }
    }

    // Vraća true ako nema nesačuvanih izmena ili ako korisnik potvrdi da ih odbacuje
    private boolean smeDaOdbaciIzmene(Stage prozor, String naslov, String pitanjeTekst) {
        if (prikaz.getImage() == null || Arrays.equals(trenutnoStanje(), sacuvanoStanje)) {
            return true;
        }
        Alert pitanje = new Alert(Alert.AlertType.CONFIRMATION,
                "Slika ima nesačuvane izmene. " + pitanjeTekst,
                ButtonType.YES, ButtonType.NO);
        pitanje.setTitle(naslov);
        pitanje.setHeaderText(null);
        pitanje.initOwner(prozor);
        return pitanje.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    // Zatvara sliku bez čuvanja i vraća sve klizače i istoriju na početak
    private void zatvoriSliku() {
        prikaz.setImage(null);
        postaviStanje(new double[sviKlizaci.size()]);
        istorija.clear();
        buducnost.clear();
        sacuvanoStanje = trenutnoStanje();
        osveziDugmad();
    }

    private void sacuvajSliku(Stage prozor) {
        FileChooser izbor = new FileChooser();
        izbor.setTitle("Sačuvaj fotografiju");
        FileChooser.ExtensionFilter png = new FileChooser.ExtensionFilter("PNG slika", "*.png");
        FileChooser.ExtensionFilter jpg = new FileChooser.ExtensionFilter("JPG slika", "*.jpg", "*.jpeg");
        izbor.getExtensionFilters().addAll(png, jpg);
        postaviPocetniFolder(izbor);
        File fajl = izbor.showSaveDialog(prozor);
        if (fajl == null) {
            return;
        }
        zapamtiFolder(fajl);

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

        // Efekte primenjujemo na sliku u punoj rezoluciji, ne na umanjeni prikaz
        Image original = prikaz.getImage();
        double sirinaPrikaza = prikaz.getLayoutBounds().getWidth();
        double razmera = sirinaPrikaza > 0 ? original.getWidth() / sirinaPrikaza : 1;
        ImageView puna = new ImageView(original);
        puna.setEffect(napraviEfekat(original.getWidth(), original.getHeight(), razmera));

        SnapshotParameters parametri = new SnapshotParameters();
        // JPG ne podržava providnost, pa providne delove popunjavamo belom bojom
        parametri.setFill(jeJpg ? Color.WHITE : Color.TRANSPARENT);
        // Zamućenje širi sliku preko ivica; snimamo samo deo veličine originala
        parametri.setViewport(new Rectangle2D(0, 0, original.getWidth(), original.getHeight()));
        WritableImage snimak = puna.snapshot(parametri, null);

        try {
            BufferedImage zaUpis = uBufferedImage(snimak, jeJpg, 1 - providnost.getValue());
            ImageIO.write(zaUpis, jeJpg ? "jpg" : "png", fajl);
            sacuvanoStanje = trenutnoStanje();
        } catch (IOException ex) {
            Alert greska = new Alert(Alert.AlertType.ERROR, "Čuvanje nije uspelo: " + ex.getMessage());
            greska.initOwner(prozor);
            greska.showAndWait();
        }
    }

    // Pretvara JavaFX sliku u AWT sliku koju ImageIO ume da upiše.
    // alfa: faktor neprovidnosti (1 = bez promene); za JPG se providni delovi mešaju sa belom bojom
    private static BufferedImage uBufferedImage(Image slika, boolean bezProvidnosti, double alfa) {
        int sirina = (int) slika.getWidth();
        int visina = (int) slika.getHeight();
        BufferedImage rezultat = new BufferedImage(sirina, visina,
                bezProvidnosti ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        PixelReader citac = slika.getPixelReader();
        for (int y = 0; y < visina; y++) {
            for (int x = 0; x < sirina; x++) {
                int argb = citac.getArgb(x, y);
                if (alfa < 1) {
                    int a = (int) Math.round((argb >>> 24) * alfa);
                    if (bezProvidnosti) {
                        double udeo = a / 255.0;
                        int cr = (int) Math.round(((argb >> 16) & 0xFF) * udeo + 255 * (1 - udeo));
                        int cg = (int) Math.round(((argb >> 8) & 0xFF) * udeo + 255 * (1 - udeo));
                        int cb = (int) Math.round((argb & 0xFF) * udeo + 255 * (1 - udeo));
                        argb = 0xFF000000 | (cr << 16) | (cg << 8) | cb;
                    } else {
                        argb = (a << 24) | (argb & 0x00FFFFFF);
                    }
                }
                rezultat.setRGB(x, y, argb);
            }
        }
        return rezultat;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
