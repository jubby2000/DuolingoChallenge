package com.example.jacob.duolingochallenge;

import android.app.ProgressDialog;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    String url;
    ArrayList<String> letters;
    TextView introTextView;
    TextView wordTextView;
    ProgressDialog progressDialog;
    String jsonDataString;
    int mColumnsAndRows;
    boolean hintShown = false;
    //This needs to be -1 to be outside of the gridview range initially
    int downPosition = -1;
    FloatingActionButton mClearFab;
    FloatingActionButton mConfirmFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        jsonDataString = null;
        mColumnsAndRows = 0;
        progressDialog = new ProgressDialog(this);
        url = getString(R.string.JSON_URL);
        introTextView = (TextView) findViewById(R.id.intro_text_view);
        wordTextView = (TextView) findViewById(R.id.word_text_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mClearFab = (FloatingActionButton) findViewById(R.id.reset_fab);
        mConfirmFab = (FloatingActionButton) findViewById(R.id.confirm_fab);

        setSupportActionBar(toolbar);

        //request the word bank once the app launches
        new GetWordBank().execute();

//        Animation hideAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_hide);
//        mClearFab.setVisibility(View.INVISIBLE);
//        mConfirmFab.setVisibility(View.INVISIBLE);
        mClearFab.hide();
        mConfirmFab.hide();
//        mClearFab.startAnimation(hideAnimation);
//        mConfirmFab.startAnimation(hideAnimation);

        //We'll use the fab here to get the data manually (if the connection was unstable at first) or to get a new word.
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Get new words
                if (jsonDataString == null) {
                    new GetWordBank().execute();
                } else {
                    try {
                        //Look through existing data for a new word without having to query again
                        JSONObject jObject = new JSONObject(jsonDataString);
                        JSONArray jArray = jObject.getJSONArray("word_bank");
                        jObject = getNewRandomWordFromJSONArray(jArray);
                        setUpNewViews(jObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                //Once we've reset with a new word, or have completely new data, we don't need these buttons yet
                if (mClearFab.isShown() && mConfirmFab.isShown()) {
                    mClearFab.hide();
                    mConfirmFab.hide();
                }
            }
        });



        //TODO add onTouchListeners for each JSON object

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class GetWordBank extends AsyncTask <String, Void, String> {

        @Override
        protected void onPreExecute() {
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Gimme just a sec...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.show();

        }

        @Override
        protected String doInBackground(String... strings) {
            try {

                // Create a URL for the 'string' url
                URL realUrl = new URL(url);

                //Build a string from the separate JSON lines in the given txt file
                StringBuilder fullString = new StringBuilder();

                // Read all the text returned by the server
                BufferedReader in = new BufferedReader(new InputStreamReader(realUrl.openStream()));

                //string that we'll iterate over to pass to the string builder
                String str;

                while ((str = in.readLine()) != null) {
                    fullString.append(str);
                }
                in.close();

                return fullString.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
            }

            return null;
        }

        @Override
        protected void onPostExecute(String string) {
            //this string contains the JSON that we need to manipulate
            if (string != null) {
                Log.v(LOG_TAG, string);

                JSONArray jArray;
                JSONObject jObject;

                try {
                    //Fix the broken file, parse it, get a new random word (as a method so it can be reused upon success),
                    //and update the views for the same reason.
                    jsonDataString = fixTrickyTxtFileThatDuolingoSentOver(string);
                    jObject = new JSONObject(jsonDataString);
                    jArray = jObject.getJSONArray("word_bank");
                    jObject = getNewRandomWordFromJSONArray(jArray);
                    setUpNewViews(jObject);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                //Unable to get the data from the server, throw an error and an option to retry.
                Snackbar.make(findViewById(R.id.toolbar), "Oops, I couldn't get any words from the server.", Snackbar.LENGTH_LONG)
                        .setAction("Retry", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                new GetWordBank().execute();
                            }
                        })
                        .show();

            }


//            AlphaAnimation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
//            fadeOutAnimation.setDuration(1000);
//            fadeOutAnimation.setFillAfter(true);
//
//            progressDialog.startAnimation(fadeOutAnimation);
            progressDialog.dismiss();
        }
    }

    private String fixTrickyTxtFileThatDuolingoSentOver(String string) {
        //Reformat the given string so that it has one parent, as JSON expects
        string = "{\"word_bank\": [" + string + "]}";

        //More reformatting to add commas inbetween objects, again, JSON
        string = string.replace("\"target_language\": \"es\"}{",
                "\"target_language\": \"es\"},{");

        return string;
    }

    private JSONObject getNewRandomWordFromJSONArray(JSONArray jArray) throws JSONException {
        Random randomizer = new Random();
        int randomWord = randomizer.nextInt(jArray.length());

        return (JSONObject) jArray.get(randomWord);
    }

    private void setUpNewViews(JSONObject jObject) throws JSONException {

        String sourceLanguage = jObject.getString("source_language");
        String targetLanguage = jObject.getString("target_language");
        String sourceWord = jObject.getString("word");

        String wordLocations = jObject.getString("word_locations");
        wordLocations = wordLocations.replace("\",\"", "\":\"").replace("{", "").replace("}", "");

        String[] newLocation = wordLocations.split(":");

        ArrayList<String> finalList = new ArrayList<>();
        for (int i=0; i<newLocation.length; i+=2) {
                finalList.add(newLocation[i]);
        }

        final ArrayList<int[]> fixedList = new ArrayList<>();
        for (int i=0; i<finalList.size(); i++) {

            String word = finalList.get(i).replace(",", "").replace("\"", "");
            Log.v(LOG_TAG, word);
            for (int i2=2; i2<=word.length(); i2+=2) {
                int xCoord = Integer.parseInt(word.substring(i2-2, i2-1));
                int yCoord = Integer.parseInt(word.substring(i2-1, i2));

                fixedList.add(new int[]{xCoord, yCoord});
            }
        }
        Collections.sort(fixedList, new Comparator<int[]>() {
            @Override
            public int compare(int[] ints, int[] t1) {
                return t1[0] - ints[0];
            }
        });


        Log.v(LOG_TAG, Arrays.toString(fixedList.toArray()));

        JSONArray jsonArray = jObject.getJSONArray("character_grid");
        mColumnsAndRows = jsonArray.length();

        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < jsonArray.length(); i++) {

            list.add(jsonArray.getJSONArray(i)
                    .toString()
                    .replace("[", "")
                    .replace("\"", "")
                    .replace("]", ""));

            final GridView gridView = (GridView) findViewById(R.id.gridView);
            gridView.setNumColumns(mColumnsAndRows);
            gridView.setAdapter(new GridAdapter(list));

            gridView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {

                    int position = gridView.pointToPosition((int) motionEvent.getX(),
                            (int) motionEvent.getY());

                    if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                        //Set the initial downPosition for the subsequent move to check against
                        downPosition = position;

                        //We're using colors to signify as the 'selected' letters
                        TextView textView = (TextView) gridView.getChildAt(position);

                        if (textView != null) {


                            ColorDrawable cd = (ColorDrawable) textView.getBackground();
                            final int colorSelected = ContextCompat
                                    .getColor(getApplicationContext(),
                                            android.R.color.holo_green_light);

                            if (cd == null || cd.getColor() != colorSelected) {
                                textView.setBackgroundColor(colorSelected);

                                //Show the extra fabs once something has been selected
                                if (!mClearFab.isShown() && !mConfirmFab.isShown()) {
                                    mClearFab.show();
                                    mConfirmFab.show();
                                }

                                mConfirmFab.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                        ArrayList<int[]> submittedList = new ArrayList<>();
                                        for (int i = 0; i < gridView.getChildCount(); i++) {
                                            int column = i % mColumnsAndRows;
                                            int row = i / mColumnsAndRows;

                                            TextView child = (TextView) gridView.getChildAt(i);
                                            ColorDrawable cd = (ColorDrawable) child.getBackground();
                                            if (cd != null) {
                                                if (cd.getColor() == colorSelected) {
                                                    submittedList.add(new int[]{column, row});
                                                    Log.v(LOG_TAG, String.valueOf(column + "," + row));
                                                }
                                            }

                                        }

                                        Collections.sort(submittedList, new Comparator<int[]>() {
                                            @Override
                                            public int compare(int[] ints, int[] t1) {
                                                return t1[0] - ints[0];
                                            }
                                        });

                                        Log.v(LOG_TAG, Arrays.toString(submittedList.toArray()));


                                        Log.v(LOG_TAG, String.valueOf("submitted size = " + submittedList.size()) + " fixed size = " + fixedList.size());

                                        boolean areEqual = Arrays.deepEquals(fixedList.toArray(), submittedList.toArray());

//                                        if (submittedList.size() != fixedList.size()) {
//                                            for (int i=0; i<fixedList.size(); i++) {
//                                                if (!fixedList.contains(submittedList.get(i))) {
//                                                    Snackbar.make(view, "Nope, that's not quite right.", Snackbar.LENGTH_LONG).show();
//                                                }
//                                            }
//                                            Snackbar.make(view, "Hey, you did it!", Snackbar.LENGTH_LONG).show();
//                                        } else {
//                                            Snackbar.make(view, "Nope, that's not quite right.", Snackbar.LENGTH_LONG).show();
//                                        }


                                        if (!areEqual) {
                                            Snackbar.make(view, "Nope, that's not quite right.", Snackbar.LENGTH_LONG).show();
                                        } else {
                                            Snackbar.make(view, "Hey, you did it!", Snackbar.LENGTH_LONG).show();
                                        }


                                    }
                                });

                                mClearFab.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        for (int i = 0; i < gridView.getChildCount(); i++) {
                                            TextView child = (TextView) gridView.getChildAt(i);
                                            child.setBackgroundColor(0);
                                        }
                                        mClearFab.hide();
                                        mConfirmFab.hide();
                                    }
                                });

                            } else if (cd.getColor() == colorSelected) {
                                textView.setBackgroundColor(0);
                            }
                        }
                        return true;
                    }

                    if(motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                        //We need to check if the motion has moved into a new gridview position
                        //If it has, toggle the selection and update the position, if it hasn't do nothing
                        //Otherwise the code will run contstantly and toggle the color until you let go
                        if (downPosition != position) {
                            TextView textView = (TextView) gridView.getChildAt(position);
                            if (textView != null) {
                                ColorDrawable cd = (ColorDrawable) textView.getBackground();
                                final int colorSelected = ContextCompat
                                        .getColor(getApplicationContext(),
                                                android.R.color.holo_green_light);
                                if (cd == null || cd.getColor() != colorSelected) {
                                    textView.setBackgroundColor(colorSelected);
                                } else {
                                    textView.setBackgroundColor(0);
                                }
                            }
                            downPosition = position;
                            return true;
                        }
                    }

                    if(motionEvent.getAction() == MotionEvent.ACTION_UP) {

                        //Show a hint on the first time only
                        if (!hintShown) {
                            Snackbar.make(view, "Hint: Tap letters to unselect.",
                                    Snackbar.LENGTH_LONG)
                                    .setAction("Ok", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    //Leaving this empty serves the dismiss function
                                }
                            }).show();
                            hintShown = true;
                        }
                        return true;
                    }
                    return false;
                }
            });

            //Only want to use English terms if the source is in English
            if (sourceLanguage.equals("en")) {
                sourceLanguage = "English";

                if (targetLanguage.equals("es")) {
                    targetLanguage = "Spanish";
                }
            }

            //If the source is in Spanish, for example, we need to use Spanish terms
            if (sourceLanguage.equals("es")) {
                sourceLanguage = "Espa\u00F1ol";

                if (targetLanguage.equals("en")) {
                    targetLanguage = "Ingl\u00E9s";
                }
            }

            String introString = getString(R.string.native_word_intro_1)
                    + " " + targetLanguage
                    + " " + getString(R.string.native_word_intro_2)
                    + " " + sourceLanguage
                    + " " + getString(R.string.native_word_intro_3);

            introTextView.setText(introString);
            wordTextView.setText(sourceWord);
        }
    }

    private final class GridAdapter extends BaseAdapter {

        final ArrayList<String> mLetters;
        final int mCount;
        boolean mSelected;

        private GridAdapter(final ArrayList<String> letters) {

            mCount = letters.size() * mColumnsAndRows;
            mLetters = new ArrayList<String>(mCount);
            mSelected = false;

            // for small size of items it's ok to do it here, sync way
            for (String letter : letters) {
                // get separate string parts, divided by ,
                final String[] parts = letter.split(",");

                // remove spaces from parts
                for (String part : parts) {
                    part.replace(" ", "");
                    mLetters.add(part);
                }
            }
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public Object getItem(int i) {
            return mLetters.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        public boolean isSelected() {
            return mSelected;
        }

        public void setSelected(boolean selected) {
            mSelected = selected;
        }

        public void toggle() {
            setSelected(!mSelected);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            if (view == null) {
                view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(android.R.layout.simple_list_item_1, viewGroup, false);
                view.setPadding(0,0,0,0);

                //We want to adapt the size of the grid based on the size of the device
                int width = Resources.getSystem().getDisplayMetrics().widthPixels;

                //We also want it to be a square grid, so set the height equal to the width
                view.setLayoutParams(new GridView.LayoutParams(width/mColumnsAndRows, width/mColumnsAndRows));
            }

            final TextView text = (TextView) view.findViewById(android.R.id.text1);

            text.setGravity(Gravity.CENTER);
            text.setText(mLetters.get(i));

            return view;
        }
    }

    public static boolean deepContains(List<Integer[]> list, Integer[] probe) {
        for (Integer[] element : list) {
            if (Arrays.deepEquals(element, probe)) {
                return true;
            }
        }
        return false;
    }
}
