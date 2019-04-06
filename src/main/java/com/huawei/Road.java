package com.huawei;

import java.util.ArrayList;

class Road implements Cloneable {
    int roadID;
    int roadLength;
    int speedLimitofRoad;
    int channelCount;//车道数
    Cross fromCross;
    Cross toCross;
    ArrayList<Car> initCarList;

    public Road clone() {
        try {
            return (Road) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    boolean isDuplex;// 是否是单双道
    //该道路from->to上的车辆ID。按照选路优先级排列。//int[roadLength][channelCount],
    Car[][] carOnRoad;

    //该道路双车道to->from上的车辆ID。按照选路优先级排列。//int[roadLength][channelCount],
    Car[][] carOnDuplexRoad;

    public Road(int roadId) {
        this.roadID = roadId;
        initCarList = new ArrayList<>();
    }

    Car[][] getCarArray(Cross toCross) {//给定这条道路出路口，获取车辆数组
        if (this.toCross.crossID == toCross.crossID) {
            return this.carOnRoad;
        } else {
            if (!isDuplex) {
                return null;
            } else {
                return this.carOnDuplexRoad;
            }
        }
    }

    public void setRoad(int roadLength, int speedLimitofRoad, int channelCount, Cross fromCross, Cross toCross, int isDuplexT) {
        this.roadLength = roadLength;
        this.speedLimitofRoad = speedLimitofRoad;
        this.channelCount = channelCount;

        this.fromCross = fromCross;
        this.toCross = toCross;
        if (isDuplexT == 1) {
            this.isDuplex = true;
        } else {
            this.isDuplex = false;
        }

        carOnRoad = new Car[channelCount][roadLength];
        if (isDuplex)
            carOnDuplexRoad = new Car[channelCount][roadLength];
    }
}