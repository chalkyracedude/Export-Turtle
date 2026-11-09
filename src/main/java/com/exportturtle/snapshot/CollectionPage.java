package com.exportturtle.snapshot;

import java.util.ArrayList;
import java.util.List;

/** A locally captured collection-log section, without any interface references. */
public final class CollectionPage
{
    public String accountName;
    public String section;
    public List<Entry> items = new ArrayList<>();

    public static final class Entry
    {
        public int id;
        public int quantity;
        public String name;
        public boolean obtained;
    }
}
