package com.dockysoft.alias;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class RoomsActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    public static final int SIGN_IN_REQUEST_CODE = 1;
    public static DatabaseReference root;
    public static RoomListAdapter roomListAdapter;
    private static ArrayList<Room> listOfRooms = new ArrayList();
    private Menu searchMenu;
    private ValueEventListener valueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean previouslyStarted = prefs.getBoolean("is_first_v1.3", false);

        if(!previouslyStarted) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean("is_first_v1.3", true);
            edit.commit();
            showHelp();
            return;
        }

        setContentView(R.layout.activity_rooms);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Alias Topics");

        initFirebaseSignin();
        root = FirebaseDatabase.getInstance().getReference().getRoot();

        final ListView listView = (ListView) findViewById(R.id.chat_rooms_list);
        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.add_chat_room);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,  int position, long id) {
                enterChatRoom(position);
            }
        });

        //arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listOfRooms);
        //((ListView) findViewById(R.id.chat_rooms_list)).setAdapter(arrayAdapter);

        roomListAdapter = new RoomListAdapter(this, listOfRooms);
        ((ListView) findViewById(R.id.chat_rooms_list)).setAdapter(roomListAdapter);

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                addRoomDialog();
            }
        });

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Set<Room> set = new TreeSet<>();

                for(DataSnapshot child : dataSnapshot.getChildren()){
                    HashMap<String, Object> roomChildren = (HashMap<String, Object>) child.getValue();
                    if(child.hasChild("hidden")) continue;
                    set.add( new Room(child.getKey(), (String) roomChildren.get("desc"), (String) roomChildren.get("author")) );
                }

                listOfRooms.clear();
                listOfRooms.addAll(set);
                roomListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Nothing
            }
        };

        root.addValueEventListener(valueEventListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SIGN_IN_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                Toast.makeText(this,
                        "Successfully signed in. Welcome!",
                        Toast.LENGTH_LONG)
                        .show();
                root = FirebaseDatabase.getInstance().getReference().getRoot();
                root.addValueEventListener(valueEventListener);
            } else {
                Toast.makeText(this,
                        "We couldn't sign you in. Please try again later.",
                        Toast.LENGTH_LONG)
                        .show();

                restartApplication();
            }
        }

    }

    private void enterChatRoom(int position){
        Intent intent = new Intent(this, ChatRoomActivity.class);
        Room room = roomListAdapter.getData().get(position);
        intent.putExtra("room_name", room.getName());
        intent.putExtra("room_author", room.getAuthor());
        startActivity(intent);

        MenuItem searchMenuItem = searchMenu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        }
    }

    private void enterChatRoomNamed(final String name){
        FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.hasChild(name)) {
                    HashMap<String, Object> roomChild = (HashMap<String, Object>) snapshot.child(name).getValue();
                    enterChatRoomRaw(name, (String) roomChild.get("author"));
                }
                else {
                    hiddenRoomDoesntExist(name);
                }
            }

            @Override
            public void onCancelled(DatabaseError e){

            }
        });
    }

    private void enterChatRoomRaw(String name, String author){
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("room_name", name);
        intent.putExtra("room_author", author);
        startActivity(intent);
    }

    private void hiddenRoomDoesntExist(String name){
        new AlertDialog.Builder(this)
                .setTitle("Failed to Join Topic")
                .setMessage("There isn't a hidden topic named '" + name + "'")
                .setCancelable(false)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Nothing
                    }
                }).show();
    }

    private void roomAlreadyExists(String name){
        new AlertDialog.Builder(this)
                .setTitle("Failed to create topic")
                .setMessage("The topic '" + name + "' already exists")
                .setCancelable(false)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Nothing
                    }
                }).show();
    }

    private void invalidRoomName(){
        new AlertDialog.Builder(this)
                .setTitle("Failed to create topic")
                .setMessage("Topic names can only consist of letters, numbers and underscores.")
                .setCancelable(false)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Nothing
                    }
                }).show();
    }

    private void addRoomDialog(){
        MenuItem searchMenuItem = searchMenu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        }

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptsView = layoutInflater.inflate(R.layout.add_room_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
        final EditText userDesc = (EditText) promptsView.findViewById(R.id.editTextDialogUserInputDesc);
        final CheckBox makeHidden = (CheckBox) promptsView.findViewById(R.id.makeHidden);

        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                final String name = userInput.getEditableText().toString().toLowerCase();
                                final String author = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                final String desc = userDesc.getEditableText().toString();
                                final boolean isHidden = makeHidden.isChecked();

                                FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot snapshot) {
                                        if (snapshot.hasChild(name)) {
                                            roomAlreadyExists(name);
                                        }
                                        else {
                                            makeNewRoom(name, author, desc, isHidden);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError e){

                                    }
                                });
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void initFirebaseSignin(){
        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Start sign in / sign up activity
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setTheme(R.style.AppTheme)
                            .build(),
                    SIGN_IN_REQUEST_CODE
            );
        } else {
            //String displayName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            //Snackbar.make(findViewById(android.R.id.content), "Welcome " + displayName, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        searchMenu = menu;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem searchMenuItem = menu.findItem(R.id.search);
        final MenuItem signoutMenuItem = menu.findItem(R.id.menu_sign_out);
        final MenuItem secretMenuItem = menu.findItem(R.id.menu_secret);

        SearchView searchView = (SearchView) searchMenuItem.getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(this);

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signoutMenuItem.setVisible(false);
                secretMenuItem.setVisible(false);
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                signoutMenuItem.setVisible(true);
                secretMenuItem.setVisible(true);
                invalidateOptionsMenu();
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.menu_secret:
                LayoutInflater li = LayoutInflater.from(this);
                View joinHiddenView = li.inflate(R.layout.join_hidden, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setView(joinHiddenView);

                final EditText userInput = (EditText) joinHiddenView.findViewById(R.id.editTextHiddenTopic);

                alertDialogBuilder.setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        enterChatRoomNamed(userInput.getEditableText().toString());
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                break;
            case R.id.menu_sign_out:
                AuthUI.getInstance().signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(RoomsActivity.this,
                                        "You have been signed out.",
                                        Toast.LENGTH_LONG)
                                        .show();

                                restartApplication();
                            }
                        });
                break;
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        roomListAdapter.getFilter().filter(newText);
        return true;
    }

    @Override
    public void onBackPressed() {
        MenuItem searchMenuItem = searchMenu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();

        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        } else {
            super.onBackPressed();
        }
    }

    private void restartApplication(){
        Intent intent = new Intent(this, RoomsActivity.class);
        finish();
        startActivity(intent);
    }

    private void showHelp(){
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    private boolean isValidRoomName(String s){
        String pattern= "^[a-zA-Z0-9_]*$";
        return s.matches(pattern);
    }

    private void makeNewRoom(String name, String author, String desc, boolean isHidden){
        if(!isValidRoomName(name)){
            invalidRoomName();
            return;
        }

        Map<String, Object> map = new HashMap();
        Map<String, Object> messages_map = new HashMap();
        messages_map.put("desc", desc);
        messages_map.put("author", author);
        messages_map.put("messages", new HashMap<String, Object>());
        if(isHidden) messages_map.put("hidden", "");
        map.put(name, messages_map);
        root.updateChildren(map);
    }
}
