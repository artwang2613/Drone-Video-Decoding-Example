package com.dji.videostreamdecodingsample.FlightControl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Tracking {

    private Queue<ObjectPos> pastPositions = new LinkedList<>();

    public Tracking(){

    }

    public void emergencyStop(){
        //TODO set all drone movement to 0, STOPPPPP
    }

    public void track(){
        if(getXCorrection() < 50){
            //TODO do gimbal rotation
        }
        else if(getXCorrection() < 100){
            //TODO do drone rotation;
        } else{
            //TODO move drone along x axis;
        }

        if(getYCorrection() < 200){
            //TODO do gimbal rotation
        }
        else{
            //TODO move drone along y axis;
        }

       //TODO send gimbal/virtual stick command
    }

    public void addPos(ObjectPos pos){
        if(pastPositions.size() > 5){
            pastPositions.remove();
        }
        pastPositions.add(pos);
    }

    public float getXCorrection(){



        return 0;
    }

    public float getYCorrection(){



        return 0;
    }


}
