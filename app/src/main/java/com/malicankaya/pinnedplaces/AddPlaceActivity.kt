package com.malicankaya.pinnedplaces

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.malicankaya.pinnedplaces.databinding.ActivityAddPlaceBinding
import java.security.Permission
import java.text.SimpleDateFormat
import java.util.*

class AddPlaceActivity : AppCompatActivity(), View.OnClickListener {
    private var binding: ActivityAddPlaceBinding? = null
    private var cal = Calendar.getInstance()

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
        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressed()
        }


        openGalleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    val uri = result.data?.data
                    binding?.ivPlaceImage?.setImageURI(uri)
                    setFABImageViewSettings()
                }
            }

        openCameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    val bitmap = result?.data?.extras!!.get("data") as Bitmap
                    binding?.ivPlaceImage?.setImageBitmap(bitmap)
                    setFABImageViewSettings()
                }
            }


        binding?.etDate?.setOnClickListener(this)

        dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            setDateEditText()
        }

        binding?.tvAddImage?.setOnClickListener {
            selectImageAlertDialog()

        }

        binding?.fabCancelImage?.setOnClickListener {
            if (binding?.ivPlaceImage?.drawable != null) {
                binding?.ivPlaceImage?.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.add_screen_image_placeholder
                    )
                )
            }

            binding?.fabCancelImage?.visibility = View.INVISIBLE
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
        val format = "dd.mm.yyyy"
        val sdf = SimpleDateFormat(format, Locale.getDefault())

        binding?.etDate?.setText(sdf.format(cal.time).toString())
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
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }


}