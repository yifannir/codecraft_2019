package com.huawei;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

public class RoadCarInfo {
    int carID;
    int startTime;
    int roadID;
    int isForword;
    int channel;
    int offset;
    int isPreset;
    int isPriority;

    static ArrayList<RoadCarInfo> roadCarInfos = new ArrayList<>();

    // logDirPath = "D:\\judgeLog\\";
    public static void recordRoadCarInfo(int time, String logDirPath, ArrayList<Road> roads){
        roadCarInfos.clear();
        for (Road road : roads) {
            for (int i = 0; i < road.channelCount; ++i) {
                for (int j = 0; j < road.roadLength; ++j) {
                    if (road.carOnRoad[i][j] != null) {
                        RoadCarInfo roadCarInfo = new RoadCarInfo();
                        roadCarInfo.carID = road.carOnRoad[i][j].carID;
                        roadCarInfo.startTime = road.carOnRoad[i][j].startTime;
                        roadCarInfo.roadID = road.roadID;
                        roadCarInfo.offset = j;
                        roadCarInfo.channel = i;
                        roadCarInfo.isForword = road.carOnRoad[i][j].isForwardRoad ? 1 : 0; // car on Road
                        roadCarInfo.isPreset = road.carOnRoad[i][j].isPresetCar ? 1 : 0;
                        roadCarInfo.isPriority = road.carOnRoad[i][j].isPriorityCar ? 1 : 0;
                        roadCarInfos.add(roadCarInfo);
                    }
                    if (road.isDuplex && road.carOnDuplexRoad[i][j] != null) {
                        RoadCarInfo roadCarInfo = new RoadCarInfo();
                        roadCarInfo.carID = road.carOnDuplexRoad[i][j].carID;
                        roadCarInfo.startTime = road.carOnDuplexRoad[i][j].startTime;
                        roadCarInfo.roadID = road.roadID;
                        roadCarInfo.offset = j;
                        roadCarInfo.channel = i;
                        roadCarInfo.isForword = road.carOnDuplexRoad[i][j].isForwardRoad ? 1 : 0; // car on Road
                        roadCarInfo.isPreset = road.carOnDuplexRoad[i][j].isPresetCar ? 1 : 0;
                        roadCarInfo.isPriority = road.carOnDuplexRoad[i][j].isPriorityCar ? 1 : 0;
                        roadCarInfos.add(roadCarInfo);
                    }
                }
            }
        }//end for Roads
        //将time时刻的路上车辆信息写入到time.txt文件中
        String logFileName = String.valueOf(time) + ".txt";
        File file = new File(logDirPath + logFileName);
        if (file.exists() && file.isFile())
            file.delete();
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(logDirPath + logFileName));
            String headerLine = "(carID, startTime, roadID, isForword, channel, offset, isPriority, isPreset)";
            bw.write(headerLine);
            bw.newLine();
            bw.flush();
            for (RoadCarInfo roadCarInfo : roadCarInfos) {
                String strLine = "(" + roadCarInfo.carID + ", " + roadCarInfo.startTime + ", " + roadCarInfo.roadID + ", " +  roadCarInfo.isForword + ", " + roadCarInfo.channel + ", " + roadCarInfo.offset + ", "
                        + roadCarInfo.isPriority + ", "  + roadCarInfo.isPreset + ")";
                bw.write(strLine);
                bw.newLine();
                bw.flush();
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
