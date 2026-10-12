package com.example.campusconnectmobile;

import java.util.ArrayList;
import java.util.List;

/** A generic page of items plus the pagination metadata returned alongside them. */
public class ListPage<T> {
    public final List<T> items = new ArrayList<>();
    public Pagination pagination = new Pagination();

    public boolean hasMore() {
        return pagination.hasMore(items.size());
    }
}
