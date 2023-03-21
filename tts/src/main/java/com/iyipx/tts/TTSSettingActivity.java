package com.iyipx.tts;

import static com.iyipx.tts.services.Constants.CUSTOM_VOICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.iyipx.tts.adapters.TtsActorAdapter;
import com.iyipx.tts.adapters.TtsStyleAdapter;
import com.iyipx.tts.services.Constants;
import com.iyipx.tts.services.TtsActorManger;
import com.iyipx.tts.services.TtsStyle;
import com.iyipx.tts.services.TtsStyleManger;
import com.iyipx.tts.services.TtsVoiceSample;

import java.util.Locale;


public class TTSSettingActivity extends Activity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "CheckVoiceData";

    boolean connected = false;
    TextToSpeech textToSpeech;
    int styleDegree;
    int volumeValue;
    private LinearLayout btnKillBattery;
    private Button ttsStyleDegreeAdd;
    private Button ttsStyleDegreeReduce;
    private Button ttsVoiceVolumeAdd;
    private Button ttsVoiceVolumeReduce;
    private SeekBar ttsStyleDegree;
    private SeekBar ttsVoiceVolume;
    private RecyclerView rvVoiceStyles;
    private LinearLayout ttsStyleDegreeParent;
    private TextView ttsStyleDegreeValue;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tts);

        btnKillBattery = findViewById(R.id.btn_kill_battery);
        ttsStyleDegreeAdd = findViewById(R.id.tts_style_degree_add);
        ttsStyleDegreeReduce = findViewById(R.id.tts_style_degree_reduce);
        ttsVoiceVolumeAdd = findViewById(R.id.tts_voice_volume_add);
        ttsVoiceVolumeReduce = findViewById(R.id.tts_voice_volume_reduce);

        ttsStyleDegree = findViewById(R.id.tts_style_degree);
        ttsVoiceVolume = findViewById(R.id.tts_voice_volume);

        RecyclerView rvVoiceActors = findViewById(R.id.rv_voice_actors);
        rvVoiceStyles = findViewById(R.id.rv_voice_styles);

        ttsStyleDegreeParent = findViewById(R.id.tts_style_degree_parent);
        ttsStyleDegreeValue = findViewById(R.id.tts_style_degree_value);

        connectToText2Speech();

        btnKillBattery.setOnClickListener(this);
        ttsStyleDegreeAdd.setOnClickListener(this);
        ttsStyleDegreeReduce.setOnClickListener(this);
        ttsVoiceVolumeAdd.setOnClickListener(this);
        ttsVoiceVolumeReduce.setOnClickListener(this);

        int styleIndex = TTSAPP.getInt(Constants.VOICE_STYLE_INDEX, 0);//sharedPreferences.getInt(Constants.VOICE_STYLE_INDEX, 0);

        styleDegree = TTSAPP.getInt(Constants.VOICE_STYLE_DEGREE, 100);//sharedPreferences.getInt(Constants.VOICE_STYLE_DEGREE, 100);
        volumeValue = TTSAPP.getInt(Constants.VOICE_VOLUME, 100);//sharedPreferences.getInt(Constants.VOICE_VOLUME, 100);

        updateView();

        ttsStyleDegree.setProgress(styleDegree);
        ttsVoiceVolume.setProgress(volumeValue);
        ttsStyleDegree.setOnSeekBarChangeListener(this);
        ttsVoiceVolume.setOnSeekBarChangeListener(this);

        TtsStyleAdapter ttsStyleAdapter = new TtsStyleAdapter(TtsStyleManger.getInstance().getStyles());
        ttsStyleAdapter.setSelect(styleIndex);
        rvVoiceStyles.setAdapter(ttsStyleAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        rvVoiceStyles.setLayoutManager(linearLayoutManager);
        linearLayoutManager.scrollToPositionWithOffset(styleIndex, 0);
        ttsStyleAdapter.setItemClickListener((position, item) -> TTSAPP.putInt(Constants.VOICE_STYLE_INDEX, position));

        showStyleView(TTSAPP.getBoolean(Constants.USE_PREVIEW, true));


        TtsActorAdapter actorAdapter = new TtsActorAdapter(TtsActorManger.getInstance().getActors());
        rvVoiceActors.setAdapter(actorAdapter);
        rvVoiceActors.setVisibility(View.VISIBLE);
        rvVoiceActors.setLayoutManager(new GridLayoutManager(this, 3));
        actorAdapter.setSelect(rvVoiceActors, TTSAPP.getInt(Constants.CUSTOM_VOICE_INDEX, 0));
        actorAdapter.setItemClickListener((position, item) -> {
            boolean origin = TTSAPP.getBoolean(Constants.USE_CUSTOM_VOICE, true);

            if (origin) {

                TTSAPP.putString(CUSTOM_VOICE, item.getShortName());
                TTSAPP.putInt(Constants.CUSTOM_VOICE_INDEX, position);

            }

            Locale locale = item.getLocale();

            if (textToSpeech != null && !textToSpeech.isSpeaking()) {
                connectToText2Speech();

                textToSpeech.stop();
                if (!textToSpeech.isSpeaking()) {
                    textToSpeech.speak(TtsVoiceSample.getByLocate(this, locale), TextToSpeech.QUEUE_FLUSH, null, null);
                }
//                Bundle bundle = new Bundle();
//                bundle.putString(CUSTOM_VOICE, item.getShortName());
//                bundle.putInt(Constants.CUSTOM_VOICE_INDEX, position);
//                bundle.putString("voiceName", item.getShortName());
//                bundle.putString("language", locale.getISO3Language());
//                bundle.putString("country", locale.getISO3Country());
//                bundle.putString("variant", item.getGender() ? "Female" : "Male");
//                bundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Sample");
//                textToSpeech.speak(TtsVoiceSample.getByLocate(this, locale), TextToSpeech.QUEUE_FLUSH, bundle, MainActivity.class.getName() + mNextRequestId.getAndIncrement());
            } else {
                if (textToSpeech == null) {
                    connectToText2Speech();
                }
            }

        });

    }


    /**
     * 连接Text2Speech
     */
    private void connectToText2Speech() {
        if (textToSpeech == null || textToSpeech.speak("", TextToSpeech.QUEUE_FLUSH, null, null) != TextToSpeech.SUCCESS) {
            textToSpeech = new TextToSpeech(TTSSettingActivity.this, status -> {

                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.CHINA);
                    if (result != TextToSpeech.LANG_MISSING_DATA
                            && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        connected = true;
//                        if (!textToSpeech.isSpeaking()) {
//                            textToSpeech.speak("初始化成功。", TextToSpeech.QUEUE_FLUSH, null, null);
//                        }
                    }
                }
            }, this.getPackageName());
        }

    }


    private void showStyleView(boolean show) {
        if (show) {
            rvVoiceStyles.setVisibility(View.VISIBLE);
            ttsStyleDegreeParent.setVisibility(View.VISIBLE);
        } else {
            rvVoiceStyles.setVisibility(View.GONE);
            ttsStyleDegreeParent.setVisibility(View.GONE);
        }
    }


    @SuppressLint("SetTextI18n")
    private void updateView() {
        TTSAPP.putInt(Constants.VOICE_STYLE_DEGREE, styleDegree);
        TTSAPP.putInt(Constants.VOICE_VOLUME, volumeValue);
        ttsStyleDegree.setProgress(styleDegree);

        String format = String.format(Locale.US, "强度:%01d.%02d 音量:%03d", styleDegree / TtsStyle.DEFAULT_DEGREE, styleDegree % TtsStyle.DEFAULT_DEGREE, volumeValue);
        ttsStyleDegreeValue.setText(format);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            boolean i = powerManager.isIgnoringBatteryOptimizations(this.getPackageName());
            if (i) {
                btnKillBattery.setVisibility(View.GONE);
            } else {
                btnKillBattery.setVisibility(View.VISIBLE);
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("BatteryLife")
    private void killBATTERY() {
        Intent intent = new Intent();
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (pm.isIgnoringBatteryOptimizations(packageName))
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            else {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }

    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
       if (id == btnKillBattery.getId()) {
            killBATTERY();
        } else if (id == ttsStyleDegreeAdd.getId()) {
            if (styleDegree < 200) {
                styleDegree++;
                updateView();
            }
        } else if (id == ttsStyleDegreeReduce.getId()) {
            if (styleDegree > 1) {
                styleDegree--;
                updateView();
            }
        } else if (id == ttsVoiceVolumeReduce.getId()) {
            if (volumeValue > 1) {
                volumeValue--;
                updateView();
            }
        } else if (id == ttsVoiceVolumeAdd.getId()) {
            if (volumeValue < 100) {
                volumeValue++;
                updateView();
            }
        }

    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int id = seekBar.getId();
        if (id == ttsStyleDegree.getId()) {
            styleDegree = progress;
            updateView();
        } else if (id == ttsVoiceVolume.getId()) {
            volumeValue = progress;
            updateView();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}