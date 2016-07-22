package com.example.vaibhav.chatapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.vaibhav.chatapp.POJO.ChatHelper;
import com.example.vaibhav.chatapp.POJO.ReferenceClass;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends Activity {
    private static final String TAG="RegisterActivity";
    private EditText mUserFirstNameRegister;
    private EditText mUserEmailRegister;
    private EditText mUserPassWordRegister;
    private Button mRegisterButton;
    private Button mCancelRegister;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth=FirebaseAuth.getInstance();
        mUserFirstNameRegister=(EditText)findViewById(R.id.userFirstNameRegister);
        mUserEmailRegister=(EditText)findViewById(R.id.userEmailRegister);
        mUserPassWordRegister=(EditText)findViewById(R.id.passWordRegister);
        mRegisterButton=(Button)findViewById(R.id.registerButton);
        mCancelRegister=(Button)findViewById(R.id.cancelRegisterButton);
        mAuthListener=new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user!=null) {
                    //user signed in
                    Log.d(TAG,"onAuthStateChanged:signed_in: "+user.getUid());
                }
                else {
                    //user is signed out
                    Log.d(TAG,"onAuthStateChanged:signed_out");
                }
            }
        };
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userFirstName=mUserFirstNameRegister.getText().toString();
                String userEmail=mUserEmailRegister.getText().toString();
                String userPassword=mUserPassWordRegister.getText().toString();
                userFirstName=userFirstName.trim();
                userEmail=userEmail.trim();
                userPassword=userPassword.trim();
                if(userFirstName.isEmpty()||userEmail.isEmpty()||userPassword.isEmpty()) {
                    showErrorMessageToUser(getString(R.string.register_error_message));
                }
                else {
                    if(!emailValid(userEmail)) {
                        showErrorMessageToUser("Email not valid");
                    }
                    else {
                        final String finalUserEmail = userEmail;
                        final String finalUserPassword = userPassword;
                        final String finalUserFirstName = userFirstName;
                        mAuth.createUserWithEmailAndPassword(userEmail,userPassword)
                                .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());
                                        if(!task.isSuccessful()) {
                                            Log.w(TAG,"signInWithEmail",task.getException());
                                            showErrorMessageToUser("An error occurred!");
                                            finish();
                                        }
                                        else {
                                            Toast.makeText(RegisterActivity.this, "Successfully Registered, Logging in now.", Toast.LENGTH_SHORT).show();
                                            //storing necessary user data
                                            FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
                                            if(user!=null) {
                                                Map<String, Object> map = new HashMap<String, Object>();
                                                map.put(ReferenceClass.KEY_PROVIDER,user.getProviderId());
                                                map.put(ReferenceClass.KEY_FIRST_NAME,finalUserFirstName);
                                                map.put(ReferenceClass.KEY_USER_EMAIL,user.getEmail());
                                                map.put(ReferenceClass.CHILD_CONNECTION, ReferenceClass.KEY_ONLINE);
                                                map.put(ReferenceClass.KEY_AVATAR_ID, ChatHelper.generateRandomAvatarForUser());

                                                long createTime=new Date().getTime();
                                                map.put(ReferenceClass.KEY_TIMESTAMP,String.valueOf(createTime));
                                                mDatabase= FirebaseDatabase.getInstance().getReference();
                                                mDatabase.child(ReferenceClass.CHILD_USERS).child(user.getUid()).setValue(map);
                                                //after storing data, going to MainActivity
                                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                            }
                                            else {
                                                Toast.makeText(RegisterActivity.this, "SignInResult:user=null", Toast.LENGTH_SHORT).show();
                                                Log.w(TAG,"afterRegisterAndLogIn:user=null");
                                            }
                                        }
                                    }
                                });
                    }
                }
            }
        });
        mCancelRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }
    private void showErrorMessageToUser(String errorMessage){
        //Create an AlertDialog to show error message
        AlertDialog.Builder builder=new AlertDialog.Builder(RegisterActivity.this);
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
        if(mAuthListener!=null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
