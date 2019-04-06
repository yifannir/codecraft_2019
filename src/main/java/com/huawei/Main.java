package com.huawei;

import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.*;


public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length != 5) {
            logger.error("please input args: inputFilePath, resultFilePath");
            return;
        }
        String carPath = args[0];
        String roadPath = args[1];
        String crossPath = args[2];
        String presetAnswerPath = args[3];
        String answerPath = args[4];
        ArrayList<Car> cars = new ArrayList<>(); //
        ArrayList<Road> roads = new ArrayList<>();
        ArrayList<Cross> crosses = new ArrayList<>();
        HashMap<Integer, Road> roadHashMap = new HashMap<>();
        HashMap<Integer, Car> carHashMap = new HashMap<>();
        HashMap<Integer, Cross> crossHashMap = new HashMap<>();

        // car的信息读到carFileInfo里面，每一行7个数字，分别为(id,from,to,speed,planTime, isPriority, isPreset)
        ArrayList<int[]> carFileInfo = new ArrayList<>();
        // road的信息读到roadFileInfo里面，每一行7个数字，分别为#(id,length,speed,channel,from,to,isDuplex)
        ArrayList<int[]> roadFileInfo = new ArrayList<>();
        // cross的信息读到crossFileInfo里面，每一行5个数字，分别为#(id,roadId,roadId,roadId,roadId)
        ArrayList<int[]> crossFileInfo = new ArrayList<>();
        Func func = new Func();
        func.readFile(carPath, roadPath, crossPath, carFileInfo, roadFileInfo, crossFileInfo);
        func.processData(carFileInfo, roadFileInfo, crossFileInfo, cars, roads, crosses, roadHashMap, carHashMap, crossHashMap);
        func.processAnswerCar(carHashMap, answerPath);
        func.processAnswerCar(carHashMap, presetAnswerPath);
        for (Car car : cars) {
            roadHashMap.get(car.roadChoiceList.get(0)).initCarList.add(car);
        }
        for (Road road : roads) {
            Collections.sort(road.initCarList, new Comparator<Car>() {
                @Override
                public int compare(Car o1, Car o2) {
                    if (o1.isPriorityCar ^ o2.isPriorityCar) {
                        return o1.isPriorityCar ? -1 : 1;
                    } else {
                        if(o1.startTime ==o2.startTime)
                            return o1.carID - o2.carID;
                        else
                            return o1.startTime - o2.startTime;
                    }
                }
            });
        }
        DispatchCenter dispatchCenter = new DispatchCenter(func, cars, roads, crosses, roadHashMap, carHashMap, crossHashMap);
        while (!func.allCarFinished(dispatchCenter.cars)) {
            while (true) {
                if (dispatchCenter.DispatchOneTimePiece()) {
                    System.out.println("第"+dispatchCenter.MTime+"完成调度");
                    break;
                } else {
                    System.out.println("judge tool error");
                    break;
                }
            }
            int num = 0;
            for (int i = 0; i < dispatchCenter.cars.size(); i++) {
                if (dispatchCenter.cars.get(i).isFinish)
                    num++;
            }
            System.out.println("finish cars num:" + num);
        }

        System.out.println("优先车辆调度的时间片为："+(dispatchCenter.priorityArrivedLastTime-dispatchCenter.priorityFirstStartTime));
        System.out.println("所有车辆调度时间为："+dispatchCenter.MTime);
        System.out.println("所有车辆调度时间为："+dispatchCenter.allDispatcherTime);
        System.out.println("优先车辆调度时间为："+dispatchCenter.priorityDispatcherTime);
        double a = getAlpha(cars);
        double score = new BigDecimal(getAlpha(cars)*(dispatchCenter.priorityArrivedLastTime-dispatchCenter.priorityFirstStartTime) )
                .setScale(5,BigDecimal.ROUND_HALF_UP).doubleValue() + dispatchCenter.MTime;
        System.out.println("最终调度时间为"+score);
        double scoreCount =  new BigDecimal(getAlphb(cars)*dispatchCenter.priorityDispatcherTime ).setScale(5,BigDecimal.ROUND_HALF_UP).
                doubleValue() + dispatchCenter.allDispatcherTime;
        System.out.println("最终总调度时间为"+scoreCount);
    }

    public  static double getAlpha(ArrayList<Car> cars) {
        double alpha=0;
        float priCarNum = 0;
        float carNum = 0;
        float priCarMaxSpeed = 0;
        float priCarMinSpeed = 0x3f3f3f3f;
        float carMaxSpeed = 0;
        float carMinSpeed = 0x3f3f3f3f;
        float priCarMaxStartTime = 0;
        float priCarMinStartTime = 0x3f3f3f3f;
        float carMaxStartTime = 0;
        float carMinStartTime = 0x3f3f3f3f;
        float priCarStartCount = 0;
        float carStartCount = 0;
        float priCarEndCount = 0;
        float carEndCount = 0;
        HashSet<Integer> carStartCrossHashSet = new HashSet<>();
        HashSet<Integer> priCarStartCrossHashSet = new HashSet<>();
        HashSet<Integer> carEndCrossHashSet = new HashSet<>();
        HashSet<Integer> priCarEndCrossHashSet = new HashSet<>();
        for (Car car : cars) {
            carStartCrossHashSet.add(car.startCrossID);
            carEndCrossHashSet.add(car.endCrossID);
            carNum++;
            if (car.maxSpeedofCar > carMaxSpeed) carMaxSpeed = car.maxSpeedofCar;
            if (car.maxSpeedofCar < carMinSpeed) carMinSpeed = car.maxSpeedofCar;
            if (car.planTime > carMaxStartTime) carMaxStartTime = car.planTime;
            if (car.planTime < carMinStartTime) carMinStartTime = car.planTime;
            if (car.isPriorityCar){
                priCarStartCrossHashSet.add(car.startCrossID);
                priCarEndCrossHashSet.add(car.endCrossID);
                priCarNum++;
                if (car.maxSpeedofCar > priCarMaxSpeed) priCarMaxSpeed = car.maxSpeedofCar;
                if (car.maxSpeedofCar < priCarMinSpeed) priCarMinSpeed = car.maxSpeedofCar;
                if (car.planTime > priCarMaxStartTime) priCarMaxStartTime = car.planTime;
                if (car.planTime < priCarMinStartTime) priCarMinStartTime = car.planTime;

            }
        }
        priCarStartCount = priCarStartCrossHashSet.size();
        carStartCount = carStartCrossHashSet.size();
        priCarEndCount = priCarEndCrossHashSet.size();
        carEndCount = carEndCrossHashSet.size();
        alpha = new BigDecimal(0.05*carNum/priCarNum ).setScale(5,BigDecimal.ROUND_HALF_UP).doubleValue() +
                new BigDecimal(0.2375*(carMaxSpeed/carMinSpeed)/(priCarMaxSpeed/priCarMinSpeed) ).setScale(5,BigDecimal.ROUND_HALF_UP).doubleValue() +
                new BigDecimal(0.2375*(carMaxStartTime/carMinStartTime)/(priCarMaxStartTime/priCarMinStartTime) ).setScale(5,BigDecimal.ROUND_HALF_UP).doubleValue()+
                new BigDecimal(0.2375*(carEndCount)/(priCarEndCount) ).setScale(5,BigDecimal.ROUND_HALF_UP).doubleValue()+
                new BigDecimal(0.2375*(carStartCount)/(priCarStartCount) ).setScale(5,BigDecimal.ROUND_HALF_UP).doubleValue();
        return alpha;
    }

    public  static double getAlphb(ArrayList<Car> cars) {
        double alphb=0;
        float priCarNum = 0;
        float carNum = 0;
        float priCarMaxSpeed = 0;
        float priCarMinSpeed = 0x3f3f3f3f;
        float carMaxSpeed = 0;
        float carMinSpeed = 0x3f3f3f3f;
        float priCarMaxStartTime = 0;
        float priCarMinStartTime = 0x3f3f3f3f;
        float carMaxStartTime = 0;
        float carMinStartTime = 0x3f3f3f3f;
        float priCarStartCount = 0;
        float carStartCount = 0;
        float priCarEndCount = 0;
        float carEndCount = 0;
        HashSet<Integer> carStartCrossHashSet = new HashSet<>();
        HashSet<Integer> priCarStartCrossHashSet = new HashSet<>();
        HashSet<Integer> carEndCrossHashSet = new HashSet<>();
        HashSet<Integer> priCarEndCrossHashSet = new HashSet<>();
        for (Car car : cars) {
            carStartCrossHashSet.add(car.startCrossID);
            carEndCrossHashSet.add(car.endCrossID);
            carNum++;
            if (car.maxSpeedofCar > carMaxSpeed) carMaxSpeed = car.maxSpeedofCar;
            if (car.maxSpeedofCar < carMinSpeed) carMinSpeed = car.maxSpeedofCar;
            if (car.planTime > carMaxStartTime) carMaxStartTime = car.planTime;
            if (car.planTime < carMinStartTime) carMinStartTime = car.planTime;
            if (car.isPriorityCar){
                priCarStartCrossHashSet.add(car.startCrossID);
                priCarEndCrossHashSet.add(car.endCrossID);
                priCarNum++;
                if (car.maxSpeedofCar > priCarMaxSpeed) priCarMaxSpeed = car.maxSpeedofCar;
                if (car.maxSpeedofCar < priCarMinSpeed) priCarMinSpeed = car.maxSpeedofCar;
                if (car.planTime > priCarMaxStartTime) priCarMaxStartTime = car.planTime;
                if (car.planTime < priCarMinStartTime) priCarMinStartTime = car.planTime;

            }
        }
        priCarStartCount = priCarStartCrossHashSet.size();
        carStartCount = carStartCrossHashSet.size();
        priCarEndCount = priCarEndCrossHashSet.size();
        carEndCount = carEndCrossHashSet.size();
        alphb = new BigDecimal(0.8*carNum/priCarNum ).setScale(5,BigDecimal.ROUND_HALF_UP).doubleValue() +
                new BigDecimal(0.05*(carMaxSpeed/carMinSpeed)/(priCarMaxSpeed/priCarMinSpeed) ).setScale(5,BigDecimal.ROUND_HALF_UP).doubleValue() +
                new BigDecimal(0.05*(carMaxStartTime/carMinStartTime)/(priCarMaxStartTime/priCarMinStartTime) ).setScale(5,BigDecimal.ROUND_HALF_UP).doubleValue()+
                new BigDecimal(0.05*(carEndCount)/(priCarEndCount) ).setScale(5,BigDecimal.ROUND_HALF_UP).doubleValue()+
                new BigDecimal(0.05*(carStartCount)/(priCarStartCount) ).setScale(5,BigDecimal.ROUND_HALF_UP).doubleValue();
        return alphb;
    }
}



















