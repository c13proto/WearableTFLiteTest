package com.example.appphone.glclass

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.opengl.GLES20
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glTexParameteri
import android.opengl.GLUtils
import android.opengl.GLSurfaceView.Renderer
import android.opengl.Matrix
import android.util.Log
import com.example.appphone.CustomViewMediaCodec
import com.sonymobile.agent.robot.camera.CvUtils

class GLRenderer(// Misc
        internal var mContext: Context) : Renderer {

    // Our matrices
    private val mtrxProjection = FloatArray(16)
    private val mtrxView = FloatArray(16)
    private val mtrxProjectionAndView = FloatArray(16)
    lateinit var vertexBuffer: FloatBuffer
    lateinit var drawListBuffer: ShortBuffer
    lateinit var uvBuffer: FloatBuffer

    private val drawAspectRatio=3f/4//  y/x

    // Our screenresolution
    internal var mScreenWidth = 1280f
    internal var mScreenHeight = 768f
    internal var mLastTime: Long = 0
    internal var mProgram: Int = 0

    internal var mTexturename:Int=0

    init {
        mLastTime = System.currentTimeMillis() + 100
        // Initial rect
        Log.d("yama GLRenderer","init")
        drawField = RectF()
        drawField.left = 10f
        drawField.right = drawField.left+320f
        drawField.bottom = 20f
        drawField.top = drawField.bottom+240f
        CustomViewMediaCodec.onFrameChange = { nv12Buffer, width, height, pitch ->
            Log.d("yama onFrameChange","inGLRender")
            val frame=CvUtils.convertYuvToBitmap(nv12Buffer,CvUtils.YUV_NV21, width, height, pitch)
            updateTexture(mTexturename,frame)
            frame.recycle()

//            UpdateImage(CvUtils.convertI420ToBitmap(i420Buffer, width, height, pitch))


        }
    }

    fun onPause() {
        /* Do stuff to pause the renderer */
    }

    fun onResume() {
        /* Do stuff to resume the renderer */
        mLastTime = System.currentTimeMillis()
    }

    override fun onDrawFrame(unused: GL10) {

        // Get the current time
        val now = System.currentTimeMillis()

        // We should make sure we are valid and sane
        if (mLastTime > now) return

        // Get the amount of time the last frame took.
        val elapsed = now - mLastTime

        // Update our example

        // Render our example
        Render(mtrxProjectionAndView)

        // Save the current time to see how long it took :).
        mLastTime = now

    }

    private fun Render(m: FloatArray) {

        // clear Screen and Depth Buffer, we have set the clear color as black.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // get handle to vertex shader's vPosition member
        val mPositionHandle = GLES20.glGetAttribLocation(riGraphicTools.sp_Image, "vPosition")

        // Enable generic vertex attribute array
        GLES20.glEnableVertexAttribArray(mPositionHandle)

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, 3,
                GLES20.GL_FLOAT, false,
                0, vertexBuffer)

        // Get handle to texture coordinates location
        val mTexCoordLoc = GLES20.glGetAttribLocation(riGraphicTools.sp_Image, "a_texCoord")

        // Enable generic vertex attribute array
        GLES20.glEnableVertexAttribArray(mTexCoordLoc)

        // Prepare the texturecoordinates
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT,
                false,
                0, uvBuffer)

        // Get handle to shape's transformation matrix
        val mtrxhandle = GLES20.glGetUniformLocation(riGraphicTools.sp_Image, "uMVPMatrix")

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, m, 0)

        // Get handle to textures locations
        val mSamplerLoc = GLES20.glGetUniformLocation(riGraphicTools.sp_Image, "s_texture")

        // Set the sampler texture unit to 0, where we have saved the texture.
        GLES20.glUniform1i(mSamplerLoc, 0)

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer)

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mTexCoordLoc)

    }


    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {

        // We need to know the current width and height.
        mScreenWidth = width.toFloat()
        mScreenHeight = height.toFloat()

        // Redo the Viewport, making it fullscreen.
        GLES20.glViewport(0, 0, mScreenWidth.toInt(), mScreenHeight.toInt())

        // Clear our matrices
        for (i in 0..15) {
            mtrxProjection[i] = 0.0f
            mtrxView[i] = 0.0f
            mtrxProjectionAndView[i] = 0.0f
        }

        // Setup our screen width and height for normal sprite translation.
        Matrix.orthoM(mtrxProjection, 0, 0f, mScreenWidth, 0.0f, mScreenHeight, 0f, 50f)

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0)

        adjustDrawField()

    }
    private fun adjustDrawField(){
        val theta=Math.atan(drawAspectRatio.toDouble())
        val width=mScreenWidth*Math.cos(theta)
        val height=mScreenWidth*Math.sin(theta)
        drawField.left=(mScreenWidth-width.toFloat())/2
        drawField.bottom=(mScreenHeight-height.toFloat())/2
        drawField.top= drawField.bottom+height.toFloat()
        drawField.right= drawField.left+width.toFloat()
        TranslateSprite()
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {

        // Create the triangles
        SetupTriangle()
        // Create the image information
        SetupImage()

        // Set the clear color to black
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0f)

        // Create the shaders, solid color
        var vertexShader = riGraphicTools.loadShader(GLES20.GL_VERTEX_SHADER, riGraphicTools.vs_SolidColor)
        var fragmentShader = riGraphicTools.loadShader(GLES20.GL_FRAGMENT_SHADER, riGraphicTools.fs_SolidColor)

        riGraphicTools.sp_SolidColor = GLES20.glCreateProgram()             // create empty OpenGL ES Program
        GLES20.glAttachShader(riGraphicTools.sp_SolidColor, vertexShader)   // add the vertex shader to program
        GLES20.glAttachShader(riGraphicTools.sp_SolidColor, fragmentShader) // add the fragment shader to program
        GLES20.glLinkProgram(riGraphicTools.sp_SolidColor)                  // creates OpenGL ES program executables

        // Create the shaders, images
        vertexShader = riGraphicTools.loadShader(GLES20.GL_VERTEX_SHADER, riGraphicTools.vs_Image)
        fragmentShader = riGraphicTools.loadShader(GLES20.GL_FRAGMENT_SHADER, riGraphicTools.fs_Image)

        riGraphicTools.sp_Image = GLES20.glCreateProgram()             // create empty OpenGL ES Program
        GLES20.glAttachShader(riGraphicTools.sp_Image, vertexShader)   // add the vertex shader to program
        GLES20.glAttachShader(riGraphicTools.sp_Image, fragmentShader) // add the fragment shader to program
        GLES20.glLinkProgram(riGraphicTools.sp_Image)                  // creates OpenGL ES program executables


        // Set our shader programm
        GLES20.glUseProgram(riGraphicTools.sp_Image)
    }

    fun createTexture(bitmap: Bitmap): Int {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)
        glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])
        glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        updateTexture(textureHandle[0], bitmap)
        return textureHandle[0]
    }

    fun updateTexture(textureName: Int, bitmap: Bitmap) {
        glBindTexture(GLES20.GL_TEXTURE_2D, textureName)
        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    }

    fun SetupImage() {
        // Create our UV coordinates.
        uvs = floatArrayOf(
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f)

        // The texture buffer
        val bb = ByteBuffer.allocateDirect(uvs.size * 4)
        bb.order(ByteOrder.nativeOrder())
        uvBuffer = bb.asFloatBuffer()
        uvBuffer.put(uvs)
        uvBuffer.position(0)

        // Generate Textures, if more needed, alter these numbers.
        val texturenames = IntArray(1)
        GLES20.glGenTextures(1, texturenames, 0)

        // Retrieve our image from resources.
        val id = mContext.resources.getIdentifier("drawable/test", null, mContext.packageName)

        // Temporary create a bitmap
        val bmp = BitmapFactory.decodeResource(mContext.resources, id)

//        // Bind texture to texturename
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[0])
//
//        // Set filtering
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
//
//        // Load the bitmap into the bound texture.
//        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
//
        mTexturename=createTexture(bmp)
//        // We are done using the bitmap so we should recycle it.
        bmp.recycle()

    }

    fun TranslateSprite() {
        vertices = floatArrayOf(
                drawField.left,  drawField.top, 0.0f,
                drawField.left,  drawField.bottom, 0.0f,
                drawField.right, drawField.bottom, 0.0f,
                drawField.right, drawField.top, 0.0f)
        // The vertex buffer.
        val bb = ByteBuffer.allocateDirect(vertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)
    }
    fun SetupTriangle() {

//        // We have to create the vertices of our triangle.
//        vertices = floatArrayOf(
//                10.0f, 200f, 0.0f,
//                10.0f, 100f, 0.0f,
//                100f, 100f, 0.0f,
//                100f, 200f, 0.0f)
//
        indices = shortArrayOf(0, 1, 2, 0, 2, 3) // The order of vertexrendering.
//
//        // The vertex buffer.
//        val bb = ByteBuffer.allocateDirect(vertices.size * 4)
//        bb.order(ByteOrder.nativeOrder())
//        vertexBuffer = bb.asFloatBuffer()
//        vertexBuffer.put(vertices)
//        vertexBuffer.position(0)
        TranslateSprite()

        // initialize byte buffer for the draw list
        val dlb = ByteBuffer.allocateDirect(indices.size * 2)
        dlb.order(ByteOrder.nativeOrder())
        drawListBuffer = dlb.asShortBuffer()
        drawListBuffer.put(indices)
        drawListBuffer.position(0)


    }

    companion object {

        // Geometric variables
        lateinit var vertices: FloatArray
        lateinit var indices: ShortArray
        lateinit var uvs: FloatArray
        lateinit var drawField:RectF
    }
}
