package com.bueroservice.spechtest;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
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
            Arrays.asList("Ulf", "Johanna", "Magret", "Carina", "Björn", "Nora"));
    private static final List<String> possibleFood = new ArrayList<>(
            Arrays.asList("Vegetarisch", "Bürgerlich", "Topf", "Diät", "Moslem", "Fettreduziert"));
    private static final List<String> possibleDays = new ArrayList<>(
            Arrays.asList("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag"));
    private static final List<String> possibleRemoveCommands = new ArrayList<>(Arrays.asList("entfernen", "löschen"));

    private TableLayout entryTable;
    private TextToSpeechUtil textToSpeech;

    // Later it has to be Map<String (Name der Person), Map<Date, String (Gericht)>)
    // to store the selection for multiple dates
    private Map<String, Map<String, String>> entries; // Map<String (Name der Person), Map<String (Wochentag), String (Gericht)>>

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
        // possibly complete.

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
            if (entryParts.length >= 1) {
                // search for person name, day & food
                String person = searchForWord(entryParts, possiblePersonNames);
                String food = searchForWord(entryParts, possibleFood);
                String day = searchForWord(entryParts, possibleDays);
                boolean foodFound = !food.isEmpty();
                boolean personFound = !person.isEmpty();
                boolean dayFound = !day.isEmpty();
                boolean removeCommandFound = !searchForWord(entryParts, possibleRemoveCommands).isEmpty();

                ArrayList<String> missingInformation = new ArrayList<>();
                if (!personFound) missingInformation.add("Name der Person");
                if (!dayFound) missingInformation.add("Wochentag");
                if ((!foodFound) && (!removeCommandFound)) missingInformation.add("Gericht");

                // input validation
                if (missingInformation.size() > 0) {
                    String msg;
                    switch (missingInformation.size()) {
                        case 1:
                            msg = String.format("%s konnte nicht erkannt werden", missingInformation.get(0));
                            break;
                        case 2:
                            msg = String.format("%s und %s konnten nicht erkannt werden", missingInformation.get(0), missingInformation.get(1));
                            break;
                        default:
                            msg = "Eingabe konnte nicht verstanden werden";
                            break;
                    }
                    notification(msg);
                } else {
                    // FOUND EVERYTHING
                    try {
                        entries.computeIfAbsent(person, k -> new HashMap<String, String>());    // add new person if no map for person yet
                        Map<String, String> mapForPerson = entries.get(person);
                        assert mapForPerson != null;
                        boolean wasUpdated = false;
                        if (removeCommandFound) {
                            // Eintrag aus Liste löschen
                            if (mapForPerson.containsKey(day)) {
                                food = mapForPerson.get(day);
                                mapForPerson.remove(day);
                                wasUpdated = true;
                            } else
                                notification("" + person + " hat bisher keinen Eintrag für " + day);
                        } else {
                            // Hinzufügen des Eintrags zur Liste
                            mapForPerson.put(day, food);
                            wasUpdated = true;
                        }
                        if (wasUpdated) {
                            System.out.println("Map updated");

                            // Aktualisieren der Ansicht
                            updateEntryTable();

                            String text;
                            if (removeCommandFound) text = "Eintragt entfernt: ";
                            else text = "Eintrag hinzugefügt: ";
                            text = text + person + " " + day + " " + food;
                            notification(text);
                        }
                    } catch (Exception e) {
                        notification("Eingabe konnte nicht verstanden werden");
                        System.out.println("Recognized entry parts: " + Arrays.toString(entryParts));
                        System.out.println("Error: " + e.toString());
                    }
                }
            } else {
                notification("Eingabe konnte nicht verstanden werden.");
            }
        }
    }

    private void updateEntryTable() {
        entryTable.removeAllViews();    // clear all views

        TableRow headerRow = new TableRow(this);
        TextView personHeader = createTextView("Person");
        personHeader.setTypeface(null, Typeface.BOLD);
        ;
        headerRow.addView(personHeader);

        // add days of week to header row
        for (String day : possibleDays) {
            TextView dayTextView = createTextView(day);
            dayTextView.setTypeface(null, Typeface.BOLD);
            ;
            headerRow.addView(dayTextView);
        }
        entryTable.addView(headerRow);  // add header row to table

        // add rows with content
        for (String person : entries.keySet()) {
            // add row for current person
            Map<String, String> mapForPerson = entries.get(person);
            TableRow row = new TableRow(this);
            TextView personTextView = createTextView(person); // TextView for name of person
            row.addView(personTextView);
            for (String day : possibleDays) {
                String food = mapForPerson != null ? mapForPerson.get(day) : "";
                TextView foodTextView = createTextView(food); // TextView for food choice of person
                row.addView(foodTextView);
            }
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

    private String searchForWord(String[] recognizedWords, List<String> possibleOptions) {
        for (String word : recognizedWords) {
            // search for name of person
            for (String option : possibleOptions) {
                if (word.equalsIgnoreCase(option)) {
                    // option was found
                    return option;
                }
            }
        }
        return "";
    }
}
