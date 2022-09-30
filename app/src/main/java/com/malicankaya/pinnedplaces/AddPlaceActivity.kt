package com.malicankaya.pinnedplaces

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
    private var readPermissions: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Permission denied for choose photo.", Toast.LENGTH_SHORT)
                    .show()
            }
        }


    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressed()
        }

        binding?.etDate?.setOnClickListener(this)

        dateSetListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            setDateEditText()
        }

        binding?.tvAddImage?.setOnClickListener {
            selectImage()
        }

    }

    private fun selectImage() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Select action")

        val pictureDialogItems = arrayOf("Select photo from Gallery", "Capture photo from Camera")
        builder.setItems(pictureDialogItems) { _, which ->
            when (which) {
                0 -> choosePhotoFromGallery()
                1 -> TODO()
            }
        }

        builder.create().show()
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
            }else{

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