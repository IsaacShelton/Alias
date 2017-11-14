package com.dockysoft.alias;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ChatRoomActivity extends AppCompatActivity {

    public static FirebaseListAdapter<ChatMessage> adapter;
    public static boolean justAdded;
    private String chatRoom;
    private String chatRoomAuthor;
    private DatabaseReference root;
    private String tempKey;
    public static String alias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        alias = "Anonymous";
        justAdded = false;
        chatRoom = getIntent().getExtras().get("room_name").toString();
        chatRoomAuthor = getIntent().getExtras().get("room_author").toString();
        root = FirebaseDatabase.getInstance().getReference().child(chatRoom).child("messages");
        getSupportActionBar().setTitle("Anonymous");
        displayChatMessages();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText input = (EditText) findViewById(R.id.input);

                if(!input.getEditableText().toString().equals("")) {
                    root = FirebaseDatabase.getInstance().getReference().child(chatRoom).child("messages");

                    Map<String, Object> map = new HashMap();
                    tempKey = root.push().getKey();
                    root.updateChildren(map);

                    String text = input.getText().toString();
                    DatabaseReference messageRoot = root.child(tempKey);
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("alias", alias);
                    messageMap.put("text", text);
                    messageMap.put("sender", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    messageMap.put("key", tempKey);
                    messageRoot.updateChildren(messageMap);

                    input.setText("");
                    justAdded = true;

                    sendNotificationOut(alias, text, chatRoom);
                }
            }
        });

        ListView listView = (ListView) findViewById(R.id.list_of_messages);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,  int position, long id) {
                deleteMessage(adapter.getItem(position));
            }
        });
    }

    private void sendNotificationOut(final String notifAlias, final String notifMessage, final String notifTopic){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    HttpURLConnection client = null;

                    try {
                        URL url = new URL("https://dockysoft.com/scripts/alias_notif.php");
                        client = (HttpURLConnection) url.openConnection();
                        client.setRequestMethod("POST");
                        client.setDoInput(true);
                        client.setDoOutput(true);
                        client.setReadTimeout(10000);
                        client.setConnectTimeout(10000);

                        HashMap<String, String> map = new HashMap<>();
                        map.put("alias", notifAlias);
                        map.put("message", notifMessage);
                        map.put("topic", notifTopic);

                        OutputStream os = client.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(
                                new OutputStreamWriter(os, "UTF-8"));
                        writer.write(getPostDataString(map));

                        writer.flush();
                        writer.close();
                        os.close();
                        int responseCode = client.getResponseCode();

                        if (responseCode != HttpURLConnection.HTTP_OK) {
                            Log.e("POST FAILED", "Failed to send POST request");
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if(client != null) client.disconnect();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LayoutInflater layoutInflater;
        AlertDialog.Builder alertDialogBuilder;
        AlertDialog alertDialog;

        switch (item.getItemId()) {
            case R.id.menu_sign_out:
                AuthUI.getInstance().signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(ChatRoomActivity.this,
                                        "You have been signed out.",
                                        Toast.LENGTH_LONG)
                                        .show();
                                restartApplication();
                            }
                        });
                return true;
            case R.id.menu_change_alias:
                layoutInflater = LayoutInflater.from(this);
                View promptsView = layoutInflater.inflate(R.layout.prompt, null);

                alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setView(promptsView);
                final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);

                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        alias = userInput.getText().toString();
                                        getSupportActionBar().setTitle(alias);
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return true;
            case R.id.menu_follow_topic:
                layoutInflater = LayoutInflater.from(this);
                View followPrompt = layoutInflater.inflate(R.layout.follow_prompt, null);

                alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setView(followPrompt);
                final CheckBox checkBox = (CheckBox) followPrompt.findViewById(R.id.checkBoxFollow);

                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("Save",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        if(checkBox.isChecked()){
                                            FirebaseMessaging.getInstance().subscribeToTopic(chatRoom);
                                        } else {
                                            FirebaseMessaging.getInstance().unsubscribeFromTopic(chatRoom);
                                        }
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return true;
            case R.id.menu_delete_topic:
                if(chatRoomAuthor.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                    new AlertDialog.Builder(this).setCancelable(false).setTitle("Delete topic?").setPositiveButton("Delete",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    removeRoom();
                                }
                            }
                        ).setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            }
                        ).create().show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Failed to Delete Topic")
                            .setMessage("You can only delete topics you've created")
                            .setCancelable(false)
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Do nothing
                                }
                            }).show();

                }
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void appendMessage(DataSnapshot dataSnapshot){
        String messageAlias, messageText;
        Iterator iterator = dataSnapshot.getChildren().iterator();
        while(iterator.hasNext()){
            messageAlias = (String) ((DataSnapshot)iterator.next()).getValue();
            messageText = (String) ((DataSnapshot)iterator.next()).getValue();
        }
    }

    private void displayChatMessages() {
        final ListView listOfMessages = (ListView) findViewById(R.id.list_of_messages);
        adapter = new FirebaseListAdapter<ChatMessage>(this, ChatMessage.class, R.layout.message, root) {
            @Override
            protected void populateView(View view, ChatMessage model, int position) {
                // Get references to the views of message.xml
                TextView messageText = (TextView) view.findViewById(R.id.message_text);
                TextView messageUser = (TextView) view.findViewById(R.id.message_user);
                TextView messageMe = (TextView) view.findViewById(R.id.message_me);

                // Set their text
                messageText.setText(model.getText());
                messageUser.setText(model.getAlias());

                if(model.getSender().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                    messageMe.setText("Sent by Me");
                }

                if(justAdded){
                    // Scroll to bottom
                    listOfMessages.post(new Runnable() {
                        @Override
                        public void run() {
                            listOfMessages.setSelection(adapter.getCount() - 1);
                        }
                    });
                    justAdded = false;
                }
            }
        };

        listOfMessages.setAdapter(adapter);
    }

    private void deleteMessage(final ChatMessage message){
        if(!message.getSender().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
            // That's not the user
            return;
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        root.child(message.getKey()).removeValue();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.cancel();
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to delete this message?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private void removeRoom(){
        FirebaseDatabase.getInstance().getReference().child(chatRoom).removeValue();
        Intent intent = new Intent(this, RoomsActivity.class);
        startActivity(intent);
    }

    private void restartApplication(){
        Intent intent = new Intent(this, RoomsActivity.class);
        finish();
        startActivity(intent);
    }
}
