package ml.emlyn.torrentx;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class torrents {

    private final static String TAG = "ml.emlyn.torrentx.tag";

    //Format: {{API, API_URL}, ...}
    private static final String[][] VidApiList  = new String[][]{
            {"TPB", "https://apibay.org/q.php?q={SEARCH_Q}&cat=201"},
            {"TPB", "https://apibay.org/q.php?q={SEARCH_Q}&cat=205"},
    };
    private static final String[][] MusicApiList = new String[][]{
            {"TPB", "https://apibay.org/q.php?q={SEARCH_Q}&cat=101"},
    };
    private static final String[][] BookApiList  = new String[][]{
            {"TPB", "https://apibay.org/q.php?q={SEARCH_Q}&cat=601"},
    };

    private static final String tbpTrackerSuffix = "&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969%2Fannounce&tr=udp%3A%2F%2F9.rarbg.to%3A2920%2Fannounce&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=udp%3A%2F%2Ftracker.internetwarriors.net%3A1337%2Fannounce&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.pirateparty.gr%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.cyberia.is%3A6969%2Fannounce";


    public static void searchTorrent(String name, String cat, Activity activity, Consumer<String[][]> onComplete) {

        //Search by name and return String[][]
        //Sample return: [[{NAME}, {SIZE}, {MAGNET URL}], [{NAME}, {SIZE}, {MAGNET URL}]]

        ArrayList<String[]> results = new ArrayList<>();

        switch (cat) {
            case "video": {
                for (String[] apiUrl : VidApiList) {
                    if (apiUrl[0].equals("TPB")) {
                        results.addAll(getAndParseTPB(apiUrl[1].replace("{SEARCH_Q}", name)));
                    }
                }
                break;
            }

            case "music": {
                for (String[] apiUrl : MusicApiList) {
                    if (apiUrl[0].equals("TPB")) {
                        results.addAll(getAndParseTPB(apiUrl[1].replace("{SEARCH_Q}", name)));
                    }
                }
                break;
            }

            case "books": {
                for (String[] apiUrl : BookApiList) {
                    if (apiUrl[0].equals("TPB")) {
                        results.addAll(getAndParseTPB(apiUrl[1].replace("{SEARCH_Q}", name)));
                    }
                }
                break;
            }
        }

        final String[][] res = results.toArray(new String[0][0]);
        activity.runOnUiThread(() -> onComplete.accept(res));
    }

    @NonNull
    private static ArrayList<String[]> getAndParseTPB(String searchQ) {
        ArrayList<String[]> results = new ArrayList<>();
        JSONArray res = null;

        try {
            res = new JSONArray(Jsoup.connect(searchQ).ignoreContentType(true).execute().body());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
            for (int i=0; i<res.length(); i++) {
                try {
                    if (Integer.parseInt(res.getJSONObject(i).getString("seeders")) <= 0) { continue; }
                    String magUri = "magnet:?xt=urn:bith:" + res.getJSONObject(i).getString("info_hash")+"&dn="+ URLEncoder.encode(res.getJSONObject(i).getString("name"), StandardCharsets.UTF_8.toString()) + tbpTrackerSuffix;
                    results.add(new String[]{res.getJSONObject(i).getString("name"), res.getJSONObject(i).getString("size"), magUri});
                } catch (JSONException | UnsupportedEncodingException e) {
                    Log.e(TAG, String.valueOf(e));
                    e.printStackTrace();
                }
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

