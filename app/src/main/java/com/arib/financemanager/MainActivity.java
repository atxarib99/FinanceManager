package com.arib.financemanager;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.prefs.Preferences;

import static java.net.Proxy.Type.HTTP;

//TODO: LISTVIEW VIEW CHANGE ON CLICK
public class MainActivity extends Activity {

    //holds the expenses
    protected static ArrayList<Expenses> expenses;
    //holds the categories (for autocomplete)
    protected static ArrayList<String> categories;
    //The expense that is selected by long hold
    Expenses selectedExpense;
    //SMSManager service to receive texts
    SmsManager smsManager;
    //LOG_TAG for this class
    private final String LOG_TAG = this.getClass().getSimpleName();

    //handles full page ads (sorry guys need $)
    InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get the shared preference manager
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //add should read SMS preference if it does not exist
        if(!prefs.contains(getString(R.string.readsms_key))) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(getString(R.string.readsms_key), true);
            editor.commit();
        }
        //add should add star preference if it does not exist
        if(!prefs.contains(getString(R.string.addstar_key))) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(getString(R.string.addstar_key), true);
            editor.commit();
        }
        //add back up to drive preference if it does not exist
//        if(!prefs.contains(getString(R.string.drive_key))) {
//            SharedPreferences.Editor editor = prefs.edit();
//            editor.putBoolean(getString(R.string.drive_key), false);
//            getDriveAskDialog();
//        }

        //ask for permissions from the user
        askForPermissions();

        //launch the default SMSManager
        smsManager = SmsManager.getDefault();

        //get the action bar and set its background to the application color
        ActionBar bar = getActionBar();
        assert bar != null;
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#4E9455")));

        //set the view
        setContentView(R.layout.activity_main);

        //get the calendar object and get the month and the year
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        //get the month and year in a string format
        String strMonth = getMonth(month);

        //Display the month at the top of the screen
        TextView monthView = (TextView) findViewById(R.id.month);
        monthView.setText(strMonth);

        //create the DataFile object we will try to load the data file if it exists
        DataFile dataFile;
        try {
            //open an input stream with the month and year string as the file name
            FileInputStream fis = this.openFileInput(strMonth + year);
            //import the data as an object and cast it as a DataFile
            ObjectInputStream ois = new ObjectInputStream(fis);
            dataFile = (DataFile) ois.readObject();
            //the DataFile will hold all the data as a string/text so change that into an array
            expenses = getExpensesFromString(dataFile.toString());

            //if the array we just created is empty: the file is either corrupted or no data exists
            if(expenses.size() == 0) {
                //start with empty array
                expenses = new ArrayList<>();
                //tell the user that we did not load a file
                Toast.makeText(this, "Couldn't find this month's file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            //if there was an exception in loading the file display to the user there was an error
            Toast.makeText(this, "Couldn't find this month's file", Toast.LENGTH_SHORT).show();
            //start with empty array to prevent crash
            expenses = new ArrayList<>();
        }

        //start with empty categories
        categories = new ArrayList<>();

        //update the balance displayed at the top of the screen
        updateBalance();

        //update the listview from the array
        updateList();

        //once the listview is updated check if the list is empty
        if(!expenses.isEmpty())
            ((TextView) findViewById(R.id.mainactivity_helptext)).setText("");

        //set a listener to listen for the long click on a listview item
        setOptionsListener();

        //get the Intent; This is used to see if a notification launched the activity
        Intent intent = getIntent();
        //see if the activity was launched from a notification
        boolean openReadEncrypt = intent.getBooleanExtra(getString(R.string.notification_key), false);

        //if the activity was launched from a notification
        if(openReadEncrypt) {
            //get the message data from the intent
            String msg = intent.getStringExtra(getString(R.string.notification_data_key));
            //if the message isn't blank open the read encryption dialog
            if(!msg.equals("")) {
                readPublicFile(msg);
            }
        }

        //initialize mobile ads
        MobileAds.initialize(this, "ca-app-pub-4951063651201264/6004145428");

        //load an ad
        AdView mAdView = (AdView) findViewById(R.id.mainactivity_adView);
        AdRequest bannerAdRequest = new AdRequest.Builder().build();
        mAdView.loadAd(bannerAdRequest);

        //load full page ad
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-4951063651201264/2801490916");
        AdRequest interstitialAdRequest = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(interstitialAdRequest);

        //load new add on close
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

        });
    }

    //creates the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //on menu item selected
    public boolean onOptionsItemSelected(MenuItem item) {
        //get the id of the item selected
        int id = item.getItemId();

        //if it was the add button launch the add dialog
        if (id == R.id.action_addition) {
            getAddDialog().show();
        }

        //if it was the past data option launch the past month dialog to ask which data to open
        if (id == R.id.action_pastdata) {
            getPastMonthDialog().show();
        }

        if(id == R.id.action_share) {
            getShareDialog().show();
        }

        //if it was the graph option launch the graph activity
        if(id == R.id.action_graph) {

            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
                Log.d("TAG", "The interstitial was shown.");
            } else {
                Log.d("TAG", "The interstitial wasn't loaded yet.");
            }

            Intent intent = new Intent(this, GraphActivity.class);
            //give the balance in the intent
            intent.putExtra("Total Balance Main", updateBalance());
            //say that the intent is being sent from the "main" class
            intent.putExtra("type", "Main");
            //start the activity
            startActivity(intent);
        }

        //if the input data option is selected read open the dialog to past the data
        if(id == R.id.action_inputdata) {
            readPublicFile();
        }

        //if the user presses restore backup
        if(id == R.id.action_restorebackup) {
            //get the sharedpreferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            //declare a default string for comparison later
            String defaultString = "KEY_NOT_FOUND";
            //get the data from the backup storage and if none is found return the default string
            String backupData = prefs.getString(getString(R.string.backup_key), defaultString);
            //if we received the default string
            if(backupData.equals(defaultString)) {
                //tell the user the backup data does not exist
                Toast.makeText(MainActivity.this, "Backup not found!", Toast.LENGTH_LONG).show();
                //leave the method
                return false;
            }

            //if we found good data import it into the array and update UI
            expenses = getExpensesFromString(backupData);
            updateList();
            updateBalance();
            saveFile();
        }

        //if settings option is selected
        if(id == R.id.action_settings)
        {
            //start the settings activity
            startActivity(new Intent(this, SettingsActivity.class));
        }

        //if some option clicked that does not exist, let hell break loose
        return super.onOptionsItemSelected(item);
    }

    //default parameters for add dialog
    public Dialog getAddDialog() {
        return getAddDialog("", "0.0", "", null);
    }

    //creates the add dialog
    public Dialog getAddDialog(String stitle, String samount, String scategory,
                               final Expenses editExpense) {

        //create the calendar
        Calendar cal = Calendar.getInstance();
        final String date = cal.get(Calendar.MONTH) + 1 + "/" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.YEAR);

        //create a dialog object and give it the view from the layout folder
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.alertdialog_view);

        //update the autocomplete suggestions
        updateSuggestions(dialog);

        //set the title to the following string
        dialog.setTitle("Enter your expense");

        //Create an EditText object and link it to title edittext in the view
        EditText title = (EditText) dialog.findViewById(R.id.dialog_title);
        //set its text to the given value
        title.setText(stitle);

        //if the given amount is not 0
        if(!samount.equals("0.0")) {
            //Create an EditText object and link it to amount edittext in the view
            EditText amount = (EditText) dialog.findViewById(R.id.dialog_amount);
            //set the text
            amount.setText(samount + "");
        }

        //Create an EditText object and link it to category edittext in the view
        EditText category = (EditText) dialog.findViewById(R.id.dialog_category);
        //set the text to the given value
        category.setText(scategory + "");

        //create an object that will temporary hold the current expense
        final Expenses expensesToAdd;

        //if the given expense in the parameter is null create a new
        if(editExpense == null) {
            expensesToAdd = new Expenses();
        } else {
            //else get its reference so we can edit that reference
            expensesToAdd = editExpense;
        }

        //Create a button object and link it to the ok button in the view
        Button button = (Button) dialog.findViewById(R.id.adddialog_ok);
        //on ok click
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //get the amount title and category
                EditText amount = (EditText) dialog.findViewById(R.id.dialog_amount);
                EditText title = (EditText) dialog.findViewById(R.id.dialog_title);
                EditText category = (EditText) dialog.findViewById(R.id.dialog_category);

                //turn the amount into a string
                String str = amount.getText().toString();
                //get the amount as a double
                double amountD;

                //get the title and category that will be applied to the expense object
                String titleToApply = title.getText().toString();
                String categoryToApply = category.getText().toString();

                //try to parse the string we created into a double
                try {
                    amountD = Double.parseDouble(str);
                } catch (NumberFormatException e) {
                    //if we fail to parse properly tell the user to enter an amount
                    Toast.makeText(MainActivity.this, "Please enter an amount", Toast.LENGTH_LONG).show();
                    //finish this on click
                    return;
                }

                //if there is no title tell the user to enter a title
                if(titleToApply.equals("")) {
                    Toast.makeText(MainActivity.this, "Please enter a title", Toast.LENGTH_LONG).show();
                    return;
                }

                //if there is no category: set the category to other and let the user know it was set to Other
                if(categoryToApply.equals("")) {
                    categoryToApply = "Other";
                    Toast.makeText(MainActivity.this, "Category set to 'Other'", Toast.LENGTH_SHORT).show();
                }

                //apply the edit text data to the expense object
                expensesToAdd.setAmount(amountD);
                expensesToAdd.setTitle(titleToApply);
                expensesToAdd.setDate(date);
                expensesToAdd.setCategory(categoryToApply);

                //if we were not given a expense in the parameter add it to the array because it will not already exist
                if(editExpense == null)
                    expenses.add(expensesToAdd);

                //update UI
                updateList();
                updateBalance();

                //save the data to the phone
                saveFile();
                saveBackup();

                //close the dialog
                dialog.dismiss();

                //close the keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(),0);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
            }
        });

        //create a button object and link it to the cancel button in the view
        Button buttonCancel = (Button) dialog.findViewById(R.id.adddialog_cancel);
        //on click of the button
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //put the keyboard down
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(),0);
                //close the dialog and lose the data
                dialog.cancel();
            }
        });

        //finish method
        return dialog;
    }

    //get the option dialog
    public AlertDialog getOptionDialog() {

        //declare options
        CharSequence[] items = {"Edit", "Delete", "Info"};

        //Create a builder of an alertdialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //set the title to the following string
        builder.setTitle(getText(R.string.option_dialog_title));

        //set the items to the array declared earlier
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //on click of each item

                //if the first item is clicked "edit": launch the add dialog with this data already filled in
                //this effectively makes it an edit dialog
                if(which == 0) {
                    getAddDialog(selectedExpense.getTitle(), selectedExpense.getAmount() + "",
                            selectedExpense.getCategory(), selectedExpense).show();
                    dialog.dismiss();
                }

                //if the second item is clicked "delete": remove the data
                if(which == 1) {
                    //remove from the array
                    expenses.remove(selectedExpense);
                    //close the dialog
                    dialog.dismiss();
                    //update the UI
                    updateList();
                    updateBalance();
                    //Save to the file but not the backup!!!!
                    //this way the backup is effective
                    saveFile();
                }

                //if the second item is clicked "Info": launch the info dialog
                if(which == 2) {
                    getInfoDialog().show();
                    dialog.dismiss();
                }
            }
        });

        //return the dialog
        return builder.create();
    }

    //creates and returns the info dialog
    public Dialog getInfoDialog() {
        //create the dialog
        Dialog builder = new Dialog(this);
        //set the title to the following
        builder.setTitle("Info");
        //set the view to a layout from the layout folder
        builder.setContentView(R.layout.dialog_info);

        //Get the data from the selected object
        String title = selectedExpense.getTitle();
        String expense = String.format("%.2f", selectedExpense.getAmount()); //check locale
        expense = "$" + expense;
        String category = selectedExpense.getCategory();
        String date = selectedExpense.getDate();

        //Create textviews and link them to the view
        TextView titleView = (TextView) builder.findViewById(R.id.infodialog_title);
        TextView amountView = (TextView) builder.findViewById(R.id.infodialog_amount);
        TextView categoryView = (TextView) builder.findViewById(R.id.infodialog_category);
        TextView dateView = (TextView) builder.findViewById(R.id.infodialog_date);

        //set the data from the object to the textviews
        titleView.setText(title);
        amountView.setText(expense);
        categoryView.setText(category);
        dateView.setText(date);

        //return the dialog
        return builder;
    }

    //creates the past month dialog
    public Dialog getPastMonthDialog() {

        //Create an Alert dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //create an array adapter that holds filenames with a view provided in android sdk
        final ArrayAdapter<String> fileNames = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);

        //get the calendar object
        Calendar cal = Calendar.getInstance();

        //add the past three months to the arrayadapter
        for(int i = 3; i > 0; i--) {
            int month = cal.get(Calendar.MONTH) - i + 1;
            int year = cal.get(Calendar.YEAR);
            String monthName;
            if(month == 0) {
                month = 12;
                year--;
            }
            else if(month == -1) {
                month = 11;
                year--;
            }
            else if(month == -2) {
                month = 10;
                year--;
            }
            monthName = getMonth(month);
            fileNames.add(monthName + year);
        }

        //set the adapter to the view: creates the UI
        builder.setAdapter(fileNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = fileNames.getItem(which);
                openFile(fileName);
            }
        });

        //add a cancel button which closes the dialog
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //returns the AlertDialog
        return builder.create();
    }

    //creates the share dialog
    public Dialog getShareDialog() {
        //create the dialog
        final Dialog builder = new Dialog(this);
        //set the title to the following
        builder.setTitle("Share");
        //set the view to a layout from the layout folder
        builder.setContentView(R.layout.dialog_share);

        //link objects to button in the view
        Button encrypted = (Button) builder.findViewById(R.id.sharedialog_encrypt);
        Button unencrypted = (Button) builder.findViewById(R.id.sharedialog_unencrypted);

        //set on clicks
        encrypted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, 25);
                builder.dismiss();
            }
        });
        unencrypted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent smsIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(smsIntent, 26);
                builder.dismiss();
            }
        });

        //return the dialog
        return builder;
    }

    //creates dialog to ask user if they want to backup to drive
    public void getDriveAskDialog() {

        //get shared preference manager and editor
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = prefs.edit();

        //create AlertDialog that will prompt the user to allow drive access
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Allow Drive Access?");
        builder.setMessage("Allow this app to save data to your google drive account as cloud backup?");

        //set preference to true to allow
        builder.setPositiveButton("Allow", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                editor.putBoolean(getString(R.string.drive_key), true);
                editor.commit();
            }
        });

        //set preference to false to disallow
        builder.setNegativeButton("Don't Allow", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                editor.putBoolean(getString(R.string.drive_key), false);
                editor.commit();
            }
        });

        builder.create().show();

    }

    //saves the data to a backup location
    public void saveBackup() {
        //get the shared preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //edit the shared preferences
        SharedPreferences.Editor editor = prefs.edit();
        //put the data as a preference
        editor.putString(getString(R.string.backup_key), expenses.toString());
        //apply the data
        editor.apply();
    }

    //save the file in the default location
    public void saveFile() {
        //get the current month and year
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        //get the month as a string
        String strMonth = getMonth(month);

        //creates a datafile object with month and year and filename and array to save
        DataFile dataFile = new DataFile(strMonth + year, expenses);

        //try to save the data in object storage method
        try {
            FileOutputStream fos = this.openFileOutput(strMonth + year, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(dataFile);
        } catch (Exception e) {
            //if it fails let the user know that the save failed
            Toast.makeText(this, "Could not save this month's file", Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, e.toString());
        }
    }

    //opens a file with a given filename to be sent to the PastDataActivity
    public void openFile(String filename) {

        //create a DataFile object
        DataFile dataFile;

        //try to open the file
        try {
            FileInputStream fis = this.openFileInput(filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            //cast the object loaded as a DataFile
            dataFile = (DataFile) ois.readObject();

            //create the intent to launch past data
            Intent intent = new Intent(this, PastDataActivity.class);
            //put the data into the intent
            intent.putExtra("list", dataFile.toString());
            //put the filename into the intent
            intent.putExtra("month", filename.substring(0, filename.length() - 4));
            //start the activity
            startActivity(intent);
        } catch (Exception e) {
            //if the file couldn't be loaded tell the user we couldn't find it
            Toast.makeText(this, "Couldn't find this month's file", Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, e.toString());
        }
    }

    //default parameters for public file
    protected void readPublicFile() {
        readPublicFile("");
    }

    //reads encrypted data
    protected void readPublicFile(String encryptedFile) {

        //create dialog
        final Dialog enterEncryptedDataDialog = new Dialog(this);
        //set the view from the layout folder
        enterEncryptedDataDialog.setContentView(R.layout.dialog_readencrypteddata);
        //set the title to the following string
        enterEncryptedDataDialog.setTitle(getString(R.string.enterencrypteddatadialog_title));

        //create an EditText object and link it to the view
        final EditText textBox = (EditText) enterEncryptedDataDialog.findViewById(R.id.readencrypteddatadialog_textbox);
        //set the text to the parameter: could be blank
        textBox.setText(encryptedFile);

        //create ok, cancel, and view buttons and link them to the view
        Button addButton = (Button) enterEncryptedDataDialog.findViewById(R.id.readencrypteddatadialog_ok);
        Button cancelButton = (Button) enterEncryptedDataDialog.findViewById(R.id.readencrypteddatadialog_cancel);
        Button viewButton = (Button) enterEncryptedDataDialog.findViewById(R.id.readencrypteddatadialog_view);

        //set the on click for the ok button
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get the text from the edit text box
                String encryptedData = textBox.getText().toString();

                //boolean that holds that everything is fine
                boolean isOk = true;

                //if its blank say that no data was given and close
                if(encryptedData.equals("")) {
                    Toast.makeText(MainActivity.this, "No Data Given!", Toast.LENGTH_LONG).show();
                    enterEncryptedDataDialog.dismiss();
                    isOk = false;
                }

                //if data is ok
                if(isOk) {
                    //holds the decrypted data
                    String decryptedData;

                    //decrypt the data from the EncryptionManager class note:Not on Github for security reasons i guess
                    decryptedData = EncryptionManager.readEncryptedString(encryptedData);

                    //if the decrypted data returns the following string the data could not be parsed properly
                    if (decryptedData.equals("NODATAFOUND")) {
                        //Tell the user the data was not decrypted properly
                        Toast.makeText(MainActivity.this, "Decryption Error! No Expenses found", Toast.LENGTH_LONG).show();
                        //close the dialog
                        enterEncryptedDataDialog.dismiss();
                    } else {
                        //else if we do have good data

                        //store the new expenses into a new list
                        ArrayList<Expenses> newExpenses = getExpensesFromString(decryptedData);

                        //if the new list we just made has a size of 0, then there was bad or no data
                        if (newExpenses.size() == 0) {
                            Toast.makeText(MainActivity.this, "Decryption Error! No Expenses found", Toast.LENGTH_LONG).show();
                        }

                        //Get the preference manager and find out if the user has declared for there to be stars after added other user's data
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                        boolean addStar = prefs.getBoolean(getString(R.string.addstar_key), true);

                        //if the user has declared there to be a star then add a star to each expense in the newExpeneses list
                        if (addStar) {
                            for (Expenses e : newExpenses) {
                                e.setTitle(e.getTitle() + "*");
                            }
                        }

                        //add all the expenses from the newExpenses list to the list linked to the UI
                        expenses.addAll(newExpenses);

                        //close the dialog
                        enterEncryptedDataDialog.dismiss();

                        //save the data
                        saveFile();
                        saveBackup();

                        //update the UI
                        updateList();
                        updateBalance();
                    }
                }
            }
        });

        //onclick for the cancel button
        cancelButton.setOnClickListener(new View.OnClickListener() {
            //close the dialog and lose the data
            @Override
            public void onClick(View v) {
                enterEncryptedDataDialog.cancel();
            }
        });

        //onclick for the view button
        viewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get the text from the edit text box
                String encryptedData = textBox.getText().toString();

                //boolean that holds that everything is fine
                boolean isOk = true;

                //if its blank say that no data was given and close
                if(encryptedData.equals("")) {
                    Toast.makeText(MainActivity.this, "No Data Given!", Toast.LENGTH_LONG).show();
                    enterEncryptedDataDialog.dismiss();
                    isOk = false;
                }

                //if data is ok
                if(isOk) {
                    //holds the decrypted data
                    String decryptedData;

                    //decrypt the data from the EncryptionManager class note:Not on Github for security reasons i guess
                    decryptedData = EncryptionManager.readEncryptedString(encryptedData);

                    //if the decrypted data returns the following string the data could not be parsed properly
                    if (decryptedData.equals("NODATAFOUND")) {
                        //Tell the user the data was not decrypted properly
                        Toast.makeText(MainActivity.this, "Decryption Error! No Expenses found", Toast.LENGTH_LONG).show();
                        //close the dialog
                        enterEncryptedDataDialog.dismiss();
                    } else {
                        //else if we do have good data

                        //store the new expenses into a new list
                        ArrayList<Expenses> newExpenses = getExpensesFromString(decryptedData);

                        //if the new list we just made has a size of 0, then there was bad or no data
                        if (newExpenses.size() == 0) {
                            Toast.makeText(MainActivity.this, "Decryption Error! No Expenses found", Toast.LENGTH_LONG).show();
                        } else
                        {
                            //if the data is good launch PastDataActivity with the data we pulled
                            Intent viewIntent = new Intent(MainActivity.this, PastDataActivity.class);
                            viewIntent.putExtra("month", "Input");
                            viewIntent.putExtra("list", decryptedData);
                            startActivity(viewIntent);
                        }
                    }
                }


            }
        });

        //show the dialog
        enterEncryptedDataDialog.show();
    }

    //updates the listView UI element
    public void updateList() {
        //create a custrom adapter object
        MyListAdapter adapter = new MyListAdapter();

        //create a listview object and link it to the UI
        ListView listView = (ListView) findViewById(R.id.expensesList);

        //set the custom adapter to the listview object
        listView.setAdapter(adapter);

        //once the listview is updated check if the list is empty if not empty dont show help
        if(!expenses.isEmpty())
            ((TextView) findViewById(R.id.mainactivity_helptext)).setText("");
        else
            ((TextView) findViewById(R.id.mainactivity_helptext)).setText(R.string.mainactivity_emptylistviewtext);

    }

    //update the suggestions for categories
    public void updateSuggestions(Dialog dialog) {

        //create list of categories
        categories = new ArrayList<>();

        //for each expense we currently have, get the expense's category and add it to the list
        for(int i = 0; i < expenses.size(); i++) {
            if(!(categories.contains(expenses.get(i).getCategory()))) {
                categories.add(expenses.get(i).getCategory());
            }
        }

        //create an array from the list
        String[] aCategories = new String[categories.size()];
        for(int i = 0; i < aCategories.length; i++) {
            aCategories[i] = categories.get(i);
        }

        //if the length of the array is not 0, there are expenses in the array
        if(aCategories.length != 0) {
            //Create AutoCompleteTextView and link it to the UI
            AutoCompleteTextView actv = (AutoCompleteTextView) dialog.findViewById(R.id.dialog_category);

            //create an arrayadapter from the categories array
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, aCategories);

            //set the adapter to the TextView
            actv.setAdapter(categoryAdapter);
        }
    }

    //Update the balance portion of the UI
    public double updateBalance() {

        //variable that will hold the value
        double tempBalance = 0;

        //for each expense of the expenses array
        for(Expenses e : expenses) {
            //get the amount from the current expense and add its value to the tempBalance variable
            tempBalance += e.getAmount();
        }

        //Create TextView and link to the UI
        TextView balance = (TextView) findViewById(R.id.balance);

        //Locale.getDefault() get the location because letters vary in different regions of the world
        //format the decimal to 2 points
        String stringBalance = String.format(Locale.getDefault(), "%.2f", tempBalance);

        //set the balance with a preceding dollar sign to the textview
        balance.setText("$" + stringBalance);

        //return the value
        return tempBalance;
    }

    //sets the listener for when a user long presses an item
    public void setOptionsListener() {

        //get the list view and link it to the UI
        ListView listView = (ListView) findViewById(R.id.expensesList);

        //set the long clock listener
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                //Get control of the phones vibrate mechanism
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                //set the duration of the vibrate
                vibrator.vibrate(100);

                //get the selected item
                selectedExpense = expenses.get(position);

                //create the options dialog and show it
                getOptionDialog().show();

                //This properly updates the view NECESSARY
                parent.requestLayout();

                //update the view
                updateList();
                updateBalance();
                //save the file but not the backup because delete is possible
                saveFile();

                //return if the sizes changed
                return true;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(MainActivity.this, "Press and hold for options.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //get a string value of the month from an int
    public String getMonth(int intMonth) {

        //switch on a number and return the corresponding string
        switch (intMonth) {
            case 1 : return "January";
            case 2 : return "February";
            case 3 : return "March";
            case 4 : return "April";
            case 5 : return "May";
            case 6 : return "June";
            case 7 : return "July";
            case 8 : return "August";
            case 9 : return "September";
            case 10 : return "October";
            case 11 : return "November";
            case 12 : return "December";
            //heh
            default : return "lol garbo programmer";
        }
    }

    //gets a formatted string of data
    protected String getStringOfData() {

        //create string to return
        String stringOfData;

        //first line will be of the following format
        //Total Spent: $xxx.xx
        //follow by two new line
        stringOfData = "Total Spent: $" + String.format(Locale.getDefault(), "%.2f", updateBalance()) + "\n\n";

        //add a line that says "By Category" followed by newline
        stringOfData += "By Category\n";

        //create a variable that holds the total number of items
        int totalItems = MainActivity.expenses.size();

        //create a list of categories
        ArrayList<String> numberOfCats = new ArrayList<>();

        //for each item
        for(int i = 0; i < totalItems; i++) {
            //if the category currently does not exist in the list of categories
            if(!(numberOfCats.contains(MainActivity.expenses.get(i).getCategory()))) {
                //add the category
                numberOfCats.add(MainActivity.expenses.get(i).getCategory());
            }
        }

        //for each category
        for(int i = 0; i < numberOfCats.size(); i++) {
            //holds value of how much spent in this category
            double totalSpent = 0;

            //for each item in the list of expenses
            for (int k = 0; k < totalItems; k++) {
                //if the current items category matches the current category
                if (numberOfCats.get(i).equals(MainActivity.expenses.get(k).getCategory())) {
                    //add the current items amount to the totalSpent value
                    totalSpent += MainActivity.expenses.get(k).getAmount();
                }
            }

            //add a line of code that is of the format:
            //Category $xxx.xx
            //followed by newline
            stringOfData += numberOfCats.get(i) + " $" + String.format(Locale.getDefault(), "%.2f", totalSpent) + "\n";
        }

        //add empty line
        stringOfData += "\n";
        //Add string "By Item" and new line
        stringOfData += "By Item\n";

        //for each item in the list of expenses
        for(Expenses e : expenses) {
            //add a line of the following format
            //ItemName $xx.xx
            //followed by new line
            stringOfData += e.getTitle() + " $" + String.format(Locale.getDefault(), "%.2f", e.getAmount()) + "\n";
        }

        //return the string of data
        //the string of data will lead with the total amount spent then the amounts spent on each category then each item
        return stringOfData;
    }

    //get a list from a string value
    protected static ArrayList<Expenses> getExpensesFromString(String textualExpenses) {

        //note: this method takes the arraylist's to string and goes the other way
        //create a list of expenses that will be returned
        ArrayList<Expenses> returnable = new ArrayList<>();
        //if the array doesnt contain a '[' then its not an array return the blank array
        if(!textualExpenses.contains("["))
            return returnable;

        //create final string that removes all the [ from the parameter
        String finalString = textualExpenses.replace("[", "");
        //remove the ]
        finalString = finalString.replace("]", "");

        //split into an array of strings split up by commas
        String[] arrayToConvert = finalString.split(", ");

        //for each string in the array we just created
        for(String s : arrayToConvert) {
            //create a string array split up by -'s
            //each item in this array will be the individual components of an Expense object
            String[] indivExpense = s.split("-");

            //if the length of the array is less then 4 something is way wrong. we must have an array that is not of expenses
            if(indivExpense.length != 4) {
                //return a blank list
                returnable = new ArrayList<>();
                return returnable;
            }

            //the title will be the first element
            String title = indivExpense[0];
            //the amount is the second
            double amount = Double.parseDouble(indivExpense[1]);
            //category is third
            String category = indivExpense[2];
            //date is fourth
            String date = indivExpense[3];

            //add a new expense to the list we will return from the data we have
            returnable.add(new Expenses(title, amount, category, date));
        }

        //return the list
        return returnable;
    }

    //ask for permissions
    private void askForPermissions() {

        //check which permissions we currently have
        int permissionCheckReceive = this.checkSelfPermission(Manifest.permission.RECEIVE_SMS);
        int permissionCheckSend = this.checkSelfPermission(Manifest.permission.SEND_SMS);
        int permissionCheckContacts = this.checkSelfPermission(Manifest.permission.READ_CONTACTS);
        int permissionCheckPhoneState = this.checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
        Log.d(LOG_TAG, "RECEIVE PERMISSION " + permissionCheckReceive);
        Log.d(LOG_TAG, "SEND PERMISSION " + permissionCheckSend);
        Log.d(LOG_TAG, "READ CONTACTS " + permissionCheckContacts);
        Log.d(LOG_TAG, "READ PHONE STATE " + permissionCheckPhoneState);

        requestPermissions(new String[] {Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS,
                Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE}, 22);

        //check what permissions we have after asking
        permissionCheckReceive = this.checkSelfPermission(Manifest.permission.RECEIVE_SMS);
        permissionCheckSend = this.checkSelfPermission(Manifest.permission.SEND_SMS);
        permissionCheckContacts = this.checkSelfPermission(Manifest.permission.READ_CONTACTS);
        permissionCheckPhoneState = this.checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
        Log.d(LOG_TAG, "RECEIVE PERMISSION " + permissionCheckReceive);
        Log.d(LOG_TAG, "SEND PERMISSION " + permissionCheckSend);
        Log.d(LOG_TAG, "READ CONTACTS " + permissionCheckContacts);
        Log.d(LOG_TAG, "READ PHONE STATE " + permissionCheckPhoneState);

        //if we dont have the consent to read texts.. don't (if only society understood this)
        if(permissionCheckReceive == -1) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(getString(R.string.readsms_key), false);
            edit.commit();
        }

    }

    //handles what to do when we come back from an activity like contacts
    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        //switch on the request code
        switch (reqCode) {

            //request code for getting a contact to send them encrypted data
            case (25) :
                //if the result came back okay
                if (resultCode == Activity.RESULT_OK) {
                    //get the contact data as a Uri object
                    Uri contactData = data.getData();

                    //Create a cursor to navigate through the Uri
                    Cursor c =  getContentResolver().query(contactData, null, null, null, null);
                    //get the object that will get the data in the proper format
                    ContentResolver contact_resolver = getContentResolver();
                    //if we move to the front without an error
                    if (c.moveToFirst()) {
                        //get the id of the contact
                        String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                        //create a cursor for that contact
                        Cursor phoneCur = contact_resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { id }, null);

                        //if we move to the front of the contact without error
                        if (phoneCur.moveToFirst()) {
                            //get the phone number
                            String no = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            //create the encryption data with the tag
                            String smsBody = "!#@$" + EncryptionManager.createEncryptedString(expenses.toString());
                            //divide the message into parts (this is important because some carriers do this in the background
                            ArrayList<String> parts = smsManager.divideMessage(smsBody);
                            //send all the parts of the message
                            smsManager.sendMultipartTextMessage(no, null, parts, null, null);
                        }

                    }
                    // close the cursor
                    c.close();
                }
                break;
            //request code for sending a contact the data (follows the same procedure as above)
            case(26) :
                Calendar cal = Calendar.getInstance();
                if(resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor c = getContentResolver().query(contactData, null, null, null, null);
                    ContentResolver contactResolver = getContentResolver();
                    if(c.moveToFirst()) {
                        String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                        Cursor phoneCur = contactResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { id }, null);
                        if(phoneCur.moveToFirst()) {
                            String no = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            Log.d(LOG_TAG, no);

                            String smsBody = "Invoice of: " + getMonth(cal.get(Calendar.MONTH) + 1) + "\n\n";
                            smsBody += getStringOfData();
                            ArrayList<String> parts = smsManager.divideMessage(smsBody);
                            smsManager.sendMultipartTextMessage(no, null, parts, null, null);
                        }
                    }
                    c.close();
                }
        }
    }

    //List adapter for the main listview
    private class MyListAdapter extends ArrayAdapter<Expenses> {

        //constructor
        MyListAdapter() {
            super(MainActivity.this, R.layout.listview_item, expenses);
        }

        //get the view of the item
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            //get the item view
            View itemView = convertView;
            if(itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.listview_item, parent, false);
            }

            //get the values to set
            Expenses currentExpense = expenses.get(position);
            final String title = currentExpense.getTitle();
            final String amount = "$" + String.format(Locale.getDefault(), "%.2f", currentExpense.getAmount());

            //get the textviews
            TextView expensesTitle = (TextView) itemView.findViewById(R.id.expensetitle);
            TextView expensesAmount = (TextView) itemView.findViewById(R.id.expenseamount);


            //set the textviews to the values
            expensesTitle.setText(title);
            expensesAmount.setText(amount);

            //return the View as requested
            return itemView;

        }

    }
}
