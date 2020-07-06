package ml.emlyn.torrentx;

import android.util.Log;

import com.frostwire.jlibtorrent.AlertListener;
import com.frostwire.jlibtorrent.SessionManager;
import com.frostwire.jlibtorrent.SessionParams;
import com.frostwire.jlibtorrent.SettingsPack;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.swig.settings_pack;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class torrents {

    private final static String TAG = "ml.emlyn.torrentx.tag";

    //Format: {{API, API_URL}, ...}
    private static final String[][] VidApiList  = new String[][]{
            {"TPB", "https://thepiratebay.org/search.php?q={SEARCH_Q}&cat=201"},
            {"TPB", "https://thepiratebay.org/search.php?q={SEARCH_Q}&cat=205"},
    };
    private static final String[][] MusicApiList = new String[][]{
            {"TPB", "https://thepiratebay.org/search.php?q={SEARCH_Q}&cat=101"},
    };
    private static final String[][] BookApiList  = new String[][]{
            {"TPB", "https://thepiratebay.org/search.php?q={SEARCH_Q}&cat=601"},
    };

    public static String[][] searchTorrent(String name, String cat) {

        //Search by name and return String[][]
        //Sample return: [[{NAME}, {MAGNET URL}], [{NAME}, {MAGNET URL}]]

        ArrayList<String[]> results = new ArrayList<>();
        Document searchResDoc;

        switch (cat) {
            case "video": {
                for (String[] apiUrl : VidApiList) {
                    if (apiUrl[0].equals("TPB")) {
                        results = parseTPB(apiUrl[1].replace("{SEARCH_Q}", name));
                    }
                }
            }

            case "music": {
                for (String[] apiUrl : MusicApiList) {
                    if (apiUrl[0].equals("TPB")) {
                        results = parseTPB(apiUrl[1].replace("{SEARCH_Q}", name));
                    }
                }
            }

            case "books": {
                for (String[] apiUrl : BookApiList) {
                    if (apiUrl[0].equals("TPB")) {
                        results = parseTPB(apiUrl[1].replace("{SEARCH_Q}", name));
                    }
                }
            }
        }

        return results.toArray(new String[0][0]);

    }

    private static ArrayList<String[]> parseTPB(String searchQ) {
        ArrayList<String[]> results = new ArrayList<>();
        Document searchResDoc;

        try {
            searchResDoc = Jsoup.connect(searchQ).get();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        Elements nameElems = searchResDoc.select(".item-name > a");
        Elements urlElems = searchResDoc.select(".list-entry > .item-icons > a");

        for (int i=0; i<Math.min(nameElems.size(), urlElems.size()); i++) {
            Log.d(TAG, "Name is: " + nameElems.text());
            Log.d(TAG, "Url is: " + urlElems.attr("href"));
            results.add(new String[]{nameElems.text(), urlElems.attr("href")});
        }

        return results;
    }

    public static void getFromTorr(String torrFile) throws InterruptedException {

        File torrentFile = new File(torrFile);

        final SessionManager s = new SessionManager();

        final CountDownLatch signal = new CountDownLatch(1);

        s.addListener(new AlertListener() {
            @Override
            public int[] types() {
                return null;
            }

            @Override
            public void alert(Alert<?> alert) {
                AlertType type = alert.type();

                switch (type) {
                    case ADD_TORRENT:
                        //Display torrent addition
                        ((AddTorrentAlert) alert).handle().resume();
                        break;
                    case BLOCK_FINISHED:
                        BlockFinishedAlert a = (BlockFinishedAlert) alert;
                        int p = (int) (a.handle().status().progress() * 100);
                        //System.out.println("Progress: " + p + " for torrent name: " + a.torrentName()); - TODO: Update to fix UI
                        break;
                    case TORRENT_FINISHED:
                        //Update page
                        signal.countDown();
                        break;
                    }
                }
        });

        s.start();

        TorrentInfo ti = new TorrentInfo(torrentFile);
        s.download(ti, torrentFile.getParentFile());

        signal.await();

        s.stop();
    }

    public static Entry getTorrFromMag(String magUri) throws Throwable {

        final SessionManager s = new SessionManager();

        SettingsPack sp = new SettingsPack();
        //sp.listenInterfaces("0.0.0.0:43567");
        //sp.listenInterfaces("[::]:43567");
        sp.listenInterfaces("0.0.0.0:43567,[::]:43567");
        //sp.setString(settings_pack.string_types.dht_bootstrap_nodes.swigValue(), "router.silotis.us:6881");
        //sp.setString(settings_pack.string_types.dht_bootstrap_nodes.swigValue(), "router.bittorrent.com:6881");
        sp.setString(settings_pack.string_types.dht_bootstrap_nodes.swigValue(), "dht.transmissionbt.com:6881");

        //TODO: Figure out which configs work (1 in 9)

        SessionParams params = new SessionParams(sp);

        s.start(params);

        final CountDownLatch signal = new CountDownLatch(1);

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long nodes = s.stats().dhtNodes();
                // wait for at least 10 nodes in the DHT.
                if (nodes >= 10) {
                    signal.countDown();
                    timer.cancel();
                }
            }
        }, 0, 1000);

        //Wait for DHT Nodes
        boolean r = signal.await(40, TimeUnit.SECONDS);
        if (!r) {
            //DHT Bootstrap timeout
            System.exit(0);
        }

        //Fetching
        byte[] data = s.fetchMagnet(magUri, 30);
        s.stop();

        if (data != null) {
            return Entry.bdecode(data);
        } else {
            //Magnet return failed
            //Show error in UI
        }

        return null;
    }
}

