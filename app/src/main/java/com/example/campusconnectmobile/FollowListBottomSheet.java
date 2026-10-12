package com.example.campusconnectmobile;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class FollowListBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_USERNAME = "username";
    private static final String ARG_MODE = "mode";

    public interface FollowListListener {
        void onFollowCountChanged(int followers, boolean nowFollowing);
        void onOpenProfile(String username);
    }

    private String username;
    private String mode;
    private SessionManager sessionManager;
    private FollowListListener listener;
    private TextInputEditText searchInput;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private FollowUsersAdapter adapter;
    private final List<FollowUser> users = new ArrayList<>();
    private final android.os.Handler debounceHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable debouncedReload = () -> loadUsers(searchInput.getText() == null ? "" :
            searchInput.getText().toString().trim());

    public static FollowListBottomSheet newInstance(String username, String mode) {
        FollowListBottomSheet sheet = new FollowListBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_MODE, mode);
        sheet.setArguments(args);
        return sheet;
    }

    public void setFollowListListener(FollowListListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_follow_list, container, false);
        username = getArguments() == null ? "" : getArguments().getString(ARG_USERNAME, "");
        mode = getArguments() == null ? "followers" : getArguments().getString(ARG_MODE, "followers");
        sessionManager = new SessionManager(requireContext());
        TextView title = view.findViewById(R.id.tvSheetTitle);
        title.setText("following".equals(mode) ? R.string.following_title : R.string.followers_title);
        searchInput = view.findViewById(R.id.etFollowSearch);
        recyclerView = view.findViewById(R.id.rvFollowUsers);
        emptyText = view.findViewById(R.id.tvFollowEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FollowUsersAdapter();
        recyclerView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                debounceHandler.removeCallbacks(debouncedReload);
                debounceHandler.postDelayed(debouncedReload, 300);
            }
        });
        loadUsers("");
        return view;
    }

    private void loadUsers(String q) {
        CcwApiClient.Callback<ListPage<FollowUser>> callback = new CcwApiClient.Callback<ListPage<FollowUser>>() {
            @Override
            public void onSuccess(ListPage<FollowUser> result) {
                if (!isAdded()) return;
                users.clear();
                if (result != null) users.addAll(result.items);
                adapter.notifyDataSetChanged();
                emptyText.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                emptyText.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        };
        if ("following".equals(mode)) {
            CcwApiClient.getFollowing(username, q, 1, sessionManager.getAuthToken(), callback);
        } else {
            CcwApiClient.getFollowers(username, q, 1, sessionManager.getAuthToken(), callback);
        }
    }

    private final class FollowUsersAdapter extends RecyclerView.Adapter<FollowUsersAdapter.UserHolder> {
        @NonNull
        @Override
        public UserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_follow_user, parent, false);
            return new UserHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserHolder holder, int position) {
            FollowUser user = users.get(position);
            String name = user.displayName == null || user.displayName.isEmpty()
                    ? "@" + user.username : user.displayName;
            holder.name.setText(name);
            holder.meta.setText((user.campus == null ? "" : user.campus) +
                    (user.bio == null || user.bio.isEmpty() ? "" : " · " + user.bio));
            holder.action.setText("following".equals(mode)
                    ? R.string.profile_remove
                    : (user.isFollowedByMe ? R.string.profile_remove : R.string.profile_follow));
            Glide.with(holder.avatar).load(CcwHttp.resolveUrl(user.avatarUrl))
                    .placeholder(R.drawable.bg_avatar_circle)
                    .error(R.drawable.bg_avatar_circle)
                    .circleCrop()
                    .into(holder.avatar);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOpenProfile(user.username);
                dismiss();
            });
            holder.action.setOnClickListener(v -> toggleFollow(user, holder.getBindingAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        final class UserHolder extends RecyclerView.ViewHolder {
            final ImageView avatar;
            final TextView name;
            final TextView meta;
            final MaterialButton action;

            UserHolder(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.ivFollowAvatar);
                name = itemView.findViewById(R.id.tvFollowName);
                meta = itemView.findViewById(R.id.tvFollowMeta);
                action = itemView.findViewById(R.id.btnFollowAction);
            }
        }
    }

    private void toggleFollow(FollowUser user, int position) {
        boolean currentlyFollowing = "following".equals(mode) || user.isFollowedByMe;
        user.isFollowedByMe = !currentlyFollowing;
        adapter.notifyItemChanged(position);
        CcwApiClient.Callback<FollowResult> callback = new CcwApiClient.Callback<FollowResult>() {
            @Override
            public void onSuccess(FollowResult result) {
                user.isFollowedByMe = result.following;
                if ("following".equals(mode) && !result.following) {
                    users.remove(position);
                    adapter.notifyItemRemoved(position);
                } else {
                    adapter.notifyItemChanged(position);
                }
                if (listener != null && "followers".equals(mode)) {
                    listener.onFollowCountChanged(result.followers, result.following);
                }
            }

            @Override
            public void onError(String message) {
                user.isFollowedByMe = currentlyFollowing;
                adapter.notifyItemChanged(position);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        };
        if (currentlyFollowing) {
            CcwApiClient.unfollow(user.username, sessionManager.getAuthToken(), callback);
        } else {
            CcwApiClient.follow(user.username, sessionManager.getAuthToken(), callback);
        }
    }
}
