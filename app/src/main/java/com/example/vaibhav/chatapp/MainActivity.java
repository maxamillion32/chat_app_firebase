package com.example.vaibhav.chatapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.vaibhav.chatapp.Adapters.UsersChatAdapter;
import com.example.vaibhav.chatapp.POJO.ReferenceClass;
import com.example.vaibhav.chatapp.POJO.UsersChatModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private FirebaseDatabase mFirebaseChatRef;         //reference to database root
    private DatabaseReference mFireChatUsersRef;        //reference to users child
    DatabaseReference myConnectionsStatusRef;            //updating connection status
    private FirebaseAuth mAuthData=FirebaseAuth.getInstance();
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private RecyclerView mUsersFireChatRecyclerView;
    private View mProgressBarForUsers;
    private UsersChatAdapter mUsersChatAdapter;
    private String mCurrentUserUid;
    private String mCurrentUserEmail;
    private ChildEventListener mListenerUsers;
    private ValueEventListener mConnectedListener;
    private List<String> mUsersKeyList;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mFirebaseChatRef = FirebaseDatabase.getInstance();
        mFireChatUsersRef = mFirebaseChatRef.getReference(ReferenceClass.CHILD_USERS);
        mUsersFireChatRecyclerView = (RecyclerView) findViewById(R.id.usersFireChatRecyclerView);
        mProgressBarForUsers = findViewById(R.id.progress_bar_users);
        List<UsersChatModel> emptyListChat = new ArrayList<UsersChatModel>();
        mUsersChatAdapter = new UsersChatAdapter(MainActivity.this, emptyListChat);
        mUsersFireChatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mUsersFireChatRecyclerView.setHasFixedSize(true);
        mUsersFireChatRecyclerView.setAdapter(mUsersChatAdapter);
        mUsersKeyList = new ArrayList<String>();

        // Listen for changes in the authentication state
        // Because probably token expire after 24hrs or
        // user log out
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //user is signed in
                    //Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    mCurrentUserUid = user.getUid();
                    mCurrentUserEmail = user.getEmail();
                    queryFireChatUsers();               //query all users except current user
                } else {
                    //user is signed out
                    //Log.d(TAG, "onAuthStateChanged:signed_out");
                    navigateToLogin();
                }
            }
        };
    }

    public void onStart() {
        super.onStart();
        mAuthData.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthStateListener != null) {
            mAuthData.removeAuthStateListener(mAuthStateListener);
        }
        //Log.e(TAG, "I am onStop");
    }
    private void navigateToLogin() {

        // Go to LogIn screen
        Intent intent = new Intent(this, LogInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    private void queryFireChatUsers() {
        showProgressBarForUsers();
        mListenerUsers=mFireChatUsersRef.limitToFirst(50).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                //Log.d(TAG,"inside onChildAdded");
                hideProgressBarForUsers();
                if(dataSnapshot.exists()) {
                    //Log.d(TAG,"new user was added");
                    String userUid=dataSnapshot.getKey();
                    if(!userUid.equals(mCurrentUserUid)) {
                        UsersChatModel user=dataSnapshot.getValue(UsersChatModel.class);
                        user.setRecipientUid(userUid);
                        //add sender info (current user)
                        user.setCurrentUserEmail(mCurrentUserEmail);
                        user.setCurrentUserUid(mCurrentUserUid);
                        mUsersChatAdapter.refill(user);
                    }
                    else {
                        UsersChatModel currentUser = dataSnapshot.getValue(UsersChatModel.class);
                        String userName=currentUser.getFirstName();
                        String createdAt=currentUser.getCreatedAt();
                        mUsersChatAdapter.setNameAndCreatedAt(userName, createdAt);
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.exists()) {
                    String userUid = dataSnapshot.getKey();
                    if(!userUid.equals(mCurrentUserUid)) {
                        UsersChatModel user = dataSnapshot.getValue(UsersChatModel.class);
                        user.setRecipientUid(userUid);
                        //Add current user (or sender) info
                        user.setCurrentUserEmail(mCurrentUserEmail);
                        user.setCurrentUserUid(mCurrentUserUid);
                        int index = mUsersKeyList.indexOf(userUid);
                        //Log.e(TAG, "change index "+index);
                        mUsersChatAdapter.changeUser(index, user);
                    }

                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        myConnectionsStatusRef=mFireChatUsersRef.child(mCurrentUserUid).child(ReferenceClass.CHILD_CONNECTION);
        mConnectedListener=mFirebaseChatRef.getReference().child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected=(boolean)dataSnapshot.getValue();
                if(connected) {
                    myConnectionsStatusRef.setValue(ReferenceClass.KEY_ONLINE);
                    myConnectionsStatusRef.onDisconnect().setValue(ReferenceClass.KEY_OFFLINE);
                    //Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                }
                else {
                    //Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    protected void onResume() {
        super.onResume();

        //int size=mUsersKeyList.size();
        //Log.e(TAG, " size"+size);
    }


    @Override
    protected void onPause() {
        super.onPause();
        //Log.e(TAG, "I am onPause");
    }

    protected void onDestroy() {
        super.onDestroy();
        //Log.e(TAG,"I an onDestroy");
        if (mAuthStateListener != null) {
            mAuthData.removeAuthStateListener(mAuthStateListener);
        }
        mUsersKeyList.clear();
        if(mListenerUsers!=null) {
            mFireChatUsersRef.removeEventListener(mListenerUsers);
        }
        if(mConnectedListener!=null) {
            mFirebaseChatRef.getReference().child(".info/connected").removeEventListener(mConnectedListener);
        }
    }

    private void showProgressBarForUsers(){
        mProgressBarForUsers.setVisibility(View.VISIBLE);
    }


    private void hideProgressBarForUsers(){
        if(mProgressBarForUsers.getVisibility()==View.VISIBLE) {
            mProgressBarForUsers.setVisibility(View.GONE);
        }
    }
    private void logout() {

        if (this.mAuthData != null) {
            myConnectionsStatusRef.setValue(ReferenceClass.KEY_OFFLINE);
            FirebaseAuth.getInstance().signOut();
            onStop();
            navigateToLogin();
        }
    }
    /*  private void setAuthenticatedUser(FirebaseAuth authData) {
        mAuthData=authData;
        if(authData!=null) {
            //user auth has not expired yet
            FirebaseUser user=authData.getCurrentUser();
            mCurrentUserUid=user.getUid();
            mCurrentUserEmail=user.getEmail();
            queryFireChatUsers();
        }
        else {
            //Token expired or user logged out
            navigateToLogin();
        }
    }   */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.action_logout){
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
