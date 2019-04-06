package com.huawei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

class Car implements Cloneable, Comparable<Car> {
    public static final int GO_WRONG = -1;
    public static final int GO_DOWN = 0;
    public static final int GO_LEFT = 1;
    public static final int GO_RIGHT = 2;
    public static final int GO_NEVER = 3;
    public static final int END_STATUS = 0;
    public static final int WAIT_STATUS = 1;
    public static final int FORWORD_HAS_NO_CAR = 0;
    public static final int FORWORD_HAS_END_CAR = 1;
    public static final int FORWORD_HAS_WAIT_CAR = 2;
    public static final int FORWORD_NEED_THROUGH_CROSS = 3;
    public static final int NO_GO = 0;
    public static final int GOING = 1;
    public static final int GONE = 2;
    boolean isPriorityCar;
    boolean isPresetCar;
    int startCrossID;
    int endCrossID;
    int carID;
    Cross startCross;
    Cross endCross;
    int maxSpeedofCar;
    int startTime;
    int planTime;
    int countChoice = 0;//记录当前路径选择的pos
    boolean isFinishedAtTimePiece;
    float[][] graph;// 该车辆的视野地图
    HashMap<Integer, Integer> graphMapCross; //实际CrossID映射路网0123456...的ID
    HashMap<Integer, Integer> crossMapgraph; //0123...映射路网crossID
    Road nowRoad;
    int nowRoadChannel;
    int roadOffset;
    Road nextRoad;
    boolean isForwardRoad;
    ArrayList<Integer> roadChoiceList = new ArrayList<>();

    public Car clone() {
        try {
            return (Car) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }


    /*
     * 获取该车辆所在的车道数组，请务必保证nowroad不为null，否则会返回null
     * */
    Car[][] getCarArray() {
        if (nowRoad == null)
            return null;
        if (isForwardRoad)
            return nowRoad.carOnRoad;
        else
            return nowRoad.carOnDuplexRoad;
    }

    boolean isFinish;//车辆是否到达终点，true为到达状态。
    int isStart;//车辆是否出发，0为未出发状态，1为标记出发，2已出发
    int flag; //车辆是等待状态还是终止状态,0为终止状态，1为等待状态。

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof Car))
            return false;
        return carID == ((Car) obj).carID;
    }

    @Override
    public int hashCode() {
        return carID;
    }

    /**
     * 构造初始话
     *
     * @return
     */
    //id,from,to,speed,planTime, isPriority, isPreset)
    public Car(int carID, int fromCrossID, int toCrossID, int maxSpeedofCar, int planTime, int pri, int preset) {
        this.carID = carID;
        this.startCrossID = fromCrossID;
        this.endCrossID = toCrossID;
        this.maxSpeedofCar = maxSpeedofCar;
        this.planTime = planTime;
        this.isPriorityCar = pri == 1;
        this.isPresetCar = preset == 1;
    }

    int getRealSpeedOnNowRoad() {
        if (nowRoad == null)
            return GO_WRONG;
        return maxSpeedofCar < nowRoad.speedLimitofRoad ? maxSpeedofCar : nowRoad.speedLimitofRoad;
    }

    int getDirection() {
        Cross cross;
        if (isForwardRoad)
            cross = nowRoad.toCross;
        else
            cross = nowRoad.fromCross;
        if (getTypeOfForwordCar().forwardStatus != Car.FORWORD_NEED_THROUGH_CROSS) {
            return GO_NEVER;
        }

        if (!isPresetCar && cross.crossID == endCrossID) {
            return GO_DOWN;
        }
        if (isPresetCar && cross.crossID == endCrossID && countChoice == roadChoiceList.size()) {
            return GO_DOWN;
        }

        int nowRoadOrder, nextRoadOrder;
        for (nowRoadOrder = 0; nowRoadOrder < 4; nowRoadOrder++) {
            if (cross.crossRoads[nowRoadOrder] == nowRoad)
                break;
        }
        for (nextRoadOrder = 0; nextRoadOrder < 4; nextRoadOrder++) {
            if (cross.crossRoads[nextRoadOrder] == nextRoad)
                break;
        }
        if ((nowRoadOrder - nextRoadOrder) % 4 == 2 || (nowRoadOrder - nextRoadOrder) % 4 == -2)//顺时针顺序排序下标，差为2代表对面
            return GO_DOWN;
        else if ((nowRoadOrder - nextRoadOrder) % 4 == 3 || (nowRoadOrder - nextRoadOrder) % 4 == -1)//差为-1代表左转
            return GO_LEFT;
        else if ((nowRoadOrder - nextRoadOrder) % 4 == 1 || (nowRoadOrder - nextRoadOrder) % 4 == -3)//差为1代表右转
            return GO_RIGHT;
        return GO_WRONG;
    }


    /*
   获取车辆前方道路信息，ForwordInfo.toPosition该车能到达的位置，如果有车阻挡，只能到达前车的尾后。
   如果当前路段行使最大距离大于偏移量，且前方没有任何车辆阻挡，返回FORWORD_NEED_THROUGH_CROSS
   */
    public ForwordInfo getTypeOfForwordCar() {
        Car[][] roadCars;
        if (isForwardRoad)
            roadCars = nowRoad.carOnRoad;
        else
            roadCars = nowRoad.carOnDuplexRoad;
        int channelNum = nowRoadChannel;
        int nowPosition = roadOffset;
        int exceptPosition = (roadOffset - getRealSpeedOnNowRoad()) >= 0 ? (roadOffset - getRealSpeedOnNowRoad()) : 0;
        for (; nowPosition > exceptPosition; nowPosition--) {
            //(car.roadOffset-car.getRealSpeedOnNowRoad())为车辆预计到达的偏移量，如果该偏移量小于0，则最后到达位置取0。
            if (roadCars[channelNum][nowPosition - 1] != null) {
                if (roadCars[channelNum][nowPosition - 1].flag == Car.END_STATUS)
                    return new ForwordInfo(FORWORD_HAS_END_CAR, nowPosition);
                else if (roadCars[channelNum][nowPosition - 1].flag == Car.WAIT_STATUS)
                    return new ForwordInfo(FORWORD_HAS_WAIT_CAR, nowPosition);
            }
        }
        if (roadOffset >= getRealSpeedOnNowRoad())
            return new ForwordInfo(FORWORD_HAS_NO_CAR, nowPosition);
        else
            return new ForwordInfo(FORWORD_NEED_THROUGH_CROSS, nowPosition);
    }


    /*
     * add by lz
     * getFistChoice 判题器为车子选第一条路，直接从result.txt中读取
     * */
    public boolean getFistChoice(HashMap<Integer, Road> roadHashMap) {
        if (nowRoad != null) {
            return true;
        }
        boolean bLegal = false;

        try {
            nowRoad = roadHashMap.get(roadChoiceList.get(countChoice));
            Road[] roads = startCross.crossRoads;
            for (Road road : roads) {
                if (road == null) {
                    continue;
                }
                if (nowRoad.roadID == road.roadID) {
                    bLegal = true;
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("errrorrrr");
        }
        if (!bLegal) {
            System.out.println("车辆第一条路径选择错误");
            return false;
        }
        if (startCross.crossID == nowRoad.fromCross.crossID)//正想路
            isForwardRoad = true;
        else
            isForwardRoad = false;
        nextRoad = null;
        roadOffset = nowRoad.roadLength;
        ++countChoice;
        return true;
    }

    public void getNextChoice(Cross cross, HashMap<Integer, Road> roadHashMap) {
        try {
            if (nextRoad == null && getTypeOfForwordCar().forwardStatus == Car.FORWORD_NEED_THROUGH_CROSS) {
                if (!isPresetCar && cross.crossID == endCrossID) {
                    return;
                }
                if (isPresetCar && cross.crossID == endCrossID && countChoice == roadChoiceList.size()) {
                    return;
                }
                nextRoad = roadHashMap.get(roadChoiceList.get(countChoice));
                ++countChoice;
            }
        } catch (Exception e) {
            System.out.println("Error");
        }
    }


    boolean getDirectionResult(HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadHash) {
        Cross cross = isForwardRoad ? nowRoad.toCross : nowRoad.fromCross;
        int direction = getDirection();
        int nowRoadSeq = cross.getRoadInClockSeq(nowRoad.roadID);
        int conLeftRoadInClockSeq = (nowRoadSeq + 1) % 4;//左路
        int conRightRoadInClockSeq = (nowRoadSeq + 3) % 4;//右路
        int conOppositeRoadInClockSeq = (nowRoadSeq + 2) % 4;//对面的路
        if (direction == Car.GO_DOWN) {
            if (isPriorityCar)
                return true;
            else {
                return !bStrightConflict(cross.crossRoads[conLeftRoadInClockSeq], cross.crossRoads[conRightRoadInClockSeq], cross, crossRoadHash);
            }
        } else if (direction == Car.GO_LEFT) {
            if (isPriorityCar) {
                return !bLeftTopConflict(Car.GO_DOWN, cross.crossRoads[conRightRoadInClockSeq], cross, crossRoadHash);
            } else {
                return !bLeftConflict(Car.GO_DOWN, cross.crossRoads[conRightRoadInClockSeq], Car.GO_RIGHT, cross.crossRoads[conOppositeRoadInClockSeq], cross, crossRoadHash);
            }
        } else if (direction == Car.GO_RIGHT) {
            if (isPriorityCar) {
                return !bRightTopConflict(Car.GO_DOWN, cross.crossRoads[conLeftRoadInClockSeq], Car.GO_LEFT, cross.crossRoads[conOppositeRoadInClockSeq], cross, crossRoadHash);
            } else {
                return !bRightConflict(Car.GO_DOWN, cross.crossRoads[conLeftRoadInClockSeq], Car.GO_LEFT, cross.crossRoads[conOppositeRoadInClockSeq], cross, crossRoadHash);
            }
        }
        return true;
    }

    private boolean bRightConflict(int goDown, Road leftRoad, int goLeft, Road oppositeRoad, Cross cross, HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadHash) {
        boolean bConflict = false;
        if (leftRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, leftRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, leftRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.getDirection() == goDown) {
                bConflict = true;
            }
        }
        if (oppositeRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, oppositeRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, oppositeRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.getDirection() == goLeft) {
                bConflict = true;
            }
        }
        return bConflict;
    }

    private boolean bRightTopConflict(int goDown, Road leftRoad, int goLeft, Road oppositeRoad, Cross cross, HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadHash) {
        boolean bConflict = false;
        if (leftRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, leftRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, leftRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.isPriorityCar && conflictCar.getDirection() == goDown) {
                bConflict = true;
            }
        }
        if (oppositeRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, oppositeRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, oppositeRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.isPriorityCar && conflictCar.getDirection() == goLeft) {
                bConflict = true;
            }
        }
        return bConflict;
    }

    private boolean bLeftConflict(int goDown, Road rightRoad, int goRight, Road oppositeRoad, Cross cross, HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadHash) {
        boolean bConflict = false;
        if (rightRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, rightRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, rightRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.getDirection() == goDown) {
                bConflict = true;
            }
        }
        if (oppositeRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, oppositeRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, oppositeRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.isPriorityCar && conflictCar.getDirection() == goRight) {
                bConflict = true;
            }
        }
        return bConflict;
    }

    private boolean bLeftTopConflict(int goDown, Road rightRoad, Cross cross, HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadHash) {
        boolean bConflict = false;
        if (rightRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, rightRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, rightRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.isPriorityCar && conflictCar.getDirection() == goDown) {
                bConflict = true;
            }
        }
        return bConflict;
    }

    private boolean bStrightConflict(Road leftRoad, Road rightRoad, Cross cross, HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadHash) {
        boolean bConflict = false;
        if (leftRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, leftRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, leftRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.isPriorityCar && conflictCar.getDirection() == GO_LEFT) {
                bConflict = true;
            }
        }
        if (rightRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, rightRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, rightRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.isPriorityCar && conflictCar.getDirection() == GO_RIGHT) {
                bConflict = true;
            }
        }
        return bConflict;
    }

    @Override
    public int compareTo(Car other) {
        if (isPriorityCar && other.isPriorityCar) {
            if (roadOffset != other.roadOffset)
                return roadOffset - other.roadOffset;
            else
                return nowRoadChannel - other.nowRoadChannel;
        } else if (isPriorityCar ^ other.isPriorityCar) {
            return isPriorityCar ? -1 : 1;
        } else {
            if (roadOffset != other.roadOffset)
                return roadOffset - other.roadOffset;
            else
                return nowRoadChannel - other.nowRoadChannel;
        }
    }
}