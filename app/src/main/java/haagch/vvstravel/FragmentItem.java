package haagch.vvstravel;

import android.support.v4.app.Fragment;

/**
 * Created by chris on 01.09.15.
 */
public class FragmentItem {
    FragmentItem(int id, Fragment f) {
        this.id = id;
        this.f = f;
    }
    int id;
    Fragment f;
}
