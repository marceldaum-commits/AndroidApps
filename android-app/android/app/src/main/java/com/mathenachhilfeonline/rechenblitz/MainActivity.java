package com.mathenachhilfeonline.rechenblitz;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(RbWidgetPlugin.class);   // Brücke Web-App → Startbildschirm-Widget
        super.onCreate(savedInstanceState);
    }
}
