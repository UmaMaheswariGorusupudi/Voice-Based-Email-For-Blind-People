package thegenuinegourav.voicemail;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

public class UserDetailsActivity extends AppCompatActivity {

    private TextToSpeech tts;
    private TextView status;
    private TextView From,Password;
    private int numberOfClicks;
    UserLocalStore userLocalStore;
    private boolean IsInitialVoiceFinshed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);
        IsInitialVoiceFinshed = false ;
       // status.setText("Mail?");
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "This Language is not supported");
                    }
                    speak("Welcome to voice mail");
                    if (userLocalStore.getLoggedInUser() == null){
                       new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                speak("Tell me your mail address? or cancel to close the application ");
                                IsInitialVoiceFinshed = true;
                            }
                        }, 4000);
                    }
                    else {
                        SpeakOutDetails();
                    }

                } else {
                    Log.e("TTS", "Initilization Failed!");
                }
            }
        });

        status = (TextView)findViewById(R.id.status);
        From = (TextView)findViewById(R.id.from);
        Password =(TextView)findViewById((R.id.password));

        userLocalStore = new UserLocalStore(this);
        numberOfClicks = 0;
    }



    private void SpeakOutDetails(){


        User user = userLocalStore.getLoggedInUser();
        From.setText(user.username);
        Password.setText((user.password));
        speak(" your Mail is" + user.username + "and your Password is" + user.password + " say yes to confirm and proceed and no to change the mail and cancel to ");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                status.setText("Mail?");
                IsInitialVoiceFinshed = true;
                numberOfClicks = 2;
            }
        }, 4000);
    }

    private void speak(String text){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }else{
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @Override
    public void onDestroy() {
        if (tts != null) {

            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    public void layoutClicked(View view)
    {
        if(IsInitialVoiceFinshed) {
            numberOfClicks++;
            listen();
        }
    }

    private void listen(){
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");

        try {
            startActivityForResult(i, 100);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(UserDetailsActivity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }


    private void exitFromApp()
    {
       this.finishAffinity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && IsInitialVoiceFinshed) {
            IsInitialVoiceFinshed = false;
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result.get(0).equals("cancel")) {
                    speak("Cancelled!");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            exitFromApp();
                        }
                    }, 4000);

                } else {

                    switch (numberOfClicks) {
                        case 1:
                            String from;
                            from = result.get(0).replaceAll("underscore", "_");
                            from = from.replaceAll("\\s+", "");
                            From.setText(from);
                            Config.EMAIL = from;
                            status.setText("Password?");
                            speak("Password?");
                            break;
                        case 2:
                            String pwd;
                            pwd = result.get(0).replaceAll("\\s+","");
                            Config.PASSWORD = pwd;
                            Password.setText(pwd);
                            status.setText("Confirm?");
                            speak("Please Confirm the mail\n User : " + From.getText().toString() + "your password" +Config.PASSWORD + "\nSpeak Yes to confirm");
                            break;
                        default:
                             if(result.get(0).equals("yes")||result.get(0).equals("s"))
                             {
                                 User user;
                                 String username = From.getText().toString();
                                 String password = Password.getText().toString();
                                 Config.EMAIL = username;
                                 Config.PASSWORD = password;

                                 user = new User(username, password);
                                 userLocalStore.storeUserData(user);
                                 userLocalStore.setUserLoggedIn(true);

                                 Intent HomeIntent = new Intent(UserDetailsActivity.this,MainActivity.class);
                                 startActivity(HomeIntent);
                                 finish();
                             }else
                             {
                                 userLocalStore.clearUserData();
                                 status.setText("Mail?");
                                 speak("Please provide your Mail?");
                                 new Handler().postDelayed(new Runnable() {
                                     @Override
                                     public void run() {
                                         numberOfClicks = 0;
                                     }
                                 }, 2000);
                             }
                             break;


                    }
                }
            }
            IsInitialVoiceFinshed = true;
        }
    }
}