package ie.hackaccess.access.accessapp

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import ie.hackaccess.access.accessapp.classes.Booking
import ie.hackaccess.access.accessapp.firebase.SignInActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.item_bookings.view.*
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.OnConnectionFailedListener{
    private val TAG = "MainActivity"
    val MESSAGES_CHILD = "messages"
    val REQUEST_INVITE = 1
    val REQUEST_IMAGE = 2

    val ANONYMOUS = "anonymous"
    private var mSharedPreferences: SharedPreferences? = null
    private var mUsername: String? = null
    private var mPhotoUrl: String? = null

    var firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var firebaseDatabaseReference: DatabaseReference
    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var firebaseAdapter: FirebaseRecyclerAdapter<Booking, BookingViewHolder>

    private lateinit var mLinearLayoutManager: LinearLayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            startActivity(Intent(this,BookingActivity::class.java))
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // Set default username is anonymous.
        mUsername = ANONYMOUS

        firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth.currentUser == null) {
            //Not signed in, launch the Sign In activity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        } else {
            firebaseUser = firebaseAuth.currentUser!!
            mUsername = firebaseUser.displayName
            if (firebaseUser.photoUrl != null) {
                mPhotoUrl = firebaseUser.photoUrl!!.toString()
            }

            //userPicTextView.setImageURI(mPhotoUrl)
            //emailTextView.setText(firebaseUser.email)
            //usernameTextView.setText(firebaseUser.displayName)

        }

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build()

        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val firebaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(true)
                .build()

        val defaultConfigMap = HashMap<String, Any>()
        defaultConfigMap.put("friendly_msg_length", 100L)

        firebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings)
        firebaseRemoteConfig.setDefaults(defaultConfigMap)

        fetchConfig()

        //New child entries
        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference
        val parser = SnapshotParser<Booking> { dataSnapshot ->
            val booking = dataSnapshot.getValue(Booking::class.java)
            booking?.key = dataSnapshot.key
            booking
        }

        val messagesRef = firebaseDatabaseReference.child(MESSAGES_CHILD).orderByChild("user").equalTo(firebaseUser.email)
        val options = FirebaseRecyclerOptions.Builder<Booking>()
                .setQuery(messagesRef, parser)
                .build()

        firebaseAdapter = object : FirebaseRecyclerAdapter<Booking, BookingViewHolder>(options) {

            override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): BookingViewHolder {
                val inflater = LayoutInflater.from(viewGroup.context)
                return BookingViewHolder(inflater.inflate(R.layout.item_bookings, viewGroup, false))
            }

            override fun onBindViewHolder(bookingViewHolder: BookingViewHolder, i: Int, booking: Booking) {
                progressBar.visibility = ProgressBar.INVISIBLE

                bookingViewHolder.date.text = booking.date
                bookingViewHolder.departure.text = booking.from
                bookingViewHolder.arrival.text = booking.to
                bookingViewHolder.timeArr.text = booking.timeTo
                bookingViewHolder.timeDep.text = booking.timeFrom
            }
        }

        mLinearLayoutManager = LinearLayoutManager(this)
        mLinearLayoutManager.stackFromEnd = true
        bookingRecycleView.layoutManager = mLinearLayoutManager
        var dividerItemDecoration = DividerItemDecoration(this, 1)
        dividerItemDecoration.setDrawable(resources.getDrawable(R.drawable.divider))
        bookingRecycleView.addItemDecoration(dividerItemDecoration);
        firebaseAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val count = firebaseAdapter.itemCount
                val lastPosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition()

                if (lastPosition == -1 || positionStart >= count - 1 && lastPosition == positionStart - 1) {
                    bookingRecycleView.scrollToPosition(positionStart)
                }
            }
        })

        bookingRecycleView.adapter = firebaseAdapter

    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }



    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sign_out_menu -> {
                firebaseAuth.signOut()
                Auth.GoogleSignInApi.signOut(mGoogleApiClient)
                mUsername = ANONYMOUS
                startActivity(Intent (this, SignInActivity::class.java))
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    public override fun onStart() {
        super.onStart()
        // Check if user is signed in.
        // TODO: Add code to check if user is signed in.
    }

    public override fun onPause() {
        firebaseAdapter.stopListening()
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        firebaseAdapter.startListening()
    }

    public override fun onDestroy() {
        super.onDestroy()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult)
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val uri = data.data
                    Log.d(TAG, "Uri: " + uri.toString())
                    val tempMessage = Booking()
                    firebaseDatabaseReference.child(MESSAGES_CHILD).push()
                            .setValue(tempMessage, object : DatabaseReference.CompletionListener {
                                override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
                                    if (databaseError == null) {
                                        val key = databaseReference.key
                                        val storageReference = FirebaseStorage.getInstance()
                                                .getReference(firebaseUser.uid)
                                                .child(key)
                                                .child(uri.lastPathSegment)
                                    } else {
                                        val payload = Bundle()
                                        Log.w(TAG, "Unable to write message to database.", databaseError.toException())
                                    }
                                }
                            })
                }
            }
        } else if (requestCode == REQUEST_INVITE) {
            if (resultCode == Activity.RESULT_OK) {
                val ids = AppInviteInvitation.getInvitationIds(resultCode, data)
                Log.d(TAG, "Invitations sent: " + ids.size)
            } else {
                Log.d(TAG, "Failed to send invitation.")
            }
        }
    }


    //REMOTE CONFIG
    fun fetchConfig() {
        var cacheExpiration: Long = 3600

        if (firebaseRemoteConfig.info.configSettings.isDeveloperModeEnabled) {
            cacheExpiration = 0
        }

        firebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener { firebaseRemoteConfig.activateFetched() }
                .addOnFailureListener { e -> Log.w(TAG, "Error fetching config: " + e.message) }
    }




    class BookingViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        internal var date: TextView
        internal var departure: TextView
        internal var arrival: TextView
        internal var timeDep: TextView
        internal var timeArr: TextView

        init {
            date = itemView.dateTextView as TextView
            departure = itemView.departureTextView as TextView
            arrival = itemView.arrivalTextView as TextView
            timeDep = itemView.timeDepTextView as TextView
            timeArr = itemView.timeArrTextView as TextView
        }
    }

}
