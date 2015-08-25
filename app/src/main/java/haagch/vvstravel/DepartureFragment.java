package haagch.vvstravel;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import static android.R.layout.simple_list_item_1;
import static java.util.Collections.sort;

/**
 * Created by chris on 20.08.15.
 */
public class DepartureFragment extends Fragment {
    ArrayAdapter<String> aa;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.departurefrom, container, false);
        aa = new ArrayAdapter<String>(getActivity().getBaseContext(), simple_list_item_1);
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
                //aa.add(s.toString());
            }
        });


        ListView lv = (ListView) getView().findViewById(R.id.stationlistView);
        lv.setAdapter(aa);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= entrylist.size()) {
                    return; //inconsistency between backing list and displayed list
                }
                try {
                    //TODO: timezone
                    String formattedYear = new SimpleDateFormat("yy", Locale.GERMANY).format(Calendar.getInstance().getTime());
                    String formattedmonth = new SimpleDateFormat("MM", Locale.GERMANY).format(Calendar.getInstance().getTime());
                    String formattedday = new SimpleDateFormat("dd", Locale.GERMANY).format(Calendar.getInstance().getTime());
                    String formattedhour = new SimpleDateFormat("hh", Locale.GERMANY).format(Calendar.getInstance().getTime());
                    String formattedminute = new SimpleDateFormat("mm", Locale.GERMANY).format(Calendar.getInstance().getTime());

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
                            "&itdDateYear=" + formattedYear+
                            "&itdDateMonth=" + formattedmonth+
                            "&itdDateDay=" + formattedday+
                            "&itdTimeHour=" + formattedhour+
                            "&itdTimeMinute=" + formattedminute+
                            "&useRealtime=1";
                    aa.clear();
                    aa.add(url);
                     URL u = new URL(url);
                    lastTask = new RequestDeparturesByStation();
                    ((RequestDeparturesByStation) lastTask).execute(u);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                //aa.clear();
                aa.add("Departures:");
                aa.add("TODO");
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

                /*Scanner reader = new Scanner(response.getEntity().getContent(), "UTF-8").useDelimiter("\\A");
                String content = reader.hasNext() ? reader.next() : null;
                if (content == null) {
                    return al;
                }
                */

                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(response.getEntity().getContent(), null);
                parser.nextTag();

                //TODO: parse XML

                //al.add(content);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
            return al;
        }

        @Override
        protected void onPostExecute(List<String> strings) {
            aa.clear();
            aa.addAll(strings);
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
                    al.add("Obj");
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
                    //al.add(sf.toString());
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
