package gerb.com.haiku;

import gerb.com.haiku.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class HaikuActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1001;
    private static String TAG = "Haiku";
    private TextView mFirstLine;
    private TextView mSecondLine;
    private TextView mThirdLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_haiku);

        mFirstLine = (TextView)findViewById(R.id.firstLine);
        mSecondLine = (TextView)findViewById(R.id.secondLine);
        mThirdLine = (TextView)findViewById(R.id.thirdLine);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, mFirstLine, HIDER_FLAGS);
        mSystemUiHider.setup();

        // Set up the user interaction to manually show or hide the system UI.
        mFirstLine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        View addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak();
            }
        });

        View saveButton = findViewById(R.id.save_button);

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        addButton.setOnTouchListener(mDelayHideTouchListener);
        saveButton.setOnTouchListener(mDelayHideTouchListener);
    }

    public void speak() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                getClass().getPackage().getName());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Haiku");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                List<String> list = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String haiku = list.get(0);

                AsyncTask<String, Void, List<HaikuState>> task = new AsyncTask<String, Void, List<HaikuState>>() {
                    @Override
                    protected List<HaikuState> doInBackground(String... params) {
                        String haiku = params[0];
                        List<HaikuState> haikuParsedArray = new ArrayList<>();
                        int total = 0;
                        int lines = 1;

                        EnglishSyllableCounter esc = new EnglishSyllableCounter();

                        String[] words = haiku.split(" ");
                        for (String word : words) {

                            int count = esc.countSyllables(word);
                            total += count;

                            //DEBUGGING - remove once it is working. Probably never!
                            Log.d(TAG, String.format("%s:%d", word, count));

                            if (haikuParsedArray.size() <= (lines - 1))
                                haikuParsedArray.add(lines - 1,  new HaikuState(lines));
                            haikuParsedArray.get(lines - 1).addWordToLine(word, count);

                            if (((lines == 1 || lines == 3) && total >= 5)
                                    || (lines == 2 && total >= 7)) {
                                lines++;
                                total = 0;
                            }
                        }

                        return haikuParsedArray;
                    }

                    @Override
                    protected void onPostExecute(List<HaikuState> haikuStates) {
                        boolean validHaiku = true;
                        if (haikuStates.size() > 0) {
                            mFirstLine.setText(haikuStates.get(0).getLine());
                            if (!haikuStates.get(0).isGood()) {
                                mFirstLine.setTextColor(HaikuActivity.this.getResources().getColor(R.color.bad_line));
                                validHaiku = false;
                            } else {
                                mFirstLine.setTextColor(HaikuActivity.this.getResources().getColor(R.color.good_line));
                            }
                        } else {
                            mFirstLine.setText("");
                        }
                        if (haikuStates.size() > 1) {
                            mSecondLine.setText(haikuStates.get(1).getLine());
                            if (!haikuStates.get(1).isGood()) {
                                mSecondLine.setTextColor(HaikuActivity.this.getResources().getColor(R.color.bad_line));
                                validHaiku = false;
                            } else {
                                mSecondLine.setTextColor(HaikuActivity.this.getResources().getColor(R.color.good_line));
                            }
                        } else {
                            mSecondLine.setText("");
                        }
                        if (haikuStates.size() > 2) {
                            mThirdLine.setText(haikuStates.get(2).getLine());
                            if (!haikuStates.get(2).isGood()) {
                                mThirdLine.setTextColor(HaikuActivity.this.getResources().getColor(R.color.bad_line));
                                validHaiku = false;
                            } else {
                                mThirdLine.setTextColor(HaikuActivity.this.getResources().getColor(R.color.good_line));
                            }
                        } else {
                            mThirdLine.setText("");
                        }

                        if (validHaiku) {
                            showToastMessage("This is a haiku, congrats!");
                        } else {
                            showToastMessage("This is not a valid haiku");
                        }
                    }
                };

                /*
                 * Be a good citizen and do all work on a background thread.
                 */
                task.execute(haiku);
            }

        } else if(resultCode == RecognizerIntent.RESULT_AUDIO_ERROR){
            showToastMessage("Audio Error");
        }else if(resultCode == RecognizerIntent.RESULT_CLIENT_ERROR){
            showToastMessage("Client Error");
        }else if(resultCode == RecognizerIntent.RESULT_NETWORK_ERROR){
            showToastMessage("Network Error");
        }else if(resultCode == RecognizerIntent.RESULT_NO_MATCH){
            showToastMessage("No Match");
        }else if(resultCode == RecognizerIntent.RESULT_SERVER_ERROR){
            showToastMessage("Server Error");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private static class HaikuState {
        private String line;
        private int lineNumber;
        private int syllablesCount;

        public HaikuState(int lineNum) {
            line = new String();
            syllablesCount = 0;
            lineNumber = lineNum;
        }

        public void addWordToLine(String word, int sc) {
            if (line.length() > 0) {
                line += " ";
            }
            line += word;
            syllablesCount += sc;
        }

        public String getLine() {
            return line;
        }

        public int getSyllablesCount() {
            return syllablesCount;
        }

        public boolean isGood() {
            return (((lineNumber == 1 || lineNumber == 3) && syllablesCount == 5)
                    || (lineNumber == 2 && syllablesCount == 7));
        }
    }
}
