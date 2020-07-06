package ml.emlyn.torrentx.ui.books;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import ml.emlyn.torrentx.R;

public class BooksFragment extends Fragment {


    @SuppressLint("RestrictedApi")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        final View root = inflater.inflate(R.layout.fragment_books, container, false);
        ((BottomNavigationItemView) root.findViewById(R.id.open_menu)).setTitle(getText(R.string.rd_sec_title));

        final BottomNavigationView navBar = root.findViewById(R.id.bt_nav_vid);

        navBar.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()){

                    case R.id.download_menu:

                        return true;

                    case R.id.open_menu:
                        item.setTitle(getText(R.string.rd_sec_title));

                        return true;

                    }
                    return true;
                }
            });

        return root;
    }

}
