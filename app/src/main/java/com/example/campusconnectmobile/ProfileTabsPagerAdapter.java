package com.example.campusconnectmobile;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ProfileTabsPagerAdapter extends FragmentStateAdapter {

    private final String username;
    private final boolean isMe;

    public ProfileTabsPagerAdapter(@NonNull FragmentActivity fragmentActivity, String username, boolean isMe) {
        super(fragmentActivity);
        this.username = username;
        this.isMe = isMe;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 3) {
            return ProfileReviewsTabFragment.newInstance(username);
        }
        String mode = position == 0 ? "listings" : (position == 1 ? "saved" : "sold");
        return ProfileListingsTabFragment.newInstance(mode, username, isMe);
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
