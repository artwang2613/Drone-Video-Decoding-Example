package com.dji.videostreamdecodingsample.FlightControl;

public class ObjectPos {
    private int x;
    private int y;

    public ObjectPos(int xPos, int yPos){
        this.x = xPos;
        this.y = yPos;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int xPos){
        this.x = xPos;
    }

    public void setY(int yPos){
        this.y = yPos;
    }
}
