package haagch.vvstravel;

/**
 * Created by chris on 01.09.15.
 */
public class Entry implements Comparable<Entry> {
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