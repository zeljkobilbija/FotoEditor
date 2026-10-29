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

    // Umetnički filteri menjaju same piksele slike (pre ostalih efekata), 0 znači isključeno.
    // pikselizacija: veličina kvadrata u pikselima prikaza
    private final Slider pikselizacija = new Slider(0, 30, 0);
    private final Slider posterizacija = new Slider(0, 1, 0);
    private final Slider reljef = new Slider(0, 1, 0);
    private final Slider crtez = new Slider(0, 1, 0);
    private final Slider uljanaSlika = new Slider(0, 1, 0);
    private final Slider akvarel = new Slider(0, 1, 0);
    private final List<Slider> umetnickiKlizaci =
            List.of(pikselizacija, posterizacija, reljef, crtez, uljanaSlika, akvarel);

    // Svi klizači čije se vrednosti pamte u istoriji (novi klizač treba dodati ovde)
    private final List<Slider> sviKlizaci = List.of(
            svetlina, kontrast, zasicenost, nijansa,
            crvena, zelena, plava, providnost, temperatura, negativ,
            ostrina, vinjeta, zrno,
            sepija, sjaj, bloom, zamucenje, zamucenjePokretom, ugaoPokreta,
            rotacija, ogledaloLevoDesno, ogledaloGoreDole,
            pikselizacija, posterizacija, reljef, crtez, uljanaSlika, akvarel);

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
        // Dugme za otvaranje slike
        Button otvori = new Button("Otvori sliku");
        otvori.setMaxWidth(Double.MAX_VALUE);
        otvori.setOnAction(e -> otvoriSliku(prozor));

        // Svaka promena klizača osvežava prikaz i beleži se za Undo/Redo
        for (Slider klizac : sviKlizaci) {
            klizac.valueProperty().addListener((o, s, n) -> osveziEfekat());
            pratiPromene(klizac);
        }
        for (Slider klizac : umetnickiKlizaci) {
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

        // Poseban prozor sa karticama za sve komande, efekte i filtere.
        // Nova grupa komandi se dodaje kao nova kartica preko napraviKarticu(...)
        TabPane kartice = new TabPane(
                napraviKarticu("Svetlo",
                        new Label("Svetlina"), svetlina,
                        new Label("Kontrast"), kontrast),
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
                        new Label("Providnost (Alfa)"), providnost),
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
                        napraviDugme("Gore - dole", () -> ogledaloGoreDole.setValue(1 - ogledaloGoreDole.getValue()))),
                napraviKarticu("Umetnički",
                        new Label("Pikselizacija"), pikselizacija,
                        new Label("Posterizacija"), posterizacija,
                        new Label("Reljef"), reljef,
                        new Label("Crtež olovkom"), crtez,
                        new Separator(),
                        new Label("Uljana slika"), uljanaSlika,
                        new Label("Akvarel"), akvarel));

        Stage komande = new Stage();
        komande.initOwner(prozor);
        komande.setTitle("Komande");
        Scene scenaKomandi = new Scene(kartice, 300, 700);
        dodajPrecice(scenaKomandi);
        komande.setScene(scenaKomandi);

        // Dugme koje otvara i zatvara prozor sa komandama (nije vidljiv pri pokretanju)
        Button prikaziKomande = new Button("Otvori Komande");
        prikaziKomande.setMaxWidth(Double.MAX_VALUE);
        // Natpis prati stanje prozora, i kad se zatvori dugmetom i kad se zatvori sa X
        komande.showingProperty().addListener((o, s, otvoren) ->
                prikaziKomande.setText(otvoren ? "Zatvori Komande" : "Otvori Komande"));
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
        // Isto važi i za umetničke filtere, čija se jačina zadaje u pikselima prikaza
        prikaz.layoutBoundsProperty().addListener((o, s, n) -> {
            osveziEfekat();
            osveziPregled();
        });
        osveziEfekat();
        // Kad je slika okrenuta za 90° ili 270°, širina i visina prostora za prikaz menjaju uloge
        Runnable osveziVelicinu = () -> {
            boolean uspravno = rotacija.getValue() % 2 == 1;
            double sirina = centar.getWidth() - 20;
            double visina = centar.getHeight() - 20;
            prikaz.setFitWidth(uspravno ? visina : sirina);
            prikaz.setFitHeight(uspravno ? sirina : visina);
        };
        centar.widthProperty().addListener((o, s, n) -> osveziVelicinu.run());
        centar.heightProperty().addListener((o, s, n) -> osveziVelicinu.run());
        rotacija.valueProperty().addListener((o, s, n) -> osveziVelicinu.run());

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
        prikaz.setEffect(napraviEfekat(prikaz.getLayoutBounds().getWidth(), prikaz.getLayoutBounds().getHeight(), 1));
        prikaz.setOpacity(1 - providnost.getValue());
        // Prvo se primenjuje ogledalo, pa rotacija (isto kao pri čuvanju u okreni(...))
        prikaz.setRotate(90 * rotacija.getValue());
        prikaz.setScaleX(ogledaloLevoDesno.getValue() == 1 ? -1 : 1);
        prikaz.setScaleY(ogledaloGoreDole.getValue() == 1 ? -1 : 1);
    }

    // Ponovo primenjuje umetničke filtere na umanjenu kopiju slike koja se prikazuje
    private void osveziPregled() {
        if (pregled == null) {
            return;
        }
        double sirinaPrikaza = prikaz.getLayoutBounds().getWidth();
        double razmera = sirinaPrikaza > 0 ? pregled.getWidth() / sirinaPrikaza : 1;
        prikaz.setImage(obradiPiksele(pregled, razmera));
    }

    // Umetnički filteri, redom: pikselizacija, posterizacija, uljana slika, akvarel, reljef, crtež olovkom.
    // razmera: koliko je puta slika veća od prikaza (da filter izgleda isto na prikazu i u sačuvanoj slici)
    private Image obradiPiksele(Image izvor, double razmera) {
        if (umetnickiKlizaci.stream().allMatch(k -> k.getValue() == 0)) {
            return izvor;
        }
        int w = (int) izvor.getWidth();
        int h = (int) izvor.getHeight();
        int[] p = new int[w * h];
        izvor.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), p, 0, w);

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
            String adresa = fajl.toURI().toString();
            original = new Image(adresa);
            pregled = original.getWidth() > NAJVECI_PREGLED || original.getHeight() > NAJVECI_PREGLED
                    ? new Image(adresa, NAJVECI_PREGLED, NAJVECI_PREGLED, true, true)
                    : original;
            prikaz.setImage(pregled);
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
        original = null;
        pregled = null;
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
        double sirinaPrikaza = prikaz.getLayoutBounds().getWidth();
        double razmera = sirinaPrikaza > 0 ? original.getWidth() / sirinaPrikaza : 1;
        ImageView puna = new ImageView(obradiPiksele(original, razmera));
        puna.setEffect(napraviEfekat(original.getWidth(), original.getHeight(), razmera));

        SnapshotParameters parametri = new SnapshotParameters();
        // JPG ne podržava providnost, pa providne delove popunjavamo belom bojom
        parametri.setFill(jeJpg ? Color.WHITE : Color.TRANSPARENT);
        // Zamućenje širi sliku preko ivica; snimamo samo deo veličine originala
        parametri.setViewport(new Rectangle2D(0, 0, original.getWidth(), original.getHeight()));
        WritableImage snimak = puna.snapshot(parametri, null);

        try {
            BufferedImage zaUpis = okreni(uBufferedImage(snimak, jeJpg, 1 - providnost.getValue()),
                    (int) rotacija.getValue(),
                    ogledaloLevoDesno.getValue() == 1, ogledaloGoreDole.getValue() == 1);
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

    // Primenjuje ogledalo, pa rotaciju za kvartovi * 90° udesno
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
