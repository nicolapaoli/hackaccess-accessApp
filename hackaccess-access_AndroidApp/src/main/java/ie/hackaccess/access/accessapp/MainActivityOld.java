/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ie.hackaccess.access.accessapp;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

public class MainActivityOld extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener {
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

/*

    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "messages";
    private static final int REQUEST_INVITE = 1;
    private static final int REQUEST_IMAGE = 2;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 10;
    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    private String mUsername;
    private String mPhotoUrl;
    private SharedPreferences mSharedPreferences;
    private GoogleApiClient mGoogleApiClient;
//    private static final String MESSAGE_URL = "http://friendlychat.firebase.google.com/message/";
private static final String MESSAGE_URL = "http://friendlychat-nicolapaoli-it.firebaseapp.com/message/";

    private Button mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    private ImageView mAddMessageImageView;

    // Firebase instance variables
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private DatabaseReference firebaseDatabaseReference;
    private FirebaseRecyclerAdapter<Booking, MainActivity.BookingViewHolder> firebaseAdapter;
    private FirebaseRemoteConfig firebaseRemoteConfig;
    private FirebaseAnalytics firebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Set default username is anonymous.
        mUsername = ANONYMOUS;
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null){
            //Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = firebaseUser.getDisplayName();
            if (firebaseUser.getPhotoUrl() != null){
                mPhotoUrl = firebaseUser.getPhotoUrl().toString();
            }
        }


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this *//* FragmentActivity *//*, this *//* OnConnectionFailedListener *//*)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings =
                new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(true)
                .build();

        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put("friendly_msg_length", 100L);

        firebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings);
        firebaseRemoteConfig.setDefaults(defaultConfigMap);

        fetchConfig();

        //New child entries
        firebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        SnapshotParser<Booking> parser = new SnapshotParser<FriendlyMessage>() {
            @Override
            public FriendlyMessage parseSnapshot(DataSnapshot dataSnapshot) {
                Booking friendlyMessage = dataSnapshot.getValue(Booking.class);
                if (friendlyMessage != null){
                    friendlyMessage.setId(dataSnapshot.getKey());
                }
                return friendlyMessage;
            }
        };

        DatabaseReference messagesRef = firebaseDatabaseReference.child(MESSAGES_CHILD);
        FirebaseRecyclerOptions<Booking> options = new FirebaseRecyclerOptions.Builder<Booking>()
                .setQuery(messagesRef, parser)
                .build();

        firebaseAdapter = new FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>(options) {

            @Override
            public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new MessageViewHolder(inflater.inflate(R.layout.item_message,viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(final MessageViewHolder messageViewHolder, int i, FriendlyMessage friendlyMessage) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (friendlyMessage.getText() != null){
                    //on-device index
                    FirebaseAppIndex.getInstance()
                            .update(getMessageIndexable(friendlyMessage));
                    //Log a view action on it
                    FirebaseUserActions.getInstance().end(getMessageViewAction(friendlyMessage));
                    messageViewHolder.messageTextView.setText(friendlyMessage.getText());
                    messageViewHolder.messageTextView.setVisibility(View.VISIBLE);
                    messageViewHolder.messageImageView.setVisibility(View.GONE);
                } else {
                    String imageUrl = friendlyMessage.getImageUrl();
                    if (imageUrl.startsWith("gs://")) {
                        StorageReference storageReference = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(imageUrl);
                        storageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()){
                                    String downloadUrl = task.getResult().toString();
                                    Glide.with(messageViewHolder.messageImageView.getContext())
                                            .load(downloadUrl)
                                            .into(messageViewHolder.messageImageView);
                                } else {
                                    Log.w(TAG, "Getting download url was not successful.", task.getException());
                                }
                            }
                        });
                    } else {
                        Glide.with(messageViewHolder.messageImageView.getContext())
                                .load(friendlyMessage.getImageUrl())
                                .into(messageViewHolder.messageImageView);
                    }

                    messageViewHolder.messageImageView.setVisibility(View.VISIBLE);
                    messageViewHolder.messageTextView.setVisibility(View.GONE);
                }


                messageViewHolder.messengerTextView.setText(friendlyMessage.getName());
                if (friendlyMessage.getPhotoUrl() == null){
                    messageViewHolder.messageImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_account_circle_black_36dp));
                } else {
                    Glide.with(MainActivity.this)
                            .load(friendlyMessage.getPhotoUrl())
                            .into(messageViewHolder.messageImageView);
                }
            }
        };

        firebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int count = firebaseAdapter.getItemCount();
                int lastPosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();

                if (lastPosition == -1 ||
                        (positionStart >= (count -1) &&
                            lastPosition == (positionStart - 1))){
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setAdapter(firebaseAdapter);

        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(CodelabPreferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Send messages on click.
                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(),
                        mUsername,
                        mPhotoUrl,
                        null);
                firebaseDatabaseReference.child(MESSAGES_CHILD)
                        .push().setValue(friendlyMessage);
                mMessageEditText.setText("");

                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "sent message");
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.GENERATE_LEAD, payload);
            }
        });

        mAddMessageImageView = (ImageView) findViewById(R.id.addMessageImageView);
        mAddMessageImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Select image for image message on click.
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image*//*");
                startActivityForResult(intent, REQUEST_IMAGE);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in.
        // TODO: Add code to check if user is signed in.
    }

    @Override
    public void onPause() {
        firebaseAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        firebaseAdapter.startListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.invite_menu:
                sendInvitation();
                return true;
            case R.id.fresh_config_menu:
                fetchConfig();
                return true;
            case R.id.sign_out_menu:
                firebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mUsername = ANONYMOUS;
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG,"onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                if (data != null){
                    final Uri uri = data.getData();
                    Log.d(TAG, "Uri: " + uri.toString());
                    FriendlyMessage tempMessage = new FriendlyMessage(null, mUsername,mPhotoUrl,LOADING_IMAGE_URL);
                    firebaseDatabaseReference.child(MESSAGES_CHILD).push()
                            .setValue(tempMessage, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    if (databaseError == null) {
                                        Bundle payload = new Bundle();
                                        payload.putString(FirebaseAnalytics.Param.VALUE, "sent image");
                                        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.GENERATE_LEAD, payload);

                                        String key = databaseReference.getKey();
                                        StorageReference storageReference = FirebaseStorage.getInstance()
                                                .getReference(firebaseUser.getUid())
                                                .child(key)
                                                .child(uri.getLastPathSegment());

                                        putImageInStorage(storageReference, uri, key);
                                    } else {
                                        Bundle payload = new Bundle();
                                        payload.putString(FirebaseAnalytics.Param.VALUE, "image not sent");
                                        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.GENERATE_LEAD, payload);
                                        Log.w(TAG, "Unable to write message to database.", databaseError.toException());
                                    }
                                }
                            });
                }
            }
        } else if (requestCode == REQUEST_INVITE){
            if (resultCode == RESULT_OK){
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                Log.d(TAG, "Invitations sent: " + ids.length);
            } else {
                Log.d(TAG, "Failed to send invitation.");
            }
        }
    }



    private void putImageInStorage(StorageReference storageReference, Uri uri, final String key){
        storageReference.putFile(uri).addOnCompleteListener(MainActivity.this,
                new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()){
                            FriendlyMessage friendlyMessage =
                                    new FriendlyMessage(null, mUsername, mPhotoUrl, task.getResult().getMetadata().getDownloadUrl().toString());
                            firebaseDatabaseReference.child(MESSAGES_CHILD).child(key)
                                    .setValue(friendlyMessage);
                        } else {
                            Log.w(TAG, "Image upload task was not successful.", task.getException());
                        }
                    }
                });
    }



    //REMOTE CONFIG
    public void fetchConfig(){
        long cacheExpiration = 3600;

        if (firebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()){
            cacheExpiration = 0;
        }

        firebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        firebaseRemoteConfig.activateFetched();
                        applyRetrievedLengthLimit();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error fetching config: " +
                        e.getMessage());
                        applyRetrievedLengthLimit();
                    }
                });
    }


    private void applyRetrievedLengthLimit(){
        Long length = firebaseRemoteConfig.getLong("friendly_msg_length");
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(length.intValue())});
        Log.d(TAG, "FML is: " + length);
    }


    //INDEX
    private Indexable getMessageIndexable(FriendlyMessage friendlyMessage){
        PersonBuilder sender = Indexables.personBuilder()
                .setIsSelf(mUsername.equals(friendlyMessage.getName()))
                .setName(friendlyMessage.getName())
                .setUrl(MESSAGE_URL.concat(friendlyMessage.getId() + "/sender"));

        PersonBuilder recipient = Indexables.personBuilder()
                .setName(mUsername)
                .setUrl(MESSAGE_URL.concat(friendlyMessage.getId() + "/recipient"));

        Indexable messageToIndex = Indexables.messageBuilder()
                .setName(friendlyMessage.getText())
                .setUrl(MESSAGE_URL.concat(friendlyMessage.getId()))
                .setSender(sender)
                .setRecipient(recipient)
                .build();

        return messageToIndex;
    }

    //LOG ACTION
    private Action getMessageViewAction(FriendlyMessage friendlyMessage){
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(friendlyMessage.getName(), MESSAGE_URL.concat(friendlyMessage.getId()))
                .setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
    }


    //SEND INVITATION
    private void sendInvitation(){
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

*/
}
