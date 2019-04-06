package com.huawei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class Cross implements Cloneable {
    int crossID;
    Road[] crossRoads;//按照顺时针顺序存放
    ArrayList<Road> sortedCrossRoad = new ArrayList<>();//按照id从小到大存放

    public Cross clone() {
        try {
            return (Cross) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Cross(int crossID, Road fristRoad, Road secondRoad, Road thirdRoad, Road forthRoad) {
        this.crossID = crossID;
        crossRoads = new Road[4];
        crossRoads[0] = fristRoad;
        if (fristRoad != null)
            sortedCrossRoad.add(fristRoad);
        crossRoads[1] = secondRoad;
        if (secondRoad != null)
            sortedCrossRoad.add(secondRoad);
        crossRoads[2] = thirdRoad;
        if (thirdRoad != null)
            sortedCrossRoad.add(thirdRoad);
        crossRoads[3] = forthRoad;
        if (forthRoad != null)
            sortedCrossRoad.add(forthRoad);
        Collections.sort(sortedCrossRoad, new Comparator<Road>() {
            @Override
            public int compare(Road o1, Road o2) {
                if (o1 != null && o2 != null) {
                    return o1.roadID - o2.roadID;
                }
                return 0;
            }
        });
    }

    /*
     * add by luzhen
     * getRoadInClockSeq()
     */
    public int getRoadInClockSeq(int roadID) {
        for (int i = 0; i < 4; ++i) {
            if (crossRoads[i] == null) continue;
            if (roadID == crossRoads[i].roadID) {
                return i;
            }
        }
        System.out.println("getRoadInClockSeq() error...");
        return -1;//error
    }
}
