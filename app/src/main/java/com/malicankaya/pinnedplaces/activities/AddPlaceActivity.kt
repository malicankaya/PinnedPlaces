package com.malicankaya.pinnedplaces.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.malicankaya.pinnedplaces.R
import com.malicankaya.pinnedplaces.database.PlaceApp
import com.malicankaya.pinnedplaces.database.PlaceDao
import com.malicankaya.pinnedplaces.databinding.ActivityAddPlaceBinding
import com.malicankaya.pinnedplaces.models.PlaceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class AddPlaceActivity : AppCompatActivity(), View.OnClickListener {
    private var binding: ActivityAddPlaceBinding? = null
    private var cal = Calendar.getInstance()
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var placeDao: PlaceDao? = null
    private var image: String = ""
    private var customDialog: Dialog? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    //for swipe
    private var isItEdit: Boolean = false
    private var placeEditID: Int = 0

    //maps api things
    private val mapsResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val place = Autocomplete.getPlaceFromIntent(data!!)

                binding?.etLocation?.setText(place.address)
                latitude = place.latLng!!.latitude
                longitude = place.latLng!!.longitude
            }
        }

    private var cameraPermission: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Permission denied for camera.", Toast.LENGTH_SHORT).show()
            }
        }
    private var openCameraLauncher: ActivityResultLauncher<Intent>? = null
    private var readPermissions: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Permission denied for choosing photo.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    private var openGalleryLauncher: ActivityResultLauncher<Intent>? = null

    @RequiresApi(Build.VERSION_CODES.N)
    private var locationPermissions: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.forEach { (t, u) ->
                if (!u) {
                    Toast.makeText(this, "Permission denied for location.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }


    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        //toolbar things
        setSupportActionBar(binding?.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //database things
        placeDao = (application as PlaceApp).db.placeDao()

        //maps api things
        if (!Places.isInitialized()) {
            Places.initialize(
                this@AddPlaceActivity,
                resources.getString(R.string.google_maps_api_key)
            )
        }

        //imageLaunchers
        openGalleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    val uri = result.data?.data
                    binding?.ivPlaceImage?.setImageURI(uri)

                    showCustomDialog()
                    lifecycleScope.launch {
                        image = saveImageToStorage(binding?.ivPlaceImage?.drawable!!.toBitmap())
                    }
                }
            }
        openCameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    val bitmap = result?.data?.extras!!.get("data") as Bitmap
                    binding?.ivPlaceImage?.setImageBitmap(bitmap)

                    lifecycleScope.launch {
                        image = saveImageToStorage(bitmap)
                    }
                }
            }
        //bindings
        binding?.etDate?.setOnClickListener(this)
        binding?.tvAddImage?.setOnClickListener(this)
        binding?.fabCancelImage?.setOnClickListener(this)
        binding?.btnSave?.setOnClickListener(this)
        binding?.etLocation?.setOnClickListener(this)
        binding?.tvSelectCurrentLocation?.setOnClickListener(this)

        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressed()
        }

        dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            setDateEditText()
        }

        //for swipe to edit
        if (intent.hasExtra("entityPlaceIDFromSwipe")) {
            isItEdit = true
            supportActionBar?.title = "Edit Place"
            placeEditID = intent.getIntExtra("entityPlaceIDFromSwipe", 0)
            setFieldsForEdit(placeEditID)
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 1000
        mLocationRequest.numUpdates = 1

        mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallBack, Looper.myLooper())
    }

    private val mLocationCallBack = object: LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult?) {
            val mLastLocation: Location = locationResult!!.lastLocation
            latitude = mLastLocation.latitude
            longitude = mLastLocation.longitude
            Log.e("latitude",""+latitude)
            Log.e("longitude",""+longitude)
        }
    }

    private fun setFABImageViewSettings() {
        if (binding?.ivPlaceImage?.drawable?.constantState != ContextCompat.getDrawable(
                this,
                R.drawable.add_screen_image_placeholder
            )?.constantState
        ) {
            binding?.fabCancelImage?.visibility = View.VISIBLE
        } else {
            binding?.fabCancelImage?.visibility = View.INVISIBLE
        }

    }

    private fun selectImageAlertDialog() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Select action")

        val pictureDialogItems = arrayOf("Select photo from Gallery", "Capture photo from Camera")
        builder.setItems(pictureDialogItems) { _, which ->
            when (which) {
                0 -> choosePhotoFromGallery()
                1 -> choosePhotoFromCamera()
            }
        }
        builder.create().show()
    }

    private fun choosePhotoFromCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED
        ) {
            cameraPermission.launch(Manifest.permission.CAMERA)
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            openCameraLauncher?.launch(cameraIntent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_DENIED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_DENIED
        ) {
            locationPermissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            requestNewLocationData()
        }
    }

    private fun isLocationEnabled(): Boolean{
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("GOTO SETTINGS") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)

                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        builder.create().show()
    }

    private fun choosePhotoFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
        ) {
            showRationaleDialog(
                "Selecting photo requires to access photos and media",
                "Selecting photo cannot be used because accessing photos and media is denied."
            )
        } else {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED
            ) {
                readPermissions.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                val galleryIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGalleryLauncher?.launch(galleryIntent)
            }
        }
    }

    private fun setDateEditText() {
        val format = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(format, Locale.getDefault())

        binding?.etDate?.setText(sdf.format(cal.time).toString())
    }

    private suspend fun saveImageToStorage(bitmap: Bitmap): String {
        var result = ""
        setFABImageViewSettings()
        withContext(Dispatchers.IO) {
            val file = File(
                externalCacheDir?.absolutePath.toString(),
                "" + UUID.randomUUID() + ".jpg"
            )

            try {
                val stream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                stream.flush()
                stream.close()

            } catch (e: IOException) {
                e.printStackTrace()
            }

            result = file.absolutePath

            runOnUiThread {
                dismissCustomDialog()
            }
        }
        return result
    }

    private fun addOrUpdateRecord() {
        val title = binding?.etTitle?.text.toString()
        val description = binding?.etDescription?.text.toString()
        val date = binding?.etDate?.text.toString()
        val location = binding?.etLocation?.text.toString()
        var place: PlaceEntity? = null

        if (title.isEmpty()
            || description.isEmpty()
            || date.isEmpty()
            || location.isEmpty()
            || image.isEmpty()
        ) {
            Toast.makeText(
                applicationContext,
                "All fields must be filled.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            if (isItEdit) {
                place = getPlaceForUpdateOrAdd(placeEditID)
                showCustomDialog()
                lifecycleScope.launch {
                    placeDao?.update(place!!)
                    runOnUiThread {
                        dismissCustomDialog()
                        Toast.makeText(applicationContext, "Record updated", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    }
                }
            } else {
                place = getPlaceForUpdateOrAdd()
                showCustomDialog()
                lifecycleScope.launch {
                    placeDao?.insert(place)
                    runOnUiThread {
                        dismissCustomDialog()
                        Toast.makeText(applicationContext, "Record saved", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    }
                }
            }
        }
    }

    private fun getPlaceForUpdateOrAdd(id: Int = 0): PlaceEntity {
        val title = binding?.etTitle?.text.toString()
        val description = binding?.etDescription?.text.toString()
        val date = binding?.etDate?.text.toString()
        val location = binding?.etLocation?.text.toString()
        return PlaceEntity(
            id = id,
            title = title,
            image = image,
            description = description,
            date = date,
            location = location,
            latitude = latitude,
            longitude = longitude
        )
    }


    private fun showCustomDialog() {
        customDialog = Dialog(this)

        customDialog?.setContentView(R.layout.dialog_custom_progress)

        customDialog?.show()
    }

    private fun dismissCustomDialog() {
        if (customDialog != null) {
            customDialog?.dismiss()
            customDialog = null
        }
    }

    private fun setFieldsForEdit(id: Int) {
        var placeDetails: PlaceEntity
        lifecycleScope.launch {
            placeDao?.getPlaceById(id)?.collect {
                placeDetails = it
                binding?.etTitle?.setText(placeDetails.title)
                binding?.etDescription?.setText(placeDetails.description)
                binding?.etDate?.setText(placeDetails.date)
                binding?.etLocation?.setText(placeDetails.location)
                binding?.ivPlaceImage?.setImageURI(Uri.parse(placeDetails.image))
                image = placeDetails.image
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.et_date -> {
                DatePickerDialog(
                    this,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            R.id.tv_add_image -> {
                selectImageAlertDialog()
            }
            R.id.fabCancelImage -> {
                if (binding?.ivPlaceImage?.drawable != null) {
                    binding?.ivPlaceImage?.setImageDrawable(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.add_screen_image_placeholder
                        )
                    )
                    image = ""
                }
                binding?.fabCancelImage?.visibility = View.INVISIBLE
            }
            R.id.btn_save -> {
                addOrUpdateRecord()
            }
            R.id.et_location -> {

                try {
                    val fields = listOf(
                        Place.Field.ID, Place.Field.NAME,
                        Place.Field.LAT_LNG, Place.Field.ADDRESS
                    )
                    val intent =
                        Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                            .build(this@AddPlaceActivity)
                    mapsResultLauncher.launch(intent)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            R.id.tv_select_current_location ->{
                if(isLocationEnabled()){
                    getCurrentLocation()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}