package ie.hackaccess.access.accessapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import ie.hackaccess.access.accessapp.classes.Booking
import kotlinx.android.synthetic.main.activity_booking.*
import kotlinx.android.synthetic.main.content_main.*
import java.util.*

class BookingActivity : AppCompatActivity() {

    val MESSAGES_CHILD = "messages"

    var isReturn: Boolean = false

    var firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var firebaseDatabaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        supportActionBar?.setDisplayHomeAsUpEnabled(true);

        if (firebaseAuth.currentUser == null){
            finish()
        }

        firebaseUser = firebaseAuth.currentUser!!
        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference

        returnchecked.setOnClickListener { view ->
            if (isReturn) {
                returnLabel.visibility = View.GONE
            } else {
                returnLabel.visibility = View.VISIBLE
            }

            isReturn = !isReturn
        }

        returnchecked.setOnClickListener { view ->
            if (isReturn) {
                returnLabel.visibility = View.GONE
            } else {
                returnLabel.visibility = View.VISIBLE
            }

            isReturn = !isReturn
        }


        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        val hours = c.get(Calendar.HOUR)
        val minutes = c.get(Calendar.MINUTE)

        departureCalendar.setOnClickListener(View.OnClickListener {
            val dpd = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                // Display Selected date in textbox
                dateEditText.setText("" + dayOfMonth + "/" + monthOfYear + "/" + year)
            }, year, month, day)
            dpd.show()
        })

        returnCalendar.setOnClickListener(View.OnClickListener {
            val dpd = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                // Display Selected date in textbox
                returnDateEditText.setText("" + dayOfMonth + "/" + monthOfYear + "/" + year)
            }, year, month, day)
            dpd.show()
        })

        timeWatch.setOnClickListener(View.OnClickListener {
            val dpd = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { view, hours, minutes ->
                // Display Selected date in textbox
                timeEditText.setText("" + hours + " " +  "." + String.format("%02d", minutes))
                timeNextStationEditText.setText("" + hours + " " + "." + (minutes+10).toString())
            }, hours, minutes, false)
            dpd.show()
        })

        returnTimeWatch.setOnClickListener(View.OnClickListener {
            val dpd = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { view, hours, minutes ->
                // Display Selected date in textbox
                returnTimeEditText.setText("" + hours + " " +  "." + String.format("%02d", minutes))
                returnTimeNextStationEditText.setText("" + hours + " " + "." + String.format("%02d", (minutes+10)))
            }, hours, minutes, false)
            dpd.show()
        })

        invert.setOnClickListener(View.OnClickListener {
            var dep = departureEditText.text
            var arr = arrivalEditText.text
            departureEditText.setText(arr)
            arrivalEditText.setText(dep)
        })

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_booking, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_booking -> {
                sendBooking()
                return true
            }
            R.id.home -> {
                finish();
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun sendBooking() {

        val booking = Booking();
        booking.from = departureEditText.text.toString()
        booking.to = arrivalEditText.text.toString()
        booking.date = dateEditText.text.toString()
        booking.timeFrom = timeEditText.text.toString()
        booking.timeTo = timeNextStationEditText.text.toString()
        booking.user = firebaseUser.email
        booking.platform = "3"
        booking.requirements = "Ramp Access"

        val returnBooking = Booking();
        if (isReturn) {
            returnBooking.from = arrivalEditText.text.toString()
            returnBooking.to = departureEditText.text.toString()
            returnBooking.date = returnDateEditText.text.toString()
            returnBooking.timeFrom = returnTimeEditText.text.toString()
            returnBooking.timeTo = returnTimeNextStationEditText.text.toString()
            returnBooking.user = firebaseUser.email
            returnBooking.platform = "3"
            returnBooking.requirements = "Ramp Access"
        }

        firebaseDatabaseReference.child(MESSAGES_CHILD)
                .push().setValue(booking)
        if (isReturn) {
            firebaseDatabaseReference.child(MESSAGES_CHILD)
                    .push().setValue(returnBooking)
        }

        finish()
    }



}
