package com.example.arib.financemanager;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import static java.net.Proxy.Type.HTTP;

//TODO: FINISH CATEGORY IMPLEMENTATION
//TODO: GRAPHICAL INTERFACE
public class MainActivity extends Activity {

    protected static ArrayList<Expenses> expenses;
    protected static ArrayList<String> categories;
    Expenses selectedExpense;

    private final String LOG_TAG = this.getClass().getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            expenses = dataFile.getExpenses();
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't find this month's file", Toast.LENGTH_SHORT).show();
            expenses = new ArrayList<>();
        }
        TextView monthView = (TextView) findViewById(R.id.month);
        monthView.setText(strMonth);
        categories = new ArrayList<>();
        updateBalance();
        updateList();
        setOptionsListener();
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
            intent.putExtra("Total Balance", updateBalance());
            startActivity(intent);
        }
        if(id == R.id.action_invoice) {
            Calendar cal = Calendar.getInstance();
//
//            Intent emailIntent = new Intent(Intent.ACTION_SEND);
//            emailIntent.setType("plain/text");
//            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {""}); // recipients
//            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Invoice of " + getMonth(cal.get(Calendar.MONTH) + 1));
//            emailIntent.putExtra(Intent.EXTRA_TEXT, getStringOfData());
//            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            Intent intent = new Intent(
                    Intent.ACTION_SENDTO,
                    Uri.parse("mailto:testemail@gmail.com")
            );
            startActivity(intent);
        }
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
                dialog.dismiss();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(),0);
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
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
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
                    getAddDialog(selectedExpense.getTitle(), selectedExpense.getAmount() + "", selectedExpense.getCategory(), selectedExpense).show();
                    dialog.dismiss();
                }
                if(which == 1) {
                    expenses.remove(selectedExpense);
                    //TODO: add confirmation dialog.
                    dialog.dismiss();
                    updateList();
                    updateBalance();
                }
                if(which == 2) {
                    getInfoDialog().show();
                    dialog.dismiss();
                }
            }
        });
        return builder.create();
    }

    public AlertDialog getInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Info");
        String title = selectedExpense.getTitle();
        String expense = "$" + selectedExpense.getAmount();
        String category = selectedExpense.getCategory();
        String date = selectedExpense.getDate();
        String info = title + "\n"+ "\n" + expense + "\n" + "\n" + category  + "\n" + "\n" + date;
        builder.setMessage(info);

        return builder.create();
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
            intent.putExtra("list", dataFile.getExpenses().toString());
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't find this month's file", Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, e.toString());
        }
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
//                expenses.remove(position);
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

    public String getStringOfData() {
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
