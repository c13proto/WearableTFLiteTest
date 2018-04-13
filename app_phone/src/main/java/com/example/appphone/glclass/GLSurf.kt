package com.example.appphone.glclass

import android.content.Context
import android.opengl.GLSurfaceView

class GLSurf(context: Context) : GLSurfaceView(context) {

    private val mRenderer: GLRenderer

    init {

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2)

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = GLRenderer(context)
        setRenderer(mRenderer)

        // Render the view only when there is a change in the drawing data
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    override fun onPause() {
        super.onPause()
        mRenderer.onPause()
    }

    override fun onResume() {
        super.onResume()
        mRenderer.onResume()
    }

}
