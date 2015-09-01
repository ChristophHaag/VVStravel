package haagch.vvstravel;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Map;

import static android.R.layout.simple_list_item_1;

/**
 * Created by chris on 26.08.15.
 */
public class DepartureFavoritesFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.departurefavorites, container, false);
        return rootView;
    }

    //TODO: useful ordering
    ArrayList<String> orderednames = new ArrayList<>();
    Map<String, Integer> all;

    @Override
    public void onStart() {
        super.onStart();
        ListView lv = (ListView) getView().findViewById(R.id.departurefavoriteslv);
        ArrayAdapter<String> aa = new ArrayAdapter<String>(getActivity().getBaseContext(), simple_list_item_1);
        lv.setAdapter(aa);
        registerForContextMenu(lv); // TODO: for deleting items
        SharedPreferences preferences = getActivity().getApplicationContext().
                getSharedPreferences("favorites", Context.MODE_PRIVATE);
        all = (Map<String, Integer>) preferences.getAll();
        for (String name: all.keySet())
        {
            Integer id = all.get(name);
            aa.add(name);
            orderednames.add(name);
        }

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = orderednames.get(position);
                int stationId = all.get(name);

                ViewPager mViewPager = (ViewPager) getActivity().findViewById(R.id.pager);
                int tabid = ((VVSMain)getActivity()).getFragmentTabNum(R.string.depfromDesc); //abuse desc string
                mViewPager.setCurrentItem(tabid);
                DepartureFragment f = (DepartureFragment) ((VVSMain)getActivity()).getFragmentItem(R.string.depfromDesc).f; //abuse desc string
                //insert our station and trigger async request
                f.state = DepartureFragment.states.SETEXTERNAL;
                f.entrylist.clear();
                f.entrylist.add(new Entry(0, "", stationId));
                ListView lv = (ListView) f.getView().findViewById(R.id.stationlistView);
                EditText et = (EditText) f.getView().findViewById(R.id.stationtf);
                et.setText(name);
                lv.performItemClick(null, 0, 0);
            }
        });
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(ContextMenu.NONE, 1, Menu.NONE, R.string.DeleteFavorite);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case 1:
                String name = orderednames.get(info.position);
                Log.i("aa", "Remove " + name + " from favorites");
                SharedPreferences preferences = getActivity().getApplicationContext().
                        getSharedPreferences("favorites", Context.MODE_PRIVATE);
                SharedPreferences.Editor prefsEditor = preferences.edit();
                prefsEditor.remove(name);
                prefsEditor.commit();
                final FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.detach(this);
                ft.attach(this);
                ft.commit();
        }
        return super.onContextItemSelected(item);
    }
}
