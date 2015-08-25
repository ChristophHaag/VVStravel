package haagch.vvstravel;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;

import static android.R.layout.simple_list_item_1;
import static java.util.Collections.sort;

/**
 * Created by chris on 20.08.15.
 */
public class DepartureFragment extends Fragment {
    private enum states {
        DEPARTURELIST,
        STATIONLIST,
        NONE
    }
    private static states state = states.NONE;
    private class MonospaceAdapter extends ArrayAdapter<String> {
        public MonospaceAdapter(Context context, int resource) {
            super(context, resource);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);

            Typeface monospace = Typeface.MONOSPACE;
            ((TextView)v).setTypeface(monospace);
            return v;
        }
    }
    ArrayAdapter<String> aa;
    SimpleDateFormat outputformat = new SimpleDateFormat("HH:mm");
    TimeZone tz = TimeZone.getTimeZone("Europe/Berlin"); // VVS in germany

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.departurefrom, container, false);
        aa = new MonospaceAdapter(getActivity().getBaseContext(), simple_list_item_1);
        outputformat.setTimeZone(tz);
        return rootView;
    }

    AsyncTask lastTask = null;
    ArrayList<Entry> entrylist = new ArrayList<>();

    @Override
    public void onStart() {
        super.onStart();
        final EditText et = (EditText) getView().findViewById(R.id.stationtf);
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String vvsurl = "http://www2.vvs.de/vvs/XSLT_STOPFINDER_REQUEST?jsonp=func&suggest_macro=vvs&name_sf=";
                URL u = null;
                try {
                    u = new URL(vvsurl + s.toString());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                if (lastTask != null && lastTask.getStatus() != AsyncTask.Status.FINISHED) {
                    lastTask.cancel(true);
                }
                lastTask = new RequestStation();
                ((RequestStation) lastTask).execute(u);
            }
        });


        ListView lv = (ListView) getView().findViewById(R.id.stationlistView);
        lv.setAdapter(aa);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (state == states.DEPARTURELIST) {
                    aa.clear();
                    final EditText et = (EditText) getView().findViewById(R.id.stationtf);
                    et.setText("");
                    state = states.NONE;
                    return;
                }
                if (position >= entrylist.size()) {
                    return; //inconsistency between backing list and displayed list
                }
                try {
                    Date current = Calendar.getInstance(tz).getTime();
                    SimpleDateFormat formattedYear = new SimpleDateFormat("yy", Locale.GERMANY);
                    formattedYear.setTimeZone(tz);
                    SimpleDateFormat formattedmonth = new SimpleDateFormat("MM", Locale.GERMANY);
                    formattedmonth.setTimeZone(tz);
                    SimpleDateFormat formattedday = new SimpleDateFormat("dd", Locale.GERMANY);
                    formattedday.setTimeZone(tz);
                    SimpleDateFormat formattedhour = new SimpleDateFormat("HH", Locale.GERMANY);
                    formattedhour.setTimeZone(tz);
                    SimpleDateFormat formattedminute = new SimpleDateFormat("mm", Locale.GERMANY);
                    formattedminute.setTimeZone(tz);

                    String url = "http://www2.vvs.de/vvs/widget/XML_DM_REQUEST?"+
                            "zocationServerActive=1"+
                            "&lsShowTrainsExplicit1"+
                            "&stateless=1"+
                            "&language=de"+
                            "&SpEncId=0"+
                            "&anySigWhenPerfectNoOtherMatches=1"+
                            "&limit=25" + //TODO
                            "&depArr=departure"+
                            "&type_dm=any"+
                            "&anyObjFilter_dm=2"+
                            "&deleteAssignedStops=1"+
                            "&name_dm=" + entrylist.get(position).id+ //TODO thread safety
                            "&mode=direct"+
                            "&dmLineSelectionAll=1"+
                            "&itdDateYear=" + formattedYear.format(current)+
                            "&itdDateMonth=" + formattedmonth.format(current)+
                            "&itdDateDay=" + formattedday.format(current)+
                            "&itdTimeHour=" + formattedhour.format(current)+
                            "&itdTimeMinute=" + formattedminute.format(current)+
                            "&useRealtime=1"+
                            "&outputFormat=JSON";
                    URL u = new URL(url);
                    lastTask = new RequestDeparturesByStation();
                    ((RequestDeparturesByStation) lastTask).execute(u);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private class RequestDeparturesByStation extends AsyncTask<URL, Integer, List<String>> {
        @Override
        protected List<String> doInBackground(URL... params) {
            ArrayList<String> al = new ArrayList<>();
            URL u = params[0];
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet g = new HttpGet(String.valueOf(u));
            try {
                HttpResponse response = httpclient.execute(g);

                Scanner reader = new Scanner(response.getEntity().getContent(), "UTF-8").useDelimiter("\\A");
                String content = reader.hasNext() ? reader.next() : null;
                if (content == null) {
                    return al;
                }

                JSONObject jObject = new JSONObject(content);
                JSONArray dl = jObject.getJSONArray("departureList");

                String[][] deps = new String[dl.length()][4];
                int maxlento = 0;
                for (int i = 0; i < dl.length(); i++) {
                    JSONObject departureentry = dl.getJSONObject(i);

                    JSONObject servline = departureentry.getJSONObject("servingLine");
                    String to = servline.getString("direction");
                    String line = servline.getString("symbol");
                    String delay = "0";
                    if (servline.has("delay")) {
                        delay = servline.getString("delay");
                    }

                    JSONObject dj = departureentry.getJSONObject("dateTime");

                    Calendar planneddeparture = Calendar.getInstance(tz);
                    planneddeparture.set(
                            Integer.valueOf(dj.getString("year")),
                            Integer.valueOf(dj.getString("month")),
                            Integer.valueOf(dj.getString("day")),
                            Integer.valueOf(dj.getString("hour")),
                            Integer.valueOf(dj.getString("minute"))
                    );

                    Calendar realdeparture = planneddeparture;
                    if (departureentry.has("realDateTime")) {
                        JSONObject drj = departureentry.getJSONObject("realDateTime");
                        realdeparture.set(
                                Integer.valueOf(drj.getString("year")),
                                Integer.valueOf(drj.getString("month")),
                                Integer.valueOf(drj.getString("day")),
                                Integer.valueOf(drj.getString("hour")),
                                Integer.valueOf(drj.getString("minute"))
                        );
                    }
                    deps[i] = new String[4];
                    deps[i][0] = line;
                    deps[i][1] = to;
                    maxlento = Math.max(to.length(), maxlento);
                    deps[i][2] = outputformat.format(planneddeparture.getTime());
                    deps[i][3] = delay.equals("0") ? "" : "+" + delay + " Minutes";
                }

                for (String[] d : deps) {
                    al.add(String.format("%-6s %" + -(maxlento + 4) + "s %s %s", d[0], d[1], d[2], d[3]));
                }

            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return al;
        }

        @Override
        protected void onPostExecute(List<String> strings) {
            aa.clear();
            aa.addAll(strings);
            state = states.DEPARTURELIST;
        }
    }

    private class RequestStation extends AsyncTask<URL, Integer, List<String>> {
        @Override
        protected List<String> doInBackground(URL... params) {
            ArrayList<String> al = new ArrayList<>();
            //give a second time to be cancelled before actually doing the request
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return al;
            }
            URL u = params[0];
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet g = new HttpGet(String.valueOf(u));
            try {
                HttpResponse response = httpclient.execute(g);

                Scanner reader = new Scanner(response.getEntity().getContent(), "UTF-8").useDelimiter("\\A");
                String content = reader.hasNext() ? reader.next() : null;
                if (content == null) {
                    return al;
                }
                String trunc = content.substring(5, content.length() - 2);
                JSONObject jObject = new JSONObject(trunc);
                JSONObject sf = jObject.getJSONObject("stopFinder");

                entrylist.clear();

                if ((Object) sf.get("points") instanceof JSONObject) {
                    //TODO remove duplicated code
                    JSONObject o = sf.getJSONObject("points").getJSONObject("point");
                    if (o.getString("anyType").equals("stop") || o.getString("type").equals("stop")) {
                        String n = o.getString("name");
                        Integer q = o.getInt("quality");
                        Integer id = o.getJSONObject("ref").getInt("id");
                        Entry e = new Entry(q, n, id);
                        entrylist.add(e);
                    }
                } else if ((Object) sf.get("points") instanceof JSONArray) {
                    JSONArray a = sf.getJSONArray("points");
                    for (int i = 0; i < a.length(); i++) {
                        JSONObject o = a.getJSONObject(i);
                        if (!(o.getString("anyType").equals("stop") || o.getString("type").equals("stop"))) {
                            continue;
                        }
                        String n = o.getString("name");
                        Integer q = o.getInt("quality");
                        Integer id = o.getJSONObject("ref").getInt("id");
                        Entry e = new Entry(q, n, id);
                        entrylist.add(e);
                    }
                }

                sort(entrylist);
                for (Entry e : entrylist) {
                    al.add(e.name + " (" + e.quality + ") " + e.id);
                }
                if (sf.toString().isEmpty()) {
                    al.add("Nothing");
                } else {
                    //al.add("Something: " + sf.toString()); // gets filled out by the above
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return al;
        }

        @Override
        protected void onPostExecute(List<String> strings) {
            aa.clear();
            aa.addAll(strings);
            state = states.STATIONLIST;
        }
    }

    private class Entry implements Comparable<Entry> {
        public Entry(int quality, String name, int id) {
            this.quality = quality;
            this.name = name;
            this.id = id;
        }

        int quality;
        String name;
        int id;

        @Override
        public int compareTo(Entry another) {
            if (another.quality == this.quality) return 0; else return this.quality > another.quality ? -1 : 1;
        }
    }

}