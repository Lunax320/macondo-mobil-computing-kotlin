package com.example.macondo.ui.panorama

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** Muestra una proyección equirectangular desde el punto de vista del visitante. */
class PanoramaView(context: Context, archivo: String) : GLSurfaceView(context) {
    private val panorama = PanoramaRenderer(context, archivo)
    private var previoX = 0f
    private var previoY = 0f

    init {
        setEGLContextClientVersion(2)
        setRenderer(panorama)
        renderMode = RENDERMODE_WHEN_DIRTY
        isClickable = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.x - previoX) / width.coerceAtLeast(1)
                val dy = (event.y - previoY) / height.coerceAtLeast(1)
                queueEvent { panorama.girar(dx, dy) }
                requestRender()
            }
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        previoX = event.x
        previoY = event.y
        return true
    }

    override fun performClick() = super.performClick()
}

private class PanoramaRenderer(private val context: Context, private val archivo: String) : GLSurfaceView.Renderer {
    private val vertices = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
        position(0)
    }
    private var program = 0
    private var textura = 0
    private var anchoRelativo = 1f
    private var giro = 0f
    private var inclinacion = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, shader(GLES20.GL_VERTEX_SHADER, VERTICE))
        GLES20.glAttachShader(program, shader(GLES20.GL_FRAGMENT_SHADER, FRAGMENTO))
        GLES20.glLinkProgram(program)
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        textura = textureIds[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textura)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        val imagen = context.assets.open(archivo).use { BitmapFactory.decodeStream(it) }
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, imagen, 0)
        imagen.recycle()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        anchoRelativo = width.toFloat() / height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textura)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "panorama"), 0)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "aspecto"), anchoRelativo)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "giro"), giro)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "inclinacion"), inclinacion)
        val posicion = GLES20.glGetAttribLocation(program, "posicion")
        GLES20.glEnableVertexAttribArray(posicion)
        GLES20.glVertexAttribPointer(posicion, 2, GLES20.GL_FLOAT, false, 0, vertices)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(posicion)
    }

    fun girar(dx: Float, dy: Float) {
        giro = (giro - dx * 2.2f) % (2f * Math.PI.toFloat())
        inclinacion = (inclinacion + dy * 1.7f).coerceIn(-1.48f, 1.48f)
    }

    private fun shader(tipo: Int, fuente: String): Int = GLES20.glCreateShader(tipo).also {
        GLES20.glShaderSource(it, fuente)
        GLES20.glCompileShader(it)
    }

    private companion object {
        const val VERTICE = """
            attribute vec2 posicion;
            varying vec2 punto;
            void main() {
                punto = posicion;
                gl_Position = vec4(posicion, 0.0, 1.0);
            }
        """
        const val FRAGMENTO = """
            precision mediump float;
            varying vec2 punto;
            uniform sampler2D panorama;
            uniform float aspecto;
            uniform float giro;
            uniform float inclinacion;
            void main() {
                vec3 r = normalize(vec3(punto.x * aspecto * 0.62, punto.y * 0.62, 1.0));
                float y = r.y * cos(inclinacion) + r.z * sin(inclinacion);
                float z = r.z * cos(inclinacion) - r.y * sin(inclinacion);
                float longitud = atan(r.x, z) + giro;
                float latitud = asin(clamp(y, -1.0, 1.0));
                vec2 uv = vec2(fract(0.5 + longitud / 6.283185), 0.5 - latitud / 3.141593);
                gl_FragColor = texture2D(panorama, uv);
            }
        """
    }
}
