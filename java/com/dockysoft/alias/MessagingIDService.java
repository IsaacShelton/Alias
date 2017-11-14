package com.dockysoft.alias;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class MessagingIDService extends FirebaseInstanceIdService {
    private static final String TAG = "FirebaseIDService";
}
