package com.example.kevin_sct.beastchat.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kevin_sct.beastchat.R;
import com.example.kevin_sct.beastchat.Utils.CONSTANT;
import com.example.kevin_sct.beastchat.activities.BaseFragmentActivity;
import com.example.kevin_sct.beastchat.entites.User;
import com.example.kevin_sct.beastchat.services.LiveFriendServices;
import com.example.kevin_sct.beastchat.views.FindFriendsViews.FindFriendsAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.socket.client.IO;
import io.socket.client.Socket;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by kevin_sct on 4/28/17.
 */

public class FindFriendFragment extends BaseFragment implements FindFriendsAdapter.UserListener {

    @BindView(R.id.fragment_find_friends_searchBar)
    EditText mSearchBarEt;

    @BindView(R.id.fragment_find_friends_recyclerView)
    RecyclerView mRecycleView;

    @BindView(R.id.fragment_find_friends_noResults)
    TextView mTextView;

    private Unbinder mUnbiner;
    private List<User> mAllUsers;

    private Socket mSocket;

    private PublishSubject<String> mSearchBarString;

    public static FindFriendFragment newInstance(){
        return new FindFriendFragment();
    }

    private String mUserEmailString;
    private FindFriendsAdapter mAdapter;
    private LiveFriendServices mLiveFriendServices;

    private DatabaseReference mGetAllUserReference;
    private ValueEventListener mGetAllUserListener;

    private DatabaseReference mGetAllFriendRequestsSentReference;
    private ValueEventListener mGetAllFriendRequestsListener;

    private DatabaseReference mGetAllFriendRequestReceivedReference;
    private ValueEventListener getmGetAllFriendRequestsReceivedListener;

    private DatabaseReference mGetAllCurrentUsersFriendsReference;
    private ValueEventListener mGetAllCurrentUsersFriendsListener;

    public HashMap<String, User> mFriendRequestsSentMap;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mSocket = IO.socket(CONSTANT.IP_LOCAL_HOST);
        } catch (URISyntaxException e) {
            Log.i(RegisterFragment.class.getSimpleName(), e.getMessage());
            Toast.makeText(getActivity(), "Can't connect to the server", Toast.LENGTH_SHORT).show();
        }

        mSocket.connect();

        mUserEmailString = mSharedPreferences.getString(CONSTANT.USER_EMAIL, "");
        mLiveFriendServices = LiveFriendServices.getInstance();
        mFriendRequestsSentMap = new HashMap<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_find_friends, container, false);
        mUnbiner = ButterKnife.bind(this, rootView);
        mAllUsers = new ArrayList<>();
        mAdapter = new FindFriendsAdapter((BaseFragmentActivity) getActivity(), this);

        mGetAllUserListener = getAllUsers(mAdapter, mUserEmailString);
        mGetAllUserReference = FirebaseDatabase.getInstance().getReference().child(CONSTANT.FIRE_BASE_PATH_USERS);
        mGetAllUserReference.addValueEventListener(mGetAllUserListener);

        mGetAllFriendRequestsSentReference = FirebaseDatabase.getInstance().getReference()
                .child(CONSTANT.FIRE_BASE_PATH_FRIEND_REQUEST_SENT)
                .child(CONSTANT.encodeEmail(mUserEmailString));

        mGetAllFriendRequestsListener = mLiveFriendServices.getFriendRequestsSent(mAdapter, this);

        getmGetAllFriendRequestsReceivedListener = mLiveFriendServices.getFriendRequestsReceived(mAdapter, this);

        mGetAllFriendRequestReceivedReference = FirebaseDatabase.getInstance().getReference()
                .child(CONSTANT.FIRE_BASE_PATH_FRIEND_REQUEST_RECEIVED)
                .child(CONSTANT.encodeEmail(mUserEmailString));

        mGetAllFriendRequestsSentReference.addValueEventListener(mGetAllFriendRequestsListener);

        mGetAllFriendRequestReceivedReference.addValueEventListener(getmGetAllFriendRequestsReceivedListener);

        mGetAllCurrentUsersFriendsReference = FirebaseDatabase.getInstance().getReference()
                .child(CONSTANT.FIRE_BASE_PATH_USER_FRIENDS)
                .child(CONSTANT.encodeEmail(mUserEmailString));

        mGetAllCurrentUsersFriendsListener = mLiveFriendServices.getAllCurrentUsersFriendMap(mAdapter);
        mGetAllCurrentUsersFriendsReference.addValueEventListener(mGetAllCurrentUsersFriendsListener);


        mRecycleView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecycleView.setAdapter(mAdapter);

        mCompositeSubscription.add(createSearchBarSubscription());
        listenToSearchBar();

        return rootView;
    }


    private Subscription createSearchBarSubscription(){
        mSearchBarString = PublishSubject.create();
        return mSearchBarString
                .debounce(1000, TimeUnit.MILLISECONDS) //set how long we are gonna delay
                .subscribeOn(Schedulers.io())
                .map(new Func1<String, List<User>>() {

                    @Override
                    public List<User> call(String searchString) {      //if the email matches the string, return the user
                        return mLiveFriendServices.getMatchingUsers(mAllUsers, searchString);
                    }
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<User>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(List<User> users) {
                        if(users.isEmpty()){
                            mTextView.setVisibility(View.VISIBLE);
                            mRecycleView.setVisibility(View.GONE);
                        }else{
                            mTextView.setVisibility(View.GONE);
                            mRecycleView.setVisibility(View.VISIBLE);
                        }

                        mAdapter.setmUsers(users);
                    }
                });
    }


    private void listenToSearchBar(){
        mSearchBarEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSearchBarString.onNext(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }


    public void setmFriendRequestsSentMap(HashMap<String, User> friendRequestsSentMap) {
        mFriendRequestsSentMap.clear();
        mFriendRequestsSentMap.putAll(friendRequestsSentMap);
    }

    public ValueEventListener getAllUsers(final FindFriendsAdapter adapter, String currentUserEmail){
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mAllUsers.clear();
                for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                    User user = snapshot.getValue(User.class);
                    if(!user.getEmail().equals(mUserEmailString)){  // "&& user.isHasLoggedIn()" add this sentence, make only loggedin users shown
                        mAllUsers.add(user);
                    }
                }
                adapter.setmUsers(mAllUsers);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getActivity(), "Can't Load Users", Toast.LENGTH_SHORT).show();
            }
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbiner.unbind();

        if(mGetAllUserListener != null){
            mGetAllUserReference.removeEventListener(mGetAllUserListener);
        }

        if(mGetAllFriendRequestsSentReference != null){
            mGetAllFriendRequestsSentReference.removeEventListener(mGetAllFriendRequestsListener);
        }

        if(getmGetAllFriendRequestsReceivedListener != null){
            mGetAllFriendRequestReceivedReference.removeEventListener(getmGetAllFriendRequestsReceivedListener);
        }

        if(mGetAllCurrentUsersFriendsListener !=null){
            mGetAllCurrentUsersFriendsReference.removeEventListener(mGetAllCurrentUsersFriendsListener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
    }

    @Override
    public void OnUserClicked(User user) {
        if (CONSTANT.isIncludedInMap(mFriendRequestsSentMap,user)){
            mGetAllFriendRequestsSentReference.child(CONSTANT.encodeEmail(user.getEmail()))
                    .removeValue();

            mCompositeSubscription.add(mLiveFriendServices.addOrRemoveFriendRequest(mSocket, mUserEmailString, user.getEmail(), "1"));
        } else{
            mGetAllFriendRequestsSentReference.child(CONSTANT.encodeEmail(user.getEmail()))
                    .setValue(user);

            mCompositeSubscription.add(mLiveFriendServices.addOrRemoveFriendRequest(mSocket, mUserEmailString, user.getEmail(), "0"));
        }
    }
}
