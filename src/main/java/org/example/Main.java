package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.ColorInput;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.effect.ImageInput;
import javafx.scene.effect.MotionBlur;
import javafx.scene.effect.SepiaTone;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.StringConverter;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Main extends Application {

    private final ImageView prikaz = new ImageView();

    // Trajno čuvanje podešavanja programa (npr. poslednji korišćeni folder)
    private static final String KLJUC_FOLDER = "poslednjiFolder";
    private static final String KLJUC_NEDAVNE = "nedavneSlike";
    private static final String KLJUC_NAJVECA_STRANA = "najvecaStranaPriCuvanju";
    private static final String KLJUC_KVALITET_JPG = "kvalitetJpg";
    private static final String KLJUC_PALETA = "paleta.";
    private static final String KLJUC_KARTICA = "izabranaKartica";
    private static final int NAJVISE_NEDAVNIH = 8;
    private static final List<String> PODRZANE_EKSTENZIJE = List.of(".jpg", ".jpeg", ".png", ".bmp", ".gif");

    private Stage glavniProzor;
    // Meni sa poslednjih nekoliko otvorenih slika, na glavnom prozoru ispod "Otvori sliku"
    private final MenuButton nedavneDugme = new MenuButton("Nedavne slike");

    // Podešavanja čuvanja: najveća duža strana slike u pikselima (0 = originalna veličina) i kvalitet JPG-a
    private final ChoiceBox<Integer> najvecaStrana =
            new ChoiceBox<>(FXCollections.observableArrayList(0, 3840, 2560, 1920, 1280, 800));
    private final Slider kvalitetJpg = new Slider(0.5, 1, 0.9);
    private final Label oznakaKvaliteta = new Label();
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
    // Temperatura boje: -1 = hladno (plavičasto), 0 = bez promene, 1 = toplo (narandžasto)
    private final Slider temperatura = new Slider(-1, 1, 0);

    // Negativ: 0 = obična slika, 1 = potpuno obrnute boje
    private final Slider negativ = new Slider(0, 1, 0);

    // Filteri (0 znači da je filter isključen)
    private final Slider ostrina = new Slider(0, 1, 0);
    private final Slider vinjeta = new Slider(0, 1, 0);
    private final Slider zrno = new Slider(0, 1, 0);
    private final Slider sepija = new Slider(0, 1, 0);
    private final Slider sjaj = new Slider(0, 1, 0);
    private final Slider bloom = new Slider(0, 1, 0);
    private final Slider zamucenje = new Slider(0, 30, 0);
    private final Slider zamucenjePokretom = new Slider(0, 30, 0);
    private final Slider ugaoPokreta = new Slider(0, 180, 0);

    // Geometrija: ovi klizači se ne prikazuju, menjaju se dugmadima na kartici "Geometrija",
    // ali su klizači da bi radili sa Undo/Redo, Resetuj i proverom nesačuvanih izmena
    // rotacija: broj okreta za 90° udesno (0-3); ogledala: 0 = ne, 1 = da
    private final Slider rotacija = new Slider(0, 3, 0);
    private final Slider ogledaloLevoDesno = new Slider(0, 1, 0);
    private final Slider ogledaloGoreDole = new Slider(0, 1, 0);
    // Isecanje: koliki se deo slike (od 0 do 1) odseca sa svake strane, u neokrenutoj slici
    private final Slider isecanjeLevo = new Slider(0, 1, 0);
    private final Slider isecanjeGore = new Slider(0, 1, 0);
    private final Slider isecanjeDesno = new Slider(0, 1, 0);
    private final Slider isecanjeDole = new Slider(0, 1, 0);

    // Tekst na slici. Crta se preko gotove slike (posle svih filtera i rotacije), pa ga filteri ne menjaju.
    // Sam tekst nije broj, pa se čuva u spisku tekstovi, a skriveni klizač tekstIndeks pamti njegov
    // redni broj; tako i tekst radi sa Undo/Redo i proverom nesačuvanih izmena (0 = bez teksta).
    private final List<String> tekstovi = new ArrayList<>(List.of(""));
    private final Slider tekstIndeks = new Slider(0, 1e9, 0);
    private final TextField tekstPolje = new TextField();
    // velicina: 0 = najmanji (3% visine slike), 1 = najveći (23%); polozaj: -1 = levo/gore, 0 = sredina, 1 = desno/dole
    private final Slider velicinaTeksta = new Slider(0, 1, 0);
    private final Slider tekstX = new Slider(-1, 1, 0);
    private final Slider tekstY = new Slider(-1, 1, 0);
    private final Slider rotacijaTeksta = new Slider(-180, 180, 0);
    // Senka teksta: jačina (0 = bez senke), udaljenost (0 = 6% veličine slova, 1 = 40%),
    // smer (0 = dole desno, ugao u stepenima u smeru kazaljke) i zamućenje (1 = 30% veličine slova)
    private final Slider senkaTeksta = new Slider(0, 1, 0);
    private final Slider udaljenostSenke = new Slider(0, 1, 0);
    private final Slider smerSenke = new Slider(0, 360, 0);
    private final Slider zamucenjeSenke = new Slider(0, 1, 0);
    private final Slider bojaSenke = new Slider(0, 0xFFFFFFFFL, 0);
    // Skriveni klizači za izbor fonta, podebljanja i boje (menjaju se preko izbornika na kartici "Tekst")
    private static final List<String> FONTOVI = List.of(
            "Arial", "Verdana", "Times New Roman", "Georgia", "Courier New", "Impact", "Comic Sans MS");
    private final Slider fontTeksta = new Slider(0, FONTOVI.size() - 1, 0);
    private final Slider podebljanTekst = new Slider(0, 1, 0);
    private final Slider bojaTeksta = new Slider(0, 0xFFFFFFFFL, 0);

    // Okvir oko slike: debljina je udeo kraće strane slike (0 = bez okvira);
    // polaroid ima 3,5 puta širu donju ivicu
    private final Slider debljinaOkvira = new Slider(0, 0.2, 0);
    private final Slider polaroidOkvir = new Slider(0, 1, 0);
    private final Slider bojaOkvira = new Slider(0, 0xFFFFFFFFL, 0);

    // true dok korisnik mišem pomera tekst; razlika između tačke hvatanja i središta teksta
    private boolean pomeraTekst = false;
    private final double[] hvatanjeTeksta = new double[2];

    // Potezi četkicom, ranije dodati tekstovi i ispravke crvenih očiju (vidi zapis Slojevi)
    private final List<Slojevi> verzijeSlojeva = new ArrayList<>(List.of(PRAZNI_SLOJEVI));
    private final Slider verzijaSlojeva = new Slider(0, 1e9, 0);
    // Ranije dodati tekstovi na prikazu (ispod teksta koji se uređuje)
    private final Pane dodatiTekstovi = new Pane();
    private final List<Text> cvoroviDodatihTekstova = new ArrayList<>();

    // Paleta za crtanje (kartica "Crtanje"); to su podešavanja alatke, ne izmene slike, pa nisu u istoriji,
    // ali se pamte i posle zatvaranja programa
    private final ImageView crtezPrikaz = new ImageView();
    private final ToggleGroup alati = new ToggleGroup();
    private final CheckBox popunjenOblik = new CheckBox("Popunjen oblik (strelica, pravougaonik, elipsa)");
    private final ColorPicker bojaCetkice = new ColorPicker(Color.RED);
    // debljina: 1 do 100, kao hiljaditi delovi visine slike puta dva (100 = 20% visine)
    private final Slider debljinaCetkice = new Slider(1, 100, 10);
    private final Slider neprovidnostCetkice = new Slider(0.05, 1, 1);
    private final Slider mekocaCetkice = new Slider(0, 1, 0.5);
    // Tačke poteza koji se upravo crta (null kad se ne crta)
    private List<Double> trenutneTackeX;
    private List<Double> trenutneTackeY;
    private long semePoteza;

    // Šta radi miš na slici: crta ili uklanja crvene oči (ako nijedno nije izabrano, pomera tekst i sliku)
    private final ToggleGroup rezimMisa = new ToggleGroup();
    private final ToggleButton crtajDugme = new ToggleButton("Crtaj po slici");
    private final ToggleButton crveneOciDugme = new ToggleButton("Ukloni crvene oči (klikni na oko)");
    // Poluprečnik kruga za crvene oči, u pikselima ekrana
    private final Slider velicinaOka = new Slider(4, 80, 15);
    private final Circle indikatorOka = new Circle();

    // Nalepnice na prikazu i klizači za izabranu nalepnicu (kartica "Nalepnice")
    private final Pane slojNalepnica = new Pane();
    private final List<ImageView> cvoroviNalepnica = new ArrayList<>();
    private int izabranaNalepnica = -1;
    private final Label izabranaNalepnicaOznaka = new Label();
    private final Slider velicinaNalepnice = new Slider(0.02, 1, 0.25);
    private final Slider neprovidnostNalepnice = new Slider(0, 1, 1);
    private final Slider rotacijaNalepnice = new Slider(-180, 180, 0);
    // true dok se klizači nalepnice postavljaju iz kôda (tada ne menjaju nalepnicu)
    private boolean postavljaKlizaceNalepnice = false;
    // Verzija slojeva napravljena na početku trenutnog pokreta (vidi izmeniNalepnicu), ili -1
    private int verzijaPokreta = -1;
    private boolean pomeraNalepnicu = false;
    private final double[] hvatanjeNalepnice = new double[2];

    // Prikaz teksta i okvira preko slike na glavnom prozoru
    private final Rectangle okvirPrikaz = new Rectangle();
    private final Text tekstPrikaz = new Text();
    private final Text senkaPrikaz = new Text();

    // Gotovi stilovi ne diraju geometriju, isecanje, tekst i okvir
    private final List<Slider> klizaciVanStila = List.of(
            rotacija, ogledaloLevoDesno, ogledaloGoreDole,
            isecanjeLevo, isecanjeGore, isecanjeDesno, isecanjeDole,
            tekstIndeks, velicinaTeksta, tekstX, tekstY, senkaTeksta, fontTeksta, podebljanTekst, bojaTeksta,
            rotacijaTeksta, udaljenostSenke, smerSenke, zamucenjeSenke, bojaSenke,
            debljinaOkvira, polaroidOkvir, bojaOkvira, verzijaSlojeva);
    // true dok korisnik mišem bira deo slike za isecanje
    private boolean biraIsecak = false;

    // Zumiranje i pomeranje prikaza; ne utiče na sačuvanu sliku i ne beleži se u istoriju
    private static final double NAJVECE_UVECANJE = 10;
    private double uvecanje = 1;
    private double pomakX = 0;
    private double pomakY = 0;
    // true dok je pritisnuto dugme "Pre / posle" (prikazuje se slika bez ijedne izmene)
    private boolean prikazujeOriginal = false;

    // Svetla i senke: posvetljavanje tamnih i potamnjivanje svetlih delova (0 = bez promene)
    private final Slider senke = new Slider(0, 1, 0);
    private final Slider svetla = new Slider(0, 1, 0);

    // Umetnički filteri menjaju same piksele slike (pre ostalih efekata), 0 znači isključeno.
    // pikselizacija: veličina kvadrata u pikselima prikaza
    private final Slider pikselizacija = new Slider(0, 30, 0);
    private final Slider posterizacija = new Slider(0, 1, 0);
    private final Slider reljef = new Slider(0, 1, 0);
    private final Slider crtez = new Slider(0, 1, 0);
    private final Slider uljanaSlika = new Slider(0, 1, 0);
    private final Slider akvarel = new Slider(0, 1, 0);
    // Klizači koji menjaju same piksele slike (isecanje, svetla i senke, umetnički filteri)
    private final List<Slider> klizaciPiksela = List.of(
            isecanjeLevo, isecanjeGore, isecanjeDesno, isecanjeDole, senke, svetla,
            pikselizacija, posterizacija, reljef, crtez, uljanaSlika, akvarel);

    // Imena klizača pod kojima se čuvaju sopstveni stilovi (geometrija i isecanje nisu deo stila).
    // Ime se ne sme menjati, jer se tako čuvaju već sačuvani stilovi.
    private final Map<String, Slider> kljuceviStilova = Map.ofEntries(
            Map.entry("svetlina", svetlina), Map.entry("kontrast", kontrast),
            Map.entry("senke", senke), Map.entry("svetla", svetla),
            Map.entry("zasicenost", zasicenost), Map.entry("nijansa", nijansa),
            Map.entry("temperatura", temperatura), Map.entry("negativ", negativ),
            Map.entry("crvena", crvena), Map.entry("zelena", zelena), Map.entry("plava", plava),
            Map.entry("providnost", providnost),
            Map.entry("ostrina", ostrina), Map.entry("vinjeta", vinjeta), Map.entry("zrno", zrno),
            Map.entry("sepija", sepija), Map.entry("sjaj", sjaj), Map.entry("bloom", bloom),
            Map.entry("zamucenje", zamucenje), Map.entry("zamucenjePokretom", zamucenjePokretom),
            Map.entry("ugaoPokreta", ugaoPokreta),
            Map.entry("pikselizacija", pikselizacija), Map.entry("posterizacija", posterizacija),
            Map.entry("reljef", reljef), Map.entry("crtez", crtez),
            Map.entry("uljanaSlika", uljanaSlika), Map.entry("akvarel", akvarel));
    // Dugmad sopstvenih stilova na kartici "Stilovi"
    private final VBox mojiStilovi = new VBox(5);

    // Histogram trenutnog izgleda slike, na dnu prozora "Komande"
    private final Canvas histogram = new Canvas(400, 130);
    private boolean histogramZakazan = false;

    // Svi klizači čije se vrednosti pamte u istoriji (novi klizač treba dodati ovde)
    private final List<Slider> sviKlizaci = List.of(
            svetlina, kontrast, zasicenost, nijansa,
            crvena, zelena, plava, providnost, temperatura, negativ,
            ostrina, vinjeta, zrno,
            sepija, sjaj, bloom, zamucenje, zamucenjePokretom, ugaoPokreta,
            rotacija, ogledaloLevoDesno, ogledaloGoreDole,
            pikselizacija, posterizacija, reljef, crtez, uljanaSlika, akvarel,
            senke, svetla, isecanjeLevo, isecanjeGore, isecanjeDesno, isecanjeDole,
            tekstIndeks, velicinaTeksta, tekstX, tekstY, senkaTeksta, fontTeksta, podebljanTekst, bojaTeksta,
            rotacijaTeksta, udaljenostSenke, smerSenke, zamucenjeSenke, bojaSenke,
            debljinaOkvira, polaroidOkvir, bojaOkvira, verzijaSlojeva);

    // Učitana slika u punoj rezoluciji (koristi se pri čuvanju) i njena umanjena kopija
    // za prikaz, da umetnički filteri rade brzo i na velikim slikama
    private static final double NAJVECI_PREGLED = 1600;
    private Image original;
    private Image pregled;

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
        glavniProzor = prozor;
        // Dugme za otvaranje slike
        Button otvori = new Button("Otvori sliku");
        otvori.setMaxWidth(Double.MAX_VALUE);
        otvori.setOnAction(e -> otvoriSliku(prozor));
        nedavneDugme.setMaxWidth(Double.MAX_VALUE);
        osveziNedavne();

        // Podešavanja čuvanja (kartica "Čuvanje"); nisu deo izmena slike, pa nisu u istoriji,
        // ali se pamte i posle zatvaranja programa
        najvecaStrana.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer piksela) {
                return piksela == null || piksela == 0 ? "Originalna veličina" : "Duža strana najviše " + piksela + " px";
            }

            @Override
            public Integer fromString(String tekst) {
                return null;
            }
        });
        int zapamcenaStrana = podesavanjaPrograma.getInt(KLJUC_NAJVECA_STRANA, 0);
        najvecaStrana.setValue(najvecaStrana.getItems().contains(zapamcenaStrana) ? zapamcenaStrana : 0);
        najvecaStrana.setMaxWidth(Double.MAX_VALUE);
        najvecaStrana.valueProperty().addListener((o, s, n) -> podesavanjaPrograma.putInt(KLJUC_NAJVECA_STRANA, n));
        kvalitetJpg.setValue(podesavanjaPrograma.getDouble(KLJUC_KVALITET_JPG, 0.9));
        oznakaKvaliteta.textProperty().bind(Bindings.createStringBinding(
                () -> "Kvalitet JPG slike: " + Math.round(kvalitetJpg.getValue() * 100) + "%",
                kvalitetJpg.valueProperty()));
        kvalitetJpg.valueProperty().addListener((o, s, n) ->
                podesavanjaPrograma.putDouble(KLJUC_KVALITET_JPG, n.doubleValue()));

        // Svaka promena klizača osvežava prikaz i beleži se za Undo/Redo
        for (Slider klizac : sviKlizaci) {
            klizac.valueProperty().addListener((o, s, n) -> osveziEfekat());
            pratiPromene(klizac);
        }
        for (Slider klizac : klizaciPiksela) {
            klizac.valueProperty().addListener((o, s, n) -> osveziPregled());
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
        Button sacuvaj = new Button("Sačuvaj sliku");
        sacuvaj.setMaxWidth(Double.MAX_VALUE);
        sacuvaj.disableProperty().bind(prikaz.imageProperty().isNull());
        sacuvaj.setOnAction(e -> sacuvajSliku(prozor));

        // Dugme za zatvaranje slike bez čuvanja; brišu se i sve izmene i istorija
        Button zatvori = new Button("Zatvori sliku");
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

        // Tekst se upisuje u istoriju kad se pritisne Enter ili kad polje izgubi fokus,
        // a ne posle svakog slova (inače bi Undo brisao slovo po slovo); prikaz se osvežava odmah
        tekstPolje.setPromptText("Upiši tekst i pritisni Enter");
        tekstPolje.setOnAction(e -> potvrdiTekst());
        tekstPolje.focusedProperty().addListener((o, s, fokus) -> {
            if (!fokus) {
                potvrdiTekst();
            }
        });
        tekstPolje.textProperty().addListener((o, s, n) -> osveziDodatke());
        tekstIndeks.valueProperty().addListener((o, s, n) -> {
            String tekst = tekstovi.get(Math.min(tekstovi.size() - 1, n.intValue()));
            if (!tekstPolje.getText().equals(tekst)) {
                tekstPolje.setText(tekst);
            }
        });

        ChoiceBox<String> fontIzbor = new ChoiceBox<>(FXCollections.observableArrayList(FONTOVI));
        fontIzbor.setMaxWidth(Double.MAX_VALUE);
        poveziIzbor(fontIzbor, fontTeksta);
        CheckBox podebljano = new CheckBox("Podebljano");
        podebljano.selectedProperty().addListener((o, s, n) -> podebljanTekst.setValue(n ? 1 : 0));
        podebljanTekst.valueProperty().addListener((o, s, n) -> podebljano.setSelected(n.doubleValue() == 1));
        ColorPicker bojaTekstaIzbor = new ColorPicker();
        poveziBoju(bojaTekstaIzbor, bojaTeksta, BELA);
        ColorPicker bojaSenkeIzbor = new ColorPicker();
        poveziBoju(bojaSenkeIzbor, bojaSenke, CRNA);

        ChoiceBox<String> vrstaOkvira = new ChoiceBox<>(FXCollections.observableArrayList("Običan", "Polaroid"));
        vrstaOkvira.setMaxWidth(Double.MAX_VALUE);
        poveziIzbor(vrstaOkvira, polaroidOkvir);
        ColorPicker bojaOkviraIzbor = new ColorPicker();
        poveziBoju(bojaOkviraIzbor, bojaOkvira, BELA);

        // Isecanje: posle klika na dugme korisnik mišem prevlači pravougaonik preko slike
        Button iseci = napraviDugme("Iseci (prevuci mišem preko slike)", () -> {});
        iseci.setOnAction(e -> {
            biraIsecak = !biraIsecak && prikaz.getImage() != null;
            iseci.setText(biraIsecak ? "Otkaži isecanje" : "Iseci (prevuci mišem preko slike)");
            if (biraIsecak) {
                rezimMisa.selectToggle(null);
            }
        });
        // Crtanje i crvene oči isključuju jedno drugo (ista grupa) i isecanje
        crtajDugme.setToggleGroup(rezimMisa);
        crveneOciDugme.setToggleGroup(rezimMisa);
        crtajDugme.setMaxWidth(Double.MAX_VALUE);
        crveneOciDugme.setMaxWidth(Double.MAX_VALUE);
        rezimMisa.selectedToggleProperty().addListener((o, s, n) -> {
            if (n != null && biraIsecak) {
                biraIsecak = false;
                iseci.setText("Iseci (prevuci mišem preko slike)");
            }
            indikatorOka.setVisible(false);
        });

        // Paleta: alatke, brzi izbor boja
        FlowPane paletaAlata = new FlowPane(5, 5);
        for (Alat alat : Alat.values()) {
            ToggleButton dugme = new ToggleButton(alat.naziv);
            dugme.setUserData(alat);
            dugme.setToggleGroup(alati);
            paletaAlata.getChildren().add(dugme);
        }
        alati.selectToggle(alati.getToggles().get(0));
        // Ne dozvoljava se da nijedna alatka ne bude izabrana
        alati.selectedToggleProperty().addListener((o, s, n) -> {
            if (n == null) {
                alati.selectToggle(s);
            }
        });
        bojaCetkice.setMaxWidth(Double.MAX_VALUE);
        FlowPane paletaBoja = new FlowPane(4, 4);
        for (String boja : List.of("#000000", "#ffffff", "#7f7f7f", "#e53935", "#fb8c00", "#fdd835",
                "#43a047", "#00acc1", "#1e88e5", "#3949ab", "#8e24aa", "#d81b60", "#6d4c41", "#f8bbd0")) {
            Button uzorak = new Button();
            uzorak.setMinSize(24, 24);
            uzorak.setPrefSize(24, 24);
            uzorak.setStyle("-fx-background-color: " + boja + "; -fx-border-color: #888888;");
            uzorak.setOnAction(e -> bojaCetkice.setValue(Color.web(boja)));
            paletaBoja.getChildren().add(uzorak);
        }

        // Podešavanja palete se pamte i posle zatvaranja programa
        String sacuvanaAlatka = podesavanjaPrograma.get(KLJUC_PALETA + "alat", Alat.OLOVKA.name());
        for (Toggle alatka : alati.getToggles()) {
            if (((Alat) alatka.getUserData()).name().equals(sacuvanaAlatka)) {
                alati.selectToggle(alatka);
            }
        }
        alati.selectedToggleProperty().addListener((o, s, n) -> {
            if (n != null) {
                podesavanjaPrograma.put(KLJUC_PALETA + "alat", ((Alat) n.getUserData()).name());
            }
        });
        try {
            bojaCetkice.setValue(Color.web(podesavanjaPrograma.get(KLJUC_PALETA + "boja", "#e53935")));
        } catch (IllegalArgumentException ignored) {
            // Oštećen zapis: ostaje početna boja
        }
        bojaCetkice.valueProperty().addListener((o, s, n) -> podesavanjaPrograma.put(KLJUC_PALETA + "boja", n.toString()));
        popunjenOblik.setSelected(podesavanjaPrograma.getBoolean(KLJUC_PALETA + "popunjeno", false));
        popunjenOblik.selectedProperty().addListener((o, s, n) -> podesavanjaPrograma.putBoolean(KLJUC_PALETA + "popunjeno", n));
        zapamtiKlizac(debljinaCetkice, KLJUC_PALETA + "debljina");
        zapamtiKlizac(neprovidnostCetkice, KLJUC_PALETA + "neprovidnost");
        zapamtiKlizac(mekocaCetkice, KLJUC_PALETA + "mekoca");
        zapamtiKlizac(velicinaOka, KLJUC_PALETA + "velicinaOka");

        // Promena slojeva (crtež, dodati tekstovi, crvene oči) osvežava prikaz;
        // crvene oči menjaju piksele, pa se tada slika ponovo obrađuje
        // Klizači izabrane nalepnice: jedno prevlačenje klizača je jedan korak za Undo
        poveziKlizacNalepnice(velicinaNalepnice, (n, v) -> new Nalepnica(n.slika(), n.ime(), n.x(), n.y(),
                v, n.neprovidnost(), n.rotacija()));
        poveziKlizacNalepnice(neprovidnostNalepnice, (n, v) -> new Nalepnica(n.slika(), n.ime(), n.x(), n.y(),
                n.sirina(), v, n.rotacija()));
        poveziKlizacNalepnice(rotacijaNalepnice, (n, v) -> new Nalepnica(n.slika(), n.ime(), n.x(), n.y(),
                n.sirina(), n.neprovidnost(), v));
        izaberiNalepnicu(-1);

        verzijaSlojeva.valueProperty().addListener((o, s, n) -> {
            osveziDodatke();
            // Posle Undo/Redo izabrana nalepnica možda više ne postoji ili ima druge vrednosti
            if (!postavljaKlizaceNalepnice) {
                izaberiNalepnicu(izabranaNalepnica);
            }
            if (!slojeviZa(s.intValue()).oci().equals(slojeviZa(n.intValue()).oci())) {
                osveziPregled();
            }
        });

        // Poseban prozor sa karticama za sve komande, efekte i filtere.
        // Nova grupa komandi se dodaje kao nova kartica preko napraviKarticu(...)
        TabPane kartice = new TabPane(
                napraviKarticu("Svetlo",
                        napraviDugme("Automatski popravi", this::automatskiPopravi),
                        new Separator(),
                        new Label("Svetlina"), svetlina,
                        new Label("Kontrast"), kontrast,
                        new Separator(),
                        new Label("Senke (posvetli tamne delove)"), senke,
                        new Label("Svetla (potamni svetle delove)"), svetla),
                napraviKarticu("Boja",
                        new Label("Zasićenost"), zasicenost,
                        new Label("Nijansa (Hue)"), nijansa,
                        new Label("Temperatura (hladno / toplo)"), temperatura,
                        new Label("Negativ"), negativ,
                        new Separator(),
                        new Label("Crvena (R)"), crvena,
                        new Label("Zelena (G)"), zelena,
                        new Label("Plava (B)"), plava,
                        new Separator(),
                        new Label("Providnost (Alfa)"), providnost,
                        new Separator(),
                        new Label("Crvene oči"),
                        crveneOciDugme,
                        new Label("Veličina kruga (vidi se oko kursora)"), velicinaOka,
                        napraviDugme("Poništi uklanjanje crvenih očiju", this::ponistiCrveneOci)),
                napraviKarticu("Crtanje",
                        crtajDugme,
                        new Label("Alatka"), paletaAlata,
                        new Label("Boja"), bojaCetkice, paletaBoja,
                        new Label("Debljina"), debljinaCetkice,
                        new Label("Neprovidnost"), neprovidnostCetkice,
                        new Label("Mekoća ivice (četkica, gumica i prava linija)"), mekocaCetkice,
                        popunjenOblik,
                        new Separator(),
                        napraviDugme("Obriši poslednji potez", this::obrisiPoslednjiPotez),
                        napraviDugme("Obriši ceo crtež", this::obrisiCrtez),
                        new Label("Gumica briše samo crtež, ne fotografiju.\n"
                                + "Undo vraća potez po potez. Crtež se ne menja filterima.")),
                napraviKarticu("Nalepnice",
                        napraviDugme("Dodaj nalepnicu (slika ili logo)…", () -> dodajNalepnicu(glavniProzor)),
                        new Label("Klikni na nalepnicu na slici da je izabereš, prevuci je da je pomeriš."),
                        new Separator(),
                        izabranaNalepnicaOznaka,
                        new Label("Veličina"), velicinaNalepnice,
                        new Label("Neprovidnost"), neprovidnostNalepnice,
                        new Label("Rotacija (stepeni)"), rotacijaNalepnice,
                        new Separator(),
                        napraviDugme("Obriši izabranu nalepnicu", this::obrisiNalepnicu),
                        napraviDugme("Obriši sve nalepnice", this::obrisiSveNalepnice)),
                napraviKarticu("Filteri",
                        new Label("Oštrina"), ostrina,
                        new Label("Vinjeta"), vinjeta,
                        new Label("Filmsko zrno"), zrno,
                        new Label("Sepija"), sepija,
                        new Label("Sjaj (Glow)"), sjaj,
                        new Label("Bloom"), bloom,
                        new Label("Zamućenje"), zamucenje,
                        new Label("Zamućenje pokretom"), zamucenjePokretom,
                        new Label("Ugao pokreta"), ugaoPokreta),
                napraviKarticu("Geometrija",
                        new Label("Rotacija"),
                        napraviDugme("Rotiraj ulevo (90°)", () -> rotacija.setValue((rotacija.getValue() + 3) % 4)),
                        napraviDugme("Rotiraj udesno (90°)", () -> rotacija.setValue((rotacija.getValue() + 1) % 4)),
                        new Separator(),
                        new Label("Okretanje (ogledalo)"),
                        napraviDugme("Levo - desno", () -> ogledaloLevoDesno.setValue(1 - ogledaloLevoDesno.getValue())),
                        napraviDugme("Gore - dole", () -> ogledaloGoreDole.setValue(1 - ogledaloGoreDole.getValue())),
                        new Separator(),
                        new Label("Isecanje"),
                        iseci,
                        napraviDugme("Vrati celu sliku", () -> postaviVise(Map.of(
                                isecanjeLevo, 0.0, isecanjeGore, 0.0, isecanjeDesno, 0.0, isecanjeDole, 0.0)))),
                napraviKarticu("Tekst",
                        new Label("Tekst na slici"), tekstPolje,
                        napraviDugme("Dodaj ovaj tekst i počni novi", this::dodajNoviTekst),
                        napraviDugme("Obriši sve tekstove", this::obrisiSveTekstove),
                        new Label("Klikni na tekst na slici da ga izabereš, izmeniš ili pomeriš.\n"
                                + "Podešavanja ispod važe za izabrani tekst."),
                        new Separator(),
                        new Label("Font"), fontIzbor, podebljano,
                        new Label("Boja"), bojaTekstaIzbor,
                        new Label("Veličina"), velicinaTeksta,
                        new Label("Položaj levo - desno"), tekstX,
                        new Label("Položaj gore - dole"), tekstY,
                        new Label("Rotacija (stepeni)"), rotacijaTeksta,
                        new Label("Tekst možeš i da prevučeš mišem po slici."),
                        new Separator(),
                        new Label("Senka - jačina"), senkaTeksta,
                        new Label("Boja senke"), bojaSenkeIzbor,
                        new Label("Udaljenost senke"), udaljenostSenke,
                        new Label("Smer senke (0 = dole desno)"), smerSenke,
                        new Label("Zamućenje senke"), zamucenjeSenke,
                        new Separator(),
                        new Label("Tekst se ne menja filterima i uvek ostaje oštar.")),
                napraviKarticu("Okvir",
                        new Label("Debljina okvira"), debljinaOkvira,
                        new Label("Vrsta"), vrstaOkvira,
                        new Label("Boja"), bojaOkviraIzbor,
                        new Label("Okvir se dodaje oko slike, pa je sačuvana slika veća.")),
                napraviKarticu("Umetnički",
                        new Label("Pikselizacija"), pikselizacija,
                        new Label("Posterizacija"), posterizacija,
                        new Label("Reljef"), reljef,
                        new Label("Crtež olovkom"), crtez,
                        new Separator(),
                        new Label("Uljana slika"), uljanaSlika,
                        new Label("Akvarel"), akvarel),
                // Svaki stil prvo vraća sve filtere na 0, pa postavlja svoje vrednosti
                napraviKarticu("Stilovi",
                        napraviDugme("Stara fotografija", () -> primeniStil(Map.of(
                                sepija, 0.8, zrno, 0.4, vinjeta, 0.6, kontrast, -0.1))),
                        napraviDugme("Crno-belo", () -> primeniStil(Map.of(
                                zasicenost, -1.0, kontrast, 0.2))),
                        napraviDugme("Toplo leto", () -> primeniStil(Map.of(
                                temperatura, 0.5, zasicenost, 0.2, svetlina, 0.05))),
                        napraviDugme("Hladna zima", () -> primeniStil(Map.of(
                                temperatura, -0.5, zasicenost, -0.2, svetla, 0.3))),
                        napraviDugme("Dramatično", () -> primeniStil(Map.of(
                                kontrast, 0.4, ostrina, 0.6, vinjeta, 0.5, zasicenost, -0.2))),
                        napraviDugme("Protiv svetla", () -> primeniStil(Map.of(
                                senke, 0.8, svetla, 0.4))),
                        napraviDugme("Strip", () -> primeniStil(Map.of(
                                posterizacija, 0.7, zasicenost, 0.4, ostrina, 0.5))),
                        new Separator(),
                        napraviDugme("Bez stila", () -> primeniStil(Map.of())),
                        new Separator(),
                        new Label("Moji stilovi"),
                        mojiStilovi,
                        napraviDugme("Sačuvaj trenutna podešavanja kao stil", this::sacuvajStil)),
                napraviKarticu("Čuvanje",
                        new Label("Veličina sačuvane slike"), najvecaStrana,
                        new Separator(),
                        oznakaKvaliteta, kvalitetJpg,
                        new Label("Manji kvalitet daje manji fajl. PNG se uvek čuva bez gubitka kvaliteta."),
                        new Separator(),
                        new Label("Obrada više slika odjednom"),
                        napraviDugme("Obradi više slika…", () -> obradiViseSlika(glavniProzor)),
                        new Label("Na svaku izabranu sliku primenjuju se sva trenutna podešavanja\n"
                                + "(filteri, isecanje, rotacija, tekst, okvir i veličina iznad).\n"
                                + "Originali se ne menjaju.")));
        osveziMojeStilove();
        // Prozor "Komande" se otvara na kartici koja je poslednja korišćena
        int sacuvanaKartica = podesavanjaPrograma.getInt(KLJUC_KARTICA, 0);
        if (sacuvanaKartica >= 0 && sacuvanaKartica < kartice.getTabs().size()) {
            kartice.getSelectionModel().select(sacuvanaKartica);
        }
        kartice.getSelectionModel().selectedIndexProperty().addListener(
                (o, s, n) -> podesavanjaPrograma.putInt(KLJUC_KARTICA, n.intValue()));

        Stage komande = new Stage();
        komande.initOwner(prozor);
        komande.setTitle("Komande");
        // Histogram je ispod kartica, pa se vidi koja god da je kartica otvorena
        VBox donjiDeo = new VBox(5,
                new Separator(),
                new Label("Histogram (siva: osvetljenost; linije: crvena, zelena, plava)"),
                histogram);
        donjiDeo.setPadding(new Insets(0, 15, 15, 15));
        BorderPane sadrzajKomandi = new BorderPane();
        sadrzajKomandi.setCenter(kartice);
        sadrzajKomandi.setBottom(donjiDeo);

        // Dovoljno široko da se vide naslovi svih kartica
        Scene scenaKomandi = new Scene(sadrzajKomandi, 563, 840);
        dodajPrecice(scenaKomandi);
        komande.setScene(scenaKomandi);

        // Dugme koje otvara i zatvara prozor sa komandama (nije vidljiv pri pokretanju)
        Button prikaziKomande = new Button("Otvori Komande");
        prikaziKomande.setMaxWidth(Double.MAX_VALUE);
        // Natpis prati stanje prozora, i kad se zatvori dugmetom i kad se zatvori sa X
        // Histogram se računa samo dok je prozor otvoren, pa se osvežava pri otvaranju
        komande.showingProperty().addListener((o, s, otvoren) -> {
            prikaziKomande.setText(otvoren ? "Zatvori Komande" : "Otvori Komande");
            if (otvoren) {
                zakaziHistogram();
            }
        });
        prikaziKomande.setOnAction(e -> {
            if (komande.isShowing()) {
                komande.hide();
                return;
            }
            // Desno od glavnog prozora, na razmaku od 5 mm
            double razmak = 5 / 25.4 * Screen.getPrimary().getDpi();
            komande.setX(prozor.getX() + prozor.getWidth() + razmak);
            komande.setY(prozor.getY());
            komande.show();
            komande.toFront();
        });

        // Dok se drži pritisnuto, prikazuje se slika bez izmena (radnja je ispod, kod prostora za prikaz)
        Button preIPosle = new Button("Pre / posle (drži)");
        preIPosle.setMaxWidth(Double.MAX_VALUE);
        preIPosle.disableProperty().bind(prikaz.imageProperty().isNull());

        // Na glavnom prozoru ostaju samo dugmad
        VBox kontrole = new VBox(10,
                otvori,
                nedavneDugme,
                sacuvaj,
                zatvori,
                undoRedo,
                resetuj,
                preIPosle,
                prikaziKomande);
        kontrole.setPadding(new Insets(15));
        kontrole.setPrefWidth(180);

        // Prostor za prikaz fotografije
        // Iznad slike je providan sloj na kome se crta pravougaonik za isecanje
        Rectangle okvirIsecka = new Rectangle();
        okvirIsecka.setFill(Color.color(1, 1, 1, 0.15));
        okvirIsecka.setStroke(Color.WHITE);
        okvirIsecka.getStrokeDashArray().addAll(6.0, 4.0);
        okvirIsecka.setVisible(false);
        Pane slojIsecka = new Pane(okvirIsecka);
        slojIsecka.setMouseTransparent(true);

        // Okvir je ispod slike (slika ga prekriva u sredini), a tekst iznad nje
        Pane slojOkvira = new Pane(okvirPrikaz);
        slojOkvira.setMouseTransparent(true);
        tekstPrikaz.setTextOrigin(VPos.CENTER);
        senkaPrikaz.setTextOrigin(VPos.CENTER);
        Pane slojTeksta = new Pane(dodatiTekstovi, senkaPrikaz, tekstPrikaz);
        slojTeksta.setMouseTransparent(true);
        dodatiTekstovi.setMouseTransparent(true);
        // Redom odozdo: slika, nalepnice, crtež, tekst. Crtež je slika veličine prostora za prikaz.
        Pane slojCrteza = new Pane(slojNalepnica, crtezPrikaz);
        slojCrteza.setMouseTransparent(true);
        // Mala željena veličina, da velika slika crteža ne bi sprečavala smanjivanje prozora
        // (StackPane ga ionako raširi na ceo prostor)
        slojCrteza.setPrefSize(0, 0);
        StackPane centar = new StackPane(slojOkvira, prikaz, slojCrteza, slojTeksta, slojIsecka);
        centar.widthProperty().addListener((o, s, n) -> osveziCrtez());
        centar.heightProperty().addListener((o, s, n) -> osveziCrtez());
        // Krug koji pokazuje koliki deo oka će se ispraviti
        indikatorOka.setFill(Color.TRANSPARENT);
        indikatorOka.setStroke(Color.WHITE);
        indikatorOka.getStrokeDashArray().addAll(4.0, 3.0);
        indikatorOka.setVisible(false);
        slojIsecka.getChildren().add(indikatorOka);
        centar.setStyle("-fx-background-color: #2b2b2b;");
        centar.setMinSize(0, 0);

        // Uvećana slika ne sme da prelazi preko dugmadi levo od nje
        Rectangle granica = new Rectangle();
        granica.widthProperty().bind(centar.widthProperty());
        granica.heightProperty().bind(centar.heightProperty());
        centar.setClip(granica);

        // Točkić miša zumira, tako da tačka ispod kursora ostaje na mestu
        centar.setOnScroll(e -> {
            if (prikaz.getImage() == null || e.getDeltaY() == 0) {
                return;
            }
            double novo = Math.max(1, Math.min(NAJVECE_UVECANJE, uvecanje * Math.pow(1.1, e.getDeltaY() / 40)));
            if (novo == 1) {
                ponistiUvecanje();
                return;
            }
            // Slika se uvećava oko svog središta, koje je u sredini prostora za prikaz pomereno za pomak
            double k = novo / uvecanje;
            double sx = centar.getWidth() / 2;
            double sy = centar.getHeight() / 2;
            pomakX = e.getX() - sx - k * (e.getX() - sx - pomakX);
            pomakY = e.getY() - sy - k * (e.getY() - sy - pomakY);
            uvecanje = novo;
            primeniPolozaj();
        });
        // Dvoklik vraća prikaz cele slike
        centar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !biraIsecak && rezimMisa.getSelectedToggle() == null) {
                ponistiUvecanje();
            }
        });

        double[] pocetak = new double[2];
        double[] poslednjaTacka = new double[2];
        centar.setOnMousePressed(e -> {
            poslednjaTacka[0] = e.getX();
            poslednjaTacka[1] = e.getY();
            if (biraIsecak) {
                pocetak[0] = e.getX();
                pocetak[1] = e.getY();
                okvirIsecka.setX(e.getX());
                okvirIsecka.setY(e.getY());
                okvirIsecka.setWidth(0);
                okvirIsecka.setHeight(0);
                okvirIsecka.setVisible(true);
            } else if (prikaz.getImage() == null) {
                return;
            } else if (crtajDugme.isSelected()) {
                zapocniPotez(e.getX(), e.getY());
            } else if (crveneOciDugme.isSelected()) {
                dodajCrvenoOko(e.getX(), e.getY());
            } else if (!jeIznadTeksta(e.getX(), e.getY()) && dodatiTekstIspod(e.getX(), e.getY()) >= 0) {
                // Klik na ranije dodat tekst ga bira za uređivanje, i odmah se može prevući
                izaberiDodatiTekst(dodatiTekstIspod(e.getX(), e.getY()));
                pomeraTekst = true;
                Bounds granice = tekstPrikaz.getBoundsInParent();
                hvatanjeTeksta[0] = e.getX() - granice.getCenterX();
                hvatanjeTeksta[1] = e.getY() - granice.getCenterY();
                centar.setCursor(Cursor.CLOSED_HAND);
            } else if (jeIznadTeksta(e.getX(), e.getY())) {
                // Hvatanje teksta: pamti se gde je uhvaćen u odnosu na njegovo središte
                pomeraTekst = true;
                Bounds granice = tekstPrikaz.getBoundsInParent();
                hvatanjeTeksta[0] = e.getX() - granice.getCenterX();
                hvatanjeTeksta[1] = e.getY() - granice.getCenterY();
                centar.setCursor(Cursor.CLOSED_HAND);
            } else if (nalepnicaIspod(e.getX(), e.getY()) >= 0) {
                // Klik na nalepnicu je bira; prevlačenje je pomera (celo prevlačenje je jedan korak za Undo)
                int indeks = nalepnicaIspod(e.getX(), e.getY());
                izaberiNalepnicu(indeks);
                Bounds granice = cvoroviNalepnica.get(indeks).getBoundsInParent();
                hvatanjeNalepnice[0] = e.getX() - granice.getCenterX();
                hvatanjeNalepnice[1] = e.getY() - granice.getCenterY();
                pomeraNalepnicu = true;
                verzijaPokreta = -1;
                centar.setCursor(Cursor.CLOSED_HAND);
            }
        });
        centar.setOnMouseDragged(e -> {
            if (biraIsecak) {
                okvirIsecka.setX(Math.min(pocetak[0], e.getX()));
                okvirIsecka.setY(Math.min(pocetak[1], e.getY()));
                okvirIsecka.setWidth(Math.abs(e.getX() - pocetak[0]));
                okvirIsecka.setHeight(Math.abs(e.getY() - pocetak[1]));
            } else if (trenutneTackeX != null) {
                nastaviPotez(e.getX(), e.getY());
            } else if (crveneOciDugme.isSelected()) {
                return;
            } else if (pomeraNalepnicu) {
                double[] udeo = udeoSlike(e.getX() - hvatanjeNalepnice[0], e.getY() - hvatanjeNalepnice[1]);
                izmeniNalepnicu(n -> new Nalepnica(n.slika(), n.ime(), ogranici(udeo[0], 0, 1), ogranici(udeo[1], 0, 1),
                        n.sirina(), n.neprovidnost(), n.rotacija()), true);
            } else if (pomeraTekst) {
                // Novo središte teksta se pretvara u položaj od -1 do 1 u odnosu na sliku.
                // Dok traje prevlačenje ne beleži se u istoriju, nego tek kad se miš pusti.
                Bounds slika = prikaz.localToParent(prikaz.getLayoutBounds());
                double noviX = (e.getX() - hvatanjeTeksta[0] - slika.getCenterX()) / (slika.getWidth() / 2);
                double noviY = (e.getY() - hvatanjeTeksta[1] - slika.getCenterY()) / (slika.getHeight() / 2);
                bezBelezenja = true;
                tekstX.setValue(ogranici(noviX, -1, 1));
                tekstY.setValue(ogranici(noviY, -1, 1));
                bezBelezenja = false;
            } else if (uvecanje > 1) {
                // Prevlačenje pomera uvećanu sliku
                pomakX += e.getX() - poslednjaTacka[0];
                pomakY += e.getY() - poslednjaTacka[1];
                primeniPolozaj();
            }
            poslednjaTacka[0] = e.getX();
            poslednjaTacka[1] = e.getY();
        });
        centar.setOnMouseReleased(e -> {
            if (trenutneTackeX != null) {
                zavrsiPotez();
                return;
            }
            if (pomeraNalepnicu) {
                pomeraNalepnicu = false;
                verzijaPokreta = -1;
                centar.setCursor(Cursor.OPEN_HAND);
                return;
            }
            if (pomeraTekst) {
                // Celo prevlačenje teksta je jedan korak za Undo
                pomeraTekst = false;
                zabeleziStanje();
                centar.setCursor(Cursor.OPEN_HAND);
                return;
            }
            if (!biraIsecak) {
                return;
            }
            okvirIsecka.setVisible(false);
            // Premalo prevlačenje (običan klik) se ne računa, može se pokušati ponovo
            if (Math.abs(e.getX() - pocetak[0]) < 5 || Math.abs(e.getY() - pocetak[1]) < 5) {
                return;
            }
            biraIsecak = false;
            iseci.setText("Iseci (prevuci mišem preko slike)");
            iseciNaOkvir(pocetak[0], pocetak[1], e.getX(), e.getY());
        });
        // Pri isecanju, crtanju i crvenim očima kursor je krstić; iznad teksta ili kad je slika uvećana,
        // ruka za pomeranje
        centar.setOnMouseMoved(e -> {
            boolean krstic = biraIsecak || crtajDugme.isSelected() || crveneOciDugme.isSelected();
            boolean iznadTeksta = jeIznadTeksta(e.getX(), e.getY()) || dodatiTekstIspod(e.getX(), e.getY()) >= 0
                    || nalepnicaIspod(e.getX(), e.getY()) >= 0;
            centar.setCursor(krstic ? Cursor.CROSSHAIR : iznadTeksta || uvecanje > 1 ? Cursor.OPEN_HAND : Cursor.DEFAULT);
            indikatorOka.setVisible(crveneOciDugme.isSelected() && prikaz.getImage() != null);
            indikatorOka.setCenterX(e.getX());
            indikatorOka.setCenterY(e.getY());
            indikatorOka.setRadius(velicinaOka.getValue());
        });
        centar.setOnMouseExited(e -> indikatorOka.setVisible(false));

        prikaz.setPreserveRatio(true);
        // RGB efekat zavisi od veličine prikaza, pa ga osvežavamo i kad se prozor menja
        // Isto važi i za umetničke filtere, čija se jačina zadaje u pikselima prikaza
        prikaz.layoutBoundsProperty().addListener((o, s, n) -> {
            osveziEfekat();
            osveziPregled();
        });
        osveziEfekat();
        // Tekst i okvir prate sliku i kad se ona pomeri, zumira ili okrene
        prikaz.boundsInParentProperty().addListener((o, s, n) -> osveziDodatke());
        // Kad je slika okrenuta za 90° ili 270°, širina i visina prostora za prikaz menjaju uloge.
        // Mesto za okvir se oduzima, da slika sa okvirom cela stane na ekran.
        Runnable osveziVelicinu = () -> {
            boolean uspravno = !prikazujeOriginal && rotacija.getValue() % 2 == 1;
            double okvir = debljinaOkvira.getValue();
            double sirina = (centar.getWidth() - 20) / (1 + 2 * okvir);
            double visina = (centar.getHeight() - 20) / (1 + (polaroidOkvir.getValue() == 1 ? 4.5 : 2) * okvir);
            prikaz.setFitWidth(uspravno ? visina : sirina);
            prikaz.setFitHeight(uspravno ? sirina : visina);
        };
        centar.widthProperty().addListener((o, s, n) -> osveziVelicinu.run());
        centar.heightProperty().addListener((o, s, n) -> osveziVelicinu.run());
        rotacija.valueProperty().addListener((o, s, n) -> osveziVelicinu.run());
        debljinaOkvira.valueProperty().addListener((o, s, n) -> osveziVelicinu.run());
        polaroidOkvir.valueProperty().addListener((o, s, n) -> osveziVelicinu.run());

        // Pre / posle: privremeno se prikazuje učitana slika bez filtera, isecanja i rotacije
        preIPosle.setOnMousePressed(e -> {
            if (pregled == null) {
                return;
            }
            prikazujeOriginal = true;
            prikaz.setImage(pregled);
            prikaz.setEffect(null);
            prikaz.setOpacity(1);
            primeniPolozaj();
            osveziVelicinu.run();
        });
        preIPosle.setOnMouseReleased(e -> {
            if (!prikazujeOriginal) {
                return;
            }
            prikazujeOriginal = false;
            osveziVelicinu.run();
            osveziPregled();
            osveziEfekat();
        });

        BorderPane koren = new BorderPane();
        koren.setLeft(kontrole);
        koren.setCenter(centar);

        prozor.setTitle("FotoEditor");
        Scene scena = new Scene(koren, 1100, 840);
        dodajPrecice(scena);
        prozor.setScene(scena);

        // Prečice za dugmad glavnog prozora rade u oba prozora; vide se kad se miš zadrži na dugmetu
        for (Scene s : List.of(scena, scenaKomandi)) {
            dodajPrecicu(s, KeyCode.O, otvori::fire);
            dodajPrecicu(s, KeyCode.S, sacuvaj::fire);
            dodajPrecicu(s, KeyCode.W, zatvori::fire);
            dodajPrecicu(s, KeyCode.K, prikaziKomande::fire);
            dodajPrecicu(s, KeyCode.DIGIT0, this::ponistiUvecanje);
        }
        otvori.setTooltip(new Tooltip("Ctrl+O, ili prevuci sliku u prozor"));
        sacuvaj.setTooltip(new Tooltip("Ctrl+S"));
        zatvori.setTooltip(new Tooltip("Ctrl+W"));
        undo.setTooltip(new Tooltip("Ctrl+Z"));
        redo.setTooltip(new Tooltip("Ctrl+Y ili Ctrl+Shift+Z"));
        prikaziKomande.setTooltip(new Tooltip("Ctrl+K"));

        // Slika se može otvoriti i prevlačenjem fajla iz Windows Explorera na glavni prozor
        scena.setOnDragOver(e -> {
            List<File> fajlovi = e.getDragboard().getFiles();
            if (e.getDragboard().hasFiles() && !fajlovi.isEmpty() && jeSlika(fajlovi.get(0))) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        scena.setOnDragDropped(e -> {
            List<File> fajlovi = e.getDragboard().getFiles();
            boolean uspeh = e.getDragboard().hasFiles() && !fajlovi.isEmpty() && jeSlika(fajlovi.get(0));
            e.setDropCompleted(uspeh);
            e.consume();
            if (uspeh) {
                // Dijalozi (npr. pitanje za nesačuvane izmene) se otvaraju tek kad se prevlačenje završi
                Platform.runLater(() -> ucitajSliku(prozor, fajlovi.get(0)));
            }
        });
        prozor.show();
        // Glavni prozor je 20 mm od leve ivice ekrana (po vertikali ostaje gde ga je sistem postavio)
        Screen ekran = Screen.getPrimary();
        prozor.setX(ekran.getVisualBounds().getMinX() + 20 / 25.4 * ekran.getDpi());
        // Na manjem ekranu prozor se skraćuje i pomera da ne izlazi preko donje ivice (ili trake zadataka)
        Rectangle2D vidljivo = ekran.getVisualBounds();
        if (prozor.getHeight() > vidljivo.getHeight()) {
            prozor.setHeight(vidljivo.getHeight());
        }
        prozor.setY(Math.max(vidljivo.getMinY(), Math.min(prozor.getY(), vidljivo.getMaxY() - prozor.getHeight())));

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
        // Temperatura pojačava crvenu i slabi plavu (toplo) ili obrnuto (hladno)
        double t = 0.3 * temperatura.getValue();
        double r = Math.min(2, (1 + crvena.getValue()) * (1 + t));
        double g = 1 + zelena.getValue();
        double b = Math.min(2, (1 + plava.getValue()) * (1 - t));
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

        if (negativ.getValue() > 0) {
            // Razlika sa belom daje obrnute boje; providnost bele određuje koliko je negativ jak
            Blend obrnuto = new Blend(BlendMode.DIFFERENCE, efekat,
                    new ColorInput(0, 0, sirina, visina, Color.WHITE));
            obrnuto.setOpacity(negativ.getValue());
            efekat = obrnuto;
        }

        if (ostrina.getValue() > 0) {
            // Izoštravanje "high-pass" postupkom: razlika slike i njene zamućene kopije
            // (0.5 + (slika - zamućeno) / 2) prelaže se preko slike u OVERLAY režimu
            GaussianBlur zamuceno = new GaussianBlur(Math.min(63, 3 * razmera));
            zamuceno.setInput(efekat);
            Blend obrnutoZamuceno = new Blend(BlendMode.DIFFERENCE,
                    new ColorInput(0, 0, sirina, visina, Color.WHITE), zamuceno);
            Blend razlika = new Blend(BlendMode.SRC_OVER, efekat, obrnutoZamuceno);
            razlika.setOpacity(0.5);
            Blend izostreno = new Blend(BlendMode.OVERLAY, efekat, razlika);
            izostreno.setOpacity(ostrina.getValue());
            efekat = izostreno;
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
        int sirinaMaske = (int) Math.ceil(sirina);
        int visinaMaske = (int) Math.ceil(visina);
        if (zrno.getValue() > 0 && sirinaMaske > 0 && visinaMaske > 0) {
            // Zrno: siva tačkasta maska u OVERLAY režimu; zrnce je iste veličine na prikazu i u sačuvanoj slici
            int velicinaZrna = Math.max(1, (int) Math.round(razmera));
            Blend zrnasto = new Blend(BlendMode.OVERLAY, efekat,
                    new ImageInput(napraviZrno(sirinaMaske, visinaMaske, velicinaZrna)));
            zrnasto.setOpacity(zrno.getValue());
            efekat = zrnasto;
        }
        if (vinjeta.getValue() > 0 && sirinaMaske > 0 && visinaMaske > 0) {
            // Vinjeta: slika se množi maskom koja je bela u sredini i tamni prema uglovima
            efekat = new Blend(BlendMode.MULTIPLY, efekat,
                    new ImageInput(napraviVinjetu(sirinaMaske, visinaMaske, vinjeta.getValue())));
        }
        return efekat;
    }

    // Maska za vinjetu: sredina ostaje netaknuta, a od 30% udaljenosti ka uglovima
    // slika postepeno tamni; jacina 1 znači potpuno crne uglove
    private static Image napraviVinjetu(int sirina, int visina, double jacina) {
        int[] pikseli = new int[sirina * visina];
        double cx = sirina / 2.0;
        double cy = visina / 2.0;
        for (int y = 0; y < visina; y++) {
            for (int x = 0; x < sirina; x++) {
                double dx = (x + 0.5 - cx) / cx;
                double dy = (y + 0.5 - cy) / cy;
                // 0 u sredini, 1 u uglu
                double d = Math.sqrt(dx * dx + dy * dy) / Math.sqrt(2);
                double udeo = Math.max(0, Math.min(1, (d - 0.3) / 0.7));
                udeo = udeo * udeo * (3 - 2 * udeo);
                int v = (int) Math.round(255 * (1 - jacina * udeo));
                pikseli[y * sirina + x] = 0xFF000000 | (v << 16) | (v << 8) | v;
            }
        }
        WritableImage maska = new WritableImage(sirina, visina);
        maska.getPixelWriter().setPixels(0, 0, sirina, visina, PixelFormat.getIntArgbInstance(), pikseli, 0, sirina);
        return maska;
    }

    // Maska za filmsko zrno: nasumične sive tačke oko srednje sive boje.
    // Uvek ista za iste dimenzije, da zrno ne "treperi" pri svakom pomeranju klizača.
    // velicina: koliko piksela zauzima jedno zrnce
    private static Image napraviZrno(int sirina, int visina, int velicina) {
        int[] pikseli = new int[sirina * visina];
        for (int y = 0; y < visina; y++) {
            for (int x = 0; x < sirina; x++) {
                int h = hes(x / velicina, y / velicina);
                // Zbir dve nasumične vrednosti: češće su nijanse bliže srednjoj sivoj
                int v = ((h & 0xFF) + ((h >>> 8) & 0xFF)) / 2;
                pikseli[y * sirina + x] = 0xFF000000 | (v << 16) | (v << 8) | v;
            }
        }
        WritableImage maska = new WritableImage(sirina, visina);
        maska.getPixelWriter().setPixels(0, 0, sirina, visina, PixelFormat.getIntArgbInstance(), pikseli, 0, sirina);
        return maska;
    }

    // Pseudo-nasumičan broj određen koordinatama tačke
    private static int hes(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    private void osveziEfekat() {
        if (prikazujeOriginal) {
            return;
        }
        prikaz.setEffect(napraviEfekat(prikaz.getLayoutBounds().getWidth(), prikaz.getLayoutBounds().getHeight(), 1));
        prikaz.setOpacity(1 - providnost.getValue());
        primeniPolozaj();
        zakaziHistogram();
    }

    // Više promena odjednom (npr. stil ili Undo) crta histogram samo jednom
    private void zakaziHistogram() {
        if (histogramZakazan) {
            return;
        }
        histogramZakazan = true;
        Platform.runLater(() -> {
            histogramZakazan = false;
            nacrtajHistogram();
        });
    }

    // Histogram se računa iz umanjene kopije slike (najviše 256 piksela široke) sa istim efektima,
    // tako da pokazuje izgled slike posle svih filtera
    private void nacrtajHistogram() {
        if (prikazujeOriginal) {
            return;
        }
        Window prozorKomandi = histogram.getScene() == null ? null : histogram.getScene().getWindow();
        if (prozorKomandi == null || !prozorKomandi.isShowing()) {
            return;
        }
        GraphicsContext g = histogram.getGraphicsContext2D();
        double w = histogram.getWidth();
        double h = histogram.getHeight();
        g.setFill(Color.web("#2b2b2b"));
        g.fillRect(0, 0, w, h);
        Image slika = prikaz.getImage();
        if (slika == null) {
            return;
        }

        ImageView mala = new ImageView(slika);
        mala.setPreserveRatio(true);
        mala.setFitWidth(Math.min(256, slika.getWidth()));
        double mw = mala.getLayoutBounds().getWidth();
        double mh = mala.getLayoutBounds().getHeight();
        double sirinaPrikaza = prikaz.getLayoutBounds().getWidth();
        mala.setEffect(napraviEfekat(mw, mh, sirinaPrikaza > 0 ? mw / sirinaPrikaza : 1));
        SnapshotParameters parametri = new SnapshotParameters();
        parametri.setViewport(new Rectangle2D(0, 0, mw, mh));
        WritableImage snimak = mala.snapshot(parametri, null);

        // Broj piksela za svaku vrednost 0-255: osvetljenost, crvena, zelena, plava
        int[][] broj = new int[4][256];
        PixelReader citac = snimak.getPixelReader();
        for (int y = 0; y < (int) snimak.getHeight(); y++) {
            for (int x = 0; x < (int) snimak.getWidth(); x++) {
                int c = citac.getArgb(x, y);
                if ((c >>> 24) == 0) {
                    continue;
                }
                int r = (c >> 16) & 0xFF;
                int gr = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                broj[0][(299 * r + 587 * gr + 114 * b) / 1000]++;
                broj[1][r]++;
                broj[2][gr]++;
                broj[3][b]++;
            }
        }
        // Visina se meri prema najvišem stubu osvetljenosti, bez potpuno crnih i belih piksela
        // (kojih zna da bude mnogo, pa bi ostatak bio prenizak)
        int najvise = 1;
        for (int i = 1; i < 255; i++) {
            najvise = Math.max(najvise, broj[0][i]);
        }

        g.setFill(Color.gray(0.75, 0.6));
        g.beginPath();
        g.moveTo(0, h);
        for (int i = 0; i < 256; i++) {
            g.lineTo(i * w / 255, h - Math.min(h, h * 0.95 * broj[0][i] / najvise));
        }
        g.lineTo(w, h);
        g.closePath();
        g.fill();

        Color[] boje = {Color.rgb(255, 80, 80, 0.9), Color.rgb(80, 220, 80, 0.9), Color.rgb(90, 140, 255, 0.9)};
        g.setLineWidth(1);
        for (int k = 0; k < 3; k++) {
            g.setStroke(boje[k]);
            g.beginPath();
            for (int i = 0; i < 256; i++) {
                double x = i * w / 255;
                double y = h - Math.min(h, h * 0.95 * broj[k + 1][i] / najvise);
                if (i == 0) {
                    g.moveTo(x, y);
                } else {
                    g.lineTo(x, y);
                }
            }
            g.stroke();
        }
    }

    private Preferences cvorStilova() {
        return podesavanjaPrograma.node("stilovi");
    }

    // Čuva vrednosti klizača koje nisu 0, u obliku "svetlina=0.2;kontrast=-0.1"
    private void sacuvajStil() {
        TextInputDialog unos = new TextInputDialog();
        unos.setTitle("Sačuvaj stil");
        unos.setHeaderText(null);
        unos.setContentText("Ime stila:");
        if (histogram.getScene() != null) {
            unos.initOwner(histogram.getScene().getWindow());
        }
        Optional<String> ime = unos.showAndWait().map(String::trim).filter(s -> !s.isEmpty());
        if (ime.isEmpty()) {
            return;
        }
        String naziv = ime.get();
        if (naziv.length() > Preferences.MAX_KEY_LENGTH) {
            naziv = naziv.substring(0, Preferences.MAX_KEY_LENGTH);
        }
        StringBuilder zapis = new StringBuilder();
        kljuceviStilova.forEach((kljuc, klizac) -> {
            if (klizac.getValue() != 0) {
                zapis.append(kljuc).append('=').append(klizac.getValue()).append(';');
            }
        });
        cvorStilova().put(naziv, zapis.toString());
        osveziMojeStilove();
    }

    private Map<Slider, Double> procitajStil(String ime) {
        Map<Slider, Double> stil = new HashMap<>();
        for (String deo : cvorStilova().get(ime, "").split(";")) {
            String[] kljucIVrednost = deo.split("=");
            Slider klizac = kljucIVrednost.length == 2 ? kljuceviStilova.get(kljucIVrednost[0]) : null;
            if (klizac != null) {
                try {
                    stil.put(klizac, Double.parseDouble(kljucIVrednost[1]));
                } catch (NumberFormatException ignored) {
                    // Oštećen zapis se preskače
                }
            }
        }
        return stil;
    }

    // Pravi dugme za svaki sačuvani stil (po abecedi), sa dugmetom za brisanje pored
    private void osveziMojeStilove() {
        mojiStilovi.getChildren().clear();
        String[] imena;
        try {
            imena = cvorStilova().keys();
        } catch (BackingStoreException e) {
            imena = new String[0];
        }
        Arrays.sort(imena, String.CASE_INSENSITIVE_ORDER);
        for (String ime : imena) {
            Button primeni = napraviDugme(ime, () -> primeniStil(procitajStil(ime)));
            HBox.setHgrow(primeni, Priority.ALWAYS);
            Button obrisi = new Button("Obriši");
            obrisi.setOnAction(e -> {
                Alert pitanje = new Alert(Alert.AlertType.CONFIRMATION,
                        "Obrisati stil \"" + ime + "\"?", ButtonType.YES, ButtonType.NO);
                pitanje.setTitle("Obriši stil");
                pitanje.setHeaderText(null);
                pitanje.initOwner(obrisi.getScene().getWindow());
                if (pitanje.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                    cvorStilova().remove(ime);
                    osveziMojeStilove();
                }
            });
            mojiStilovi.getChildren().add(new HBox(5, primeni, obrisi));
        }
        if (imena.length == 0) {
            mojiStilovi.getChildren().add(new Label("Još nema sačuvanih stilova."));
        }
    }

    // Rotacija, ogledalo, zumiranje i pomeranje prikaza.
    // Prvo se primenjuje ogledalo, pa rotacija (isto kao pri čuvanju u okreni(...))
    private void primeniPolozaj() {
        boolean saIzmenama = !prikazujeOriginal;
        prikaz.setRotate(saIzmenama ? 90 * rotacija.getValue() : 0);
        prikaz.setScaleX((saIzmenama && ogledaloLevoDesno.getValue() == 1 ? -1 : 1) * uvecanje);
        prikaz.setScaleY((saIzmenama && ogledaloGoreDole.getValue() == 1 ? -1 : 1) * uvecanje);
        prikaz.setTranslateX(pomakX);
        prikaz.setTranslateY(pomakY);
        osveziDodatke();
    }

    // Automatsko popravljanje: na osnovu slike (bez efekata) podešava svetlinu, kontrast i ravnotežu boja.
    // Ostali klizači se ne diraju, a sve promene su jedan korak za Undo.
    private void automatskiPopravi() {
        Image slika = prikaz.getImage();
        if (slika == null) {
            return;
        }
        int w = (int) slika.getWidth();
        int h = (int) slika.getHeight();
        // Dovoljno je pogledati oko 100.000 piksela, ravnomerno raspoređenih po slici
        int korak = Math.max(1, (int) Math.sqrt((double) w * h / 100_000));
        PixelReader citac = slika.getPixelReader();
        int[] brojPoOsvetljenosti = new int[256];
        long zbirR = 0, zbirG = 0, zbirB = 0, ukupno = 0;
        for (int y = 0; y < h; y += korak) {
            for (int x = 0; x < w; x += korak) {
                int c = citac.getArgb(x, y);
                if ((c >>> 24) == 0) {
                    continue;
                }
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                brojPoOsvetljenosti[(299 * r + 587 * g + 114 * b) / 1000]++;
                zbirR += r;
                zbirG += g;
                zbirB += b;
                ukupno++;
            }
        }
        if (ukupno == 0) {
            return;
        }

        // Najtamnijih i najsvetlijih 1% piksela se zanemaruje (npr. odsjaji), da ne pokvare procenu
        int tamno = -1, svetlo = 255;
        long brojac = 0;
        double zbirOsvetljenosti = 0;
        for (int i = 0; i < 256; i++) {
            brojac += brojPoOsvetljenosti[i];
            zbirOsvetljenosti += (double) i * brojPoOsvetljenosti[i];
            if (tamno < 0 && brojac >= ukupno * 0.01) {
                tamno = i;
            }
            if (brojac <= ukupno * 0.99) {
                svetlo = i;
            }
        }
        double prosek = zbirOsvetljenosti / ukupno / 255;
        double raspon = Math.max(0, svetlo - tamno) / 255.0;

        // Previše tamna ili svetla slika se pomera ka sredini, a slika malog raspona dobija više kontrasta
        double novaSvetlina = ogranici((0.5 - prosek) * 0.8, -0.4, 0.4);
        double noviKontrast = ogranici((0.85 - raspon) * 0.8, -0.2, 0.5);
        // Ravnoteža boja ("siva u proseku"): prosečna boja slike treba da bude neutralno siva
        double sivo = (zbirR + zbirG + zbirB) / 3.0;
        double novaCrvena = zbirR > 0 ? ogranici(sivo / zbirR - 1, -0.25, 0.25) : 0;
        double novaZelena = zbirG > 0 ? ogranici(sivo / zbirG - 1, -0.25, 0.25) : 0;
        double novaPlava = zbirB > 0 ? ogranici(sivo / zbirB - 1, -0.25, 0.25) : 0;
        postaviVise(Map.of(
                svetlina, zaokruzi(novaSvetlina), kontrast, zaokruzi(noviKontrast),
                crvena, zaokruzi(novaCrvena), zelena, zaokruzi(novaZelena), plava, zaokruzi(novaPlava)));
    }

    private static double ogranici(double vrednost, double najmanje, double najvise) {
        return Math.max(najmanje, Math.min(najvise, vrednost));
    }

    private static double zaokruzi(double vrednost) {
        return Math.round(vrednost * 100) / 100.0;
    }

    // Da li je tačka (u koordinatama prostora za prikaz) iznad vidljivog teksta; uračunava i rotaciju teksta.
    // Gleda se ceo pravougaonik reda teksta, ne samo slova, da bi se tekst lakše uhvatio.
    private boolean jeIznadTeksta(double x, double y) {
        return tekstPrikaz.isVisible()
                && tekstPrikaz.getLayoutBounds().contains(tekstPrikaz.parentToLocal(x, y));
    }

    // Upisuje tekst iz polja u istoriju (kao redni broj u spisku tekstovi)
    private void potvrdiTekst() {
        String tekst = tekstPolje.getText();
        int indeks = tekstovi.indexOf(tekst);
        if (indeks < 0) {
            tekstovi.add(tekst);
            indeks = tekstovi.size() - 1;
        }
        tekstIndeks.setValue(indeks);
    }

    // Povezuje izbornik sa skrivenim klizačem koji pamti redni broj izabrane stavke
    private static void poveziIzbor(ChoiceBox<String> izbor, Slider klizac) {
        izbor.getSelectionModel().select((int) klizac.getValue());
        izbor.getSelectionModel().selectedIndexProperty().addListener((o, s, n) -> {
            if (n.intValue() >= 0) {
                klizac.setValue(n.intValue());
            }
        });
        klizac.valueProperty().addListener((o, s, n) -> izbor.getSelectionModel().select(n.intValue()));
    }

    // Početne boje skrivenih klizača za boju, kao ARGB broj
    private static final long BELA = 0xFFFFFFFFL;
    private static final long CRNA = 0xFF000000L;

    // Povezuje izbor boje sa skrivenim klizačem. Klizač čuva ARGB broj XOR početna boja,
    // da bi vrednost 0 (početna, i posle Resetuj) značila baš početnu boju.
    private static void poveziBoju(ColorPicker izbor, Slider klizac, long pocetna) {
        izbor.setValue(uBoju(klizac.getValue(), pocetna));
        izbor.valueProperty().addListener((o, s, n) -> klizac.setValue(izBoje(n, pocetna)));
        klizac.valueProperty().addListener((o, s, n) -> izbor.setValue(uBoju(n.doubleValue(), pocetna)));
    }

    private static Color uBoju(double vrednost, long pocetna) {
        long argb = ((long) vrednost ^ pocetna) & 0xFFFFFFFFL;
        return Color.rgb((int) (argb >> 16) & 0xFF, (int) (argb >> 8) & 0xFF, (int) argb & 0xFF,
                ((argb >> 24) & 0xFF) / 255.0);
    }

    private static double izBoje(Color boja, long pocetna) {
        long argb = Math.round(boja.getOpacity() * 255) << 24 | Math.round(boja.getRed() * 255) << 16
                | Math.round(boja.getGreen() * 255) << 8 | Math.round(boja.getBlue() * 255);
        return (argb ^ pocetna) & 0xFFFFFFFFL;
    }

    // Podešavanja teksta i okvira u jednom komadu, da bi se mogla koristiti i u pozadini pri čuvanju
    private record Dodaci(String tekst, String font, boolean podebljano, double velicina, double x, double y,
                          double rotacija, Color boja,
                          double senka, Color bojaSenke, double udaljenostSenke, double smerSenke,
                          double zamucenjeSenke,
                          double okvir, boolean polaroid, Color bojaOkvira) {

        double velicinaFonta(double visinaSlike) {
            return (0.03 + 0.2 * velicina) * visinaSlike;
        }

        // Debljina okvira sa strane i gore, i debljina donje ivice, u pikselima
        double debljina(double sirinaSlike, double visinaSlike) {
            return okvir * Math.min(sirinaSlike, visinaSlike);
        }

        double donjaIvica(double sirinaSlike, double visinaSlike) {
            return debljina(sirinaSlike, visinaSlike) * (polaroid ? 3.5 : 1);
        }

        // Pomak senke u pikselima: udaljenost od 6% do 40% veličine slova, u smeru 45° (dole desno) + smer.
        // Smer je nezavisan od rotacije teksta, kao da svetlo uvek dolazi sa iste strane.
        double pomakSenkeX(double velicinaFonta) {
            return velicinaFonta * (0.06 + 0.34 * udaljenostSenke) * Math.cos(Math.toRadians(45 + smerSenke));
        }

        double pomakSenkeY(double velicinaFonta) {
            return velicinaFonta * (0.06 + 0.34 * udaljenostSenke) * Math.sin(Math.toRadians(45 + smerSenke));
        }

        // Poluprečnik zamućenja senke u pikselima (kao kod GaussianBlur u JavaFX-u)
        double zamucenjeSenkePiksela(double velicinaFonta) {
            return velicinaFonta * 0.3 * zamucenjeSenke;
        }

        // Boja senke pomnožena njenom jačinom
        Color konacnaBojaSenke() {
            return bojaSenke.deriveColor(0, 1, 1, senka);
        }
    }

    private Dodaci trenutniDodaci() {
        return new Dodaci(tekstPolje.getText(), FONTOVI.get((int) fontTeksta.getValue()),
                podebljanTekst.getValue() == 1, velicinaTeksta.getValue(), tekstX.getValue(), tekstY.getValue(),
                rotacijaTeksta.getValue(), uBoju(bojaTeksta.getValue(), BELA),
                senkaTeksta.getValue(), uBoju(bojaSenke.getValue(), CRNA), udaljenostSenke.getValue(),
                smerSenke.getValue(), zamucenjeSenke.getValue(),
                debljinaOkvira.getValue(), polaroidOkvir.getValue() == 1, uBoju(bojaOkvira.getValue(), BELA));
    }

    // Postavlja tekst i okvir preko slike na glavnom prozoru, prema tome gde se slika trenutno vidi
    private void osveziDodatke() {
        Dodaci d = trenutniDodaci();
        boolean vidljivo = prikaz.getImage() != null && !prikazujeOriginal;
        // Granice slike u prostoru za prikaz, posle rotacije, ogledala i zumiranja (bez proširenja od efekata)
        Bounds slika = prikaz.localToParent(prikaz.getLayoutBounds());
        double w = slika.getWidth();
        double h = slika.getHeight();

        double debljina = d.debljina(w, h);
        okvirPrikaz.setVisible(vidljivo && debljina > 0);
        okvirPrikaz.setFill(d.bojaOkvira());
        okvirPrikaz.setX(slika.getMinX() - debljina);
        okvirPrikaz.setY(slika.getMinY() - debljina);
        okvirPrikaz.setWidth(w + 2 * debljina);
        okvirPrikaz.setHeight(h + debljina + d.donjaIvica(w, h));

        // Ranije dodati tekstovi (ispod) i tekst koji se trenutno uređuje (iznad njih)
        dodatiTekstovi.getChildren().clear();
        cvoroviDodatihTekstova.clear();
        for (Dodaci dodat : slojevi().tekstovi()) {
            Text tekst = new Text();
            Text senka = new Text();
            postaviTekstCvorove(dodat, tekst, senka, slika, vidljivo);
            dodatiTekstovi.getChildren().addAll(senka, tekst);
            cvoroviDodatihTekstova.add(tekst);
        }
        postaviTekstCvorove(d, tekstPrikaz, senkaPrikaz, slika, vidljivo);
        osveziNalepnice(slika, vidljivo);
        osveziCrtez();
    }

    private static void postaviTekstCvorove(Dodaci d, Text tekst, Text senka, Bounds slika, boolean vidljivo) {
        double w = slika.getWidth();
        double h = slika.getHeight();
        boolean imaTeksta = vidljivo && !d.tekst().isBlank();
        double velicinaFonta = d.velicinaFonta(h);
        Font font = Font.font(d.font(), d.podebljano() ? FontWeight.BOLD : FontWeight.NORMAL, velicinaFonta);
        double cx = slika.getMinX() + w / 2 + d.x() * w / 2;
        double cy = slika.getMinY() + h / 2 + d.y() * h / 2;
        for (Text t : List.of(tekst, senka)) {
            t.setTextOrigin(VPos.CENTER);
            t.setText(d.tekst());
            t.setFont(font);
            t.setX(cx - t.getLayoutBounds().getWidth() / 2);
            t.setY(cy);
            // Rotira se oko svog središta, a to je tačka (cx, cy)
            t.setRotate(d.rotacija());
        }
        tekst.setFill(d.boja());
        tekst.setVisible(imaTeksta);
        senka.setX(senka.getX() + d.pomakSenkeX(velicinaFonta));
        senka.setY(cy + d.pomakSenkeY(velicinaFonta));
        senka.setFill(d.konacnaBojaSenke());
        double zamucenjeSenke = d.zamucenjeSenkePiksela(velicinaFonta);
        senka.setEffect(zamucenjeSenke >= 1 ? new GaussianBlur(Math.min(63, zamucenjeSenke)) : null);
        senka.setVisible(imaTeksta && d.senka() > 0);
    }

    // Crta nalepnice, poteze četkicom, sve tekstove i okvir na gotovu sliku (posle filtera i rotacije),
    // istim redom kao na prikazu. Okvir se uzima iz teksta koji se uređuje (aktivni), jer je jedan za celu sliku.
    private static BufferedImage nacrtajDodatke(BufferedImage slika, Dodaci aktivni, Slojevi slojevi) {
        int w = slika.getWidth();
        int h = slika.getHeight();
        nacrtajNalepnice(slika, slojevi.nalepnice());
        if (!slojevi.potezi().isEmpty()) {
            // Potezi idu na poseban providan sloj (zbog gumice), koji se zatim stavi preko slike
            BufferedImage sloj = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            nacrtajPoteze(sloj, slojevi.potezi(), 0, 0, w, h);
            Graphics2D crtanje = slika.createGraphics();
            crtanje.drawImage(sloj, 0, 0, null);
            crtanje.dispose();
        }
        for (Dodaci dodat : slojevi.tekstovi()) {
            nacrtajJedanTekst(slika, dodat);
        }
        nacrtajJedanTekst(slika, aktivni);

        int debljina = (int) Math.round(aktivni.debljina(w, h));
        if (debljina <= 0) {
            return slika;
        }
        int donja = (int) Math.round(aktivni.donjaIvica(w, h));
        BufferedImage saOkvirom = new BufferedImage(w + 2 * debljina, h + debljina + donja, slika.getType());
        Graphics2D crtanje = saOkvirom.createGraphics();
        crtanje.setColor(uAwtBoju(aktivni.bojaOkvira()));
        crtanje.fillRect(0, 0, saOkvirom.getWidth(), saOkvirom.getHeight());
        crtanje.drawImage(slika, debljina, debljina, null);
        crtanje.dispose();
        return saOkvirom;
    }

    // Crta jedan tekst sa senkom na sliku
    private static void nacrtajJedanTekst(BufferedImage slika, Dodaci d) {
        int w = slika.getWidth();
        int h = slika.getHeight();
        if (!d.tekst().isBlank()) {
            double velicinaFonta = d.velicinaFonta(h);
            double cx = w / 2.0 + d.x() * w / 2;
            double cy = h / 2.0 + d.y() * h / 2;
            if (d.senka() > 0) {
                double sx = cx + d.pomakSenkeX(velicinaFonta);
                double sy = cy + d.pomakSenkeY(velicinaFonta);
                // GaussianBlur poluprečnika r ≈ tri zamućenja kvadratom poluprečnika r/3
                int poluprecnik = (int) Math.round(d.zamucenjeSenkePiksela(velicinaFonta) / 3);
                if (poluprecnik < 1) {
                    Graphics2D crtanje = slika.createGraphics();
                    nacrtajTekst(crtanje, d, velicinaFonta, sx, sy, uAwtBoju(d.konacnaBojaSenke()));
                    crtanje.dispose();
                } else {
                    // Zamućena senka: tekst se crta na poseban providan sloj, zamuti mu se providnost,
                    // pa se sloj u boji senke stavi preko slike
                    BufferedImage sloj = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D crtanjeSloja = sloj.createGraphics();
                    nacrtajTekst(crtanjeSloja, d, velicinaFonta, sx, sy, java.awt.Color.WHITE);
                    crtanjeSloja.dispose();
                    int[] pikseli = sloj.getRGB(0, 0, w, h, null, 0, w);
                    int[] alfa = new int[pikseli.length];
                    for (int i = 0; i < pikseli.length; i++) {
                        alfa[i] = pikseli[i] >>> 24;
                    }
                    for (int prolaz = 0; prolaz < 3; prolaz++) {
                        alfa = zamutiKanal(alfa, w, h, poluprecnik);
                    }
                    Color boja = d.konacnaBojaSenke();
                    int rgb = (int) Math.round(boja.getRed() * 255) << 16
                            | (int) Math.round(boja.getGreen() * 255) << 8 | (int) Math.round(boja.getBlue() * 255);
                    for (int i = 0; i < pikseli.length; i++) {
                        pikseli[i] = (int) Math.round(alfa[i] * boja.getOpacity()) << 24 | rgb;
                    }
                    sloj.setRGB(0, 0, w, h, pikseli, 0, w);
                    Graphics2D crtanje = slika.createGraphics();
                    crtanje.drawImage(sloj, 0, 0, null);
                    crtanje.dispose();
                }
            }
            Graphics2D crtanje = slika.createGraphics();
            nacrtajTekst(crtanje, d, velicinaFonta, cx, cy, uAwtBoju(d.boja()));
            crtanje.dispose();
        }
    }

    // ===== Crtanje četkicom =====

    private enum Alat {
        OLOVKA("Olovka"), CETKICA("Četkica"), FLOMASTER("Flomaster"), SPREJ("Sprej"), GUMICA("Gumica"),
        LINIJA("Prava linija"), STRELICA("Strelica"), PRAVOUGAONIK("Pravougaonik"), ELIPSA("Elipsa");

        final String naziv;

        Alat(String naziv) {
            this.naziv = naziv;
        }

        // Alatke koje pamte samo početnu i krajnju tačku (od pritiska do puštanja miša)
        boolean dveTacke() {
            return this == LINIJA || this == STRELICA || this == PRAVOUGAONIK || this == ELIPSA;
        }
    }

    // Jedan potez mišem. Tačke su udeo širine i visine prikazane (gotove) slike, od 0 do 1;
    // debljina je udeo visine slike, pa potez izgleda isto na prikazu i u sačuvanoj slici.
    // seme: početni broj za nasumične tačke spreja, da sprej uvek izgleda isto.
    // popunjeno: za strelicu, pravougaonik i elipsu (popunjen oblik umesto obrisa)
    private record Potez(Alat alat, double[] x, double[] y, Color boja, double debljina,
                         double neprovidnost, double mekoca, long seme, boolean popunjeno) {
    }

    // Nalepnica: druga slika (npr. logo) preko fotografije. Središte je udeo prikazane slike (0 do 1),
    // širina je udeo širine slike, a rotacija u stepenima.
    private record Nalepnica(Image slika, String ime, double x, double y, double sirina,
                             double neprovidnost, double rotacija) {

        double visina(double sirinaSlike) {
            return sirina * sirinaSlike * slika.getHeight() / slika.getWidth();
        }
    }

    // Ispravka crvenih očiju: središte i poluprečnik su udeo širine/visine cele (neisečene, neokrenute) slike
    private record CrvenoOko(double x, double y, double poluprecnik) {
    }

    // Sve što nije broj na klizaču: potezi četkicom, ranije dodati tekstovi i ispravke crvenih očiju.
    // Svaka izmena pravi novu verziju u spisku verzijeSlojeva, a skriveni klizač verzijaSlojeva pamti
    // redni broj trenutne verzije; tako i ovo radi sa Undo/Redo, Resetuj i proverom nesačuvanih izmena.
    private record Slojevi(List<Potez> potezi, List<Dodaci> tekstovi, List<CrvenoOko> oci,
                           List<Nalepnica> nalepnice) {

        Slojevi saPotezima(List<Potez> novi) {
            return new Slojevi(List.copyOf(novi), tekstovi, oci, nalepnice);
        }

        Slojevi saTekstovima(List<Dodaci> novi) {
            return new Slojevi(potezi, List.copyOf(novi), oci, nalepnice);
        }

        Slojevi saOcima(List<CrvenoOko> novi) {
            return new Slojevi(potezi, tekstovi, List.copyOf(novi), nalepnice);
        }

        Slojevi saNalepnicama(List<Nalepnica> novi) {
            return new Slojevi(potezi, tekstovi, oci, List.copyOf(novi));
        }
    }

    private static final Slojevi PRAZNI_SLOJEVI = new Slojevi(List.of(), List.of(), List.of(), List.of());

    private Slojevi slojevi() {
        return slojeviZa((int) verzijaSlojeva.getValue());
    }

    private Slojevi slojeviZa(int verzija) {
        return verzijeSlojeva.get(Math.max(0, Math.min(verzijeSlojeva.size() - 1, verzija)));
    }

    private void promeniSlojeve(Slojevi novi) {
        verzijeSlojeva.add(novi);
        verzijaSlojeva.setValue(verzijeSlojeva.size() - 1);
    }

    private static <T> List<T> saDodatim(List<T> spisak, T novi) {
        List<T> rezultat = new ArrayList<>(spisak);
        rezultat.add(novi);
        return List.copyOf(rezultat);
    }

    // Crta potez na slici čiji je gornji levi ugao (x0, y0), a veličina w x h piksela.
    // Isti kod crta i na ekranu i u sačuvanoj slici (oba preko Java AWT), pa potezi svuda izgledaju isto.
    // Potezi se crtaju na poseban providan sloj, da bi gumica brisala samo crtež, a ne fotografiju.
    private static void nacrtajPotez(Graphics2D g, Potez p, double x0, double y0, double w, double h) {
        int n = p.x().length;
        if (n == 0) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = x0 + p.x()[i] * w;
            ys[i] = y0 + p.y()[i] * h;
        }
        double debljina = Math.max(0.5, p.debljina() * h);
        Composite prethodni = g.getComposite();
        if (p.alat() == Alat.GUMICA) {
            // Gumica: gde prolazi, sloj sa crtežom postaje providan (koliko je gumica neprovidna)
            g.setComposite(AlphaComposite.DstOut);
        }

        switch (p.alat()) {
            case SPREJ -> {
                // Sprej: oko svake tačke 12 sitnih kapljica nasumično raspoređenih u krugu prečnika 2 x debljina
                Random nasumicno = new Random(p.seme());
                g.setColor(uAwtBoju(p.boja().deriveColor(0, 1, 1, p.neprovidnost())));
                double kapljica = Math.max(0.5, debljina * 0.05);
                for (int i = 0; i < n; i++) {
                    for (int k = 0; k < 12; k++) {
                        double ugao = nasumicno.nextDouble() * 2 * Math.PI;
                        double udaljenost = Math.sqrt(nasumicno.nextDouble()) * debljina;
                        g.fill(new Ellipse2D.Double(xs[i] + Math.cos(ugao) * udaljenost - kapljica,
                                ys[i] + Math.sin(ugao) * udaljenost - kapljica, 2 * kapljica, 2 * kapljica));
                    }
                }
            }
            case PRAVOUGAONIK, ELIPSA, STRELICA -> nacrtajOblik(g, p, xs[0], ys[0], xs[n - 1], ys[n - 1], debljina);
            default -> nacrtajLiniju(g, p, xs, ys, debljina);
        }
        g.setComposite(prethodni);
    }

    // Olovka, četkica, flomaster, gumica i prava linija
    private static void nacrtajLiniju(Graphics2D g, Potez p, double[] xs, double[] ys, double debljina) {
        boolean kvadratniVrh = p.alat() == Alat.FLOMASTER;
        boolean meka = p.alat() == Alat.CETKICA || p.alat() == Alat.LINIJA || p.alat() == Alat.GUMICA;
        double mekoca = meka ? p.mekoca() : 0;
        // Meka ivica: potez se crta 6 puta, od najšireg do najužeg, poluprovidnom bojom.
        // Na sredini se slojevi saberu do izabrane neprovidnosti, a ka ivici je potez sve providniji.
        int prolaza = mekoca > 0 ? 6 : 1;
        double cilj = prolaza > 1 ? Math.min(p.neprovidnost(), 0.97) : p.neprovidnost();
        g.setColor(uAwtBoju(p.boja().deriveColor(0, 1, 1, 1 - Math.pow(1 - cilj, 1.0 / prolaza))));
        double najsira = debljina * (1 + mekoca);
        double najuza = debljina * Math.max(0.25, 1 - 0.75 * mekoca);
        Path2D.Double putanja = new Path2D.Double();
        putanja.moveTo(xs[0], ys[0]);
        for (int i = 1; i < xs.length; i++) {
            putanja.lineTo(xs[i], ys[i]);
        }
        for (int i = 0; i < prolaza; i++) {
            double udeo = prolaza == 1 ? 1 : i / (prolaza - 1.0);
            double sirina = najsira + (najuza - najsira) * udeo;
            if (xs.length == 1) {
                // Jedan klik: tačka u obliku vrha alatke
                if (kvadratniVrh) {
                    g.fill(new java.awt.geom.Rectangle2D.Double(xs[0] - sirina / 2, ys[0] - sirina / 2, sirina, sirina));
                } else {
                    g.fill(new Ellipse2D.Double(xs[0] - sirina / 2, ys[0] - sirina / 2, sirina, sirina));
                }
            } else {
                g.setStroke(new BasicStroke((float) sirina,
                        kvadratniVrh ? BasicStroke.CAP_SQUARE : BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(putanja);
            }
        }
    }

    // Pravougaonik i elipsa između dve tačke, i strelica od prve ka drugoj tački
    private static void nacrtajOblik(Graphics2D g, Potez p, double x1, double y1, double x2, double y2,
                                     double debljina) {
        g.setColor(uAwtBoju(p.boja().deriveColor(0, 1, 1, p.neprovidnost())));
        g.setStroke(new BasicStroke((float) debljina, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double levo = Math.min(x1, x2);
        double gore = Math.min(y1, y2);
        double sirina = Math.abs(x2 - x1);
        double visina = Math.abs(y2 - y1);
        if (p.alat() == Alat.STRELICA) {
            // Vrh strelice: dve kraće linije pod uglom od 25° unazad, dužine 4 debljine (najmanje 1/4 strelice)
            double ugao = Math.atan2(y2 - y1, x2 - x1);
            double duzinaVrha = Math.min(Math.hypot(x2 - x1, y2 - y1) * 0.4, Math.max(debljina * 4, 8));
            double lx = x2 - duzinaVrha * Math.cos(ugao - Math.toRadians(25));
            double ly = y2 - duzinaVrha * Math.sin(ugao - Math.toRadians(25));
            double dx = x2 - duzinaVrha * Math.cos(ugao + Math.toRadians(25));
            double dy = y2 - duzinaVrha * Math.sin(ugao + Math.toRadians(25));
            Path2D.Double vrh = new Path2D.Double();
            if (p.popunjeno()) {
                // Popunjen vrh je trougao; telo strelice ide do njegove osnove
                vrh.moveTo(x2, y2);
                vrh.lineTo(lx, ly);
                vrh.lineTo(dx, dy);
                vrh.closePath();
                g.draw(new java.awt.geom.Line2D.Double(x1, y1, (lx + dx) / 2, (ly + dy) / 2));
                g.fill(vrh);
                g.draw(vrh);
            } else {
                vrh.moveTo(lx, ly);
                vrh.lineTo(x2, y2);
                vrh.lineTo(dx, dy);
                g.draw(new java.awt.geom.Line2D.Double(x1, y1, x2, y2));
                g.draw(vrh);
            }
            return;
        }
        java.awt.Shape oblik = p.alat() == Alat.ELIPSA
                ? new Ellipse2D.Double(levo, gore, sirina, visina)
                : new java.awt.geom.Rectangle2D.Double(levo, gore, sirina, visina);
        if (p.popunjeno()) {
            g.fill(oblik);
        } else {
            g.draw(oblik);
        }
    }

    // Potez koji se upravo crta mišem, sa trenutnim podešavanjima palete
    private Potez trenutniPotez() {
        double[] xs = trenutneTackeX.stream().mapToDouble(Double::doubleValue).toArray();
        double[] ys = trenutneTackeY.stream().mapToDouble(Double::doubleValue).toArray();
        return new Potez(izabranaAlatka(), xs, ys, bojaCetkice.getValue(), debljinaCetkice.getValue() / 500,
                neprovidnostCetkice.getValue(), mekocaCetkice.getValue(), semePoteza, popunjenOblik.isSelected());
    }

    private Alat izabranaAlatka() {
        return alati.getSelectedToggle() == null ? Alat.OLOVKA : (Alat) alati.getSelectedToggle().getUserData();
    }

    // Crta sve poteze na providan sloj veličine w x h (gornji levi ugao slike je u (x0, y0))
    private static void nacrtajPoteze(BufferedImage sloj, List<Potez> potezi, double x0, double y0,
                                      double w, double h) {
        Graphics2D g = sloj.createGraphics();
        g.clip(new java.awt.geom.Rectangle2D.Double(x0, y0, w, h));
        for (Potez potez : potezi) {
            nacrtajPotez(g, potez, x0, y0, w, h);
        }
        g.dispose();
    }

    // Iscrtava sve poteze preko slike na glavnom prozoru (samo unutar slike). Sloj je veličine
    // prostora za prikaz, a ne uvećane slike, da ne bi bio ogroman kad je slika zumirana.
    private void osveziCrtez() {
        List<Potez> potezi = new ArrayList<>(slojevi().potezi());
        if (trenutneTackeX != null) {
            potezi.add(trenutniPotez());
        }
        Region prostor = (Region) prikaz.getParent();
        int sirina = prostor == null ? 0 : (int) Math.ceil(prostor.getWidth());
        int visina = prostor == null ? 0 : (int) Math.ceil(prostor.getHeight());
        if (prikaz.getImage() == null || prikazujeOriginal || potezi.isEmpty() || sirina <= 0 || visina <= 0) {
            crtezPrikaz.setImage(null);
            return;
        }
        Bounds slika = prikaz.localToParent(prikaz.getLayoutBounds());
        BufferedImage sloj = new BufferedImage(sirina, visina, BufferedImage.TYPE_INT_ARGB);
        nacrtajPoteze(sloj, potezi, slika.getMinX(), slika.getMinY(), slika.getWidth(), slika.getHeight());
        crtezPrikaz.setImage(uFxSliku(sloj));
    }

    // Pretvara AWT sliku u JavaFX sliku (bez modula javafx.swing)
    private static WritableImage uFxSliku(BufferedImage slika) {
        int w = slika.getWidth();
        int h = slika.getHeight();
        WritableImage rezultat = new WritableImage(w, h);
        rezultat.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(),
                slika.getRGB(0, 0, w, h, null, 0, w), 0, w);
        return rezultat;
    }

    // Pretvara JavaFX sliku u AWT sliku sa providnošću (za nalepnice pri čuvanju)
    private static BufferedImage uAwtSliku(Image slika) {
        int w = (int) slika.getWidth();
        int h = (int) slika.getHeight();
        int[] pikseli = new int[w * h];
        slika.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pikseli, 0, w);
        BufferedImage rezultat = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        rezultat.setRGB(0, 0, w, h, pikseli, 0, w);
        return rezultat;
    }

    // Tačka miša kao udeo prikazane slike (0 do 1), za crtanje i nalepnice
    private double[] udeoSlike(double x, double y) {
        Bounds slika = prikaz.localToParent(prikaz.getLayoutBounds());
        return new double[]{(x - slika.getMinX()) / slika.getWidth(), (y - slika.getMinY()) / slika.getHeight()};
    }

    private void zapocniPotez(double x, double y) {
        trenutneTackeX = new ArrayList<>();
        trenutneTackeY = new ArrayList<>();
        semePoteza = System.nanoTime();
        nastaviPotez(x, y);
    }

    private void nastaviPotez(double x, double y) {
        double[] udeo = udeoSlike(x, y);
        // Linija, strelica i oblici imaju samo početnu i krajnju tačku
        if (izabranaAlatka().dveTacke() && trenutneTackeX.size() == 2) {
            trenutneTackeX.set(1, udeo[0]);
            trenutneTackeY.set(1, udeo[1]);
        } else {
            trenutneTackeX.add(udeo[0]);
            trenutneTackeY.add(udeo[1]);
        }
        osveziCrtez();
    }

    // Završen potez postaje deo slike (jedan korak za Undo)
    private void zavrsiPotez() {
        Potez potez = trenutniPotez();
        trenutneTackeX = null;
        trenutneTackeY = null;
        promeniSlojeve(slojevi().saPotezima(saDodatim(slojevi().potezi(), potez)));
    }

    private void obrisiPoslednjiPotez() {
        List<Potez> potezi = slojevi().potezi();
        if (!potezi.isEmpty()) {
            promeniSlojeve(slojevi().saPotezima(potezi.subList(0, potezi.size() - 1)));
        }
    }

    private void obrisiCrtez() {
        if (!slojevi().potezi().isEmpty()) {
            promeniSlojeve(slojevi().saPotezima(List.of()));
        }
    }

    // ===== Nalepnice =====

    private void dodajNalepnicu(Stage prozor) {
        if (prikaz.getImage() == null) {
            return;
        }
        FileChooser izbor = new FileChooser();
        izbor.setTitle("Izaberi sliku za nalepnicu");
        izbor.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Slike", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));
        postaviPocetniFolder(izbor);
        File fajl = izbor.showOpenDialog(prozor);
        if (fajl == null) {
            return;
        }
        Image slika = new Image(fajl.toURI().toString());
        if (slika.isError()) {
            prikaziGresku(prozor, "Slika ne može da se otvori:\n" + fajl.getAbsolutePath());
            return;
        }
        // Nova nalepnica je na sredini, četvrtina širine slike
        Nalepnica nova = new Nalepnica(slika, fajl.getName(), 0.5, 0.5, 0.25, 1, 0);
        promeniSlojeve(slojevi().saNalepnicama(saDodatim(slojevi().nalepnice(), nova)));
        izaberiNalepnicu(slojevi().nalepnice().size() - 1);
    }

    // Klizač podešavanja (ne izmene slike) dobija sačuvanu vrednost i pamti svaku promenu
    private void zapamtiKlizac(Slider klizac, String kljuc) {
        klizac.setValue(podesavanjaPrograma.getDouble(kljuc, klizac.getValue()));
        klizac.valueProperty().addListener((o, s, n) -> podesavanjaPrograma.putDouble(kljuc, n.doubleValue()));
    }

    private void poveziKlizacNalepnice(Slider klizac, BiFunction<Nalepnica, Double, Nalepnica> izmena) {
        klizac.valueChangingProperty().addListener((o, s, menjaSe) -> {
            if (menjaSe) {
                verzijaPokreta = -1;
            }
        });
        klizac.valueProperty().addListener((o, s, n) -> {
            if (!postavljaKlizaceNalepnice) {
                izmeniNalepnicu(nal -> izmena.apply(nal, n.doubleValue()), klizac.isValueChanging());
            }
        });
    }

    // Postavlja klizače nalepnice na vrednosti izabrane nalepnice (bez menjanja slike)
    private void izaberiNalepnicu(int indeks) {
        izabranaNalepnica = indeks;
        List<Nalepnica> nalepnice = slojevi().nalepnice();
        boolean ima = indeks >= 0 && indeks < nalepnice.size();
        postavljaKlizaceNalepnice = true;
        if (ima) {
            Nalepnica n = nalepnice.get(indeks);
            velicinaNalepnice.setValue(n.sirina());
            neprovidnostNalepnice.setValue(n.neprovidnost());
            rotacijaNalepnice.setValue(n.rotacija());
        }
        postavljaKlizaceNalepnice = false;
        izabranaNalepnicaOznaka.setText(ima ? "Izabrana: " + nalepnice.get(indeks).ime() : "Nijedna nalepnica nije izabrana");
        for (Node klizac : List.of(velicinaNalepnice, neprovidnostNalepnice, rotacijaNalepnice)) {
            klizac.setDisable(!ima);
        }
    }

    // Menja izabranu nalepnicu. Prva promena u jednom pokretu (prevlačenje mišem ili klizača)
    // pravi novu verziju (jedan korak za Undo), a sledeće promene u istom pokretu menjaju tu istu verziju.
    private void izmeniNalepnicu(UnaryOperator<Nalepnica> izmena, boolean nastavakPokreta) {
        List<Nalepnica> nalepnice = new ArrayList<>(slojevi().nalepnice());
        if (izabranaNalepnica < 0 || izabranaNalepnica >= nalepnice.size()) {
            return;
        }
        nalepnice.set(izabranaNalepnica, izmena.apply(nalepnice.get(izabranaNalepnica)));
        Slojevi novi = slojevi().saNalepnicama(nalepnice);
        int trenutna = (int) verzijaSlojeva.getValue();
        if (nastavakPokreta && verzijaPokreta == trenutna && trenutna == verzijeSlojeva.size() - 1) {
            verzijeSlojeva.set(trenutna, novi);
            osveziDodatke();
        } else {
            promeniSlojeve(novi);
            verzijaPokreta = nastavakPokreta ? (int) verzijaSlojeva.getValue() : -1;
        }
    }

    private void obrisiNalepnicu() {
        List<Nalepnica> nalepnice = new ArrayList<>(slojevi().nalepnice());
        if (izabranaNalepnica >= 0 && izabranaNalepnica < nalepnice.size()) {
            nalepnice.remove(izabranaNalepnica);
            promeniSlojeve(slojevi().saNalepnicama(nalepnice));
            izaberiNalepnicu(-1);
        }
    }

    private void obrisiSveNalepnice() {
        if (!slojevi().nalepnice().isEmpty()) {
            promeniSlojeve(slojevi().saNalepnicama(List.of()));
            izaberiNalepnicu(-1);
        }
    }

    // Redni broj nalepnice ispod tačke miša (gornja ima prednost), ili -1
    private int nalepnicaIspod(double x, double y) {
        for (int i = cvoroviNalepnica.size() - 1; i >= 0; i--) {
            ImageView v = cvoroviNalepnica.get(i);
            if (v.isVisible() && v.getLayoutBounds().contains(v.parentToLocal(x, y))) {
                return i;
            }
        }
        return -1;
    }

    // Postavlja nalepnice preko slike na glavnom prozoru
    private void osveziNalepnice(Bounds slika, boolean vidljivo) {
        slojNalepnica.getChildren().clear();
        cvoroviNalepnica.clear();
        for (Nalepnica n : slojevi().nalepnice()) {
            ImageView v = new ImageView(n.slika());
            double sirina = n.sirina() * slika.getWidth();
            double visina = n.visina(slika.getWidth());
            v.setFitWidth(sirina);
            v.setFitHeight(visina);
            v.setX(slika.getMinX() + n.x() * slika.getWidth() - sirina / 2);
            v.setY(slika.getMinY() + n.y() * slika.getHeight() - visina / 2);
            v.setRotate(n.rotacija());
            v.setOpacity(n.neprovidnost());
            v.setVisible(vidljivo);
            slojNalepnica.getChildren().add(v);
            cvoroviNalepnica.add(v);
        }
    }

    // Crta nalepnice na gotovu sliku, isto kao na prikazu
    private static void nacrtajNalepnice(BufferedImage slika, List<Nalepnica> nalepnice) {
        int w = slika.getWidth();
        int h = slika.getHeight();
        for (Nalepnica n : nalepnice) {
            int sirina = Math.max(1, (int) Math.round(n.sirina() * w));
            int visina = Math.max(1, (int) Math.round(n.visina(w)));
            // Prvo se postepeno smanji (lepše od jednog velikog koraka), pa se nacrta tačno na meru
            BufferedImage izvor = smanji(uAwtSliku(n.slika()), Math.max(sirina, visina));
            Graphics2D g = slika.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) n.neprovidnost()));
            g.translate(n.x() * w, n.y() * h);
            g.rotate(Math.toRadians(n.rotacija()));
            g.drawImage(izvor, -sirina / 2, -visina / 2, sirina, visina, null);
            g.dispose();
        }
    }

    // ===== Više tekstova =====

    // Tekst iz polja se dodaje na sliku, a polje se prazni za sledeći tekst (sa istim podešavanjima)
    private void dodajNoviTekst() {
        potvrdiTekst();
        Dodaci aktivni = trenutniDodaci();
        if (aktivni.tekst().isBlank()) {
            return;
        }
        Slojevi s = slojevi();
        bezBelezenja = true;
        promeniSlojeve(s.saTekstovima(saDodatim(s.tekstovi(), aktivni)));
        tekstPolje.setText("");
        potvrdiTekst();
        bezBelezenja = false;
        zabeleziStanje();
        tekstPolje.requestFocus();
    }

    // Klik na ranije dodat tekst: on postaje tekst koji se uređuje, a dotadašnji se vraća među dodate
    private void izaberiDodatiTekst(int indeks) {
        potvrdiTekst();
        Slojevi s = slojevi();
        List<Dodaci> tekstovi = new ArrayList<>(s.tekstovi());
        Dodaci izabran = tekstovi.remove(indeks);
        Dodaci aktivni = trenutniDodaci();
        if (!aktivni.tekst().isBlank()) {
            tekstovi.add(aktivni);
        }
        bezBelezenja = true;
        promeniSlojeve(s.saTekstovima(tekstovi));
        tekstPolje.setText(izabran.tekst());
        potvrdiTekst();
        fontTeksta.setValue(Math.max(0, FONTOVI.indexOf(izabran.font())));
        podebljanTekst.setValue(izabran.podebljano() ? 1 : 0);
        velicinaTeksta.setValue(izabran.velicina());
        tekstX.setValue(izabran.x());
        tekstY.setValue(izabran.y());
        rotacijaTeksta.setValue(izabran.rotacija());
        bojaTeksta.setValue(izBoje(izabran.boja(), BELA));
        senkaTeksta.setValue(izabran.senka());
        bojaSenke.setValue(izBoje(izabran.bojaSenke(), CRNA));
        udaljenostSenke.setValue(izabran.udaljenostSenke());
        smerSenke.setValue(izabran.smerSenke());
        zamucenjeSenke.setValue(izabran.zamucenjeSenke());
        bezBelezenja = false;
        zabeleziStanje();
    }

    private void obrisiSveTekstove() {
        Slojevi s = slojevi();
        bezBelezenja = true;
        promeniSlojeve(s.saTekstovima(List.of()));
        tekstPolje.setText("");
        potvrdiTekst();
        bezBelezenja = false;
        zabeleziStanje();
    }

    // Redni broj ranije dodatog teksta ispod tačke miša (gornji ima prednost), ili -1
    private int dodatiTekstIspod(double x, double y) {
        for (int i = cvoroviDodatihTekstova.size() - 1; i >= 0; i--) {
            Text t = cvoroviDodatihTekstova.get(i);
            if (t.isVisible() && t.getLayoutBounds().contains(t.parentToLocal(x, y))) {
                return i;
            }
        }
        return -1;
    }

    // ===== Crvene oči =====

    // Klik na oko: pamti se središte i poluprečnik kao udeo cele slike (pre isecanja i rotacije),
    // pa ispravka ostaje na oku i kad se slika kasnije iseče ili okrene
    private void dodajCrvenoOko(double x, double y) {
        double sirina = prikaz.getLayoutBounds().getWidth();
        double visina = prikaz.getLayoutBounds().getHeight();
        if (sirina <= 0 || visina <= 0) {
            return;
        }
        Point2D tacka = prikaz.parentToLocal(x, y);
        double sirinaDela = 1 - isecanjeLevo.getValue() - isecanjeDesno.getValue();
        double visinaDela = 1 - isecanjeGore.getValue() - isecanjeDole.getValue();
        double ux = isecanjeLevo.getValue() + tacka.getX() / sirina * sirinaDela;
        double uy = isecanjeGore.getValue() + tacka.getY() / visina * visinaDela;
        // Poluprečnik je zadat u pikselima ekrana; deli se uvećanjem da bi bio u pikselima neuvećane slike
        double poluprecnik = velicinaOka.getValue() / uvecanje / sirina * sirinaDela;
        Slojevi s = slojevi();
        promeniSlojeve(s.saOcima(saDodatim(s.oci(), new CrvenoOko(ux, uy, poluprecnik))));
    }

    private void ponistiCrveneOci() {
        Slojevi s = slojevi();
        if (!s.oci().isEmpty()) {
            promeniSlojeve(s.saOcima(List.of()));
        }
    }

    // Unutar kruga se jako crveni pikseli prave sivim (crvena = prosek zelene i plave);
    // spoljnih 30% kruga se postepeno meša sa originalom, da se ne vidi oštra ivica
    private static void ukloniCrveneOci(int[] p, int w, int h, int x0, int y0, int ukupnoW, int ukupnoH,
                                        List<CrvenoOko> oci) {
        for (CrvenoOko oko : oci) {
            double cx = oko.x() * ukupnoW - x0;
            double cy = oko.y() * ukupnoH - y0;
            double r = Math.max(1, oko.poluprecnik() * ukupnoW);
            int odX = Math.max(0, (int) (cx - r));
            int doX = Math.min(w - 1, (int) (cx + r) + 1);
            int odY = Math.max(0, (int) (cy - r));
            int doY = Math.min(h - 1, (int) (cy + r) + 1);
            for (int y = odY; y <= doY; y++) {
                for (int x = odX; x <= doX; x++) {
                    double d = Math.hypot(x + 0.5 - cx, y + 0.5 - cy) / r;
                    if (d > 1) {
                        continue;
                    }
                    int c = p[y * w + x];
                    int cr = (c >> 16) & 0xFF;
                    int cg = (c >> 8) & 0xFF;
                    int cb = c & 0xFF;
                    double zelenoPlavo = (cg + cb) / 2.0;
                    if (cr > 50 && cr > 1.4 * zelenoPlavo) {
                        double tezina = d < 0.7 ? 1 : (1 - d) / 0.3;
                        int novaCrvena = (int) Math.round(cr + (zelenoPlavo - cr) * tezina);
                        p[y * w + x] = (c & 0xFF00FFFF) | novaCrvena << 16;
                    }
                }
            }
        }
    }

    // Crta tekst centriran oko tačke (cx, cy) i rotiran oko nje, kao na prikazu
    private static void nacrtajTekst(Graphics2D crtanje, Dodaci d, double velicinaFonta,
                                     double cx, double cy, java.awt.Color boja) {
        crtanje.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        crtanje.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        crtanje.setFont(new java.awt.Font(d.font(), d.podebljano() ? java.awt.Font.BOLD : java.awt.Font.PLAIN, 1)
                .deriveFont((float) velicinaFonta));
        FontMetrics mere = crtanje.getFontMetrics();
        crtanje.rotate(Math.toRadians(d.rotacija()), cx, cy);
        // Red teksta je centriran po visini oko tačke cy
        float x = (float) (cx - mere.stringWidth(d.tekst()) / 2.0);
        float y = (float) (cy + (mere.getAscent() - mere.getDescent()) / 2.0);
        crtanje.setColor(boja);
        crtanje.drawString(d.tekst(), x, y);
    }

    // Zamućenje jednog kanala (npr. providnosti) prosekom kvadrata poluprečnika r: po redovima, pa po kolonama
    private static int[] zamutiKanal(int[] kanal, int w, int h, int r) {
        int[] vodoravno = new int[kanal.length];
        for (int y = 0; y < h; y++) {
            zamutiLinijuKanala(kanal, vodoravno, y * w, 1, w, r);
        }
        int[] rezultat = new int[kanal.length];
        for (int x = 0; x < w; x++) {
            zamutiLinijuKanala(vodoravno, rezultat, x, w, h, r);
        }
        return rezultat;
    }

    private static void zamutiLinijuKanala(int[] izvor, int[] cilj, int pocetak, int korak, int duzina, int r) {
        int zbir = 0;
        for (int k = -r; k <= r; k++) {
            zbir += izvor[pocetak + Math.max(0, Math.min(duzina - 1, k)) * korak];
        }
        int sirinaProzora = 2 * r + 1;
        for (int i = 0; i < duzina; i++) {
            cilj[pocetak + i * korak] = zbir / sirinaProzora;
            zbir += izvor[pocetak + Math.min(duzina - 1, i + r + 1) * korak]
                    - izvor[pocetak + Math.max(0, i - r) * korak];
        }
    }

    private static java.awt.Color uAwtBoju(Color boja) {
        return new java.awt.Color((float) boja.getRed(), (float) boja.getGreen(), (float) boja.getBlue(),
                (float) boja.getOpacity());
    }

    private void ponistiUvecanje() {
        uvecanje = 1;
        pomakX = 0;
        pomakY = 0;
        primeniPolozaj();
    }

    // Ponovo primenjuje umetničke filtere na umanjenu kopiju slike koja se prikazuje
    private void osveziPregled() {
        if (pregled == null || prikazujeOriginal) {
            return;
        }
        double sirinaPrikaza = prikaz.getLayoutBounds().getWidth();
        double razmera = sirinaPrikaza > 0 ? sirinaIsecka(pregled) / sirinaPrikaza : 1;
        prikaz.setImage(obradiPiksele(pregled, razmera, slojevi().oci()));
    }

    // Širina slike u pikselima posle isecanja
    private double sirinaIsecka(Image slika) {
        return slika.getWidth() * Math.max(0, 1 - isecanjeLevo.getValue() - isecanjeDesno.getValue());
    }

    // Pretvara pravougaonik nacrtan mišem (u koordinatama prostora za prikaz) u isecanje.
    // parentToLocal uračunava i rotaciju i ogledalo, pa se dobijaju koordinate u neokrenutoj slici.
    private void iseciNaOkvir(double x1, double y1, double x2, double y2) {
        double sirina = prikaz.getLayoutBounds().getWidth();
        double visina = prikaz.getLayoutBounds().getHeight();
        if (sirina <= 0 || visina <= 0) {
            return;
        }
        Point2D a = prikaz.parentToLocal(x1, y1);
        Point2D b = prikaz.parentToLocal(x2, y2);
        // Udeo trenutno prikazanog dela slike, ograničen na samu sliku
        double odX = Math.max(0, Math.min(1, Math.min(a.getX(), b.getX()) / sirina));
        double doX = Math.max(0, Math.min(1, Math.max(a.getX(), b.getX()) / sirina));
        double odY = Math.max(0, Math.min(1, Math.min(a.getY(), b.getY()) / visina));
        double doY = Math.max(0, Math.min(1, Math.max(a.getY(), b.getY()) / visina));
        if (doX - odX < 0.01 || doY - odY < 0.01) {
            return;
        }
        // Isečena slika se prikazuje cela, bez zumiranja
        ponistiUvecanje();
        // Novi isečak je deo već isečenog dela, pa se preračunava u udeo cele slike
        double levo = isecanjeLevo.getValue();
        double gore = isecanjeGore.getValue();
        double sirinaDela = 1 - levo - isecanjeDesno.getValue();
        double visinaDela = 1 - gore - isecanjeDole.getValue();
        postaviVise(Map.of(
                isecanjeLevo, levo + odX * sirinaDela,
                isecanjeDesno, isecanjeDesno.getValue() + (1 - doX) * sirinaDela,
                isecanjeGore, gore + odY * visinaDela,
                isecanjeDole, isecanjeDole.getValue() + (1 - doY) * visinaDela));
    }

    // Menja više klizača odjednom, kao jedan korak u istoriji (jedan Undo)
    private void postaviVise(Map<Slider, Double> vrednosti) {
        bezBelezenja = true;
        vrednosti.forEach(Slider::setValue);
        bezBelezenja = false;
        zabeleziStanje();
    }

    // Gotov stil: svi klizači osim geometrije, isecanja, teksta i okvira se vraćaju na 0,
    // pa se postavljaju vrednosti stila
    private void primeniStil(Map<Slider, Double> stil) {
        bezBelezenja = true;
        for (Slider klizac : sviKlizaci) {
            if (!klizaciVanStila.contains(klizac)) {
                klizac.setValue(stil.getOrDefault(klizac, 0.0));
            }
        }
        bezBelezenja = false;
        zabeleziStanje();
    }

    // Obrada piksela, redom: isecanje, svetla i senke, pikselizacija, posterizacija,
    // uljana slika, akvarel, reljef, crtež olovkom.
    // razmera: koliko je puta slika veća od prikaza (da filter izgleda isto na prikazu i u sačuvanoj slici)
    // oci: ispravke crvenih očiju (pri obradi više slika prazno, jer važe samo za otvorenu sliku)
    private Image obradiPiksele(Image izvor, double razmera, List<CrvenoOko> oci) {
        if (klizaciPiksela.stream().allMatch(k -> k.getValue() == 0) && oci.isEmpty()) {
            return izvor;
        }
        // Isecanje: čita se samo izabrani deo slike
        int ukupnoW = (int) izvor.getWidth();
        int ukupnoH = (int) izvor.getHeight();
        int x0 = Math.min(ukupnoW - 1, (int) Math.round(isecanjeLevo.getValue() * ukupnoW));
        int y0 = Math.min(ukupnoH - 1, (int) Math.round(isecanjeGore.getValue() * ukupnoH));
        int w = Math.max(1, Math.min(ukupnoW - x0, (int) Math.round((1 - isecanjeDesno.getValue()) * ukupnoW) - x0));
        int h = Math.max(1, Math.min(ukupnoH - y0, (int) Math.round((1 - isecanjeDole.getValue()) * ukupnoH) - y0));
        int[] p = new int[w * h];
        izvor.getPixelReader().getPixels(x0, y0, w, h, PixelFormat.getIntArgbInstance(), p, 0, w);
        // Crvene oči se ispravljaju pre svih filtera, na originalnim bojama
        ukloniCrveneOci(p, w, h, x0, y0, ukupnoW, ukupnoH, oci);

        // Svetla i senke: menja se osvetljenost piksela, a boje se skaliraju istim faktorom.
        // Senke najviše posvetljavaju oko 1/3 osvetljenosti, svetla najviše tamne oko 2/3.
        if (senke.getValue() > 0 || svetla.getValue() > 0) {
            for (int i = 0; i < p.length; i++) {
                int c = p[i];
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                double l = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
                if (l <= 0) {
                    continue;
                }
                double cilj = l + senke.getValue() * 2 * l * (1 - l) * (1 - l)
                        - svetla.getValue() * 2 * l * l * (1 - l);
                double faktor = cilj / l;
                p[i] = (c & 0xFF000000)
                        | (int) Math.min(255, Math.round(r * faktor)) << 16
                        | (int) Math.min(255, Math.round(g * faktor)) << 8
                        | (int) Math.min(255, Math.round(b * faktor));
            }
        }

        // Pikselizacija: svaki kvadrat dobija prosečnu boju svojih piksela
        int blok = (int) Math.round(pikselizacija.getValue() * razmera);
        if (blok > 1) {
            for (int by = 0; by < h; by += blok) {
                for (int bx = 0; bx < w; bx += blok) {
                    int krajX = Math.min(bx + blok, w);
                    int krajY = Math.min(by + blok, h);
                    long a = 0, r = 0, g = 0, b = 0;
                    for (int y = by; y < krajY; y++) {
                        for (int x = bx; x < krajX; x++) {
                            int c = p[y * w + x];
                            a += c >>> 24;
                            r += (c >> 16) & 0xFF;
                            g += (c >> 8) & 0xFF;
                            b += c & 0xFF;
                        }
                    }
                    int n = (krajX - bx) * (krajY - by);
                    int prosek = (int) (a / n) << 24 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
                    for (int y = by; y < krajY; y++) {
                        Arrays.fill(p, y * w + bx, y * w + krajX, prosek);
                    }
                }
            }
        }

        // Posterizacija: manje nijansi po kanalu (od 16 za slab filter do 2 za najjači)
        if (posterizacija.getValue() > 0) {
            int nivoi = (int) Math.round(2 + (1 - posterizacija.getValue()) * 14);
            for (int i = 0; i < p.length; i++) {
                int c = p[i];
                p[i] = (c & 0xFF000000)
                        | smanjiNijanse((c >> 16) & 0xFF, nivoi) << 16
                        | smanjiNijanse((c >> 8) & 0xFF, nivoi) << 8
                        | smanjiNijanse(c & 0xFF, nivoi);
            }
        }

        // Uljana slika: jači klizač = veći potezi četkice (do 5 piksela prikaza)
        int poluprecnik = (int) Math.round(uljanaSlika.getValue() * 5 * razmera);
        if (poluprecnik > 0) {
            p = uljanaSlika(p, w, h, poluprecnik);
        }
        if (akvarel.getValue() > 0) {
            p = akvarel(p, w, h, razmera, akvarel.getValue());
        }

        if (reljef.getValue() > 0 || crtez.getValue() > 0) {
            // Oba filtera rade sa sivom verzijom slike; korak d je razmak suseda u pikselima slike
            int[] siva = new int[w * h];
            for (int i = 0; i < p.length; i++) {
                int c = p[i];
                siva[i] = (299 * ((c >> 16) & 0xFF) + 587 * ((c >> 8) & 0xFF) + 114 * (c & 0xFF)) / 1000;
            }
            int d = Math.max(1, (int) Math.round(razmera));
            for (int y = 0; y < h; y++) {
                int gore = Math.max(0, y - d) * w;
                int sred = y * w;
                int dole = Math.min(h - 1, y + d) * w;
                for (int x = 0; x < w; x++) {
                    int levo = Math.max(0, x - d);
                    int desno = Math.min(w - 1, x + d);
                    int c = p[sred + x];
                    if (reljef.getValue() > 0) {
                        // Reljef: razlika osvetljenosti po dijagonali, oko srednje sive
                        int e = Math.max(0, Math.min(255, 128 + 2 * (siva[dole + desno] - siva[gore + levo])));
                        c = pomesaj(c, e, reljef.getValue());
                    }
                    if (crtez.getValue() > 0) {
                        // Crtež: jačina ivica (Sobel), tamne linije na belom papiru
                        int gx = siva[gore + desno] + 2 * siva[sred + desno] + siva[dole + desno]
                                - siva[gore + levo] - 2 * siva[sred + levo] - siva[dole + levo];
                        int gy = siva[dole + levo] + 2 * siva[dole + x] + siva[dole + desno]
                                - siva[gore + levo] - 2 * siva[gore + x] - siva[gore + desno];
                        int ivica = (int) Math.min(255, Math.sqrt(gx * gx + gy * gy) / 2);
                        c = pomesaj(c, 255 - ivica, crtez.getValue());
                    }
                    p[sred + x] = c;
                }
            }
        }

        WritableImage rezultat = new WritableImage(w, h);
        rezultat.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), p, 0, w);
        return rezultat;
    }

    // Uljana slika: u okolini svakog piksela traži se najčešća osvetljenost (od 20 nivoa),
    // a piksel dobija prosečnu boju piksela te osvetljenosti. Okolina se pomera kolonu po kolonu,
    // da se ne bi za svaki piksel ponovo brojali svi susedi.
    private static int[] uljanaSlika(int[] p, int w, int h, int r) {
        final int brojNivoa = 20;
        int[] nivo = new int[p.length];
        for (int i = 0; i < p.length; i++) {
            int c = p[i];
            int siva = (299 * ((c >> 16) & 0xFF) + 587 * ((c >> 8) & 0xFF) + 114 * (c & 0xFF)) / 1000;
            nivo[i] = siva * brojNivoa / 256;
        }
        int[] rezultat = new int[p.length];
        int[] broj = new int[brojNivoa];
        int[] zbirR = new int[brojNivoa];
        int[] zbirG = new int[brojNivoa];
        int[] zbirB = new int[brojNivoa];
        for (int y = 0; y < h; y++) {
            int y0 = Math.max(0, y - r);
            int y1 = Math.min(h - 1, y + r);
            Arrays.fill(broj, 0);
            Arrays.fill(zbirR, 0);
            Arrays.fill(zbirG, 0);
            Arrays.fill(zbirB, 0);
            for (int x = 0; x <= Math.min(w - 1, r); x++) {
                promeniKolonu(p, nivo, w, x, y0, y1, 1, broj, zbirR, zbirG, zbirB);
            }
            for (int x = 0; x < w; x++) {
                int najcesci = 0;
                for (int n = 1; n < brojNivoa; n++) {
                    if (broj[n] > broj[najcesci]) {
                        najcesci = n;
                    }
                }
                int k = broj[najcesci];
                rezultat[y * w + x] = (p[y * w + x] & 0xFF000000)
                        | (zbirR[najcesci] / k) << 16 | (zbirG[najcesci] / k) << 8 | zbirB[najcesci] / k;
                if (x - r >= 0) {
                    promeniKolonu(p, nivo, w, x - r, y0, y1, -1, broj, zbirR, zbirG, zbirB);
                }
                if (x + r + 1 < w) {
                    promeniKolonu(p, nivo, w, x + r + 1, y0, y1, 1, broj, zbirR, zbirG, zbirB);
                }
            }
        }
        return rezultat;
    }

    // Dodaje (znak 1) ili oduzima (znak -1) jednu kolonu piksela iz okoline uljane slike
    private static void promeniKolonu(int[] p, int[] nivo, int w, int x, int y0, int y1, int znak,
                                      int[] broj, int[] zbirR, int[] zbirG, int[] zbirB) {
        for (int y = y0; y <= y1; y++) {
            int i = y * w + x;
            int n = nivo[i];
            int c = p[i];
            broj[n] += znak;
            zbirR[n] += znak * ((c >> 16) & 0xFF);
            zbirG[n] += znak * ((c >> 8) & 0xFF);
            zbirB[n] += znak * (c & 0xFF);
        }
    }

    // Akvarel: razlivene boje (dvostruko zamućenje), manje nijansi, svetliji tonovi,
    // tamnije ivice mrlja i tekstura papira; udeo određuje mešanje sa slikom
    private static int[] akvarel(int[] p, int w, int h, double razmera, double udeo) {
        int r = Math.max(1, (int) Math.round(3 * razmera));
        int[] mutno = zamuti(zamuti(p, w, h, r), w, h, r);
        int[] siva = new int[p.length];
        for (int i = 0; i < p.length; i++) {
            int c = mutno[i];
            siva[i] = (299 * ((c >> 16) & 0xFF) + 587 * ((c >> 8) & 0xFF) + 114 * (c & 0xFF)) / 1000;
        }
        int d = Math.max(1, (int) Math.round(razmera));
        int[] rezultat = new int[p.length];
        for (int y = 0; y < h; y++) {
            int gore = Math.max(0, y - d) * w;
            int dole = Math.min(h - 1, y + d) * w;
            for (int x = 0; x < w; x++) {
                int levo = Math.max(0, x - d);
                int desno = Math.min(w - 1, x + d);
                int gx = siva[y * w + desno] - siva[y * w + levo];
                int gy = siva[dole + x] - siva[gore + x];
                // Ivice mrlja su tamnije, a papir ima blago zrnastu teksturu
                double ivica = Math.min(0.4, Math.sqrt(gx * gx + gy * gy) / 255.0);
                double papir = 0.94 + 0.06 * (hes(x / d, y / d) & 0xFF) / 255.0;
                double faktor = (1 - ivica) * papir;

                int c = mutno[y * w + x];
                int original = p[y * w + x];
                int[] kanali = {(c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF};
                int[] izvorni = {(original >> 16) & 0xFF, (original >> 8) & 0xFF, original & 0xFF};
                int boja = original & 0xFF000000;
                for (int k = 0; k < 3; k++) {
                    double v = smanjiNijanse(kanali[k], 8);
                    v = (v + (255 - v) * 0.15) * faktor;
                    v = izvorni[k] * (1 - udeo) + v * udeo;
                    boja |= (int) Math.round(Math.max(0, Math.min(255, v))) << (16 - 8 * k);
                }
                rezultat[y * w + x] = boja;
            }
        }
        return rezultat;
    }

    // Brzo zamućenje (prosek kvadrata poluprečnika r): prvo po redovima, pa po kolonama
    private static int[] zamuti(int[] p, int w, int h, int r) {
        int[] vodoravno = new int[p.length];
        for (int y = 0; y < h; y++) {
            zamutiLiniju(p, vodoravno, y * w, 1, w, r);
        }
        int[] rezultat = new int[p.length];
        for (int x = 0; x < w; x++) {
            zamutiLiniju(vodoravno, rezultat, x, w, h, r);
        }
        return rezultat;
    }

    // Zamućuje jednu liniju piksela: pocetak je indeks prvog piksela, korak razmak između susednih
    private static void zamutiLiniju(int[] izvor, int[] cilj, int pocetak, int korak, int duzina, int r) {
        int zbirR = 0, zbirG = 0, zbirB = 0;
        for (int k = -r; k <= r; k++) {
            int c = izvor[pocetak + Math.max(0, Math.min(duzina - 1, k)) * korak];
            zbirR += (c >> 16) & 0xFF;
            zbirG += (c >> 8) & 0xFF;
            zbirB += c & 0xFF;
        }
        int sirinaProzora = 2 * r + 1;
        for (int i = 0; i < duzina; i++) {
            int indeks = pocetak + i * korak;
            cilj[indeks] = (izvor[indeks] & 0xFF000000)
                    | (zbirR / sirinaProzora) << 16 | (zbirG / sirinaProzora) << 8 | zbirB / sirinaProzora;
            int izlazi = izvor[pocetak + Math.max(0, i - r) * korak];
            int ulazi = izvor[pocetak + Math.min(duzina - 1, i + r + 1) * korak];
            zbirR += ((ulazi >> 16) & 0xFF) - ((izlazi >> 16) & 0xFF);
            zbirG += ((ulazi >> 8) & 0xFF) - ((izlazi >> 8) & 0xFF);
            zbirB += (ulazi & 0xFF) - (izlazi & 0xFF);
        }
    }

    private static int smanjiNijanse(int vrednost, int nivoi) {
        return (int) Math.round(Math.round(vrednost * (nivoi - 1) / 255.0) * 255.0 / (nivoi - 1));
    }

    // Meša boju piksela sa sivom nijansom; udeo 1 znači samo siva
    private static int pomesaj(int boja, int siva, double udeo) {
        int r = (int) Math.round(((boja >> 16) & 0xFF) * (1 - udeo) + siva * udeo);
        int g = (int) Math.round(((boja >> 8) & 0xFF) * (1 - udeo) + siva * udeo);
        int b = (int) Math.round((boja & 0xFF) * (1 - udeo) + siva * udeo);
        return (boja & 0xFF000000) | r << 16 | g << 8 | b;
    }

    private static Button napraviDugme(String natpis, Runnable akcija) {
        Button dugme = new Button(natpis);
        dugme.setMaxWidth(Double.MAX_VALUE);
        dugme.setOnAction(e -> akcija.run());
        return dugme;
    }

    // Prečice: Ctrl+Z za Undo, Ctrl+Y ili Ctrl+Shift+Z za Redo (rade u oba prozora)
    private void dodajPrecice(Scene scena) {
        scena.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), undo::fire);
        scena.getAccelerators().put(new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN), redo::fire);
        scena.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), redo::fire);
    }

    private static void dodajPrecicu(Scene scena, KeyCode taster, Runnable radnja) {
        scena.getAccelerators().put(new KeyCodeCombination(taster, KeyCombination.SHORTCUT_DOWN), radnja);
    }

    private static Tab napraviKarticu(String naziv, Node... sadrzaj) {
        VBox okvir = new VBox(10, sadrzaj);
        okvir.setPadding(new Insets(15));
        // Ako sadržaj ne stane u prozor, kartica dobija traku za pomeranje
        ScrollPane klizanje = new ScrollPane(okvir);
        klizanje.setFitToWidth(true);
        Tab kartica = new Tab(naziv, klizanje);
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
            ucitajSliku(prozor, fajl);
        }
    }

    // Zajedničko za dijalog, prevlačenje fajla i nedavne slike
    private void ucitajSliku(Stage prozor, File fajl) {
        if (!fajl.isFile()) {
            prikaziGresku(prozor, "Fajl više ne postoji:\n" + fajl.getAbsolutePath());
            ukloniIzNedavnih(fajl);
            return;
        }
        zapamtiFolder(fajl);
        if (!smeDaOdbaciIzmene(prozor, "Otvori sliku", "Otvoriti novu sliku bez čuvanja?")) {
            return;
        }
        String adresa = fajl.toURI().toString();
        Image ucitana = new Image(adresa);
        if (ucitana.isError()) {
            prikaziGresku(prozor, "Slika ne može da se otvori:\n" + fajl.getAbsolutePath());
            return;
        }
        // Nova slika počinje od nule: bez izmena i bez istorije
        zatvoriSliku();
        original = ucitana;
        pregled = original.getWidth() > NAJVECI_PREGLED || original.getHeight() > NAJVECI_PREGLED
                ? new Image(adresa, NAJVECI_PREGLED, NAJVECI_PREGLED, true, true)
                : original;
        prikaz.setImage(pregled);
        dodajUNedavne(fajl);
    }

    private static void prikaziGresku(Stage prozor, String tekst) {
        Alert greska = new Alert(Alert.AlertType.ERROR, tekst);
        greska.setHeaderText(null);
        greska.initOwner(prozor);
        greska.showAndWait();
    }

    // Da li fajl ima ekstenziju slike koju program ume da otvori
    private static boolean jeSlika(File fajl) {
        String ime = fajl.getName().toLowerCase();
        return PODRZANE_EKSTENZIJE.stream().anyMatch(ime::endsWith);
    }

    // Nedavne slike se čuvaju kao jedan tekst, putanje odvojene novim redom, najnovija prva
    private List<File> nedavneSlike() {
        List<File> spisak = new ArrayList<>();
        for (String putanja : podesavanjaPrograma.get(KLJUC_NEDAVNE, "").split("\n")) {
            if (!putanja.isBlank()) {
                spisak.add(new File(putanja));
            }
        }
        return spisak;
    }

    private void zapamtiNedavne(List<File> spisak) {
        StringBuilder zapis = new StringBuilder();
        for (File fajl : spisak.subList(0, Math.min(NAJVISE_NEDAVNIH, spisak.size()))) {
            zapis.append(fajl.getAbsolutePath()).append('\n');
        }
        podesavanjaPrograma.put(KLJUC_NEDAVNE, zapis.toString());
        osveziNedavne();
    }

    private void dodajUNedavne(File fajl) {
        List<File> spisak = nedavneSlike();
        spisak.removeIf(f -> f.getAbsolutePath().equalsIgnoreCase(fajl.getAbsolutePath()));
        spisak.add(0, fajl);
        zapamtiNedavne(spisak);
    }

    private void ukloniIzNedavnih(File fajl) {
        List<File> spisak = nedavneSlike();
        spisak.removeIf(f -> f.getAbsolutePath().equalsIgnoreCase(fajl.getAbsolutePath()));
        zapamtiNedavne(spisak);
    }

    // Ponovo pravi stavke menija "Nedavne slike": ime fajla i, u zagradi, folder u kome je
    private void osveziNedavne() {
        nedavneDugme.getItems().clear();
        List<File> spisak = nedavneSlike();
        for (File fajl : spisak) {
            File folder = fajl.getParentFile();
            MenuItem stavka = new MenuItem(fajl.getName() + (folder != null ? "   (" + folder.getAbsolutePath() + ")" : ""));
            stavka.setMnemonicParsing(false);
            stavka.setOnAction(e -> ucitajSliku(glavniProzor, fajl));
            nedavneDugme.getItems().add(stavka);
        }
        if (spisak.isEmpty()) {
            MenuItem prazno = new MenuItem("Nema nedavnih slika");
            prazno.setDisable(true);
            nedavneDugme.getItems().add(prazno);
        } else {
            MenuItem obrisi = new MenuItem("Obriši spisak");
            obrisi.setOnAction(e -> zapamtiNedavne(List.of()));
            nedavneDugme.getItems().addAll(new SeparatorMenuItem(), obrisi);
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
        // Tekst koji se još kuca takođe je izmena
        potvrdiTekst();
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
        uvecanje = 1;
        pomakX = 0;
        pomakY = 0;
        original = null;
        pregled = null;
        prikaz.setImage(null);
        // Istorija se briše, pa ni stare verzije crteža, tekstova i crvenih očiju više ne trebaju
        verzijeSlojeva.clear();
        verzijeSlojeva.add(PRAZNI_SLOJEVI);
        postaviStanje(new double[sviKlizaci.size()]);
        istorija.clear();
        buducnost.clear();
        sacuvanoStanje = trenutnoStanje();
        osveziDugmad();
    }

    private void sacuvajSliku(Stage prozor) {
        potvrdiTekst();
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
        double sirinaPrikaza = prikaz.getLayoutBounds().getWidth();
        double razmera = sirinaPrikaza > 0 ? sirinaIsecka(original) / sirinaPrikaza : 1;
        // Vrednosti potrebne u pozadini čitamo odmah, na glavnoj niti JavaFX-a
        Image izvor = original;
        File odrediste = fajl;
        double[] stanjePriCuvanju = trenutnoStanje();
        PodesavanjaCuvanja podesavanja = trenutnaPodesavanjaCuvanja(true);
        Stage poruka = prikaziPoruku(prozor, "Čuvanje", new Label("Čuvanje u toku…"));

        // Obrada piksela i upis u fajl traju dugo, pa se rade u pozadini, a program ne "zamrzne".
        // Prozor sa porukom je modalan, pa se za to vreme ne mogu menjati klizači koje obradiPiksele čita.
        Thread pozadina = new Thread(() -> {
            try {
                obradiIUpisi(izvor, razmera, jeJpg, podesavanja, odrediste);
                Platform.runLater(() -> {
                    poruka.close();
                    sacuvanoStanje = stanjePriCuvanju;
                });
            } catch (Exception | OutOfMemoryError ex) {
                Platform.runLater(() -> {
                    poruka.close();
                    prikaziGresku(prozor, "Čuvanje nije uspelo: " + opisGreske(ex));
                });
            }
        }, "cuvanje-slike");
        pozadina.setDaemon(true);
        pozadina.start();
    }

    private static String opisGreske(Throwable ex) {
        Throwable uzrok = ex instanceof ExecutionException && ex.getCause() != null ? ex.getCause() : ex;
        return uzrok.getMessage() != null ? uzrok.getMessage() : uzrok.toString();
    }

    // Sve što se pri čuvanju primenjuje posle obrade piksela i efekata; čita se na glavnoj niti
    // oci: ispravke crvenih očiju koje se primenjuju (pri obradi više slika prazno)
    private record PodesavanjaCuvanja(double alfa, int kvartovi, boolean levoDesno, boolean goreDole,
                                      Dodaci dodaci, Slojevi slojevi, List<CrvenoOko> oci,
                                      int najvecaStrana, float kvalitet) {
    }

    private PodesavanjaCuvanja trenutnaPodesavanjaCuvanja(boolean saCrvenimOcima) {
        return new PodesavanjaCuvanja(1 - providnost.getValue(), (int) rotacija.getValue(),
                ogledaloLevoDesno.getValue() == 1, ogledaloGoreDole.getValue() == 1, trenutniDodaci(),
                slojevi(), saCrvenimOcima ? slojevi().oci() : List.of(),
                najvecaStrana.getValue() == null ? 0 : najvecaStrana.getValue(), (float) kvalitetJpg.getValue());
    }

    // Primenjuje sve izmene na sliku u punoj rezoluciji i upisuje je u fajl. Poziva se iz pozadinske niti.
    // razmera: koliko je puta slika veća od prikaza (filteri se zadaju u pikselima prikaza)
    private void obradiIUpisi(Image izvor, double razmera, boolean jeJpg, PodesavanjaCuvanja p, File odrediste)
            throws Exception {
        Image obradjena = obradiPiksele(izvor, razmera, p.oci());
        // Efekti i snimak se rade na glavnoj niti JavaFX-a; pozadina čeka rezultat
        FutureTask<WritableImage> snimanje = new FutureTask<>(() -> {
            ImageView puna = new ImageView(obradjena);
            puna.setEffect(napraviEfekat(obradjena.getWidth(), obradjena.getHeight(), razmera));
            SnapshotParameters parametri = new SnapshotParameters();
            // JPG ne podržava providnost, pa providne delove popunjavamo belom bojom
            parametri.setFill(jeJpg ? Color.WHITE : Color.TRANSPARENT);
            // Zamućenje širi sliku preko ivica; snimamo samo deo veličine originala
            parametri.setViewport(new Rectangle2D(0, 0, obradjena.getWidth(), obradjena.getHeight()));
            return puna.snapshot(parametri, null);
        });
        Platform.runLater(snimanje);
        WritableImage snimak = snimanje.get();

        BufferedImage zaUpis = smanji(nacrtajDodatke(
                okreni(uBufferedImage(snimak, jeJpg, p.alfa()), p.kvartovi(), p.levoDesno(), p.goreDole()),
                p.dodaci(), p.slojevi()), p.najvecaStrana());
        if (jeJpg) {
            upisiJpg(zaUpis, odrediste, p.kvalitet());
        } else {
            ImageIO.write(zaUpis, "png", odrediste);
        }
    }

    // Obrada više slika odjednom: na svaku se primenjuju sva trenutna podešavanja (filteri, stil,
    // isecanje, rotacija, tekst, okvir, veličina) i čuva se u izabrani folder pod istim imenom
    private void obradiViseSlika(Stage prozor) {
        potvrdiTekst();
        FileChooser izbor = new FileChooser();
        izbor.setTitle("Izaberi slike za obradu");
        izbor.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Slike", "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.gif"));
        postaviPocetniFolder(izbor);
        List<File> fajlovi = izbor.showOpenMultipleDialog(prozor);
        if (fajlovi == null || fajlovi.isEmpty()) {
            return;
        }
        DirectoryChooser izborFoldera = new DirectoryChooser();
        izborFoldera.setTitle("Izaberi folder za obrađene slike");
        izborFoldera.setInitialDirectory(fajlovi.get(0).getParentFile());
        File folder = izborFoldera.showDialog(prozor);
        if (folder == null) {
            return;
        }

        // Filteri se zadaju u pikselima prikaza; za slike koje nisu otvorene uzima se trenutna širina prikaza
        double sirinaPrikaza = prikaz.getLayoutBounds().getWidth() > 0 ? prikaz.getLayoutBounds().getWidth()
                : Math.max(400, prikaz.getFitWidth());
        // Crvene oči su vezane za otvorenu sliku, pa se na druge slike ne primenjuju
        PodesavanjaCuvanja podesavanja = trenutnaPodesavanjaCuvanja(false);
        Label tekst = new Label("Obrađujem sliku 1 od " + fajlovi.size() + "…");
        Stage poruka = prikaziPoruku(prozor, "Obrada više slika", tekst);

        Thread pozadina = new Thread(() -> {
            List<String> greske = new ArrayList<>();
            for (int i = 0; i < fajlovi.size(); i++) {
                File fajl = fajlovi.get(i);
                int redniBroj = i + 1;
                Platform.runLater(() -> tekst.setText(
                        "Obrađujem sliku " + redniBroj + " od " + fajlovi.size() + "…\n" + fajl.getName()));
                try {
                    Image izvor = new Image(fajl.toURI().toString());
                    if (izvor.isError()) {
                        throw new IOException("slika ne može da se otvori");
                    }
                    String ime = fajl.getName();
                    boolean jeJpg = ime.toLowerCase().endsWith(".jpg") || ime.toLowerCase().endsWith(".jpeg");
                    // BMP i GIF se čuvaju kao PNG; original se nikad ne prepisuje
                    String osnova = ime.contains(".") ? ime.substring(0, ime.lastIndexOf('.')) : ime;
                    String ekstenzija = jeJpg ? ime.substring(ime.lastIndexOf('.')) : ".png";
                    File odrediste = new File(folder, osnova + ekstenzija);
                    if (odrediste.getAbsoluteFile().equals(fajl.getAbsoluteFile())) {
                        odrediste = new File(folder, osnova + "_obradjeno" + ekstenzija);
                    }
                    obradiIUpisi(izvor, sirinaIsecka(izvor) / sirinaPrikaza, jeJpg, podesavanja, odrediste);
                } catch (Exception | OutOfMemoryError ex) {
                    greske.add(fajl.getName() + ": " + opisGreske(ex));
                }
            }
            Platform.runLater(() -> {
                poruka.close();
                int uspesno = fajlovi.size() - greske.size();
                Alert kraj = new Alert(greske.isEmpty() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING,
                        "Obrađeno " + uspesno + " od " + fajlovi.size() + " slika u folder:\n"
                                + folder.getAbsolutePath()
                                + (greske.isEmpty() ? "" : "\n\nNije uspelo:\n" + String.join("\n", greske)));
                kraj.setTitle("Obrada više slika");
                kraj.setHeaderText(null);
                kraj.initOwner(prozor);
                kraj.showAndWait();
            });
        }, "obrada-vise-slika");
        pozadina.setDaemon(true);
        pozadina.start();
    }

    // Mali prozor sa porukom (npr. "Čuvanje u toku…") koji se ne može zatvoriti i blokira ostale
    // prozore dok traje posao u pozadini
    private static Stage prikaziPoruku(Stage vlasnik, String naslov, Label tekst) {
        ProgressIndicator krug = new ProgressIndicator();
        krug.setPrefSize(40, 40);
        HBox sadrzaj = new HBox(15, krug, tekst);
        sadrzaj.setAlignment(Pos.CENTER_LEFT);
        sadrzaj.setPadding(new Insets(20));
        sadrzaj.setMinWidth(320);
        Stage poruka = new Stage(StageStyle.UTILITY);
        poruka.initOwner(vlasnik);
        poruka.initModality(Modality.APPLICATION_MODAL);
        poruka.setTitle(naslov);
        poruka.setResizable(false);
        poruka.setOnCloseRequest(e -> e.consume());
        poruka.setScene(new Scene(sadrzaj));
        poruka.show();
        return poruka;
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

    // Primenjuje ogledalo, pa rotaciju za kvartovi * 90° udesno
    // Smanjuje sliku tako da duža strana bude najviše najvecaStrana piksela (0 = bez promene; slika se ne uvećava).
    // Smanjuje se postepeno, najviše dvostruko u jednom koraku, jer jedan veliki korak daje nazubljenu sliku.
    private static BufferedImage smanji(BufferedImage slika, int najvecaStrana) {
        int w = slika.getWidth();
        int h = slika.getHeight();
        if (najvecaStrana <= 0 || Math.max(w, h) <= najvecaStrana) {
            return slika;
        }
        double faktor = (double) najvecaStrana / Math.max(w, h);
        int ciljW = Math.max(1, (int) Math.round(w * faktor));
        int ciljH = Math.max(1, (int) Math.round(h * faktor));
        BufferedImage trenutna = slika;
        while (w != ciljW || h != ciljH) {
            w = Math.max(ciljW, w / 2);
            h = Math.max(ciljH, h / 2);
            BufferedImage manja = new BufferedImage(w, h, slika.getType());
            Graphics2D crtanje = manja.createGraphics();
            crtanje.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            crtanje.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            crtanje.drawImage(trenutna, 0, 0, w, h, null);
            crtanje.dispose();
            trenutna = manja;
        }
        return trenutna;
    }

    // Upisuje JPG sa zadatim kvalitetom (0.5 - 1); obično ImageIO.write uvek koristi 75%
    private static void upisiJpg(BufferedImage slika, File fajl, float kvalitet) throws IOException {
        ImageWriter pisac = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam parametri = pisac.getDefaultWriteParam();
        parametri.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parametri.setCompressionQuality(kvalitet);
        // Postojeći fajl se prvo briše, inače bi duži stari sadržaj ostao na kraju novog fajla
        Files.deleteIfExists(fajl.toPath());
        try (ImageOutputStream izlaz = ImageIO.createImageOutputStream(fajl)) {
            if (izlaz == null) {
                throw new IOException("Fajl ne može da se napravi: " + fajl.getAbsolutePath());
            }
            pisac.setOutput(izlaz);
            pisac.write(null, new IIOImage(slika, null, null), parametri);
        } finally {
            pisac.dispose();
        }
    }

    private static BufferedImage okreni(BufferedImage slika, int kvartovi, boolean levoDesno, boolean goreDole) {
        if (kvartovi == 0 && !levoDesno && !goreDole) {
            return slika;
        }
        int w = slika.getWidth();
        int h = slika.getHeight();
        boolean uspravno = kvartovi % 2 == 1;
        BufferedImage rezultat = new BufferedImage(uspravno ? h : w, uspravno ? w : h, slika.getType());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int ox = levoDesno ? w - 1 - x : x;
                int oy = goreDole ? h - 1 - y : y;
                int nx;
                int ny;
                switch (kvartovi) {
                    case 1 -> { nx = h - 1 - oy; ny = ox; }
                    case 2 -> { nx = w - 1 - ox; ny = h - 1 - oy; }
                    case 3 -> { nx = oy; ny = w - 1 - ox; }
                    default -> { nx = ox; ny = oy; }
                }
                rezultat.setRGB(nx, ny, slika.getRGB(x, y));
            }
        }
        return rezultat;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
