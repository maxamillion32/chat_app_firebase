package com.example.vaibhav.chatapp;

//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LogInActivity extends Activity {
    private EditText mUserEmail;
    private EditText mUserPassWord;
    private Button mLoginToMChat;
    private Button mRegisterUser;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private static final String TAG="LogInActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        //this.getActionBar().hide();
        mUserEmail= (EditText) findViewById(R.id.userEmailChat);
        mUserPassWord =(EditText)findViewById(R.id.passWordChat);
        mLoginToMChat =(Button)findViewById(R.id.btn_LogInChat);
        mRegisterUser=(Button)findViewById(R.id.registerUser);
        mAuth=FirebaseAuth.getInstance();
        mAuthListener=new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user=firebaseAuth.getCurrentUser();
                if(user!=null) {
                    //user signed in
                    Log.d(TAG,"onAuthStateChanged:signed_in:"+user.getUid());
                }
                else {
                    //user signed out
                    Log.d(TAG,"onAuthStateChanged:signed_out");
                }
            }
        };
        mLoginToMChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userName = mUserEmail.getText().toString();
                String passWord = mUserPassWord.getText().toString();
                userName = userName.trim();
                passWord = passWord.trim();
                if(userName.isEmpty() || passWord.isEmpty()) {
                    showErrorMessageToUser(getString(R.string.login_error_message));
                }
                else {
                    if(!emailValid(userName)) {
                        showErrorMessageToUser("Email not valid");
                    }
                    else {
                        mAuth.signInWithEmailAndPassword(userName,passWord)
                                .addOnCompleteListener(LogInActivity.this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        //Log.d(TAG,"signInWithEmail:onComplete:"+task.isSuccessful());
                                        if(!task.isSuccessful()) {
                                            //Log.w(TAG,"signInWithEmail",task.getException());
                                            showErrorMessageToUser("Authentication Failed!");
                                        }
                                        else {
                                            Intent intent=new Intent(LogInActivity.this, MainActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                        }
                                    }
                                });
                    }
                }
            }
        });
        mRegisterUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(LogInActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
    private void showErrorMessageToUser(String errorMessage){
        // Create an AlertDialog to show error message
        AlertDialog.Builder builder=new AlertDialog.Builder(LogInActivity.this);
        builder.setMessage(errorMessage)
                .setTitle(getString(R.string.login_error_title))
                .setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog=builder.create();
        dialog.show();
    }
    private static boolean emailValid(String mail) {
        Pattern pattern=Pattern.compile("^([a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})$");
        Matcher matcher=pattern.matcher(mail);
        return matcher.matches();
    }
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
