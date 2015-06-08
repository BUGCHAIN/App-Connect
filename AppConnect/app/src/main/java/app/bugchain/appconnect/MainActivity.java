package app.bugchain.appconnect;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.ProfilePictureView;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;

import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends FragmentActivity {

    private static final String PERMISSION = "publish_actions";


    private static final String PENDING_ACTION_BUNDLE_KEY = "app.bugchain.appconnect.PendingAction";

    private Button postStatusUpdateButton;
    private Button postPhotoButton;
    private ProfilePictureView profilePictureView;
    private TextView greeting;
    private PendingAction pendingAction = PendingAction.NONE;
    private boolean canPresentShareDialog;
    private boolean canPresentShareDialogWithPhoto;
    private CallbackManager callbackManager;
    private ProfileTracker profileTracker;
    private ShareDialog shareDialog;

    private FacebookCallback<Sharer.Result> shareCallback= new FacebookCallback<Sharer.Result>() {
        @Override
        public void onSuccess(Sharer.Result result) {
            Log.d("AppConnect","Success!");
            if(result.getPostId() != null){
                String title = getResources().getString(R.string.success);
                String id = result.getPostId();
                String alertMessage = getResources().getString(R.string.successfully_posted_post,id);
                showResult(title,alertMessage);
            }
        }

        @Override
        public void onCancel() {
            Log.d("AppConnect","Canceled");
        }

        @Override
        public void onError(FacebookException e) {
            Log.d("AppConnect",String.format("Error: %s",e.toString()));
            String title = getResources().getString(R.string.error);
            String alertMessage = e.getMessage();
            showResult(title,alertMessage);
        }

        private void showResult(String title,String alertMessage){
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(title)
                    .setMessage(alertMessage)
                    .setPositiveButton(R.string.ok,null)
                    .show();
        }
    };

    private enum PendingAction{
        NONE,
        POST_PHOTO,
        POST_STATUS_UPDATE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(this.getApplicationContext());

        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        handlePendingAction();
                        updateUI();
                    }

                    @Override
                    public void onCancel() {
                        if (pendingAction != PendingAction.NONE) {
                            showAlert();
                            pendingAction = PendingAction.NONE;
                        }
                        updateUI();
                    }

                    @Override
                    public void onError(FacebookException e) {
                        if (pendingAction != PendingAction.NONE
                                && e instanceof FacebookAuthorizationException) {
                            showAlert();
                            pendingAction = PendingAction.NONE;
                        }
                        updateUI();
                    }

                    private void showAlert() {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.cancelled)
                                .setMessage(R.string.permission_not_granted)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    }
                });

        shareDialog = new ShareDialog(this);
        shareDialog.registerCallback(callbackManager,shareCallback);

        if(savedInstanceState != null){
            String name = savedInstanceState.getString(PENDING_ACTION_BUNDLE_KEY);
            pendingAction = PendingAction.valueOf(name);
        }

        setContentView(R.layout.activity_main);

        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile profile, Profile profile1) {
                updateUI();
                // It's possible that we were waiting for Profile to be populated in order to
                // post a status update.
                handlePendingAction();
            }
        };

        profilePictureView = (ProfilePictureView)findViewById(R.id.profilePicture);
        greeting = (TextView)findViewById(R.id.greeting);

        postStatusUpdateButton = (Button)findViewById(R.id.postStatusUpdateButton);
        postStatusUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickPostStatusUpdate();
            }
        });

        postPhotoButton = (Button)findViewById(R.id.postPhotoButton);
        postPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickPostPhoto();
            }
        });

        // Can we present the share dialog for regular links?
        canPresentShareDialog = ShareDialog.canShow(ShareLinkContent.class);
        // Can we present the share dialog for photos?
        canPresentShareDialogWithPhoto = ShareDialog.canShow(SharePhotoContent.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Logs 'install' and activate app Events
        AppEventsLogger.activateApp(this);
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Logs 'app deactivate App Event'
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PENDING_ACTION_BUNDLE_KEY,pendingAction.name());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        profileTracker.startTracking();
    }

    // Update UI
    private void updateUI(){
        boolean enableButtons = AccessToken.getCurrentAccessToken() != null;

        postStatusUpdateButton.setEnabled(enableButtons || canPresentShareDialog);
        postPhotoButton.setEnabled(enableButtons || canPresentShareDialogWithPhoto);

        Profile profile = Profile.getCurrentProfile();
        if(enableButtons && profile != null){
            profilePictureView.setProfileId(profile.getId());
            greeting.setText(getResources().getString(R.string.hello_user,profile.getFirstName()));
        }else{
            profilePictureView.setProfileId(null);
            greeting.setText(null);
        }
    }// Update UI

    private void handlePendingAction(){
        PendingAction previouslyPendingAction = pendingAction;
        // These actions may re-set pendingAction if they are still pending, but we assume they
        // will succeed.
        pendingAction = PendingAction.NONE;

        switch (previouslyPendingAction){
            case NONE:

                break;
            case POST_PHOTO:
                    postPhoto();
                break;
            case POST_STATUS_UPDATE:
                    postStatusUpdate();
                break;
        }
    }

    private void onClickPostStatusUpdate(){
        performPublish(PendingAction.POST_STATUS_UPDATE,canPresentShareDialog);
    }

    private void postStatusUpdate(){
        Profile profile = Profile.getCurrentProfile();
        ShareLinkContent linkContent = new ShareLinkContent.Builder()
                .setContentTitle("Hello facebook")
                .setContentDescription("The 'AppConnect' sample showcases simple Facebook integretion")
                .setContentUrl(Uri.parse("http://developers.facebook.com/docs/android"))
                .build();


        if(canPresentShareDialog){
            shareDialog.show(linkContent);
        }else if(profile != null && hasPublishPermission()){
            ShareApi.share(linkContent,shareCallback);
        }else{
            pendingAction = PendingAction.POST_STATUS_UPDATE;
        }
    }

    private void onClickPostPhoto(){
        performPublish(PendingAction.POST_PHOTO,canPresentShareDialogWithPhoto);
    }

    private void postPhoto(){
        Bitmap image = BitmapFactory.decodeResource(this.getResources(),R.drawable.icon);
        SharePhoto sharePhoto = new SharePhoto.Builder().setBitmap(image).build();
        ArrayList<SharePhoto> photos = new ArrayList<>();
        photos.add(sharePhoto);

        SharePhotoContent sharePhotoContent = new SharePhotoContent.Builder().setPhotos(photos).build();
        if(canPresentShareDialogWithPhoto){
            shareDialog.show(sharePhotoContent);
        }else if(hasPublishPermission()){
            ShareApi.share(sharePhotoContent,shareCallback);
        }else{
            pendingAction = PendingAction.POST_PHOTO;
        }
    }// end post photo

    private boolean hasPublishPermission(){
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null && accessToken.getPermissions().contains(PERMISSION);
    }

    private void performPublish(PendingAction action,boolean allowNoToken){
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if(accessToken != null){
            pendingAction = action;
            if(hasPublishPermission()){
                // We can do the action right away.
                handlePendingAction();
                return;
            }else {
                // We need to get new permissions,  then complete the action when we get called back.
                LoginManager.getInstance().logInWithPublishPermissions(this,
                        Arrays.asList(PERMISSION));
            }
        }

        if(allowNoToken){
            pendingAction = action;
            handlePendingAction();
        }
    }// end permission publish

}// end main class
