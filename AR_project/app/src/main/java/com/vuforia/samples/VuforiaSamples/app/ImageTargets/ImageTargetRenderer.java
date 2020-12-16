/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/

package com.vuforia.samples.VuforiaSamples.app.ImageTargets;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.widget.Switch;

import com.vuforia.Device;
import com.vuforia.Matrix44F;
import com.vuforia.MultiTargetResult;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;
import com.vuforia.samples.SampleApplication.SampleAppRenderer;
import com.vuforia.samples.SampleApplication.SampleAppRendererControl;
import com.vuforia.samples.SampleApplication.SampleApplicationSession;
import com.vuforia.samples.SampleApplication.utils.CubeObject;
import com.vuforia.samples.SampleApplication.utils.CubeShaders;
import com.vuforia.samples.SampleApplication.utils.LoadingDialogHandler;
import com.vuforia.samples.SampleApplication.utils.MeshObject;
import com.vuforia.samples.SampleApplication.utils.SampleApplication3DModel;
import com.vuforia.samples.SampleApplication.utils.SampleUtils;
import com.vuforia.samples.SampleApplication.utils.Targets;
import com.vuforia.samples.SampleApplication.utils.Teapot;
import com.vuforia.samples.SampleApplication.utils.Texture;
import com.vuforia.samples.VuforiaSamples.R;

import static android.R.attr.src;
import static android.R.attr.value;


// The renderer class for the ImageTargets sample.
public class ImageTargetRenderer implements GLSurfaceView.Renderer, SampleAppRendererControl
{
    private static final String LOGTAG = "ImageTargetRenderer";

    private SampleApplicationSession vuforiaAppSession;
    private ImageTargets mActivity;
    private SampleAppRenderer mSampleAppRenderer;

    private Vector<Texture> mTextures;


    private double prevTime;
    private float rotateAngle;

    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;

    private CubeObject mCubeObject;
    private Teapot mTeapot;
    private Targets mTarget;

    private float kBuildingScale = 0.012f;
    private SampleApplication3DModel mBuildingsModel;

    private boolean mIsActive = false;
    private boolean mModelIsLoaded = false;

    private static  float OBJECT_SCALE_FLOAT = 0.003f;

//    MediaPlayer mediaPlayer = new MediaPlayer();
//    boolean sound = true;


    public ImageTargetRenderer(ImageTargets activity, SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;
        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivity, Device.MODE.MODE_AR, false, 0.01f , 5f);
    }


    // Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;

        // Call our function to render content from SampleAppRenderer class
        mSampleAppRenderer.render();
    }


    public void setActive(boolean active)
    {
        mIsActive = active;

        if(mIsActive)
            mSampleAppRenderer.configureVideoBackground();
    }


    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");

        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();

        mSampleAppRenderer.onSurfaceCreated();
    }


    // Called when the surface changed size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");

        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mSampleAppRenderer.onConfigurationChanged(mIsActive);

        State state;
        state = TrackerManager.getInstance().getStateUpdater().updateState();
        initRendering();
    }


    // Function for initializing the renderer.
    private void initRendering()
    {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);

        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, t.mData);
        }

        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
            CubeShaders.CUBE_MESH_VERTEX_SHADER,
            CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "texSampler2D");


        if(!mModelIsLoaded) {
            mTeapot = new Teapot();
            mCubeObject = new CubeObject() ;


            try {
                mBuildingsModel = new SampleApplication3DModel();
                mBuildingsModel.loadModel(mActivity.getResources().getAssets(),
                        "ImageTargets/Buildings.txt");
                mModelIsLoaded = true;
            } catch (IOException e) {
                Log.e(LOGTAG, "Unable to load buildings");
            }

            // Hide the Loading Dialog
            mActivity.loadingDialogHandler
                    .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        }

    }

    public void updateConfiguration()
    {
        mSampleAppRenderer.onConfigurationChanged(mIsActive);
    }

    // The render function called from SampleAppRendering by using RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling it's lifecycle.
    // State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix)
    {
        // Renders video background replacing Renderer.DrawVideoBackground()
        mSampleAppRenderer.renderVideoBackground();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);




        HashMap<Integer,Targets> tmap =new HashMap<Integer, Targets>();
        tmap.put(0,new Targets(0.003f,mTeapot,true));
        tmap.put(4,new Targets(0.03f,mCubeObject,true));
        tmap.put(5,new Targets(0.03f,mCubeObject,true));
        tmap.put(6,new Targets(0.03f,mCubeObject,true));
        tmap.put(7,new Targets(0.03f,mCubeObject));



        // Did we find any trackables this frame?
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
            TrackableResult result = state.getTrackableResult(tIdx);
            Trackable trackable = result.getTrackable();
            printUserData(trackable);
            Matrix44F modelViewMatrix_Vuforia = Tool
                    .convertPose2GLMatrix(result.getPose());
            float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();

           // int textureIndex = trackable.getName().equalsIgnoreCase("stones") ? 0
               // : 4;
            int textureIndex;
           // textureIndex = trackable.getName().equalsIgnoreCase("tarmac") ? 2
               //     : textureIndex;
            String _name =trackable.getName();
//            switch (_name){
//                case "stones":
//                    textureIndex=0;
//                    break;
//                case "chips":
//                    textureIndex=4;
//                    break;
//                case "tarmac":
//                    textureIndex=5;
//                    break;
//                case "cross":
//                    textureIndex=6;
//                    break;
//                case "colorful_daisies-wallpaper-1920x1080":
//                    textureIndex=7;
//                    break;
//            }
           textureIndex= setImagescortoId(_name);
//            HashMap<Integer, String> hmap = new HashMap<Integer, String>();
//            hmap.put(0,"stones");
//            hmap.put(4,"chips");
//            hmap.put(5,"tarmac");
//            hmap.put(6,"cross");
//            hmap.put(7,"colorful_daisies-wallpaper-1920x1080");
//            if(hmap.containsValue(_name)){
//                for (Map.Entry<Integer,String> e : hmap.entrySet()) {
//                    Integer key = e.getKey();
//                    Object value2 = e.getValue();
//                    if ((value2.toString()).equalsIgnoreCase(_name))
//                    {
//                       textureIndex=key;
//                    }
//                }
//            }


//            textureIndex = trackable.getName().equalsIgnoreCase("cross") ? 5
//                    :textureIndex ;
//            textureIndex = trackable.getName().equalsIgnoreCase("all") ? 2

//                    :textureIndex ;

            mTarget=tmap.get(textureIndex);



            // deal with the modelview and projection matrices
            float[] modelViewProjection = new float[16];



            if (!mActivity.isExtendedTrackingActive()) {
                if (mTarget.animated){
                animateBowl(modelViewMatrix);
                Matrix.translateM(modelViewMatrix, 0, 0.18f, -0.50f * 0.12f,
                        0.00135f * 0.12f);
                Matrix.rotateM(modelViewMatrix, 0, -90.0f, 1.0f, 0, 0);

                Matrix.scaleM(modelViewMatrix, 0, mTarget.objectScale, mTarget.objectScale,
                        mTarget.objectScale);}
                else{

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        mTarget.objectScale);
                Matrix.scaleM(modelViewMatrix, 0, mTarget.objectScale,
                        mTarget.objectScale, mTarget.objectScale);}
            } else {
                Matrix.rotateM(modelViewMatrix, 0, 90.0f, 1.0f, 0, 0);
                Matrix.scaleM(modelViewMatrix, 0, kBuildingScale,
                        kBuildingScale, kBuildingScale);
            }
           //objectScaleFloatAdjust(mTarget.objectScale,modelViewMatrix);

//            switch (textureIndex) {
//                case 0:
//                    OBJECT_SCALE_FLOAT = 0.003f;
//                    objectScaleFloatAdjust(OBJECT_SCALE_FLOAT,modelViewMatrix);
//                    break;
//                case 2:
//                    OBJECT_SCALE_FLOAT = 0.003f;
//                    objectScaleFloatAdjust(OBJECT_SCALE_FLOAT,modelViewMatrix);
//                    break;
//                case 3:
//                    OBJECT_SCALE_FLOAT = 0.003f;
//                    objectScaleFloatAdjust(OBJECT_SCALE_FLOAT,modelViewMatrix);
//                    break;
//                case 4:
//                    OBJECT_SCALE_FLOAT = 0.03f;
//                    objectScaleFloatAdjust(OBJECT_SCALE_FLOAT,modelViewMatrix);
//                    break;
//                case 5:
//                    OBJECT_SCALE_FLOAT = 0.03f;
//                    objectScaleFloatAdjust(OBJECT_SCALE_FLOAT,modelViewMatrix);
//                    break;
//                case 6:
//                    OBJECT_SCALE_FLOAT = 0.3f;
//                    objectScaleFloatAdjust(OBJECT_SCALE_FLOAT,modelViewMatrix);
//                    break;
//                case 7:
//                    OBJECT_SCALE_FLOAT = 0.003f;
//                    objectScaleFloatAdjust(OBJECT_SCALE_FLOAT,modelViewMatrix);
//                    break;
//            }
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);

                     // activate the shader program and bind the vertex/normal/tex coords
            GLES20.glUseProgram(shaderProgramID);

            if (!mActivity.isExtendedTrackingActive()) {



                  render_model_context(mTarget.itsModel,modelViewProjection,textureIndex);
                  //mTarget.playMusic();
//
//                switch (textureIndex) {
//                    case 0:
//                        render_model_context(mTeapot,modelViewProjection,textureIndex);
                    if (mTarget.used) {
//
                        mTarget.playMusic();
                        mTarget.used=false;
                    }

//
//                        break;
//                    case 2:
//                        render_model_context(mTeapot,modelViewProjection,textureIndex);
//                        mediaPlayer.stop();
//                        mediaPlayer=null;
//                        sound=true;
//                        break;
//                    case 3:
//                        render_model_context(mTeapot,modelViewProjection,textureIndex);
//                        break;
//                    case 4:
//                        render_model_context(mCubeObject,modelViewProjection,textureIndex);
//                        break;
//                    case 5:
//                        render_model_context(mCubeObject,modelViewProjection,textureIndex);
//                        break;
//                    case 6:
//                        render_model_context(mTeapot,modelViewProjection,textureIndex);
//                        if(!mediaPlayer.isPlaying()){
//                            mediaPlayer.start();
//                        }
//
//                        break;
//                    case 7:
//                        render_model_context(mTeapot,modelViewProjection,textureIndex);
//
//
//
//                        break;



//
//
//
//                }
            } else {
                GLES20.glDisable(GLES20.GL_CULL_FACE);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mBuildingsModel.getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mBuildingsModel.getTexCoords());

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        mTextures.get(3).mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjection, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0,
                        mBuildingsModel.getNumObjectVertex());

                SampleUtils.checkGLError("Renderer DrawBuildings");
            }

            SampleUtils.checkGLError("Render Frame");

        }

        //GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        Renderer.getInstance().end();

    }

    private void printUserData(Trackable trackable)
    {
        String userData = (String) trackable.getUserData();
        Log.d(LOGTAG, "UserData:Retreived User Data	\"" + userData + "\"");
    }


    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;

    }
    public void render_model_context(MeshObject mObject,float[] projectionMatrix,int textureIndex){


        // Remove the following line to make the bowl stop spinning:

        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                false, 0,  mObject.getVertices());
        GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                GLES20.GL_FLOAT, false, 0,  mObject.getTexCoords());

        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);

        // activate texture 0, bind it, and pass to shader
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                mTextures.get(textureIndex).mTextureID[0]);
        //mTextures.get(4).mTextureID[0]);
        GLES20.glUniform1i(texSampler2DHandle, 0);

        // pass the model view matrix to the shader
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                projectionMatrix, 0);

        // finally draw the teapot
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                mObject.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                mObject.getIndices());

        // disable the enabled arrays
        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);



    }

//    public void objectScaleFloatAdjust(float OBJECT_SCALE_FLOAT,float[] modelViewMatrix){
//        if (!mActivity.isExtendedTrackingActive()) {
//            Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
//                    OBJECT_SCALE_FLOAT);
//            Matrix.scaleM(modelViewMatrix, 0, OBJECT_SCALE_FLOAT,
//                    OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);
//        } else {
//            Matrix.rotateM(modelViewMatrix, 0, 90.0f, 1.0f, 0, 0);
//            Matrix.scaleM(modelViewMatrix, 0, kBuildingScale,
//                    kBuildingScale, kBuildingScale);
//        }
//    }

    public int setImagescortoId(String _Name){
        HashMap<Integer, String> hmap = new HashMap<Integer, String>();
        hmap.put(0,"stones");
        hmap.put(4,"chips");
        hmap.put(5,"tarmac");
        hmap.put(6,"cross");
        hmap.put(7,"colorful_daisies-wallpaper-1920x1080");
        if(hmap.containsValue(_Name)){
            for (Map.Entry<Integer,String> e : hmap.entrySet()) {
                Integer key = e.getKey();
                Object value2 = e.getValue();
                if ((value2.toString()).equalsIgnoreCase(_Name))
                {
                    return key;
                }
            }
        }
        return 3;
    }

    private void animateBowl(float[] modelViewMatrix)
    {
        double time = System.currentTimeMillis(); // Get real time difference
        float dt = (float) (time - prevTime) / 1000; // from frame to frame

        rotateAngle += dt * 180.0f / 3.1415f; // Animate angle based on time
        rotateAngle %= 360;
        Log.d(LOGTAG, "Delta animation time: " + rotateAngle);

        Matrix.rotateM(modelViewMatrix, 0, rotateAngle, 0.1f, 3.0f, 0.1f);

        prevTime = time;
    }



}
