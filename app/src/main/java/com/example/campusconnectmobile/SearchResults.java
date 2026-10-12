package com.example.campusconnectmobile;

import java.util.ArrayList;
import java.util.List;

/** Result of GET /api/search/index.php */
public class SearchResults {
    public String query = "";
    public final List<SearchUserResult> users = new ArrayList<>();
    public final List<Listing> listings = new ArrayList<>();
    public int usersTotal;
    public int listingsTotal;
}
