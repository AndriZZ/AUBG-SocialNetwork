package com.example.andri.socialnetwork;

import android.app.ProgressDialog;
import android.app.admin.SystemUpdatePolicy;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {
private Toolbar mToolbar;
private EditText userName,userProfName,userStatus,userCountry,userGender,userRelation,userDOB;
private Button UpdateAccountSettingsButton;
private CircleImageView userProfImage;

private DatabaseReference SettingsUserRef;
private FirebaseAuth mAuth;

private String currentUserId;
    final static int Gallery_Pick = 1;
    private ProgressDialog loadingBar;
    private StorageReference UserProfileImageRef;
    String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

         mAuth=FirebaseAuth.getInstance();
         currentUserId=mAuth.getCurrentUser().getUid();
         SettingsUserRef= FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);
        UserProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");
        currentUserID = mAuth.getCurrentUser().getUid();


        mToolbar=(Toolbar) findViewById(R.id.settings_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Account Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        userName=(EditText) findViewById(R.id.settings_username);
        userProfName=(EditText) findViewById(R.id.settings_profile_full_name);
        userStatus=(EditText) findViewById(R.id.settings_status);
        userCountry=(EditText) findViewById(R.id.settings_country);
        userGender=(EditText) findViewById(R.id.settings_gender);
        userRelation=(EditText) findViewById(R.id.settings_relationship_status);
        userDOB=(EditText) findViewById(R.id.settings_dob);
        userProfImage=(CircleImageView) findViewById(R.id.settings_profile_image);

        UpdateAccountSettingsButton=(Button)findViewById(R.id.update_account_settings_buttons);
        loadingBar = new ProgressDialog(this);

        SettingsUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String myProfileImage = dataSnapshot.child("profileimage").getValue().toString();
                    String myUserName = dataSnapshot.child("username").getValue().toString();
                    String myProfileName = dataSnapshot.child("fullname").getValue().toString();
                    String myProfileStatus = dataSnapshot.child("status").getValue().toString();
                    String myDOB = dataSnapshot.child("dob").getValue().toString();
                    String myCountry = dataSnapshot.child("country").getValue().toString();
                    String myGender = dataSnapshot.child("gender").getValue().toString();
                    String myRelationshipStatus = dataSnapshot.child("relationshipstatus").getValue().toString();

                    Picasso.with(SettingsActivity.this).load(myProfileImage).placeholder(R.drawable.profile).into(userProfImage);

                    userName.setText(myUserName);
                    userProfName.setText(myProfileName);
                    userStatus.setText(myProfileStatus);
                    userDOB.setText(myDOB);
                    userCountry.setText(myCountry);
                    userGender.setText(myGender);
                    userRelation.setText(myRelationshipStatus);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        UpdateAccountSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ValidateAccountInfo();
            }
        });


        userProfImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, Gallery_Pick);
            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==Gallery_Pick && resultCode==RESULT_OK && data!=null)
        {
            Uri ImageUri = data.getData();

            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
        }

        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if(resultCode == RESULT_OK)
            {
                loadingBar.setTitle("Profile Image");
                loadingBar.setMessage("Please wait, while we updating your profile image...");
                loadingBar.setCanceledOnTouchOutside(true);
                loadingBar.show();


                Uri resultUri = result.getUri();

                StorageReference filePath = UserProfileImageRef.child(currentUserID + ".jpg");

                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task)
                    {
                        if(task.isSuccessful())
                        {
                            Toast.makeText(SettingsActivity.this, "Profile Image stored successfully to Firebase storage...", Toast.LENGTH_SHORT).show();

                            final String downloadUrl = task.getResult().getDownloadUrl().toString();

                            SettingsUserRef.child("profileimage").setValue(downloadUrl)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if(task.isSuccessful())
                                            {
                                                Intent selfIntent = new Intent(SettingsActivity.this, SettingsActivity.class);
                                                startActivity(selfIntent);

                                                Toast.makeText(SettingsActivity.this, "Profile Image stored to Firebase Database Successfully...", Toast.LENGTH_SHORT).show();
                                                loadingBar.dismiss();
                                            }
                                            else
                                            {
                                                String message = task.getException().getMessage();
                                                Toast.makeText(SettingsActivity.this, "Error Occured: " + message, Toast.LENGTH_SHORT).show();
                                                loadingBar.dismiss();
                                            }
                                        }
                                    });
                        }
                    }
                });
            }
            else
            {
                Toast.makeText(this, "Error Occured: Image can not be cropped. Try Again.", Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
            }
        }
    }

    private void ValidateAccountInfo() {
    String username=userName.getText().toString();
    String profilename=userProfName.getText().toString();
    String status=userStatus.getText().toString();
    String dob=userDOB.getText().toString();
    String country=userCountry.getText().toString();
    String gender=userGender.getText().toString();
    String relation=userRelation.getText().toString();
        if(TextUtils.isEmpty(username)){
            Toast.makeText(this,"Please insert username.",Toast.LENGTH_SHORT).show();
        }
        else  if(TextUtils.isEmpty(profilename)){
            Toast.makeText(this,"Please insert profile name.",Toast.LENGTH_SHORT).show();
        }
        else  if(TextUtils.isEmpty(status)){
            Toast.makeText(this,"Please insert status.",Toast.LENGTH_SHORT).show();
        }
        else  if(TextUtils.isEmpty(dob)){
            Toast.makeText(this,"Please insert date of birth.",Toast.LENGTH_SHORT).show();
        }
        else  if(TextUtils.isEmpty(country)){
            Toast.makeText(this,"Please insert country.",Toast.LENGTH_SHORT).show();
        }
        else  if(TextUtils.isEmpty(gender)){
            Toast.makeText(this,"Please insert gender.",Toast.LENGTH_SHORT).show();
        }
        else  if(TextUtils.isEmpty(relation)){
            Toast.makeText(this,"Please insert relationship status.",Toast.LENGTH_SHORT).show();
        }
        else{
            loadingBar.setTitle("Profile Image");
            loadingBar.setMessage("Please wait, while we updating your profile image...");
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();
            UpdateAccountInfo(username,profilename,status,country,dob,gender,relation);
        }

    }

    private void UpdateAccountInfo(String username, String profilename, String status, String country, String dob, String gender, String relation) {


        HashMap userMap= new HashMap();
        userMap.put("username",username);
        userMap.put("profilename",profilename);
        userMap.put("status",status);
        userMap.put("country",country);
        userMap.put("gender",gender);
        userMap.put("dob",dob);
        userMap.put("relationshipstatus",relation);
        SettingsUserRef.updateChildren(userMap).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if(task.isSuccessful()){
                    SendUserToMainActivity();
                    Toast.makeText(SettingsActivity.this,"Account Settings have been updated successfully.",Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
                else{
                    Toast.makeText(SettingsActivity.this, "Error occured while updating information", Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }
    private void SendUserToMainActivity()
    {
        Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

}
