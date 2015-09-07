package haagch.vvstravel;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import static android.R.layout.simple_list_item_1;

/**
 * Created by chris on 26.08.15.
 */
public class DepartureFavoritesFragment extends Fragment implements AdapterView.OnItemClickListener {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.departurefavorites, container, false);
        return rootView;
    }

    ArrayAdapter<String> aa;
    SharedPreferences favoritesPreferences;

    @Override
    public void onStart() {
        aa = new ArrayAdapter<>(getActivity().getBaseContext(), simple_list_item_1);
        super.onStart();
        final ListView lv = (ListView) getView().findViewById(R.id.departurefavoriteslv);
        lv.setAdapter(aa);

        lv.setOnItemClickListener(this);

        registerForContextMenu(lv);
        favoritesPreferences = getActivity().getApplicationContext().
                getSharedPreferences("favorites", Context.MODE_PRIVATE);
        aa.addAll(favoritesPreferences.getAll().keySet());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(ContextMenu.NONE, 1, Menu.NONE, R.string.DeleteFavorite);
    }

    void addEntry(String name, int stationId) {
        SharedPreferences preferences = getActivity().getApplicationContext().
                getSharedPreferences("favorites", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = preferences.edit();
        prefsEditor.putInt(name, stationId);
        prefsEditor.commit();
        aa.add(name);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(getUserVisibleHint()) {
            final ListView lv = (ListView) getView().findViewById(R.id.departurefavoriteslv);
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            switch (item.getItemId()) {
                case 1:
                    String name = lv.getItemAtPosition(info.position).toString();
                    Log.i("aa", "Remove " + name + " from favorites");
                    SharedPreferences preferences = getActivity().getApplicationContext().
                            getSharedPreferences("favorites", Context.MODE_PRIVATE);
                    SharedPreferences.Editor prefsEditor = preferences.edit();
                    prefsEditor.remove(name);
                    prefsEditor.commit();
                    aa.remove(name);
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final ListView lv = (ListView) getView().findViewById(R.id.departurefavoriteslv);
        String name = lv.getItemAtPosition(position).toString();
        int stationId = favoritesPreferences.getInt(name, 0);

        ViewPager mViewPager = (ViewPager) getActivity().findViewById(R.id.pager);
        int tabid = ((VVSMain)getActivity()).getFragmentTabNum(R.string.depfromDesc); //abuse desc string
        mViewPager.setCurrentItem(tabid);

        ((DepartureFragment) ((VVSMain)getActivity()).getFragmentItem(R.string.depfromDesc).f).triggerRequest(name, stationId);
    }
}
