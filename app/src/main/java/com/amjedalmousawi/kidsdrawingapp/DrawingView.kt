package com.amjedalmousawi.kidsdrawingapp
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View


class DrawingView(context: Context, attrbset: AttributeSet) : View(context, attrbset){

    private var mDrawPath: CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 0F
    private var color = Color.BLACK
    private var canvas: Canvas? = null
    private val mPaths = ArrayList<CustomPath>()
    private val mUndoPaths = ArrayList<CustomPath>()


    init {
        setUpDrawing()
    }

    /*
    Initializer function
     */
    private fun setUpDrawing(){
        mDrawPaint = Paint()
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mDrawPath = CustomPath(color, mBrushSize)
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        //mBrushSize = 20F
    }

    /**
     * Function that will be used to undo in the drawing.
     * Removes last path in mPaths and saves if to a variable.
     * After that invalidate the whole View to call onDraw
     */
    fun onUndoClick(){
        if(mPaths.isNotEmpty()){
            mUndoPaths.add(mPaths.removeAt(mPaths.size - 1))
            invalidate()
        }
    }

    /*
    initializes canvas with bitmap image of the size of the screen
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }

    /*
    Draws the path at start point continuously. This still has no color so this is not visible until
    mDrawPath has returned values
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas!!.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)

        // loops over the saved Paths in mPaths and draw them
        for(path in mPaths){
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas.drawPath(path,mDrawPaint!!)
        }

        // making sure that our CustomPath returns something then draws the path
        if(!mDrawPath!!.isEmpty){
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!,mDrawPaint!!)
        }
    }

    /*
    When the user actually touches the screen then we set mDrawPath to return something.
    Then in onDraw adds the color to mDrawPaint. Here path is colored too.
    ACTION_DOWN - resets the path and move it to the current point
    ACTION_MOVE - creates a line from the current point to the new point
    ACTION_UP - make the path a new path with 0 points
    invalidate - invalidate the view changes
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize
                mDrawPath!!.reset()
                mDrawPath!!.moveTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_MOVE -> {
                mDrawPath!!.lineTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_UP -> {
                // saves drawn path to an arraylist for persistence
                mPaths.add(mDrawPath!!)
                // resets the drawpath
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }

        invalidate()

        return true // important flag

    }

    // set the brush size to a size in relation to the dimension of the screen
    fun setBrushSize(newSize: Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    // change the color of the mdrawpaint
    fun setPaintColor(newColor: String){
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }


    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path(){

    }

}