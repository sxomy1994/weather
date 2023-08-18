package com.milos.weather.activity;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.os.LocaleListCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.milos.weather.R;


import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SettingsActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ImageView backButton = findViewById(R.id.back_image_button_settings_activity);
        backButton.setOnClickListener(e -> finish());
    }


    public static class WeatherPreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.settings_main, rootKey);

            Preference temperatureUnit = findPreference("temperature_unit");

            bindPreferenceSummaryOnValue(temperatureUnit);
            bindPreferenceSummaryOnValue(findPreference("wind_speed_unit"));

            Preference language = findPreference("language");
            bindPreferenceSummaryOnValue(language);

            Preference feedback = findPreference("feedback_key");
            feedback.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull Preference preference) {
                    FeedbackDialog feedbackDialog = new FeedbackDialog();
                    feedbackDialog.show(getParentFragmentManager(), feedbackDialog.getTag());
                    return false;
                }
            });
        }


        //Metoda prikazuje izabrane opcije za preference ispod naziva (npr. Temperature units - ispod  °C  )
        private void bindPreferenceSummaryOnValue(Preference preference) {
            preference.setOnPreferenceChangeListener(this);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
            String preferenceString = sharedPreferences.getString(preference.getKey(), "");
            onPreferenceChange(preference, preferenceString);
        }

        @Override
        public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(newValue.toString());
            if (prefIndex >= 0) {
                CharSequence[] label = listPreference.getEntries();
                preference.setSummary(label[prefIndex]);
                if (listPreference.getKey().equals("language")) {
                    if (label[prefIndex].equals("Srpski")) {
                        setLocale("sr-Latn");
                    } else {
                        setLocale("en");
                    }
                }
            } else {
                preference.setSummary(newValue.toString());
            }
            return true;
        }

        private void setLocale(String language) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language));
        }
    }


    public static class FeedbackDialog extends BottomSheetDialogFragment {
        private EditText email, description;
        private Button cancel, submit;
        private RatingBar ratingBar;
        private LinearLayout sendingLayout;
        private ConstraintLayout inputLayout;
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.feedback_dialog, container, false);
            email = view.findViewById(R.id.feedback_dialog_sender_email);
            description = view.findViewById(R.id.feedback_dialog_feedback_detail);
            cancel = view.findViewById(R.id.cancel_feedback_button);
            submit = view.findViewById(R.id.submit_feedback_button);
            ratingBar = view.findViewById(R.id.feedback_dialog_rating_bar);
            inputLayout = view.findViewById(R.id.dialog_feedback_input_layout);
            sendingLayout = view.findViewById(R.id.dialog_feedback_sending_layout);
            setCancel();
            setSubmit();
            return view;
        }
        @Override
        public void onStart() {
            super.onStart();
            //Kada je tablet u landscape dijalog ne iskace skroz, zato stavljamo STATE EXPAND da bude ceo vidljiv
            BottomSheetBehavior.from((View) requireView().getParent()).setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        private void setSubmit() {
                submit.setOnClickListener(e -> {
                    if(TextUtils.isEmpty(email.getText().toString()) ||
                            TextUtils.isEmpty(description.getText().toString())){
                        Toast.makeText(getActivity(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                    }else {
                    String username = "libraryofficial.email@gmail.com";
                    String password = "bjqitgohprnuyfak";  //2-Step Verification from gmail, we created this password for using in this app
                    Executor executor = Executors.newSingleThreadExecutor();
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            Properties properties = new Properties();
                            properties.put("mail.smtp.auth", "true");
                            properties.put("mail.smtp.starttls.enable", "true");
                            properties.put("mail.smtp.host", "smtp.gmail.com");
                            properties.put("mail.smtp.port", "587");
                            Session session = Session.getInstance(properties, new Authenticator() {
                                @Override
                                protected PasswordAuthentication getPasswordAuthentication() {
                                    return new PasswordAuthentication(username, password);
                                }
                            });
                            try {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showProgressBar();
                                    }
                                });
                                Message message = new MimeMessage(session);
                                message.setFrom(new InternetAddress(email.getText().toString()));
                                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("sxomy1994@gmail.com"));
                                message.setSubject("Weather Feedback, " + ratingBar.getRating() + " star(s)");
                                message.setText(description.getText().toString()+"\n\n"+ getString(R.string.from_feedback_message) + " " + email.getText().toString());
                                Transport.send(message);
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), R.string.feedback_sent, Toast.LENGTH_SHORT).show();
                                    dismiss();
                                });
                            } catch (MessagingException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    });
                }});
        }
        private void showProgressBar(){
            sendingLayout.setVisibility(View.VISIBLE);
            inputLayout.setVisibility(View.GONE);
        }
        private void setCancel() {
            cancel.setOnClickListener(e -> {
                dismiss();
            });
        }
    }
}