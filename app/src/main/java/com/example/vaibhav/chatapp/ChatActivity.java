package com.example.vaibhav.chatapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.example.vaibhav.chatapp.Adapters.MessageChatAdapter;
import com.example.vaibhav.chatapp.POJO.MessageChatModel;
import com.example.vaibhav.chatapp.POJO.ReferenceClass;
import com.example.vaibhav.chatapp.POJO.UsersChatModel;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private RecyclerView mChatRecyclerView;
    private TextView mUserMessageChatText;
    private MessageChatAdapter mMessageChatAdapter;
    private static final int SENDER_STATUS=0;
    private static final int RECIPIENT_STATUS=1;
    private String mRecipientUid;
    private String mSenderUid;
    private FirebaseDatabase mDatabaseRef;
    private DatabaseReference mFirebaseMessagesChat;
    private ChildEventListener mMessageChatListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent getUsersData=getIntent();
        UsersChatModel usersDataModel=getUsersData.getParcelableExtra(ReferenceClass.KEY_PASS_USERS_INFO);
        mRecipientUid=usersDataModel.getRecipientUid();
        mSenderUid=usersDataModel.getCurrentUserUid();
        mChatRecyclerView=(RecyclerView)findViewById(R.id.chat_recycler_view);
        mUserMessageChatText=(TextView)findViewById(R.id.chat_user_message);
        mChatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mChatRecyclerView.setHasFixedSize(true);
        List<MessageChatModel> emptyMessageChat=new ArrayList<MessageChatModel>();
        mMessageChatAdapter=new MessageChatAdapter(emptyMessageChat);
        mChatRecyclerView.setAdapter(mMessageChatAdapter);
        mDatabaseRef=FirebaseDatabase.getInstance();
        mFirebaseMessagesChat=mDatabaseRef.getReference().child(ReferenceClass.CHILD_CHAT).child(usersDataModel.getChatRef());

    }
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Log.e(TAG, " I am onDestroy");
    }
    protected void onStart() {
        super.onStart();
        //Log.d(TAG,"I am onStart");
        mMessageChatListener=mFirebaseMessagesChat.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.exists()) {
                    //Log.d(TAG,"A new chat was inserted");
                    MessageChatModel newMessage=dataSnapshot.getValue(MessageChatModel.class);
                    if(newMessage.getSender().equals(mSenderUid)) {
                        newMessage.setRecipientOrSenderStatus(SENDER_STATUS);
                    }
                    else {
                        newMessage.setRecipientOrSenderStatus(RECIPIENT_STATUS);
                    }
                    mMessageChatAdapter.refillAdapter(newMessage);
                    mChatRecyclerView.scrollToPosition(mMessageChatAdapter.getItemCount()-1);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

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
    }
    protected void onPause() {
        super.onPause();
        //Log.e(TAG, "I am onPause");

    }

    @Override
    protected void onStop() {
        super.onStop();
        //Log.e(TAG, "I am onStop");

        // Remove listener
        if(mMessageChatListener !=null) {
            // Remove listener
            mFirebaseMessagesChat.removeEventListener(mMessageChatListener);
        }
        // Clean chat message
        mMessageChatAdapter.cleanUp();

    }

    public void sendMessageToFireChat(View sendButton) {
        String senderMessage=mUserMessageChatText.getText().toString();
        senderMessage=senderMessage.trim();
        if(!senderMessage.isEmpty()) {
            //Log.d(TAG,"Send message");
            Map<String,String> newMessage =new HashMap<String, String>();
            newMessage.put("sender",mSenderUid);
            newMessage.put("recipient",mRecipientUid);
            newMessage.put("message",senderMessage);
            mFirebaseMessagesChat.push().setValue(newMessage);
            mUserMessageChatText.setText("");
        }
    }
    public void onBackPressed() {
        Intent intent=new Intent(ChatActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
