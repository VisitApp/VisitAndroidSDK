package com.getvisitapp.google_fit.activity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.getvisitapp.google_fit.R;
import com.getvisitapp.google_fit.model.RoomDetails;
import com.getvisitapp.google_fit.network.APIServiceInstance;
import com.getvisitapp.google_fit.network.ApiService;
import com.getvisitapp.google_fit.presenter.TwillioVideoPresenter;
import com.getvisitapp.google_fit.util.AudioHelper;
import com.getvisitapp.google_fit.util.CameraCapturerCompat;
import com.getvisitapp.google_fit.view.TwillioVideoView;
import com.twilio.audioswitch.AudioDevice;
import com.twilio.audioswitch.AudioSwitch;
import com.twilio.video.AudioCodec;
import com.twilio.video.ConnectOptions;
import com.twilio.video.EncodingParameters;
import com.twilio.video.G722Codec;
import com.twilio.video.H264Codec;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalParticipant;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.OpusCodec;
import com.twilio.video.PcmaCodec;
import com.twilio.video.PcmuCodec;
import com.twilio.video.RemoteAudioTrack;
import com.twilio.video.RemoteAudioTrackPublication;
import com.twilio.video.RemoteDataTrack;
import com.twilio.video.RemoteDataTrackPublication;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.RemoteVideoTrackPublication;
import com.twilio.video.Room;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;
import com.twilio.video.VideoCodec;
import com.twilio.video.VideoTextureView;
import com.twilio.video.VideoTrack;
import com.twilio.video.VideoView;
import com.twilio.video.Vp8Codec;
import com.twilio.video.Vp9Codec;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import kotlin.Unit;
import tvi.webrtc.VideoSink;

;

@Keep
public class TwillioVideoCallActivity extends AppCompatActivity implements TwillioVideoView {
    public static final String PREF_AUDIO_CODEC = "audio_codec";
    public static final String PREF_AUDIO_CODEC_DEFAULT = OpusCodec.NAME;
    public static final String PREF_VIDEO_CODEC = "video_codec";
    public static final String PREF_VIDEO_CODEC_DEFAULT = Vp8Codec.NAME;
    public static final String PREF_SENDER_MAX_AUDIO_BITRATE = "sender_max_audio_bitrate";
    public static final String PREF_SENDER_MAX_AUDIO_BITRATE_DEFAULT = "0";
    public static final String PREF_SENDER_MAX_VIDEO_BITRATE = "sender_max_video_bitrate";
    public static final String PREF_SENDER_MAX_VIDEO_BITRATE_DEFAULT = "0";
    public static final String PREF_VP8_SIMULCAST = "vp8_simulcast";
    public static final String PREF_ENABLE_AUTOMATIC_SUBSCRIPTION = "enable_automatic_subscription";
    public static final boolean PREF_ENABLE_AUTOMATIC_SUBSCRIPTION_DEFAULT = true;
    public static final boolean PREF_VP8_SIMULCAST_DEFAULT = false;
    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "VideoActivity";
    private static final String[] VIDEO_CODEC_NAMES = new String[]{
            Vp8Codec.NAME, H264Codec.NAME, Vp9Codec.NAME
    };
    private static final String[] AUDIO_CODEC_NAMES = new String[]{
           OpusCodec.NAME, PcmaCodec.NAME, PcmuCodec.NAME, G722Codec.NAME
    };
    /*
     * Audio and video tracks can be created with names. This feature is useful for categorizing
     * tracks of participants. For example, if one participant publishes a video track with
     * ScreenCapturer and CameraCapturer with the names "screen" and "camera" respectively then
     * other participants can use RemoteVideoTrack#getName to determine which video track is
     * produced from the other participant's screen or camera.
     */
    private static final String LOCAL_AUDIO_TRACK_NAME = "mic";
    private static final String LOCAL_VIDEO_TRACK_NAME = "camera";
    public boolean isPush;
    public int seconds = 0;
    public int minutes = 0;
    public RoomDetails roomDetails;
    TwillioVideoPresenter twillioVideoPresenter;
    String name;
    int consultationId;
    int sessionId;
    boolean callEnded;
    Animation anim;
    boolean userEndCall;
    /*
     * A Room represents communication between a local participant and one or more participants.
     */
    private Room room;
    private LocalParticipant localParticipant;
    /*
     * AudioCodec and VideoCodec represent the preferred codec for encoding and decoding audio and
     * video.
     */
    private AudioCodec audioCodec;
    private VideoCodec videoCodec;
    /*
     * Encoding parameters represent the sender side bandwidth constraints.
     */
    private EncodingParameters encodingParameters;
    /*
     * A VideoView receives frames from a local or remote video track and renders them
     * to an associated view.
     */
    private VideoView primaryVideoView;
    // private VideoView thumbnailVideoView;
    private VideoTextureView videoTextureView;
    private ImageView endCall;
    private ImageView back;
    private TextView duration;
    private Button button;
    private ImageView imageView;
    /*
     * Android shared preferences used for settings
     */
    private SharedPreferences preferences;
    /*
     * Android application UI elements
     */
    private CameraCapturerCompat cameraCapturerCompat;
    private LocalAudioTrack localAudioTrack;
    private LocalVideoTrack localVideoTrack;
    //private FloatingActionButton connectActionFab;
    private ImageView switchCameraActionFab;
    private TextView textView;
    private CardView card_view;
    private RelativeLayout parent;
    private ImageView localVideoActionFab;
    private ImageView muteActionFab;
    private ProgressBar reconnectingProgressBar;
    private AlertDialog connectDialog;
    private String remoteParticipantIdentity;
    /*
     * Audio management
     */
    private AudioSwitch audioSwitch;
    private int savedVolumeControlStream;
    private MenuItem audioDeviceMenuItem;
    private VideoSink localVideoView;
    private boolean disconnectedFromOnDestroy;
    private boolean enableAutomaticSubscription;
    ApiService apiService = null;
    String authToken = "";
    boolean isDebug;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twillio_video_call);

        consultationId = getIntent().getIntExtra("consultationId", 0);
        authToken = getIntent().getStringExtra("authToken");
        sessionId = getIntent().getIntExtra("sessionId", 0);
        isPush = getIntent().getBooleanExtra("isPush", false);
        isDebug = getIntent().getBooleanExtra("isDebug", false);


        if (authToken != null) {
            if (isDebug) {
                apiService = APIServiceInstance.INSTANCE.getApiService("https://api.getvisitapp.xyz/v4/", getApplicationContext(), authToken, true);
            } else {
                apiService = APIServiceInstance.INSTANCE.getApiService("https://api.getvisitapp.com/v4/", getApplicationContext(), authToken, true);
            }

        }

        twillioVideoPresenter = new TwillioVideoPresenter(apiService, this);
        primaryVideoView = findViewById(R.id.primary_video_view);
        endCall = findViewById(R.id.end_call);
        back = findViewById(R.id.back);
        duration = findViewById(R.id.duration);
        button = findViewById(R.id.button1);
        imageView = findViewById(R.id.image);
        parent = findViewById(R.id.parent);
        // thumbnailVideoView = findViewById(R.id.thumbnail_video_view);
        videoTextureView = findViewById(R.id.thumbnail_video_view_1);
        reconnectingProgressBar = findViewById(R.id.reconnecting_progress_bar);
        textView = findViewById(R.id.title);
        card_view = findViewById(R.id.card_view);

        //connectActionFab = findViewById(R.id.connect_action_fab);
        switchCameraActionFab = findViewById(R.id.switch_camera_action_fab);
        localVideoActionFab = findViewById(R.id.local_video_action_fab);
        muteActionFab = findViewById(R.id.mute_action_fab);

        // getFeedback();

        roomDetails = new RoomDetails();

        if (isPush) {
            duration.setText("Calling..");
            textView.setText("Doctor");
            ObjectAnimator rotate = ObjectAnimator.ofFloat(endCall, "rotation", 0f, 20f, 0f, -20f, 0f); // rotate o degree then 20 degree and so on for one loop of rotation.
            rotate.setRepeatCount(50); // repeat the loop 20 times
            rotate.setDuration(200); // animation play time 100 ms
            rotate.start();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                endCall.setImageDrawable(getDrawable(R.drawable.ic_call_green));
            }
        }



        /*
         * Get shared preferences to read settings
         */
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        endCall.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                if (isPush) {
                    endCall.setImageDrawable(getDrawable(R.drawable.ic_call_cut));
                    twillioVideoPresenter.getRoomDetails(sessionId, consultationId);
                } else {
                    if (room != null) {
                        room.disconnect();
                        userEndCall = true;
                    } else {
                        userEndCall = true;
                        // twillioVideoPresenter.endVideoCall(sessionId);
                        finish();
                    }
                }
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userEndCall = true;
                if (room != null) {
                    room.disconnect();
                } else {

                    // twillioVideoPresenter.endVideoCall(sessionId);
                    finish();
                }
            }
        });

        /*
         * Setup audio management and set the volume control stream
         */
        audioSwitch = new AudioSwitch(getApplicationContext());
        savedVolumeControlStream = getVolumeControlStream();
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        /*
         * Check camera and microphone permissions. Needed in Android M.
         */
        if (!checkPermissionForCameraAndMicrophone()) {
            requestPermissionForCameraAndMicrophone();
        } else {
            createAudioAndVideoTracks();
            //setAccessToken();
        }

        /*
         * Set the initial state of the UI
         */
        intializeUI();

        if (!isPush)
            twillioVideoPresenter.getRoomDetails(sessionId, consultationId);


        audioSwitch.start((audioDevices, audioDevice) -> {
            return Unit.INSTANCE;
        });

        showAudioDevices();
    }


    private void showAudioDevices() {
        List<AudioDevice> availableAudioDevices = audioSwitch.getAvailableAudioDevices();

        AudioHelper.Companion.selectDevice(audioSwitch, availableAudioDevices);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE) {
            boolean cameraAndMicPermissionGranted = true;

            for (int grantResult : grantResults) {
                cameraAndMicPermissionGranted &= grantResult == PackageManager.PERMISSION_GRANTED;
            }

            if (cameraAndMicPermissionGranted) {
                createAudioAndVideoTracks();
                //setAccessToken();
            } else {
                Toast.makeText(this,
                        R.string.permissions_needed,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Update preferred audio and video codec in case changed in settings
         */
        audioCodec = getAudioCodecPreference(PREF_AUDIO_CODEC,
                PREF_AUDIO_CODEC_DEFAULT);
        videoCodec = getVideoCodecPreference(PREF_VIDEO_CODEC,
                PREF_VIDEO_CODEC_DEFAULT);
        enableAutomaticSubscription = getAutomaticSubscriptionPreference(PREF_ENABLE_AUTOMATIC_SUBSCRIPTION,
                PREF_ENABLE_AUTOMATIC_SUBSCRIPTION_DEFAULT);
        /*
         * Get latest encoding parameters
         */
        final EncodingParameters newEncodingParameters = getEncodingParameters();

        /*
         * If the local video track was released when the app was put in the background, recreate.
         */
        if (localVideoTrack == null && checkPermissionForCameraAndMicrophone()) {
            localVideoTrack = LocalVideoTrack.create(this,
                    true,
                    cameraCapturerCompat,
                    LOCAL_VIDEO_TRACK_NAME);
            localVideoTrack.addSink(localVideoView);

            /*
             * If connected to a Room then share the local video track.
             */
            if (localParticipant != null) {
                localParticipant.publishTrack(localVideoTrack);

                /*
                 * Update encoding parameters if they have changed.
                 */
                if (!newEncodingParameters.equals(encodingParameters)) {
                    localParticipant.setEncodingParameters(newEncodingParameters);
                }
            }
        }

        /*
         * Update encoding parameters
         */
        encodingParameters = newEncodingParameters;

        /*
         * Update reconnecting UI
         */
        if (room != null) {
            reconnectingProgressBar.setVisibility((room.getState() != Room.State.RECONNECTING) ?
                    View.GONE :
                    View.VISIBLE);
        }
    }

    @Override
    protected void onPause() {
        /*
         * Release the local video track before going in the background. This ensures that the
         * camera can be used by other applications while this app is in the background.
         */
        if (localVideoTrack != null) {
            /*
             * If this local video track is being shared in a Room, unpublish from room before
             * releasing the video track. Participants will be notified that the track has been
             * unpublished.
             */
            if (localParticipant != null) {
                localParticipant.unpublishTrack(localVideoTrack);
            }

            localVideoTrack.release();
            localVideoTrack = null;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        /*
         * Tear down audio management and restore previous volume stream
         */
        audioSwitch.stop();
        setVolumeControlStream(savedVolumeControlStream);

        /*
         * Always disconnect from the room before leaving the Activity to
         * ensure any memory allocated to the Room resource is freed.
         */
        if (room != null && room.getState() != Room.State.DISCONNECTED) {
            room.disconnect();
            disconnectedFromOnDestroy = true;
        }

        /*
         * Release the local audio and video tracks ensuring any memory allocated to audio
         * or video is freed.
         */
        if (localAudioTrack != null) {
            localAudioTrack.release();
            localAudioTrack = null;
        }
        if (localVideoTrack != null) {
            localVideoTrack.release();
            localVideoTrack = null;
        }

        super.onDestroy();
    }

    private boolean checkPermissionForCameraAndMicrophone() {
        int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return resultCamera == PackageManager.PERMISSION_GRANTED &&
                resultMic == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionForCameraAndMicrophone() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this,
                    R.string.permissions_needed,
                    Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    CAMERA_MIC_PERMISSION_REQUEST_CODE);
        }
    }

    private void createAudioAndVideoTracks() {
        // Share your microphone
        localAudioTrack = LocalAudioTrack.create(this, true, LOCAL_AUDIO_TRACK_NAME);

        // Share your camera
        cameraCapturerCompat = new CameraCapturerCompat(this, CameraCapturerCompat.Source.FRONT_CAMERA);
        localVideoTrack = LocalVideoTrack.create(this,
                true,
                cameraCapturerCompat,
                LOCAL_VIDEO_TRACK_NAME);
        videoTextureView.setMirror(true);
        localVideoTrack.addSink(videoTextureView);
        localVideoView = videoTextureView;


        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms
                // scaleView(card_view, 1f, 0.16f);
            }
        }, 2000);

    }


    public void scaleView(View v, float startScale, float endScale) {
        anim = new ScaleAnimation(
                1f, 0.2f, // Start and end values for the X axis scaling
                startScale, endScale, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.95f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.1f); // Pivot point of Y scaling
        anim.setFillAfter(true); // Needed to keep the result of the animation
        anim.setDuration(600);
        v.startAnimation(anim);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

//    private CameraCapturer.CameraSource getAvailableCameraSource() {
//        return (CameraCapturer.isSourceAvailable(CameraCapturer.CameraSource.FRONT_CAMERA)) ?
//                (CameraCapturer.CameraSource.FRONT_CAMERA) :
//                (CameraCapturer.CameraSource.BACK_CAMERA);
//    }


    private void connectToRoom(String roomName, String accessToken) {
        audioSwitch.activate();
        ConnectOptions.Builder connectOptionsBuilder = new ConnectOptions.Builder(accessToken)
                .roomName(roomName);

        /*
         * Add local audio track to connect options to share with participants.
         */
        if (localAudioTrack != null) {
            connectOptionsBuilder
                    .audioTracks(Collections.singletonList(localAudioTrack));
        }

        /*
         * Add local video track to connect options to share with participants.
         */
        if (localVideoTrack != null) {
            connectOptionsBuilder.videoTracks(Collections.singletonList(localVideoTrack));
        }

        /*
         * Set the preferred audio and video codec for media.
         */
        connectOptionsBuilder.preferAudioCodecs(Collections.singletonList(audioCodec));
        connectOptionsBuilder.preferVideoCodecs(Collections.singletonList(videoCodec));

        /*
         * Set the sender side encoding parameters.
         */
        connectOptionsBuilder.encodingParameters(encodingParameters);

        /*
         * Toggles automatic track subscription. If set to false, the LocalParticipant will receive
         * notifications of track publish events, but will not automatically subscribe to them. If
         * set to true, the LocalParticipant will automatically subscribe to tracks as they are
         * published. If unset, the default is true. Note: This feature is only available for Group
         * Rooms. Toggling the flag in a P2P room does not modify subscription behavior.
         */
        connectOptionsBuilder.enableAutomaticSubscription(enableAutomaticSubscription);

        room = Video.connect(this, connectOptionsBuilder.build(), roomListener());
        setDisconnectAction();
    }

    /*
     * The initial state when there is no active room.
     */
    private void intializeUI() {
//        connectActionFab.setImageDrawable(ContextCompat.getDrawable(this,
//                R.drawable.vw_ic_video_camera));
//        connectActionFab.show();
        //connectActionFab.setOnClickListener(connectActionClickListener());
//        switchCameraActionFab.show();
        switchCameraActionFab.setOnClickListener(switchCameraClickListener());
//        localVideoActionFab.show();
        localVideoActionFab.setOnClickListener(localVideoClickListener());
        //  muteActionFab.show();
        muteActionFab.setOnClickListener(muteClickListener());
    }


    /*
     * Get the preferred audio codec from shared preferences
     */
    private AudioCodec getAudioCodecPreference(String key, String defaultValue) {
        final String audioCodecName = preferences.getString(key, defaultValue);

        switch (audioCodecName) {
            case OpusCodec.NAME:
                return new OpusCodec();
            case PcmaCodec.NAME:
                return new PcmaCodec();
            case PcmuCodec.NAME:
                return new PcmuCodec();
            case G722Codec.NAME:
                return new G722Codec();
            default:
                return new OpusCodec();
        }
    }

    /*
     * Get the preferred video codec from shared preferences
     */
    private VideoCodec getVideoCodecPreference(String key, String defaultValue) {
        final String videoCodecName = preferences.getString(key, defaultValue);

        switch (videoCodecName) {
            case Vp8Codec.NAME:
                boolean simulcast = preferences.getBoolean(PREF_VP8_SIMULCAST,
                        PREF_VP8_SIMULCAST_DEFAULT);
                return new Vp8Codec(simulcast);
            case H264Codec.NAME:
                return new H264Codec();
            case Vp9Codec.NAME:
                return new Vp9Codec();
            default:
                return new Vp8Codec();
        }
    }

    private boolean getAutomaticSubscriptionPreference(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }


    /*
     * The actions performed during disconnect.
     */
    private void setDisconnectAction() {
//        connectActionFab.show();
//        connectActionFab.setOnClickListener(disconnectClickListener());
    }

    /*
     * Creates an connect UI dialog
     */
    private void showConnectDialog() {
        EditText roomEditText = new EditText(this);
//        connectDialog = DialogHelper.createConnectDialog(roomEditText,
//                connectClickListener(roomEditText),
//                cancelConnectDialogClickListener(),
//                this);
//        connectDialog.show();
    }

    /*
     * Called when remote participant joins the room
     */
    @SuppressLint("SetTextI18n")
    private void addRemoteParticipant(RemoteParticipant remoteParticipant) {
        /*
         * This app only displays video for one additional participant per Room
         */
//        if (videoTextureView.getVisibility() == View.VISIBLE) {
////            Snackbar.make(connectActionFab,
////                    "Multiple participants are not currently support in this UI",
////                    Snackbar.LENGTH_LONG)
////                    .setAction("Action", null).show();
//            return;
//        }
        remoteParticipantIdentity = remoteParticipant.getIdentity();

        /*
         * Add remote participant renderer
         */
        if (remoteParticipant.getRemoteVideoTracks().size() > 0) {
            RemoteVideoTrackPublication remoteVideoTrackPublication =
                    remoteParticipant.getRemoteVideoTracks().get(0);

            /*
             * Only render video tracks that are subscribed to
             */
            if (remoteVideoTrackPublication.isTrackSubscribed()) {
                addRemoteParticipantVideo(remoteVideoTrackPublication.getRemoteVideoTrack());
            }
        }

        /*
         * Start listening for participant events
         */
        remoteParticipant.setListener(remoteParticipantListener());
    }

    /*
     * Set primary view as renderer for participant video track
     */
    private void addRemoteParticipantVideo(VideoTrack videoTrack) {
        moveLocalVideoToThumbnailView();
        primaryVideoView.setMirror(false);
        videoTrack.addSink(primaryVideoView);
    }

    private void moveLocalVideoToThumbnailView() {
//        if (videoTextureView.getVisibility() == View.GONE) {
        scaleView(card_view, 1f, 0.16f);
        // videoTextureView.setVisibility(View.VISIBLE);
//            localVideoTrack.removeRenderer(primaryVideoView);
//            localVideoTrack.addRenderer(videoTextureView);
//            localVideoView = videoTextureView;
//            videoTextureView.setMirror(cameraCapturerCompat.getCameraSource() ==
//                    CameraCapturer.CameraSource.FRONT_CAMERA);
        //  }
    }

    /*
     * Called when remote participant leaves the room
     */
    @SuppressLint("SetTextI18n")
    private void removeRemoteParticipant(RemoteParticipant remoteParticipant) {
        if (!remoteParticipant.getIdentity().equals(remoteParticipantIdentity)) {
            return;
        }

        /*
         * Remove remote participant renderer
         */
        if (!remoteParticipant.getRemoteVideoTracks().isEmpty()) {
            RemoteVideoTrackPublication remoteVideoTrackPublication =
                    remoteParticipant.getRemoteVideoTracks().get(0);

            /*
             * Remove video only if subscribed to participant track
             */
            if (remoteVideoTrackPublication.isTrackSubscribed()) {
                removeParticipantVideo(remoteVideoTrackPublication.getRemoteVideoTrack());
            }
        }
        moveLocalVideoToPrimaryView();
    }

    private void removeParticipantVideo(VideoTrack videoTrack) {
        videoTrack.removeSink(primaryVideoView);
    }

    private void moveLocalVideoToPrimaryView() {
        if (videoTextureView.getVisibility() == View.VISIBLE) {
            card_view.setVisibility(View.GONE);
            videoTextureView.setVisibility(View.GONE);
            try {
                anim.setFillAfter(false); // Needed to keep the result of the animation
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (localVideoTrack != null) {
                localVideoTrack.removeSink(videoTextureView);
                localVideoTrack.addSink(primaryVideoView);
            }
            localVideoView = primaryVideoView;
            primaryVideoView.setMirror(cameraCapturerCompat.getCameraSource() ==
                    CameraCapturerCompat.Source.FRONT_CAMERA);
        }
    }

    /*
     * Room events listener
     */
    @SuppressLint("SetTextI18n")
    private Room.Listener roomListener() {
        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                localParticipant = room.getLocalParticipant();
                setTitle(room.getName());
                showDuration();
                isPush = false;
                textView.setText(name);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    endCall.setImageDrawable(getDrawable(R.drawable.ic_call_cut));
                }
                for (RemoteParticipant remoteParticipant : room.getRemoteParticipants()) {
                    addRemoteParticipant(remoteParticipant);
                    break;
                }
            }

            @Override
            public void onReconnecting(@NonNull Room room, @NonNull TwilioException twilioException) {
                reconnectingProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReconnected(@NonNull Room room) {
                reconnectingProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onConnectFailure(Room room, TwilioException e) {
                audioSwitch.deactivate();
                intializeUI();
            }

            @Override
            public void onDisconnected(Room room, TwilioException e) {

                //twillioVideoPresenter.endVideoCall(sessionId);
                localParticipant = null;
                reconnectingProgressBar.setVisibility(View.GONE);
                TwillioVideoCallActivity.this.room = null;
                if (!userEndCall) {
                    getFeedback();
                } else finish();
                // Only reinitialize the UI if disconnect was not called from onDestroy()
                if (!disconnectedFromOnDestroy) {
                    audioSwitch.deactivate();
                    intializeUI();
                    moveLocalVideoToPrimaryView();
                }
            }

            @Override
            public void onParticipantConnected(Room room, RemoteParticipant remoteParticipant) {
                addRemoteParticipant(remoteParticipant);

            }

            @Override
            public void onParticipantDisconnected(Room room, RemoteParticipant remoteParticipant) {
                callEnded = true;
                Toast.makeText(TwillioVideoCallActivity.this, "Doctor left call", Toast.LENGTH_LONG).show();
                room.disconnect();
                removeRemoteParticipant(remoteParticipant);
            }

            @Override
            public void onRecordingStarted(Room room) {
                /*
                 * Indicates when media shared to a Room is being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
                Log.d(TAG, "onRecordingStarted");
            }

            @Override
            public void onRecordingStopped(Room room) {
                /*
                 * Indicates when media shared to a Room is no longer being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
                Log.d(TAG, "onRecordingStopped");
            }
        };
    }

    @SuppressLint("SetTextI18n")
    private RemoteParticipant.Listener remoteParticipantListener() {
        return new RemoteParticipant.Listener() {
            @Override
            public void onAudioTrackPublished(RemoteParticipant remoteParticipant,
                                              RemoteAudioTrackPublication remoteAudioTrackPublication) {
                Log.i(TAG, String.format("onAudioTrackPublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteAudioTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteAudioTrackPublication.getTrackSid(),
                        remoteAudioTrackPublication.isTrackEnabled(),
                        remoteAudioTrackPublication.isTrackSubscribed(),
                        remoteAudioTrackPublication.getTrackName()));
            }

            @Override
            public void onAudioTrackUnpublished(RemoteParticipant remoteParticipant,
                                                RemoteAudioTrackPublication remoteAudioTrackPublication) {
                Log.i(TAG, String.format("onAudioTrackUnpublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteAudioTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteAudioTrackPublication.getTrackSid(),
                        remoteAudioTrackPublication.isTrackEnabled(),
                        remoteAudioTrackPublication.isTrackSubscribed(),
                        remoteAudioTrackPublication.getTrackName()));
            }

            @Override
            public void onDataTrackPublished(RemoteParticipant remoteParticipant,
                                             RemoteDataTrackPublication remoteDataTrackPublication) {
                Log.i(TAG, String.format("onDataTrackPublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteDataTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteDataTrackPublication.getTrackSid(),
                        remoteDataTrackPublication.isTrackEnabled(),
                        remoteDataTrackPublication.isTrackSubscribed(),
                        remoteDataTrackPublication.getTrackName()));
            }

            @Override
            public void onDataTrackUnpublished(RemoteParticipant remoteParticipant,
                                               RemoteDataTrackPublication remoteDataTrackPublication) {
                Log.i(TAG, String.format("onDataTrackUnpublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteDataTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteDataTrackPublication.getTrackSid(),
                        remoteDataTrackPublication.isTrackEnabled(),
                        remoteDataTrackPublication.isTrackSubscribed(),
                        remoteDataTrackPublication.getTrackName()));
            }

            @Override
            public void onVideoTrackPublished(RemoteParticipant remoteParticipant,
                                              RemoteVideoTrackPublication remoteVideoTrackPublication) {
                Log.i(TAG, String.format("onVideoTrackPublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteVideoTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteVideoTrackPublication.getTrackSid(),
                        remoteVideoTrackPublication.isTrackEnabled(),
                        remoteVideoTrackPublication.isTrackSubscribed(),
                        remoteVideoTrackPublication.getTrackName()));
            }

            @Override
            public void onVideoTrackUnpublished(RemoteParticipant remoteParticipant,
                                                RemoteVideoTrackPublication remoteVideoTrackPublication) {
                Log.i(TAG, String.format("onVideoTrackUnpublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteVideoTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteVideoTrackPublication.getTrackSid(),
                        remoteVideoTrackPublication.isTrackEnabled(),
                        remoteVideoTrackPublication.isTrackSubscribed(),
                        remoteVideoTrackPublication.getTrackName()));
            }

            @Override
            public void onAudioTrackSubscribed(RemoteParticipant remoteParticipant,
                                               RemoteAudioTrackPublication remoteAudioTrackPublication,
                                               RemoteAudioTrack remoteAudioTrack) {
                Log.i(TAG, String.format("onAudioTrackSubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteAudioTrack: enabled=%b, playbackEnabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteAudioTrack.isEnabled(),
                        remoteAudioTrack.isPlaybackEnabled(),
                        remoteAudioTrack.getName()));
            }

            @Override
            public void onAudioTrackUnsubscribed(RemoteParticipant remoteParticipant,
                                                 RemoteAudioTrackPublication remoteAudioTrackPublication,
                                                 RemoteAudioTrack remoteAudioTrack) {
                Log.i(TAG, String.format("onAudioTrackUnsubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteAudioTrack: enabled=%b, playbackEnabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteAudioTrack.isEnabled(),
                        remoteAudioTrack.isPlaybackEnabled(),
                        remoteAudioTrack.getName()));
            }

            @Override
            public void onAudioTrackSubscriptionFailed(RemoteParticipant remoteParticipant,
                                                       RemoteAudioTrackPublication remoteAudioTrackPublication,
                                                       TwilioException twilioException) {
                Log.i(TAG, String.format("onAudioTrackSubscriptionFailed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteAudioTrackPublication: sid=%b, name=%s]" +
                                "[TwilioException: code=%d, message=%s]",
                        remoteParticipant.getIdentity(),
                        remoteAudioTrackPublication.getTrackSid(),
                        remoteAudioTrackPublication.getTrackName(),
                        twilioException.getCode(),
                        twilioException.getMessage()));
            }

            @Override
            public void onDataTrackSubscribed(RemoteParticipant remoteParticipant,
                                              RemoteDataTrackPublication remoteDataTrackPublication,
                                              RemoteDataTrack remoteDataTrack) {
                Log.i(TAG, String.format("onDataTrackSubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteDataTrack: enabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteDataTrack.isEnabled(),
                        remoteDataTrack.getName()));
            }

            @Override
            public void onDataTrackUnsubscribed(RemoteParticipant remoteParticipant,
                                                RemoteDataTrackPublication remoteDataTrackPublication,
                                                RemoteDataTrack remoteDataTrack) {
                Log.i(TAG, String.format("onDataTrackUnsubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteDataTrack: enabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteDataTrack.isEnabled(),
                        remoteDataTrack.getName()));
            }

            @Override
            public void onDataTrackSubscriptionFailed(RemoteParticipant remoteParticipant,
                                                      RemoteDataTrackPublication remoteDataTrackPublication,
                                                      TwilioException twilioException) {
                Log.i(TAG, String.format("onDataTrackSubscriptionFailed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteDataTrackPublication: sid=%b, name=%s]" +
                                "[TwilioException: code=%d, message=%s]",
                        remoteParticipant.getIdentity(),
                        remoteDataTrackPublication.getTrackSid(),
                        remoteDataTrackPublication.getTrackName(),
                        twilioException.getCode(),
                        twilioException.getMessage()));
            }

            @Override
            public void onVideoTrackSubscribed(RemoteParticipant remoteParticipant,
                                               RemoteVideoTrackPublication remoteVideoTrackPublication,
                                               RemoteVideoTrack remoteVideoTrack) {
                Log.i(TAG, String.format("onVideoTrackSubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteVideoTrack: enabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteVideoTrack.isEnabled(),
                        remoteVideoTrack.getName()));
                addRemoteParticipantVideo(remoteVideoTrack);
            }

            @Override
            public void onVideoTrackUnsubscribed(RemoteParticipant remoteParticipant,
                                                 RemoteVideoTrackPublication remoteVideoTrackPublication,
                                                 RemoteVideoTrack remoteVideoTrack) {
                Log.i(TAG, String.format("onVideoTrackUnsubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteVideoTrack: enabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteVideoTrack.isEnabled(),
                        remoteVideoTrack.getName()));
                removeParticipantVideo(remoteVideoTrack);
            }

            @Override
            public void onVideoTrackSubscriptionFailed(RemoteParticipant remoteParticipant,
                                                       RemoteVideoTrackPublication remoteVideoTrackPublication,
                                                       TwilioException twilioException) {
                Log.i(TAG, String.format("onVideoTrackSubscriptionFailed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteVideoTrackPublication: sid=%b, name=%s]" +
                                "[TwilioException: code=%d, message=%s]",
                        remoteParticipant.getIdentity(),
                        remoteVideoTrackPublication.getTrackSid(),
                        remoteVideoTrackPublication.getTrackName(),
                        twilioException.getCode(),
                        twilioException.getMessage()));
//                Snackbar.make(connectActionFab,
//                        String.format("Failed to subscribe to %s video track",
//                                remoteParticipant.getIdentity()),
//                        Snackbar.LENGTH_LONG)
//                        .show();
            }

            @Override
            public void onAudioTrackEnabled(RemoteParticipant remoteParticipant,
                                            RemoteAudioTrackPublication remoteAudioTrackPublication) {

            }

            @Override
            public void onAudioTrackDisabled(RemoteParticipant remoteParticipant,
                                             RemoteAudioTrackPublication remoteAudioTrackPublication) {

            }

            @Override
            public void onVideoTrackEnabled(RemoteParticipant remoteParticipant,
                                            RemoteVideoTrackPublication remoteVideoTrackPublication) {

            }

            @Override
            public void onVideoTrackDisabled(RemoteParticipant remoteParticipant,
                                             RemoteVideoTrackPublication remoteVideoTrackPublication) {

            }
        };
    }

    private DialogInterface.OnClickListener connectClickListener(final EditText roomEditText) {
        return (dialog, which) -> {
            /*
             * Connect to room
             */
            // connectToRoom(roomEditText.getText().toString());
        };
    }

    private View.OnClickListener disconnectClickListener() {
        return v -> {
            /*
             * Disconnect from room
             */
            if (room != null) {
                room.disconnect();
            }
            intializeUI();
        };
    }

    private View.OnClickListener connectActionClickListener() {
        return v -> showConnectDialog();
    }

    private DialogInterface.OnClickListener cancelConnectDialogClickListener() {
        return (dialog, which) -> {
            intializeUI();
            connectDialog.dismiss();
        };
    }

    private View.OnClickListener switchCameraClickListener() {
        return v -> {
            if (cameraCapturerCompat != null) {
                CameraCapturerCompat.Source cameraSource = cameraCapturerCompat.getCameraSource();
                cameraCapturerCompat.switchCamera();
                if (videoTextureView.getVisibility() == View.VISIBLE) {
                    videoTextureView.setMirror(cameraSource == CameraCapturerCompat.Source.BACK_CAMERA);
                } else {
                    primaryVideoView.setMirror(cameraSource == CameraCapturerCompat.Source.BACK_CAMERA);
                }
            }
        };
    }

    private View.OnClickListener localVideoClickListener() {
        return v -> {
            /*
             * Enable/disable the local video track
             */
            if (localVideoTrack != null) {
                boolean enable = !localVideoTrack.isEnabled();
                localVideoTrack.enable(enable);
                int icon;
                if (enable) {
                    icon = R.drawable.ic_video_on;
                    // switchCameraActionFab.show();
                } else {
                    icon = R.drawable.ic_videocam_off;
                    //switchCameraActionFab.hide();
                }
                localVideoActionFab.setImageDrawable(
                        ContextCompat.getDrawable(TwillioVideoCallActivity.this, icon));
            }
        };
    }

    private View.OnClickListener muteClickListener() {
        return v -> {
            /*
             * Enable/disable the local audio track. The results of this operation are
             * signaled to other Participants in the same Room. When an audio track is
             * disabled, the audio is muted.
             */
            if (localAudioTrack != null) {
                boolean enable = !localAudioTrack.isEnabled();
                localAudioTrack.enable(enable);
                int icon = enable ?
                        R.drawable.ic_mic_on : R.drawable.ic_mic_off;
                muteActionFab.setImageDrawable(ContextCompat.getDrawable(
                        TwillioVideoCallActivity.this, icon));
            }
        };
    }


    private EncodingParameters getEncodingParameters() {
        final int maxAudioBitrate = Integer.parseInt(
                preferences.getString(PREF_SENDER_MAX_AUDIO_BITRATE,
                        PREF_SENDER_MAX_AUDIO_BITRATE_DEFAULT));
        final int maxVideoBitrate = Integer.parseInt(
                preferences.getString(PREF_SENDER_MAX_VIDEO_BITRATE,
                        PREF_SENDER_MAX_VIDEO_BITRATE_DEFAULT));

        return new EncodingParameters(maxAudioBitrate, maxVideoBitrate);
    }


    @Override
    public void roomDetails(RoomDetails roomDetails) {

        this.roomDetails = roomDetails;
        connectToRoom(roomDetails.roomName, roomDetails.token);
        name = roomDetails.doctorName;
        textView.setText(name);

        Glide.with(imageView).load(roomDetails.doctorProfileImage).into(imageView);


    }

    @Override
    public void setError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onBackPressed() {

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.end_your_call_dialog);
        TextView cancel = dialog.findViewById(R.id.cancel_tv);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        TextView confirm = dialog.findViewById(R.id.continue_tv);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userEndCall = true;
                if (room != null) {
                    room.disconnect();
                    dialog.dismiss();
                    finish();
                } else {
                    //twillioVideoPresenter.endVideoCall(sessionId);
                    dialog.dismiss();
                    finish();
                }
            }
        });


        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.getWindow().setLayout(
                (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.9),
                WindowManager.LayoutParams.WRAP_CONTENT);

        dialog.show();


//        new AlertDialog.Builder(this)
//                .setTitle("End Video Call ?")
//                .setMessage("Are you sure you want to exit?")
//                .setNegativeButton(android.R.string.no, null)
//                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//
//                    public void onClick(DialogInterface arg0, int arg1) {
//
//                    }
//                }).create().show();
    }


    private void showDuration() {

        // Get the text view.
        final TextView timeView
                = (TextView) findViewById(
                R.id.duration);

        // Creates a new Handler
        final Handler handler
                = new Handler();

        // Call the post() method,
        // passing in a new Runnable.
        // The post() method processes
        // code without a delay,
        // so the code in the Runnable
        // will run almost immediately.
        handler.post(new Runnable() {
            @Override

            public void run() {
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;

                // Format the seconds into hours, minutes,
                // and seconds.
                String time
                        = String
                        .format(Locale.getDefault(),
                                "%d:%02d:%02d", hours,
                                minutes, secs);

                // Set the text view text.
                if (!callEnded) {
                    timeView.setText(time);
                }

                // If running is true, increment the
                // seconds variable.
                seconds++;

                // Post the code again
                // with a delay of 1 second.
                handler.postDelayed(this, 1000);
            }
        });
    }


    private void getFeedback() {
        duration.setText("Call Ended");
        endCall.setVisibility(View.GONE);
        // finish();

    }
}
