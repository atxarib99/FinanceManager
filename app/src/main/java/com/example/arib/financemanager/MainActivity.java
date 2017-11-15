package com.example.arib.financemanager;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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

//TODO: GRAPHICAL INTERFACE
public class MainActivity extends Activity {

    protected static ArrayList<Expenses> expenses;
    protected static ArrayList<String> categories;
    protected static ArrayList<String> receivedTexts;
    private String sendingNumber;
    Expenses selectedExpense;
    SmsManager smsManager;
    private final String LOG_TAG = this.getClass().getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        boolean openReadEncrypt = intent.getBooleanExtra(getString(R.string.notification_key), false);
        Log.d(LOG_TAG, openReadEncrypt + ": should we open encrypt dialog");
        askForPermissions();
        smsManager = SmsManager.getDefault();
        sendingNumber = "";
        ActionBar bar = getActionBar();
        assert bar != null;
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#4E9455")));
        setContentView(R.layout.activity_main);
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        String strMonth = getMonth(month);
        Log.d(LOG_TAG, strMonth + year);
        DataFile dataFile;
        try {
            FileInputStream fis = this.openFileInput(strMonth + year);
            ObjectInputStream ois = new ObjectInputStream(fis);
            dataFile = (DataFile) ois.readObject();
            expenses = getExpensesFromString(dataFile.toString());
            Log.d(LOG_TAG, ""+expenses.size() + expenses.toString());
            if(expenses.size() == 0) {
                expenses = new ArrayList<>();
                Toast.makeText(this, "Couldn't find this month's file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't find this month's file", Toast.LENGTH_SHORT).show();
            expenses = new ArrayList<>();
        }
        TextView monthView = (TextView) findViewById(R.id.month);
        monthView.setText(strMonth);
        categories = new ArrayList<>();
        receivedTexts = new ArrayList<>();
        updateBalance();
        updateList();
        setOptionsListener();
        if(openReadEncrypt) {
            String msg = intent.getStringExtra(getString(R.string.notification_data_key));
            Log.d(LOG_TAG, msg);
            if(!msg.equals("")) {
                readPublicFile(msg);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_addition) {
            getAddDialog().show();
        }
        if (id == R.id.action_pastdata) {
            getPastMonthDialog().show();
        }
        if(id == R.id.action_graph) {
            Intent intent = new Intent(this, GraphActivity.class);
            intent.putExtra("Total Balance Main", updateBalance());
            intent.putExtra("type", "Main");
            startActivity(intent);
        }
        if(id == R.id.action_invoice) {
            Intent smsIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(smsIntent, 26);
        }

        if(id == R.id.action_invoiceencrypted) {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, 25);
        }

        if(id == R.id.action_inputdata) {
            readPublicFile();
        }

        if(id == R.id.action_restorebackup) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String defaultString = "KEY_NOT_FOUND";
            String backupData = prefs.getString(getString(R.string.backup_key), defaultString);
            if(backupData.equals(defaultString)) {
                Toast.makeText(MainActivity.this, "Backup not found!", Toast.LENGTH_LONG).show();
                return false;
            }
            expenses = getExpensesFromString(backupData);
            updateList();
            updateBalance();
            saveFile();
        }

        //TODO: IMPLEMENT SETTINGS LAUNCH
        return super.onOptionsItemSelected(item);
    }

    public Dialog getAddDialog() {
        return getAddDialog("", "0.0", "", null);
    }

    public Dialog getAddDialog(String stitle, String samount, String scategory,
                               final Expenses editExpense) {
        Calendar cal = Calendar.getInstance();
        final String date = cal.get(Calendar.MONTH) + 1 + "/" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.YEAR);
        Log.d(LOG_TAG, date);

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.alertdialog_view);
        updateSuggestions(dialog);
        dialog.setTitle("Enter your expense");

        EditText title = (EditText) dialog.findViewById(R.id.dialog_title);
        title.setText(stitle);
        if(!samount.equals("0.0")) {
            EditText amount = (EditText) dialog.findViewById(R.id.dialog_amount);
            amount.setText(samount + "");
        }
        EditText category = (EditText) dialog.findViewById(R.id.dialog_category);
        category.setText(scategory + "");

        final Expenses expensesToAdd;
        if(editExpense == null) {
            expensesToAdd = new Expenses();
        } else {
            expensesToAdd = editExpense;
        }

        Button button = (Button) dialog.findViewById(R.id.button3);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText amount = (EditText) dialog.findViewById(R.id.dialog_amount);
                EditText title = (EditText) dialog.findViewById(R.id.dialog_title);
                EditText category = (EditText) dialog.findViewById(R.id.dialog_category);

                String str = amount.getText().toString();
                double amountD;
                String titleToApply = title.getText().toString();
                String categoryToApply = category.getText().toString();

                try {
                    amountD = Double.parseDouble(str);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Please enter an amount", Toast.LENGTH_LONG).show();
                    return;
                }
                if(titleToApply.equals("")) {
                    Toast.makeText(MainActivity.this, "Please enter a title", Toast.LENGTH_LONG).show();
                    return;
                }
                if(categoryToApply.equals("")) {
                    categoryToApply = "Other";
                    Toast.makeText(MainActivity.this, "Category set to 'Other'", Toast.LENGTH_SHORT).show();
                }

                expensesToAdd.setAmount(amountD);
                expensesToAdd.setTitle(titleToApply);
                expensesToAdd.setDate(date);
                expensesToAdd.setCategory(categoryToApply);

                if(editExpense == null)
                    expenses.add(expensesToAdd);

                updateList();
                updateBalance();
                saveFile();
                saveBackup();
                dialog.dismiss();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(),0);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
            }
        });
        Button buttonCancel = (Button) dialog.findViewById(R.id.button);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(),0);
                dialog.cancel();
            }
        });
        return dialog;
    }

    public AlertDialog getOptionDialog() {
        CharSequence[] items = {"Edit", "Delete", "Info"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getText(R.string.option_dialog_title));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 0) {
                    getAddDialog(selectedExpense.getTitle(), selectedExpense.getAmount() + "",
                            selectedExpense.getCategory(), selectedExpense).show();
                    dialog.dismiss();
                }
                if(which == 1) {
                    expenses.remove(selectedExpense);
                    dialog.dismiss();
                    updateList();
                    updateBalance();
                    saveFile();
                }
                if(which == 2) {
                    getInfoDialog().show();
                    dialog.dismiss();
                }
            }
        });
        return builder.create();
    }

    public Dialog getInfoDialog() {
        Dialog builder = new Dialog(this);
        builder.setTitle("Info");
        builder.setContentView(R.layout.dialog_info);
        String title = selectedExpense.getTitle();
        String expense = String.format("%.2f", selectedExpense.getAmount()); //check locale
        expense = "$" + expense;
        String category = selectedExpense.getCategory();
        String date = selectedExpense.getDate();

        TextView titleView = (TextView) builder.findViewById(R.id.infodialog_title);
        TextView amountView = (TextView) builder.findViewById(R.id.infodialog_amount);
        TextView categoryView = (TextView) builder.findViewById(R.id.infodialog_category);
        TextView dateView = (TextView) builder.findViewById(R.id.infodialog_date);

        titleView.setText(title);
        amountView.setText(expense);
        categoryView.setText(category);
        dateView.setText(date);

        return builder;
    }

    public Dialog getPastMonthDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final ArrayAdapter<String> fileNames = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);
        Calendar cal = Calendar.getInstance();
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
        builder.setAdapter(fileNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = fileNames.getItem(which);
                openFile(fileName);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return builder.create();
    }

    public void saveBackup() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getString(R.string.backup_key), expenses.toString());
        editor.apply();
    }

    public void saveFile() {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        String strMonth = getMonth(month);
        DataFile dataFile = new DataFile(strMonth + year, expenses);
        Log.d(LOG_TAG, strMonth + year + "saved");
        try {
            FileOutputStream fos = this.openFileOutput(strMonth + year, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(dataFile);
        } catch (Exception e) {
            Toast.makeText(this, "Could not make this month's file", Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, e.toString());
        }
    }

    public void openFile(String filename) {
        DataFile dataFile;
        try {
            FileInputStream fis = this.openFileInput(filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            dataFile = (DataFile) ois.readObject();
            Intent intent = new Intent(this, PastDataActivity.class);
            intent.putExtra("list", dataFile.toString());
            intent.putExtra("month", filename.substring(0, filename.length() - 4));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't find this month's file", Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, e.toString());
        }
    }

    protected void readPublicFile() {
        readPublicFile("");
    }

    protected void readPublicFile(String encryptedFile) {
        final Dialog enterEncryptedDataDialog = new Dialog(this);
        enterEncryptedDataDialog.setContentView(R.layout.dialog_readencrypteddata);
        enterEncryptedDataDialog.setTitle(getString(R.string.enterencrypteddatadialog_title));

        final EditText textBox = (EditText) enterEncryptedDataDialog.findViewById(R.id.readencrypteddatadialog_textbox);
        textBox.setText(encryptedFile);

        Button okButton = (Button) enterEncryptedDataDialog.findViewById(R.id.readencrypteddatadialog_ok);
        Button cancelButton = (Button) enterEncryptedDataDialog.findViewById(R.id.readencrypteddatadialog_cancel);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String encryptedData = textBox.getText().toString();
                if(encryptedData.equals("")) {
                    Toast.makeText(MainActivity.this, "No Data Given!", Toast.LENGTH_LONG).show();
                    enterEncryptedDataDialog.dismiss();
                }
                String decryptedData = EncryptionManager.readEncryptedString(encryptedData);
                if(decryptedData.equals("NODATAFOUND")) {
                    Toast.makeText(MainActivity.this, "Decryption Error! No Expenses found", Toast.LENGTH_LONG).show();
                    enterEncryptedDataDialog.dismiss();
                }
                else {
                    ArrayList<Expenses> newExpenses = getExpensesFromString(decryptedData);
                    if (newExpenses.size() == 0) {
                        Toast.makeText(MainActivity.this, "Decryption Error! No Expenses found", Toast.LENGTH_LONG).show();
                    }
                    for(Expenses e : newExpenses) {
                        e.setTitle(e.getTitle() + "*");
                    }
                    expenses.addAll(newExpenses);
                    enterEncryptedDataDialog.dismiss();
                    saveFile();
                    saveBackup();
                    updateList();
                    updateBalance();
                }

            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterEncryptedDataDialog.cancel();
            }
        });
        enterEncryptedDataDialog.show();
    }

    public void updateList() {
        MyListAdapter adapter = new MyListAdapter();
        ListView listView = (ListView) findViewById(R.id.expensesList);
        listView.setAdapter(adapter);
    }

    public void updateSuggestions(Dialog dialog) {
        categories = new ArrayList<>();
        for(int i = 0; i < expenses.size(); i++) {
            if(!(categories.contains(expenses.get(i).getCategory()))) {
                categories.add(expenses.get(i).getCategory());
            }
        }
        String[] aCategories = new String[categories.size()];
        for(int i = 0; i < aCategories.length; i++) {
            aCategories[i] = categories.get(i);
        }
        if(aCategories.length != 0) {
            AutoCompleteTextView actv = (AutoCompleteTextView) dialog.findViewById(R.id.dialog_category);
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, aCategories);
            actv.setAdapter(categoryAdapter);
        }
    }

    public double updateBalance() {
        double tempBalance = 0;
        for(Expenses e : expenses) {
            tempBalance += e.getAmount();
        }
        TextView balance = (TextView) findViewById(R.id.balance);
        //Locale.getDefault() get the location because letters vary in different regions of the world
        String stringBalance = String.format(Locale.getDefault(), "%.2f", tempBalance);
        balance.setText("$" + stringBalance);
        return tempBalance;
    }

    public void setOptionsListener() {
        ListView listView = (ListView) findViewById(R.id.expensesList);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(100);
                int startSize = expenses.size();
                selectedExpense = expenses.get(position);
                getOptionDialog().show();
                parent.requestLayout(); //This properly updates the view NECESSARY
                updateList();
                updateBalance();
                saveFile();
                int endSize = expenses.size();
                return startSize != endSize;
            }
        });
    }

    public String getMonth(int intMonth) {
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
            default : return "lol garbo programmer";
        }
    }

    protected String getStringOfData() {
        String stringOfData;
        stringOfData = "Total Spent: $" + String.format(Locale.getDefault(), "%.2f", updateBalance()) + "\n\n";
        stringOfData += "By Category\n";
        int totalItems = MainActivity.expenses.size();
        ArrayList<String> numberOfCats = new ArrayList<>();
        for(int i = 0; i < totalItems; i++) {
            if(!(numberOfCats.contains(MainActivity.expenses.get(i).getCategory()))) {
                numberOfCats.add(MainActivity.expenses.get(i).getCategory());
            }
        }
        // how many of first category divided by total number
        for(int i = 0; i < numberOfCats.size(); i++) {
            double totalSpent = 0;
            for (int k = 0; k < totalItems; k++) {
                if (numberOfCats.get(i).equals(MainActivity.expenses.get(k).getCategory())) {
                    totalSpent += MainActivity.expenses.get(k).getAmount();
                }
            }
            stringOfData += numberOfCats.get(i) + " $" + String.format(Locale.getDefault(), "%.2f", totalSpent) + "\n";
        }

        stringOfData += "\n";
        stringOfData += "By Item\n";
        for(Expenses e : expenses) {
            stringOfData += e.getTitle() + " $" + String.format(Locale.getDefault(), "%.2f", e.getAmount()) + "\n";
        }
        Log.d(LOG_TAG, stringOfData);
        return stringOfData;
    }

    protected static ArrayList<Expenses> getExpensesFromString(String textualExpenses) {
        String localLOG_TAG = "getExpensesFromString Method";
        ArrayList<Expenses> returnable = new ArrayList<>();
        if(!textualExpenses.contains("["))
            return returnable;
        String finalString = textualExpenses.replace("[", "");
        finalString = finalString.replace("]", "");
        String[] arrayToConvert = finalString.split(", ");
        for(String s : arrayToConvert) {
            String[] indivExpense = s.split("-");
            if(indivExpense.length != 4) {
                returnable = new ArrayList<>();
                return returnable;
            }
            String title = indivExpense[0];
            double amount = Double.parseDouble(indivExpense[1]);
            String category = indivExpense[2];
            String date = indivExpense[3];
            returnable.add(new Expenses(title, amount, category, date));
        }

        return returnable;
    }

    private void askForPermissions() {
        //check out current permissions and Log them
        int permissionCheckReceive = this.checkSelfPermission(Manifest.permission.RECEIVE_SMS);
        int permissionCheckSend = this.checkSelfPermission(Manifest.permission.SEND_SMS);
        int permissionCheckContacts = this.checkSelfPermission(Manifest.permission.READ_CONTACTS);
        int permissionCheckPhoneState = this.checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
        Log.d(LOG_TAG, "RECEIVE PERMISSION " + permissionCheckReceive);
        Log.d(LOG_TAG, "SEND PERMISSION " + permissionCheckSend);
        Log.d(LOG_TAG, "READ CONTACTS " + permissionCheckContacts);
        Log.d(LOG_TAG, "READ PHONE STATE " + permissionCheckPhoneState);

        //if we need the permission, ask user.
        if(permissionCheckReceive == -1) { //-1 is denied, 0 is granted
            requestPermissions(new String[] {Manifest.permission.RECEIVE_SMS}, 22);
            Log.d(LOG_TAG, "ASKING FOR PERMISSIONS FOR RECEIVE");
        }
        if(permissionCheckSend == -1) {
            requestPermissions(new String[] {Manifest.permission.SEND_SMS}, 23);
            Log.d(LOG_TAG, "ASKING FOR PERMISSIONS FOR SEND");
        }
        if(permissionCheckContacts == -1) {
            requestPermissions(new String[] {Manifest.permission.READ_CONTACTS}, 24);
            Log.d(LOG_TAG, "ASKING FOR PERMISSIONS FOR CONTACTS");
        }
        if(permissionCheckPhoneState == -1) {
            requestPermissions(new String[] {Manifest.permission.READ_PHONE_STATE}, 27);
            Log.d(LOG_TAG, "ASKING FOR PERMISSION TO CHECK THE PHONES STATE");
        }

        //check what permissions we have after asking
        permissionCheckReceive = this.checkSelfPermission(Manifest.permission.RECEIVE_SMS);
        permissionCheckSend = this.checkSelfPermission(Manifest.permission.SEND_SMS);
        permissionCheckContacts = this.checkSelfPermission(Manifest.permission.READ_CONTACTS);
        permissionCheckPhoneState = this.checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
        Log.d(LOG_TAG, "RECEIVE PERMISSION " + permissionCheckReceive);
        Log.d(LOG_TAG, "SEND PERMISSION " + permissionCheckSend);
        Log.d(LOG_TAG, "READ CONTACTS " + permissionCheckContacts);
        Log.d(LOG_TAG, "READ PHONE STATE " + permissionCheckPhoneState);

    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        switch (reqCode) {
            case (25) :
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor c =  getContentResolver().query(contactData, null, null, null, null);
                    ContentResolver contact_resolver = getContentResolver();
                    if (c.moveToFirst()) {
                        String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                        Cursor phoneCur = contact_resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { id }, null);

                        if (phoneCur.moveToFirst()) {
                            String no = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            Log.d(LOG_TAG, no);
                            String smsBody = "!#@$" + EncryptionManager.createEncryptedString(expenses.toString());
                            Log.d(LOG_TAG, smsBody);
                            ArrayList<String> parts = smsManager.divideMessage(smsBody);
                            smsManager.sendMultipartTextMessage(no, null, parts, null, null);
                        }

                    }
                    c.close();
                }
                break;
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

    private class MyListAdapter extends ArrayAdapter<Expenses> {
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
