package com.bueroservice.spechtest;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int SPEECH_REQUEST_CODE = 123;
    private static final List<String> possiblePersonNames = new ArrayList<>(
            Arrays.asList("Tim", "Max", "Tina", "Fareed", "Moayad", "Dimitri", "Leon"));
    private static final List<String> possibleFood = new ArrayList<>(
            Arrays.asList("Fleisch", "Vegetarisch", "Vegan", "Lite"));

    private TableLayout entryTable;
    private TextToSpeechUtil textToSpeech;

    // Later it has to be Map<String (Name der Person), Map<Date, String (Gericht)>)
    // to store the selection for multiple dates
    private Map<String, String> entries; // Map<String (Name der Person), String (Gericht)>

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        entryTable = findViewById(R.id.entryTable);
        entries = new HashMap<>();
        textToSpeech = new TextToSpeechUtil();

        Button addEntryButton = findViewById(R.id.addEntryButton);
        addEntryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpeechRecognition();
            }
        });

        Animation anim = new ScaleAnimation(
                1.0f, 1.2f, // Start- und Endskalierungsfaktor
                1.0f, 1.2f, // Start- und Endskalierungsfaktor
                Animation.RELATIVE_TO_SELF, 0.5f, // Skalierung um die X-Achse
                Animation.RELATIVE_TO_SELF, 0.5f // Skalierung um die Y-Achse
        );
        anim.setDuration(1000); // Setzen Sie hier die gewünschte Pulsdauer in Millisekunden ein
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setRepeatCount(Animation.INFINITE);
        anim.setRepeatMode(Animation.REVERSE);
        addEntryButton.startAnimation(anim);
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Sprechen Sie Ihre gewünschte Person, das Datum und das Essen ein.");

        // increase recording duration
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, "5000"); // The amount of
                                                                                                     // time that it
                                                                                                     // should take
                                                                                                     // after we stop
                                                                                                     // hearing speech
                                                                                                     // to consider the
                                                                                                     // input complete.
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, "5000"); // The
                                                                                                              // amount
                                                                                                              // of time
                                                                                                              // that it
                                                                                                              // should
                                                                                                              // take
                                                                                                              // after
                                                                                                              // we stop
                                                                                                              // hearing
                                                                                                              // speech
                                                                                                              // to
                                                                                                              // consider
                                                                                                              // the
                                                                                                              // input
                                                                                                              // possibly
                                                                                                              // complete.

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            notification("Spracherkennung wird auf diesem Gerät nicht unterstützt.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);

            // Extrahieren von Person, Datum und Gericht aus dem gesprochenen Text
            String[] entryParts = spokenText.split("\\s+");
            if (entryParts.length >= 2) {
                // search for person name & food
                boolean personFound = false;
                boolean foodFound = false;
                String person = "";
                String food = "";
                for (String word : entryParts) {
                    if (!personFound) {
                        // search for name of person
                        for (String personName : possiblePersonNames) {
                            if (word.equalsIgnoreCase(personName)) {
                                // name was found
                                person = personName;
                                personFound = true;
                                break;
                            }
                        }
                    } else if (!foodFound) {
                        // search for food
                        for (String possibleFoodValue : possibleFood) {
                            if (word.equalsIgnoreCase(possibleFoodValue)) {
                                // food was found
                                food = possibleFoodValue;
                                foodFound = true;
                                break;
                            }
                        }
                    } else
                        break;
                }

                if (!personFound) {
                    // the person was not found --> search again for food to give feedback about
                    // food
                    for (String word : entryParts) {
                        // search for food
                        if (foodFound)
                            break;
                        for (String possibleFoodValue : possibleFood) {
                            if (word.equalsIgnoreCase(possibleFoodValue)) {
                                // food was found
                                food = possibleFoodValue;
                                foodFound = true;
                                break;
                            }
                        }
                    }
                }

                // input validation
                if ((!foodFound) && (!personFound)) {
                    // NO person & NO food
                    notification("Name und Gericht konnten nicht erkannt werden");
                } else if (!personFound) {
                    // NO person
                    notification("Der Name der Person konnte nicht erkannt werden");
                } else if (!foodFound) {
                    // NO person
                    notification("Das Gericht konnte nicht erkannt werden");
                } else {
                    // FOUND name of person & food
                    try {
                        // Hinzufügen des Eintrags zur Liste
                        entries.put(person, food);
                        System.out.println("Map updated");

                        // Aktualisieren der Ansicht
                        updateEntryTable();

                        notification("Eintrag hinzugefügt: " + person + " " + food);
                    } catch (Exception e) {
                        notification("Eingabe konnte nicht verstanden werden");
                        System.out.println("Recognized entry parts: " + Arrays.toString(entryParts));
                        System.out.println("Error: " + e.toString());
                    }
                }
            } else {
                notification("Eingabe konnte nicht verstanden werden");
            }
        }
    }

    private void updateEntryTable() {
        entryTable.removeAllViews();

        TableRow headerRow = new TableRow(this);
        TextView personHeader = createTextView("Person");
        TextView foodHeader = createTextView("Gericht");
        headerRow.addView(personHeader);
        headerRow.addView(foodHeader);
        entryTable.addView(headerRow);

        for (String person : entries.keySet()) {
            String food = entries.get(person);
            TableRow row = new TableRow(this);
            TextView personTextView = createTextView(person); // TextView for name of person
            TextView foodTextView = createTextView(food); // TextView for food choice of person
            row.addView(personTextView);
            row.addView(foodTextView);
            entryTable.addView(row);
        }
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(16, 8, 16, 8);
        return textView;
    }

    private void notification(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        textToSpeech.speakText(this, text);
    }
}
