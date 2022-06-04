package polyrhythmmania.storymode.screen

import com.badlogic.gdx.graphics.glutils.ShaderProgram


object GaussianBlur {
    private val VERT = """
attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
attribute vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
attribute vec2 ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
uniform mat4 u_projTrans;

varying vec4 vColor;
varying vec2 vTexCoord;
void main() {
	vColor = ${ShaderProgram.COLOR_ATTRIBUTE};
	vTexCoord = ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
	gl_Position = u_projTrans * ${ShaderProgram.POSITION_ATTRIBUTE};
}
"""
    private val FRAG =
            """
#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif
varying LOWP vec4 vColor;
varying vec2 vTexCoord;

uniform sampler2D u_texture;
uniform float resolution;
uniform vec2 dir;

void main() {
    /* Slightly modified from https://github.com/Jam3/glsl-fast-gaussian-blur/blob/5dbb6e97aa43d4be9369bdd88e835f47023c5e2a/9.glsl */
    vec4 color = vec4(0.0);
    vec2 off1 = vec2(1.3846153846) * dir;
    vec2 off2 = vec2(3.2307692308) * dir;
    color += texture2D(u_texture, vTexCoord) * 0.2270270270;
    color += texture2D(u_texture, vTexCoord + (off1 / resolution)) * 0.3162162162;
    color += texture2D(u_texture, vTexCoord - (off1 / resolution)) * 0.3162162162;
    color += texture2D(u_texture, vTexCoord + (off2 / resolution)) * 0.0702702703;
    color += texture2D(u_texture, vTexCoord - (off2 / resolution)) * 0.0702702703;
    
    gl_FragColor = vColor * vec4(color.rgb, 1.0);
}
"""


    fun createShaderProgram(): ShaderProgram {
        ShaderProgram.pedantic = false
        val shader = ShaderProgram(VERT, FRAG)
        if (!shader.isCompiled) {
            error("Failed to compile Gaussian blur shader:\n${shader.log}")
        }
        return shader
    }
    
}