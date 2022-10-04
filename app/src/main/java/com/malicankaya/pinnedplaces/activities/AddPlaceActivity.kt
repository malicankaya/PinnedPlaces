package com.malicankaya.pinnedplaces.activities

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
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
import kotlin.collections.ArrayList

class AddPlaceActivity : AppCompatActivity(), View.OnClickListener {
    private var binding: ActivityAddPlaceBinding? = null
    private var cal = Calendar.getInstance()
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var placeDao: PlaceDao? = null
    private var image: String = ""
    private var customDialog: Dialog? = null
    //if edit swipe
    private var placeDetails: PlaceEntity? = null

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
        if(intent.hasExtra("entityPlaceIDFromSwipe")){
            supportActionBar?.title = "Edit Place"
            setFieldForEdit(intent.getIntExtra("entityPlaceIDFromSwipe",0))
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

    private fun addRecord() {
        val title = binding?.etTitle?.text.toString()
        val description = binding?.etDescription?.text.toString()
        val date = binding?.etDate?.text.toString()
        val location = binding?.etLocation?.text.toString()

        if (title.isNullOrEmpty()
            || description.isNullOrEmpty()
            || date.isNullOrEmpty()
            || location.isNullOrEmpty()
            || image.isNullOrEmpty()
        ) {
            Toast.makeText(
                applicationContext,
                "All fields must be filled.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val place = PlaceEntity(
                title = title,
                image = image,
                description = description,
                date = date,
                location = location,
                latitude = latitude,
                longitude = longitude
            )
            showCustomDialog()
            lifecycleScope.launch {
                placeDao?.insert(place)
                runOnUiThread {
                    dismissCustomDialog()
                    Toast.makeText(applicationContext, "Record saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
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

    private fun setFieldForEdit(id: Int){
        val dao = (application as PlaceApp).db.placeDao()
        var placeDetails: PlaceEntity? = null
        lifecycleScope.launch {
            dao.getPlaceById(id).collect{
                placeDetails = it
                binding?.etTitle?.setText(placeDetails?.title)
                binding?.etDescription?.setText(placeDetails?.description)
                binding?.etDate?.setText(placeDetails?.date)
                binding?.etLocation?.setText(placeDetails?.location)
                binding?.ivPlaceImage?.setImageURI(Uri.parse(placeDetails?.image))
            }
        }
    }

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
                    image= ""
                }
                binding?.fabCancelImage?.visibility = View.INVISIBLE
            }
            R.id.btn_save -> {
                addRecord()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}