package com.ytjojo.viewlib.nestedsrolllayout;

public class DirectionDetector {
    public static int getDirection(int deltaY) {
        int i=0;
        if (deltaY > 0) {
            i = 1;
        }
        if (deltaY < 0) {
            i = 2;
        }
        return i;
    }


    public int getDirection(int deltaY, CompatWebView.OnScrollChangeListener paramc) {
        int direction = getDirection(deltaY);
        if (paramc != null)
            paramc.onChildDirectionChange(direction);
        return direction;
    }
}