package com.amjedalmousawi.kidsdrawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
@Suppress("DEPRECATION")

class MainActivity : AppCompatActivity() {

    private var mInterstitialAd: InterstitialAd? = null
    private var mCurrentSelectedPaint: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        MobileAds.initialize(this) {}

        var adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this,"ca-app-pub-5757320647359935/8983227824", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
            }

        })
        mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback(){}


        // init size of the brush
        drawing_view.setBrushSize(20F)

        ib_brush_size.setOnClickListener{
            openSetBrushSizeDialog()
        }

        //If storage is allowed we created an intent with parameters to ready for data and getting URI
        //else request permission to access the image
        ib_gallery.setOnClickListener(){
            if(isReadExternalStorageAllowed()){
                if (mInterstitialAd != null) {
                    mInterstitialAd?.show(this)
                } else {
                    Log.d("TAG", "The interstitial ad wasn't ready yet.")
                }
                val photoPickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(photoPickIntent, GALLERY_CODE)

            } else {
                requestPermission()
                Toast.makeText(this, "Permission Requested", Toast.LENGTH_SHORT).show()
            }
        }


        //Call for undo function in the Drawing View
        ib_undo.setOnClickListener{
            drawing_view.onUndoClick()
        }

        // call for saving the image to the device
        ib_save.setOnClickListener{
            if(isReadExternalStorageAllowed()){
                if (mInterstitialAd != null) {
                    mInterstitialAd?.show(this)
                } else {
                    Log.d("TAG", "The interstitial ad wasn't ready yet.")
                }
                BitmapAsyncTask(getBitmapFromView(fl_drawing_view_container)).execute()
            } else {
                requestPermission()
            }
        }

        // set pressed mode in the default seleted color palette
        // linear layout in the main activity is accessed as an ArrayList feature is from androidx.view
        mCurrentSelectedPaint = ll_pallet[1] as ImageButton
        mCurrentSelectedPaint!!.setImageResource(R.drawable.pallete_pressed)

    }

    /**
     * This function will create a bitmap image based on the passed View
     * It will also finalize the appearance of the view passed
     */
    private fun getBitmapFromView(view: View): Bitmap{
        // prepare the canvas
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawableFromView = view.background
        if (bgDrawableFromView != null){
            bgDrawableFromView.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        // finalize the view passed
        view.draw(canvas)
        // return the final bitmap processed
        return returnedBitmap
    }

    // open the brush size dialog and set the brush size
    private fun openSetBrushSizeDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Select Brush Size: ")

        val xsmallBtn = brushDialog.ib_xsmall_brush
        xsmallBtn.setOnClickListener{
            drawing_view.setBrushSize(5F)
            brushDialog.dismiss()
        }
        val smallBtn = brushDialog.ib_small_brush
        smallBtn.setOnClickListener{
            drawing_view.setBrushSize(10F)
            brushDialog.dismiss()
        }

        val xmediumBtn = brushDialog.ib_xmedium_brush
        xmediumBtn.setOnClickListener{
            drawing_view.setBrushSize(15F)
            brushDialog.dismiss()
        }

        val mediumBtn = brushDialog.ib_medium_brush
        mediumBtn.setOnClickListener{
            drawing_view.setBrushSize(20F)
            brushDialog.dismiss()
        }
        val xlargeBtn = brushDialog.ib_xlarge_brush
        xlargeBtn.setOnClickListener{
            drawing_view.setBrushSize(25F)
            brushDialog.dismiss()
        }

        val largeBtn = brushDialog.ib_large_brush
        largeBtn.setOnClickListener{
            drawing_view.setBrushSize(30F)
            brushDialog.dismiss()
        }

        brushDialog.show()
    }


    /*
    set on onClick of each ImageButton for palette
    automatically a view is passed in the process
     */
    fun onPaletteClick(view: View){
        val imageButton = view as ImageButton
        if (imageButton != mCurrentSelectedPaint){
            drawing_view.setPaintColor(imageButton.tag.toString())
            imageButton.setImageResource(R.drawable.pallete_pressed)
            mCurrentSelectedPaint!!.setImageResource(R.drawable.pallete_normal)
            mCurrentSelectedPaint = imageButton
        }
    }

    /**
     * Function to help in getting storage permission
     */
    private fun requestPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){

            Toast.makeText(this, "Need permission to add a Background", Toast.LENGTH_SHORT).show()

        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE)
    }

    /**
     * Function to check if the storage is allowed.
     */
    private fun isReadExternalStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        /**
         * If gallery_code is received
         */
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == GALLERY_CODE){
                try{
                    if(data!!.data != null){
                        iv_background_image.visibility = View.VISIBLE
                        iv_background_image.setImageURI(data!!.data)
                    } else {
                        Toast.makeText(this, "Image is not supported or is corrupted", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == STORAGE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "You can now select an image from the gallery", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission is required to select an image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Inner async task class for saving the Bitmap
     */
    private inner class BitmapAsyncTask(val mBitmap: Bitmap): AsyncTask<Any, Void, String>(){

        private lateinit var mDialog: Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        /**
         * Function that compresses the bitmap passed into a byte.
         * After that a file is then created and the byte bitmap is then written to it.
         * Then the file is written.
         */
        override fun doInBackground(vararg params: Any?): String {
            var result = ""
            if(mBitmap != null){
                try{
                    val bitmapInBytesOutputStream = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 100, bitmapInBytesOutputStream)
                    val file = File(externalCacheDir!!.absoluteFile.toString()
                            + File.separator + "KidsDrawingApp_"
                            + System.currentTimeMillis() / 1000
                            + ".png")
                    val fos = FileOutputStream(file)
                    fos.write(bitmapInBytesOutputStream.toByteArray())
                    fos.close()
                    result = file.absolutePath

                } catch (e: Exception){
                    e.printStackTrace()
                    result = ""
                }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            closeProgressDialog()
            if(result!!.isNotEmpty()){
                Toast.makeText(
                    this@MainActivity,
                    "File saved successfully: $result",
                    Toast.LENGTH_SHORT
                ).show()

                showShareFile(result)

            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Something went wrong while saving the file.",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

        /**
         * Function to show the share prompt from the Android system.
         * This should be used with the URI of an image/png type file
         */
        private fun showShareFile(fileURI: String){
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(fileURI), null){
                    path, uri -> val sharingIntent = Intent()
                sharingIntent.action = Intent.ACTION_SEND
                sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
                sharingIntent.type = "image/png"

                startActivity(Intent.createChooser(sharingIntent, "Share"))
            }
        }

        private fun showProgressDialog(){
            mDialog = Dialog(this@MainActivity)
            mDialog.setContentView(R.layout.dialog_saving_image)
            mDialog.show()
        }

        private fun closeProgressDialog(){
            mDialog.dismiss()
        }
    }

    /**
     * Companion object for the constant variables
     */
    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY_CODE = 1
    }
}

