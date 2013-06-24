package com.mobileanarchy.android.widgetsdemo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.mobileanarchy.android.widgets.joystick.DualJoystickView;
import com.mobileanarchy.android.widgets.joystick.JoystickMovedListener;

import es.skastro.arduino.robotics.R;

public class DualJoystickActivity extends Activity {

    TextView txtX1, txtY1;
    TextView txtX2, txtY2;
    DualJoystickView joystick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dualjoystick);

        txtX1 = (TextView) findViewById(R.id.TextViewX1);
        txtY1 = (TextView) findViewById(R.id.TextViewY1);

        txtX2 = (TextView) findViewById(R.id.TextViewX2);
        txtY2 = (TextView) findViewById(R.id.TextViewY2);

        joystick = (DualJoystickView) findViewById(R.id.dualjoystickView);

        joystick.setOnJostickMovedListener(_listenerLeft, _listenerRight);
    }

    private JoystickMovedListener _listenerLeft = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            txtX1.setText(Integer.toString(pan));
            txtY1.setText(Integer.toString(tilt));
        }

        @Override
        public void OnReleased() {
            txtX1.setText("released");
            txtY1.setText("released");
        }

        public void OnReturnedToCenter() {
            txtX1.setText("stopped");
            txtY1.setText("stopped");
        };
    };

    private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            txtX2.setText(Integer.toString(pan));
            txtY2.setText(Integer.toString(tilt));
        }

        @Override
        public void OnReleased() {
            txtX2.setText("released");
            txtY2.setText("released");
        }

        public void OnReturnedToCenter() {
            txtX2.setText("stopped");
            txtY2.setText("stopped");
        };
    };

}
