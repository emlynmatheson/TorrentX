package ml.emlyn.torrentx.ui.video;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.math.BigInteger;
import java.util.Objects;

import ml.emlyn.torrentx.R;
import ml.emlyn.torrentx.torrents;

public class VideoFragment extends Fragment {

    private String TAG = "ml.emlyn.torrentx.tag";

    private int currScreen = 0; //0: Download, 1: Open
    private LayoutInflater inflater;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        this.inflater = inflater;
        return inflater.inflate(R.layout.fragment_video, container, false);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((BottomNavigationView) requireView().findViewById(R.id.bt_nav_vid)).setOnNavigationItemSelectedListener(
                item -> {
                    switch (item.getItemId()) {
                        case R.id.download_menu: {
                            //Replace UI with download menu

                            if (currScreen == 0) {
                                return true;
                            }

                            ((EditText) requireActivity().findViewById(R.id.vid_dl_search_bar)).setText("");
                            requireActivity().findViewById(R.id.vid_dl_search).setOnClickListener(this::dlSearchBtnOnClick);
                            dlSearchBtnOnClick(getView());

                            currScreen = 0;
                            return true;
                        }

                        case R.id.open_menu: {
                            //Replace UI with open menu

                            if (currScreen == 1) {
                                return true;
                            }

                            ((EditText) requireActivity().findViewById(R.id.vid_dl_search_bar)).setText("");
                            requireActivity().findViewById(R.id.vid_dl_search).setOnClickListener(this::opSearchBtnOnClick);
                            opSearchBtnOnClick(getView());

                            currScreen = 1;
                            return true;
                        }
                    }
                    return false;
                }

        );

        (requireView().findViewById(R.id.vid_dl_search)).setOnClickListener(this::dlSearchBtnOnClick);

        EditText searchEdit = requireView().findViewById(R.id.vid_dl_search_bar);
        searchEdit.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                if (currScreen == 0) {
                    dlSearchBtnOnClick(v);
                }
                if (currScreen == 1) {
                    opSearchBtnOnClick(v);
                }
                return true;
            }
            return false;
        });
    }

    //Onclick-esque functions

    private void dlUpdateSearchRes() {

        ((LinearLayout) requireActivity().findViewById(R.id.vid_dl_ll)).removeAllViews();

        new Thread(() -> torrents.searchTorrent(((EditText) requireView().findViewById(R.id.vid_dl_search_bar)).getText().toString(), "video", requireActivity(), searchRes -> {
            for (String[] res : searchRes) {
                createEntry(res[0], res[2], res[1], requireActivity().findViewById(R.id.vid_dl_ll));
                Log.d(TAG, "Created");
            }
        })).start();
    }

    public void dlSearchBtnOnClick(View v) {
        EditText searchEdit = requireView().findViewById(R.id.vid_dl_search_bar);
        searchEdit.clearFocus();
        hideKeyboardFrom(requireContext(), v);

        dlUpdateSearchRes();
    }

    private void opUpdateSearchRes() {

    }

    public void opSearchBtnOnClick(View v) {
        EditText searchEdit = requireView().findViewById(R.id.vid_dl_search_bar);
        searchEdit.clearFocus();
        hideKeyboardFrom(requireContext(), v);

        opUpdateSearchRes();
    }

    // Helper functions

    private static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        Objects.requireNonNull(imm).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void createEntry(String name, String magUri, String size, ViewGroup root) {
        View newEntry = this.inflater.inflate(R.layout.layout_entry, root);

        ((TextView) newEntry.findViewById(R.id.enNameTv)).setText(name);
        if (Long.parseLong(size) >= 1073741824) {
            ((TextView) newEntry.findViewById(R.id.enSizeTv)).setText(getString(R.string.size_gb, String.valueOf(Long.parseLong(size) / 1073741824)));
        } else if (Long.parseLong(size) >= 1048576) {
            ((TextView) newEntry.findViewById(R.id.enSizeTv)).setText(getString(R.string.size_mb, String.valueOf(Long.parseLong(size) / 1048576)));
        } else {
            ((TextView) newEntry.findViewById(R.id.enSizeTv)).setText(getString(R.string.size_kb, String.valueOf(Long.parseLong(size) / 1024)));
        }

        //TODO: Download shit
    }

    //Threading helpers
}
