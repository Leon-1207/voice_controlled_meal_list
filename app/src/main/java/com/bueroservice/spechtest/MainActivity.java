package com.bueroservice.spechtest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int SPEECH_REQUEST_CODE = 123;

    private Spinner foodSpinner;
    private TableLayout entryTable;

    private Map<String, Map<Date, String>> entries = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button addEntryButton = findViewById(R.id.addEntryButton);
        addEntryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpeechRecognition();
            }
        });
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Sprechen Sie Ihre gewünschte Person, das Datum und das Essen ein.");

        // increase recording duration
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, "5000");    // The amount of time that it should take after we stop hearing speech to consider the input complete.
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, "5000"); //  The amount of time that it should take after we stop hearing speech to consider the input possibly complete.

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Spracherkennung wird auf diesem Gerät nicht unterstützt.", Toast.LENGTH_SHORT).show();
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
            if (entryParts.length >= 3) {
                String person = entryParts[0];
                String dateString = entryParts[1];
                String food = entryParts[2];

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                try {
                    Date date = dateFormat.parse(dateString);

                    // Hinzufügen des Eintrags zur Liste
                    Map<Date, String> personEntries = entries.getOrDefault(person, new HashMap<>());
                    personEntries.put(date, food);
                    entries.put(person, personEntries);

                    // Aktualisieren der Ansicht
                    updateEntryTable();

                    Toast.makeText(this, "Eintrag hinzugefügt: " + spokenText, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Ungültiges Datumsformat.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Ungültiger Eintrag.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateEntryTable() {
        entryTable.removeAllViews();

        TableRow headerRow = new TableRow(this);
        TextView personHeader = createTextView("Person");
        TextView dateHeader = createTextView("Datum");
        TextView foodHeader = createTextView("Gericht");
        headerRow.addView(personHeader);
        headerRow.addView(dateHeader);
        headerRow.addView(foodHeader);
        entryTable.addView(headerRow);

        for (String person : entries.keySet()) {
            Map<Date, String> personEntries = entries.get(person);

            for (Date date : personEntries.keySet()) {
                String food = personEntries.get(date);

                TableRow row = new TableRow(this);
                TextView personTextView = createTextView(person);
                TextView dateTextView = createTextView(formatDate(date));
                TextView foodTextView = createTextView(food);
                row.addView(personTextView);
                row.addView(dateTextView);
                row.addView(foodTextView);
                entryTable.addView(row);
            }
        }
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(16, 8, 16, 8);
        return textView;
    }

    private String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return dateFormat.format(date);
    }
}
