package ru.hse.java.repetinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ru.hse.java.repetinder.R;
import ru.hse.java.repetinder.card.Card;
import ru.hse.java.repetinder.card.CardsArrayAdapter;
import ru.hse.java.repetinder.user.Storage;
import ru.hse.java.repetinder.user.UserRepetinder;

public class SwipesFragment extends Fragment {
    private CardsArrayAdapter arrayAdapter;

    private DatabaseReference usersDb;
    private String currentUId;

    private List<Card> possibleMatchesQueue;
    private String userRole;
    private String oppositeUserRole;
    private ProgressBar progressBar;
    private UserRepetinder currentUser;

    private enum RangChange {
        INCREASE(-1),
        DECREASE(1);

        private int add;
        RangChange(int delta) {
            add = delta;
        }
    }

    public SwipesFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_swipes, container, false);
        Storage storage = (Storage) this.getArguments().getSerializable(MainActivity.TEXT);
        userRole = storage.userRole;
        oppositeUserRole = storage.oppositeUserRole;
        currentUId = storage.userId;
        currentUser = storage.currentUser;
        usersDb = getDatabaseInstance().getReference().child("Users");
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        getOppositeRoleUsers();
        progressBar = view.findViewById(R.id.progressBar);
        TextView textView = view.findViewById(R.id.textView4);
        textView.setVisibility(View.GONE);
        possibleMatchesQueue = new ArrayList<>();

        arrayAdapter = new CardsArrayAdapter(getActivity(), R.layout.item, possibleMatchesQueue);

        SwipeFlingAdapterView flingContainer = view.findViewById(R.id.frame);
        flingContainer.setAdapter(arrayAdapter);
        flingContainer.setFlingListener(new SwipeFlingAdapterView.onFlingListener() {
            @Override
            public void removeFirstObjectInAdapter() {
                Log.d("LIST", "removed object!");
                possibleMatchesQueue.remove(0);
                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLeftCardExit(Object dataObject) {
                Card card = (Card) dataObject;
                String userId = card.getUserId();
                usersDb.child(oppositeUserRole).child(userId).child("Connections").child("No").child(currentUId).setValue(true);
                changeRang(RangChange.DECREASE, userId);
                Toast.makeText(getActivity(), "no...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRightCardExit(Object dataObject) {
                Card card = (Card) dataObject;
                String userId = card.getUserId();
                String key = getDatabaseInstance().getReference().child("Chats").push().getKey();
                // add users to chat
                DatabaseReference chatDB = getDatabaseInstance().getReference().child("Chats");
                chatDB.child(key).child("ChatUsers").child(    userId).setValue(    userId);
                chatDB.child(key).child("ChatUsers").child(currentUId).setValue(currentUId);
                usersDb.child(oppositeUserRole).child(    userId)
                        .child("Connections").child("Yes")
                        .child(currentUId).child("ChatId").setValue(key);
                usersDb.child(        userRole).child(currentUId)
                        .child("Connections").child("Yes")
                        .child(    userId).child("ChatId").setValue(key);
                changeRang(RangChange.INCREASE, userId);
                Toast.makeText(getActivity(), "yes!!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
                textView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onScroll(float scrollProgressPercent) {}
        });

        flingContainer.setOnItemClickListener((itemPosition, dataObject) -> {
            Card card = (Card) dataObject;
            String matchId = card.getUserId();
            Intent intent = new Intent(getActivity(), MatchProfileActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(MatchProfileActivity.TEXT1, matchId);
            bundle.putString(MatchProfileActivity.TEXT2, oppositeUserRole);
            intent.putExtras(bundle);
            startActivity(intent);
        });
        return view;
    }

    private void changeRang(RangChange rangChange, String userId) {
        usersDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long rang = 0;
                DataSnapshot currentSnap = snapshot.child(oppositeUserRole).child(userId);
                if (currentSnap.exists() && currentSnap.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) currentSnap.getValue();
                    if (oppositeUserRole.equals("Tutor") && Objects.requireNonNull(map).get("rang") != null) {
                        rang = (long) map.get("rang");
                    }
                }
                if (oppositeUserRole.equals("Tutor")) {
                    Map userInfo = new HashMap();
                    userInfo.put("rang", rang + rangChange.add);
                    Log.v("RANG", "Rang was: " + rang + ", become: " + (rang + rangChange.add));
                    usersDb.child(oppositeUserRole).child(userId).updateChildren(userInfo);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private FirebaseDatabase getDatabaseInstance() {
        return FirebaseDatabase.getInstance("https://repetinder-cb68d-default-rtdb.europe-west1.firebasedatabase.app/");
    }

    private void getOppositeRoleUsers() {
        DatabaseReference oppositeSexDb = getDatabaseInstance().getReference().child("Users").child(oppositeUserRole);
        Query query = oppositeSexDb.orderByChild("rang");
        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists() && !snapshot.child("Connections").child("No").hasChild(currentUId)
                        && !snapshot.child("Connections").child("Yes").hasChild(currentUId)) {
                    int price = ((Long) snapshot.child("price").getValue()).intValue();
                    UserRepetinder.Subject matchSubject = UserRepetinder.Subject.valueOf(Objects.requireNonNull(snapshot.child("subject").getValue()).toString());
                    boolean isSeen = (boolean) snapshot.child("seen").getValue();
                    if (isSeen && matchSubject.equals(currentUser.getSubject()) && price <= currentUser.getPrice() + 500) {
                        String profileImageUrl = "default";
                        if (!Objects.equals(snapshot.child("profileImageUrl").getValue(), "default")) {
                            profileImageUrl = Objects.requireNonNull(snapshot.child("profileImageUrl").getValue()).toString();
                        }
                        Card card = new Card(snapshot.getKey(), Objects.requireNonNull(snapshot.child("fullname").getValue()).toString(),
                                profileImageUrl);
                        possibleMatchesQueue.add(card);
                        arrayAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        oppositeSexDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}
